package com.sudies.devassist.modules.testcase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.BugSeverity;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.enums.TestCasePriority;
import com.sudies.devassist.common.enums.TestExecutionResult;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.bug.dto.CreateBugDTO;
import com.sudies.devassist.modules.bug.service.BugService;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.mapper.SprintMapper;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.testcase.dto.CreateTestCaseDTO;
import com.sudies.devassist.modules.testcase.dto.ExecuteTestCaseDTO;
import com.sudies.devassist.modules.testcase.dto.TestCaseQuery;
import com.sudies.devassist.modules.testcase.dto.UpdateTestCaseDTO;
import com.sudies.devassist.modules.testcase.entity.TestCase;
import com.sudies.devassist.modules.testcase.entity.TestExecution;
import com.sudies.devassist.modules.testcase.mapper.TestCaseMapper;
import com.sudies.devassist.modules.testcase.mapper.TestExecutionMapper;
import com.sudies.devassist.modules.testcase.vo.ExecuteResultVO;
import com.sudies.devassist.modules.testcase.vo.TestCaseVO;
import com.sudies.devassist.modules.testcase.vo.TestExecutionVO;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 测试用例管理：CRUD、执行（含 FAILED 联动建 Bug）、执行历史。
 * <p>权限语义（SRS §12.1）：测试人员是用例主人（CRUD + 执行），项目负责人任意，开发人员只读。
 * 查询走 {@link DataScope} 按项目隔离。执行失败时可选联动创建 Bug（关联需求/迭代/用例）。
 */
@Service
public class TestCaseService {

    @Resource
    private TestCaseMapper caseMapper;

    @Resource
    private TestExecutionMapper executionMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private SprintMapper sprintMapper;

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private BugService bugService;

    // ============================== 查询 ==============================

    @DataScope("project_id")
    public PageResult<TestCaseVO> page(TestCaseQuery q) {
        Page<TestCase> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<TestCase>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(TestCase::getProjectId, q.getProjectId());
        }
        if (q.getSprintId() != null) {
            w.eq(TestCase::getSprintId, q.getSprintId());
        }
        if (q.getRequirementId() != null) {
            w.eq(TestCase::getRequirementId, q.getRequirementId());
        }
        if (StringUtils.hasText(q.getPriority())) {
            w.eq(TestCase::getPriority, q.getPriority());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(TestCase::getTitle, q.getKeyword());
        }
        w.orderByDesc(TestCase::getCreateTime);
        caseMapper.selectPage(page, w);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public TestCaseVO detail(Long id) {
        TestCase tc = caseMapper.selectById(id);
        if (tc == null) {
            throw new BizException(ResultCode.NOT_FOUND, "测试用例不存在或无权访问");
        }
        return enrich(List.of(tc)).get(0);
    }

    private List<TestCaseVO> enrich(List<TestCase> cases) {
        if (cases.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> sprintIds = new HashSet<>();
        Set<Long> reqIds = new HashSet<>();
        for (TestCase tc : cases) {
            if (tc.getCreatorId() != null) {
                userIds.add(tc.getCreatorId());
            }
            if (tc.getSprintId() != null) {
                sprintIds.add(tc.getSprintId());
            }
            if (tc.getRequirementId() != null) {
                reqIds.add(tc.getRequirementId());
            }
        }
        Map<Long, User> userMap = loadMap(userIds, userMapper::selectByIds, User::getId);
        Map<Long, Sprint> sprintMap = loadMap(sprintIds, sprintMapper::selectByIds, Sprint::getId);
        Map<Long, Requirement> reqMap = loadMap(reqIds, requirementMapper::selectByIds, Requirement::getId);

        // 查最近一次执行记录（按 testCaseId 取最新一条）：按创建时间倒序，putIfAbsent 保留首个即最新
        Map<Long, TestExecution> lastExec = new java.util.LinkedHashMap<>();
        if (!cases.isEmpty()) {
            List<Long> caseIds = cases.stream().map(TestCase::getId).toList();
            List<TestExecution> execs = executionMapper.selectList(
                    Wrappers.<TestExecution>lambdaQuery()
                            .in(TestExecution::getTestCaseId, caseIds)
                            .orderByDesc(TestExecution::getCreateTime));
            for (TestExecution e : execs) {
                lastExec.putIfAbsent(e.getTestCaseId(), e);
            }
        }

        List<TestCaseVO> vos = new ArrayList<>(cases.size());
        for (TestCase tc : cases) {
            TestCaseVO vo = new TestCaseVO();
            BeanUtils.copyProperties(tc, vo);
            vo.setCreatorName(displayName(userMap.get(tc.getCreatorId())));
            if (tc.getSprintId() != null) {
                Sprint s = sprintMap.get(tc.getSprintId());
                if (s != null) {
                    vo.setSprintName(s.getName());
                }
            }
            if (tc.getRequirementId() != null) {
                Requirement r = reqMap.get(tc.getRequirementId());
                if (r != null) {
                    vo.setRequirementTitle(r.getTitle());
                }
            }
            TestExecution last = lastExec.get(tc.getId());
            if (last != null) {
                vo.setLastResult(last.getResult());
                vo.setLastActualResult(last.getActualResult());
                vo.setLastExecuteTime(last.getExecuteTime() != null ? last.getExecuteTime() : last.getCreateTime());
            }
            vos.add(vo);
        }
        return vos;
    }

    // ============================== 创建/编辑/删除 ==============================

    public Long create(CreateTestCaseDTO dto) {
        ensureMember(dto.getProjectId());
        if (dto.getSprintId() != null) {
            validateSprintInProject(dto.getSprintId(), dto.getProjectId());
        }
        if (dto.getRequirementId() != null) {
            validateRequirementInProject(dto.getRequirementId(), dto.getProjectId());
        }
        TestCase tc = new TestCase();
        tc.setProjectId(dto.getProjectId());
        tc.setSprintId(dto.getSprintId());
        tc.setRequirementId(dto.getRequirementId());
        tc.setTitle(dto.getTitle());
        tc.setPreconditions(dto.getPreconditions());
        tc.setSteps(dto.getSteps());
        tc.setExpectedResult(dto.getExpectedResult());
        tc.setPriority(parsePriority(dto.getPriority()));
        tc.setCreatorId(SecurityUtils.currentUserId());
        caseMapper.insert(tc);
        return tc.getId();
    }

    public void update(Long id, UpdateTestCaseDTO dto) {
        TestCase tc = mustGetEditable(id);
        TestCase upd = new TestCase();
        upd.setId(id);
        if (StringUtils.hasText(dto.getTitle())) {
            upd.setTitle(dto.getTitle());
        }
        if (dto.getPreconditions() != null) {
            upd.setPreconditions(dto.getPreconditions());
        }
        if (dto.getSteps() != null) {
            upd.setSteps(dto.getSteps());
        }
        if (dto.getExpectedResult() != null) {
            upd.setExpectedResult(dto.getExpectedResult());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            upd.setPriority(parsePriority(dto.getPriority()));
        }
        caseMapper.updateById(upd);
    }

    public void delete(Long id) {
        TestCase tc = mustGetEditable(id);
        caseMapper.deleteById(id);
    }

    // ============================== 执行（联动建 Bug） ==============================

    @Transactional
    public ExecuteResultVO execute(Long caseId, ExecuteTestCaseDTO dto) {
        TestCase tc = mustGetCaseAsMember(caseId);
        TestExecutionResult result = parseResult(dto.getResult());

        TestExecution exec = new TestExecution();
        exec.setTestCaseId(caseId);
        exec.setExecutorId(SecurityUtils.currentUserId());
        exec.setSprintId(dto.getSprintId() != null ? dto.getSprintId() : tc.getSprintId());
        exec.setActualResult(dto.getActualResult());
        exec.setResult(result.name());
        exec.setExecuteTime(LocalDateTime.now());
        executionMapper.insert(exec);

        ExecuteResultVO vo = new ExecuteResultVO();
        vo.setExecutionId(exec.getId());

        // FAILED 且 submitBug=true：联动创建 Bug（关联需求/迭代/用例）
        if (result == TestExecutionResult.FAILED && Boolean.TRUE.equals(dto.getSubmitBug())) {
            CreateBugDTO bugDto = new CreateBugDTO();
            bugDto.setProjectId(tc.getProjectId());
            bugDto.setSprintId(dto.getSprintId() != null ? dto.getSprintId() : tc.getSprintId());
            bugDto.setRequirementId(tc.getRequirementId());
            bugDto.setTestCaseId(caseId);
            bugDto.setTitle("【用例失败】" + tc.getTitle());
            StringBuilder desc = new StringBuilder("测试用例「").append(tc.getTitle()).append("」执行失败");
            if (StringUtils.hasText(dto.getActualResult())) {
                desc.append("，实际结果：").append(dto.getActualResult());
            }
            if (StringUtils.hasText(tc.getExpectedResult())) {
                desc.append("；预期：").append(tc.getExpectedResult());
            }
            bugDto.setDescription(desc.toString());
            bugDto.setStepsToReproduce(tc.getSteps());
            bugDto.setSeverity(BugSeverity.MAJOR.name());
            bugDto.setPriority(StringUtils.hasText(tc.getPriority()) ? tc.getPriority() : TestCasePriority.MEDIUM.name());
            vo.setBugId(bugService.create(bugDto));
        }
        return vo;
    }

    /**
     * 用例的执行历史（按执行时间倒序）。
     */
    public List<TestExecutionVO> listExecutions(Long caseId) {
        mustGetCaseAsMember(caseId);
        List<TestExecution> list = executionMapper.selectList(
                Wrappers.<TestExecution>lambdaQuery()
                        .eq(TestExecution::getTestCaseId, caseId)
                        .orderByDesc(TestExecution::getExecuteTime));
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = list.stream().map(TestExecution::getExecutorId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = loadMap(userIds, userMapper::selectByIds, User::getId);
        return list.stream().map(e -> {
            TestExecutionVO vo = new TestExecutionVO();
            BeanUtils.copyProperties(e, vo);
            vo.setExecutorName(displayName(userMap.get(e.getExecutorId())));
            return vo;
        }).toList();
    }

    // ============================== 私有：校验与工具 ==============================

    private TestCase mustGetCaseAsMember(Long id) {
        TestCase tc = caseMapper.selectById(id);
        if (tc == null) {
            throw new BizException(ResultCode.NOT_FOUND, "测试用例不存在");
        }
        ensureMember(tc.getProjectId());
        return tc;
    }

    /**
     * 编辑/删除：仅创建人或项目负责人。
     */
    private TestCase mustGetEditable(Long id) {
        TestCase tc = mustGetCaseAsMember(id);
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不参与项目业务");
        }
        if (SecurityUtils.hasRole(RoleCode.OWNER.name())) {
            return tc;
        }
        Long uid = SecurityUtils.currentUserId();
        if (!uid.equals(tc.getCreatorId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅创建人或项目负责人可操作");
        }
        return tc;
    }

    private void ensureMember(Long projectId) {
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            return;
        }
        Long uid = SecurityUtils.currentUserId();
        Long cnt = projectMemberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, uid));
        if (cnt == null || cnt == 0L) {
            throw new BizException(ResultCode.FORBIDDEN, "不属于该项目，无权操作");
        }
    }

    private void validateSprintInProject(Long sprintId, Long projectId) {
        Sprint s = sprintMapper.selectById(sprintId);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "迭代不存在");
        }
        if (!s.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "迭代与用例不属于同一项目");
        }
    }

    private void validateRequirementInProject(Long requirementId, Long projectId) {
        Requirement r = requirementMapper.selectById(requirementId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!r.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "需求与用例不属于同一项目");
        }
    }

    private String parsePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return TestCasePriority.MEDIUM.name();
        }
        try {
            return TestCasePriority.valueOf(priority.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知优先级: " + priority);
        }
    }

    private TestExecutionResult parseResult(String result) {
        try {
            return TestExecutionResult.valueOf(result.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知执行结果: " + result);
        }
    }

    private String displayName(User u) {
        if (u == null) {
            return null;
        }
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }

    private <T> Map<Long, T> loadMap(Set<Long> ids,
                                     java.util.function.Function<Set<Long>, List<T>> loader,
                                     Function<T, Long> keyExtractor) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return loader.apply(ids).stream().collect(Collectors.toMap(keyExtractor, Function.identity()));
    }
}
