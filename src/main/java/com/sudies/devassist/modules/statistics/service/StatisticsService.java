package com.sudies.devassist.modules.statistics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sudies.devassist.common.enums.*;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.ai.entity.AiRecord;
import com.sudies.devassist.modules.ai.mapper.AiRecordMapper;
import com.sudies.devassist.modules.bug.entity.Bug;
import com.sudies.devassist.modules.bug.mapper.BugMapper;
import com.sudies.devassist.modules.project.entity.Project;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMapper;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.statistics.vo.MemberLoadVO;
import com.sudies.devassist.modules.statistics.vo.ProjectStatisticsVO;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.task.entity.Task;
import com.sudies.devassist.modules.task.mapper.TaskMapper;
import com.sudies.devassist.modules.testcase.entity.TestCase;
import com.sudies.devassist.modules.testcase.entity.TestExecution;
import com.sudies.devassist.modules.testcase.mapper.TestCaseMapper;
import com.sudies.devassist.modules.testcase.mapper.TestExecutionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计聚合（实时查询，无独立表）。项目概况(全局可见) + 当前项目各模块分布 + 成员负载。
 * 对应 SRS §5.9。分布用 selectList(字段) + Java groupBy（演示数据量小，可接受）。
 */
@Service
public class StatisticsService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectMemberMapper projectMemberMapper;
    @Resource
    private RequirementMapper requirementMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private BugMapper bugMapper;
    @Resource
    private TestCaseMapper testCaseMapper;
    @Resource
    private TestExecutionMapper testExecutionMapper;
    @Resource
    private AiRecordMapper aiRecordMapper;
    @Resource
    private UserMapper userMapper;

    public ProjectStatisticsVO statistics(Long projectId) {
        ensureMember(projectId);
        ProjectStatisticsVO vo = new ProjectStatisticsVO();
        vo.setProject(buildProjectStat());
        vo.setRequirement(buildRequirementStat(projectId));
        vo.setTask(buildTaskStat(projectId));
        vo.setBug(buildBugStat(projectId));
        vo.setTest(buildTestStat(projectId));
        vo.setAi(buildAiStat(projectId));
        return vo;
    }

    public List<MemberLoadVO> memberLoad(Long projectId) {
        ensureMember(projectId);
        List<ProjectMember> members = projectMemberMapper.selectList(
                Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectByIds(
                        members.stream().map(ProjectMember::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<MemberLoadVO> loads = new ArrayList<>(members.size());
        for (ProjectMember m : members) {
            Long uid = m.getUserId();
            MemberLoadVO load = new MemberLoadVO();
            load.setUserId(uid);
            User u = userMap.get(uid);
            load.setRealName(u != null ? u.getRealName() : null);
            load.setTaskCount(taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                    .eq(Task::getProjectId, projectId).eq(Task::getAssigneeId, uid)));
            load.setBugCount(bugMapper.selectCount(Wrappers.<Bug>lambdaQuery()
                    .eq(Bug::getProjectId, projectId).eq(Bug::getAssigneeId, uid)));
            load.setDoneCount(taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                    .eq(Task::getProjectId, projectId).eq(Task::getAssigneeId, uid)
                    .in(Task::getStatus, TaskStatus.DONE.name(), TaskStatus.CLOSED.name())));
            loads.add(load);
        }
        return loads;
    }

    // ============================== 各模块统计 ==============================

    private ProjectStatisticsVO.ProjectStat buildProjectStat() {
        ProjectStatisticsVO.ProjectStat s = new ProjectStatisticsVO.ProjectStat();
        List<Project> visible = visibleProjects();
        s.setTotal(visible.size());
        s.setInProgress((int) visible.stream()
                .filter(p -> ProjectStatus.IN_PROGRESS.name().equals(p.getStatus())).count());
        s.setStatusDist(distOf(visible.stream().map(Project::getStatus)));
        return s;
    }

    private ProjectStatisticsVO.RequirementStat buildRequirementStat(Long pid) {
        List<Requirement> rs = requirementMapper.selectList(
                Wrappers.<Requirement>lambdaQuery().eq(Requirement::getProjectId, pid));
        ProjectStatisticsVO.RequirementStat s = new ProjectStatisticsVO.RequirementStat();
        s.setTotal((long) rs.size());
        long done = rs.stream().filter(r ->
                RequirementStatus.DONE.name().equals(r.getStatus())
                        || RequirementStatus.CLOSED.name().equals(r.getStatus())).count();
        s.setDoneRate(rs.isEmpty() ? 0.0 : (double) done / rs.size());
        s.setPriorityDist(distOf(rs.stream().map(Requirement::getPriority)));
        return s;
    }

    private ProjectStatisticsVO.TaskStat buildTaskStat(Long pid) {
        List<Task> ts = taskMapper.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, pid));
        ProjectStatisticsVO.TaskStat s = new ProjectStatisticsVO.TaskStat();
        s.setTotal((long) ts.size());
        long done = ts.stream().filter(t ->
                TaskStatus.DONE.name().equals(t.getStatus()) || TaskStatus.CLOSED.name().equals(t.getStatus())).count();
        s.setDoneRate(ts.isEmpty() ? 0.0 : (double) done / ts.size());
        s.setStatusDist(distOf(ts.stream().map(Task::getStatus)));
        // 按负责人聚合（assignee 为 null 的不计）
        Map<Long, Long> byAssignee = ts.stream()
                .filter(t -> t.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
        if (!byAssignee.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectByIds(byAssignee.keySet())
                    .stream().collect(Collectors.toMap(User::getId, Function.identity()));
            s.setByAssignee(byAssignee.entrySet().stream().map(e -> {
                ProjectStatisticsVO.AssigneeItem a = new ProjectStatisticsVO.AssigneeItem();
                a.setUserId(e.getKey());
                a.setCount(e.getValue());
                User u = userMap.get(e.getKey());
                a.setRealName(u != null ? u.getRealName() : null);
                return a;
            }).toList());
        } else {
            s.setByAssignee(List.of());
        }
        return s;
    }

    private ProjectStatisticsVO.BugStat buildBugStat(Long pid) {
        List<Bug> bs = bugMapper.selectList(Wrappers.<Bug>lambdaQuery().eq(Bug::getProjectId, pid));
        ProjectStatisticsVO.BugStat s = new ProjectStatisticsVO.BugStat();
        s.setTotal((long) bs.size());
        s.setSeverityDist(distOf(bs.stream().map(Bug::getSeverity)));
        s.setStatusDist(distOf(bs.stream().map(Bug::getStatus)));
        return s;
    }

    private ProjectStatisticsVO.TestStat buildTestStat(Long pid) {
        ProjectStatisticsVO.TestStat s = new ProjectStatisticsVO.TestStat();
        List<TestCase> cases = testCaseMapper.selectList(
                Wrappers.<TestCase>lambdaQuery().eq(TestCase::getProjectId, pid));
        s.setTotal((long) cases.size());
        if (cases.isEmpty()) {
            s.setPassRate(0.0);
            s.setResultDist(List.of());
            return s;
        }
        List<Long> caseIds = cases.stream().map(TestCase::getId).toList();
        List<TestExecution> execs = testExecutionMapper.selectList(
                Wrappers.<TestExecution>lambdaQuery().in(TestExecution::getTestCaseId, caseIds));
        long passed = execs.stream().filter(e -> "PASSED".equals(e.getResult())).count();
        s.setPassRate(execs.isEmpty() ? 0.0 : (double) passed / execs.size());
        s.setResultDist(distOf(execs.stream().map(TestExecution::getResult)));
        return s;
    }

    private ProjectStatisticsVO.AiStat buildAiStat(Long pid) {
        List<AiRecord> records = aiRecordMapper.selectList(
                Wrappers.<AiRecord>lambdaQuery().eq(AiRecord::getProjectId, pid));
        ProjectStatisticsVO.AiStat s = new ProjectStatisticsVO.AiStat();
        s.setTotalCalls((long) records.size());
        long adopted = records.stream().filter(r ->
                AiRecordStatus.FULL.name().equals(r.getStatus())
                        || AiRecordStatus.PARTIAL.name().equals(r.getStatus())).count();
        s.setAdoptRate(records.isEmpty() ? 0.0 : (double) adopted / records.size());
        s.setByType(distOf(records.stream().map(AiRecord::getAiType)));
        return s;
    }

    // ============================== 工具 ==============================

    /**
     * 当前用户可见项目（创建人或成员），与 ProjectService.page 一致。
     */
    private List<Project> visibleProjects() {
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            return projectMapper.selectList(null);
        }
        Long uid = SecurityUtils.currentUserId();
        List<Long> memberPids = projectMemberMapper.selectList(
                        Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getUserId, uid))
                .stream().map(ProjectMember::getProjectId).distinct().toList();
        var w = Wrappers.<Project>lambdaQuery();
        w.and(ww -> {
            ww.eq(Project::getCreatorId, uid);
            if (!memberPids.isEmpty()) {
                ww.or().in(Project::getId, memberPids);
            }
        });
        return projectMapper.selectList(w);
    }

    private List<ProjectStatisticsVO.DistItem> distOf(java.util.stream.Stream<String> stream) {
        Map<String, Long> map = stream.filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return map.entrySet().stream()
                .map(e -> new ProjectStatisticsVO.DistItem(e.getKey(), e.getValue()))
                .toList();
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
}
