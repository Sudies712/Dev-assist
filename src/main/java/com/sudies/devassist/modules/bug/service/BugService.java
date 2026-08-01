package com.sudies.devassist.modules.bug.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.BugSeverity;
import com.sudies.devassist.common.enums.BugStatus;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.enums.TaskPriority;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.bug.dto.*;
import com.sudies.devassist.modules.bug.entity.Bug;
import com.sudies.devassist.modules.bug.entity.BugAttachment;
import com.sudies.devassist.modules.bug.mapper.BugAttachmentMapper;
import com.sudies.devassist.modules.bug.mapper.BugMapper;
import com.sudies.devassist.modules.bug.vo.BugAttachmentVO;
import com.sudies.devassist.modules.bug.vo.BugVO;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.mapper.SprintMapper;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.task.entity.Task;
import com.sudies.devassist.modules.task.mapper.TaskMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 缺陷管理：CRUD、状态机（{@link BugStatus#next()}）、分配、附件。
 * <p>权限语义（SRS §12.1）：测试人员提交 Bug 并驱动状态机（确认/验证/拒绝/重开）；
 * 开发人员仅修复相关流转（PENDING_FIX→FIXING→PENDING_VERIFY）；项目负责人任意。
 * 查询走 {@link DataScope} 按项目隔离。
 */
@Service
public class BugService {

    @Resource
    private BugMapper bugMapper;

    @Resource
    private BugAttachmentMapper attachmentMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private SprintMapper sprintMapper;

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private UserMapper userMapper;

    @Value("${dev-assist.upload.dir:./uploads}")
    private String uploadDir;

    // ============================== 查询 ==============================

    @DataScope("project_id")
    public PageResult<BugVO> page(BugQuery q) {
        Page<Bug> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Bug>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(Bug::getProjectId, q.getProjectId());
        }
        if (q.getSprintId() != null) {
            w.eq(Bug::getSprintId, q.getSprintId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(Bug::getStatus, q.getStatus());
        }
        if (q.getAssigneeId() != null) {
            w.eq(Bug::getAssigneeId, q.getAssigneeId());
        }
        if (q.getReporterId() != null) {
            w.eq(Bug::getReporterId, q.getReporterId());
        }
        if (StringUtils.hasText(q.getSeverity())) {
            w.eq(Bug::getSeverity, q.getSeverity());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(Bug::getTitle, q.getKeyword());
        }
        w.orderByDesc(Bug::getCreateTime);
        bugMapper.selectPage(page, w);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public BugVO detail(Long id) {
        Bug b = bugMapper.selectById(id);
        if (b == null) {
            throw new BizException(ResultCode.NOT_FOUND, "缺陷不存在或无权访问");
        }
        return enrich(List.of(b)).get(0);
    }

    private List<BugVO> enrich(List<Bug> bugs) {
        if (bugs.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> sprintIds = new HashSet<>();
        Set<Long> reqIds = new HashSet<>();
        Set<Long> taskIds = new HashSet<>();
        for (Bug b : bugs) {
            if (b.getAssigneeId() != null) {
                userIds.add(b.getAssigneeId());
            }
            if (b.getReporterId() != null) {
                userIds.add(b.getReporterId());
            }
            if (b.getSprintId() != null) {
                sprintIds.add(b.getSprintId());
            }
            if (b.getRequirementId() != null) {
                reqIds.add(b.getRequirementId());
            }
            if (b.getTaskId() != null) {
                taskIds.add(b.getTaskId());
            }
        }
        Map<Long, User> userMap = loadMap(userIds, userMapper::selectByIds, User::getId);
        Map<Long, Sprint> sprintMap = loadMap(sprintIds, sprintMapper::selectByIds, Sprint::getId);
        Map<Long, Requirement> reqMap = loadMap(reqIds, requirementMapper::selectByIds, Requirement::getId);
        Map<Long, Task> taskMap = loadMap(taskIds, taskMapper::selectByIds, Task::getId);

        List<BugVO> vos = new ArrayList<>(bugs.size());
        for (Bug b : bugs) {
            BugVO vo = new BugVO();
            BeanUtils.copyProperties(b, vo);
            vo.setAssigneeName(displayName(userMap.get(b.getAssigneeId())));
            vo.setReporterName(displayName(userMap.get(b.getReporterId())));
            if (b.getSprintId() != null) {
                Sprint s = sprintMap.get(b.getSprintId());
                if (s != null) {
                    vo.setSprintName(s.getName());
                }
            }
            if (b.getRequirementId() != null) {
                Requirement r = reqMap.get(b.getRequirementId());
                if (r != null) {
                    vo.setRequirementTitle(r.getTitle());
                }
            }
            if (b.getTaskId() != null) {
                Task t = taskMap.get(b.getTaskId());
                if (t != null) {
                    vo.setTaskTitle(t.getTitle());
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    // ============================== 创建/编辑/删除 ==============================

    public Long create(CreateBugDTO dto) {
        ensureMember(dto.getProjectId());
        if (dto.getSprintId() != null) {
            validateSprintInProject(dto.getSprintId(), dto.getProjectId());
        }
        if (dto.getRequirementId() != null) {
            validateRequirementInProject(dto.getRequirementId(), dto.getProjectId());
        }
        if (dto.getTaskId() != null) {
            validateTaskInProject(dto.getTaskId(), dto.getProjectId());
        }
        if (dto.getAssigneeId() != null) {
            ensureMemberIs(dto.getAssigneeId(), dto.getProjectId(), "修复人须为项目成员");
        }
        Bug b = new Bug();
        b.setProjectId(dto.getProjectId());
        b.setSprintId(dto.getSprintId());
        b.setRequirementId(dto.getRequirementId());
        b.setTaskId(dto.getTaskId());
        b.setTestCaseId(dto.getTestCaseId());
        b.setTitle(dto.getTitle());
        b.setDescription(dto.getDescription());
        b.setStepsToReproduce(dto.getStepsToReproduce());
        b.setSeverity(parseSeverity(dto.getSeverity()));
        b.setPriority(parsePriority(dto.getPriority()));
        b.setStatus(BugStatus.PENDING_CONFIRM.name());
        b.setAssigneeId(dto.getAssigneeId());
        b.setReporterId(SecurityUtils.currentUserId());
        bugMapper.insert(b);
        return b.getId();
    }

    public void update(Long id, UpdateBugDTO dto) {
        Bug b = mustGetEditableBug(id);
        Bug upd = new Bug();
        upd.setId(id);
        upd.setVersion(b.getVersion());
        if (StringUtils.hasText(dto.getTitle())) {
            upd.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            upd.setDescription(dto.getDescription());
        }
        if (dto.getStepsToReproduce() != null) {
            upd.setStepsToReproduce(dto.getStepsToReproduce());
        }
        if (StringUtils.hasText(dto.getSeverity())) {
            upd.setSeverity(parseSeverity(dto.getSeverity()));
        }
        if (StringUtils.hasText(dto.getPriority())) {
            upd.setPriority(parsePriority(dto.getPriority()));
        }
        bugMapper.updateById(upd);
    }

    /**
     * 仅待确认/已拒绝的缺陷可删除，且仅提交人或项目负责人。
     */
    public void delete(Long id) {
        Bug b = mustGetEditableBug(id);
        if (!BugStatus.PENDING_CONFIRM.name().equals(b.getStatus())
                && !BugStatus.REJECTED.name().equals(b.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅待确认或已拒绝的缺陷可删除");
        }
        bugMapper.deleteById(id);
    }

    // ============================== 状态机 ==============================

    @Transactional
    public void changeStatus(Long id, BugStatusChangeDTO dto) {
        Bug b = mustGetBugAsMember(id);
        BugStatus current;
        BugStatus target;
        try {
            current = BugStatus.valueOf(b.getStatus());
            target = BugStatus.valueOf(dto.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知缺陷状态: " + dto.getTargetStatus());
        }

        // 角色边界：开发人员（仅 bug:update，无 bug:change_status）只能执行修复相关流转
        boolean canChangeStatus = SecurityUtils.hasPermission("bug:change_status");
        if (!canChangeStatus) {
            boolean devFixFlow = (current == BugStatus.PENDING_FIX && target == BugStatus.FIXING)
                    || (current == BugStatus.FIXING && target == BugStatus.PENDING_VERIFY);
            if (!devFixFlow) {
                throw new BizException(ResultCode.FORBIDDEN, "您仅可执行修复相关流转（开始修复 / 修复完成）");
            }
        }

        if (!current.next().contains(target)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_CHANGE);
        }
        // 条件必填字段
        if (target == BugStatus.REJECTED && !StringUtils.hasText(dto.getRejectReason())) {
            throw new BizException(ResultCode.BAD_REQUEST, "拒绝须填写原因");
        }
        if (current == BugStatus.FIXING && target == BugStatus.PENDING_VERIFY
                && !StringUtils.hasText(dto.getFixDescription())) {
            throw new BizException(ResultCode.BAD_REQUEST, "修复完成须填写修复说明");
        }
        if (current == BugStatus.PENDING_VERIFY && target == BugStatus.PENDING_FIX
                && !StringUtils.hasText(dto.getFailReason())) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证失败须填写原因");
        }

        Bug upd = new Bug();
        upd.setId(id);
        upd.setVersion(b.getVersion());
        upd.setStatus(target.name());
        // 修复完成写入修复说明（拼接验证失败/拒绝原因便于追溯）
        if (target == BugStatus.PENDING_VERIFY) {
            upd.setFixDescription(dto.getFixDescription());
        } else if (current == BugStatus.FIXING) {
            // FIXING 流出到非 PENDING_VERIFY（理论上不会发生，状态机限定），清空
            upd.setFixDescription(null);
        }
        bugMapper.updateById(upd);
    }

    // ============================== 分配 ==============================

    public void assign(Long id, AssignBugDTO dto) {
        Bug b = mustGetBugAsMember(id);
        if (!SecurityUtils.hasRole(RoleCode.OWNER.name())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅项目负责人可分配缺陷");
        }
        if (dto.getAssigneeId() != null) {
            ensureMemberIs(dto.getAssigneeId(), b.getProjectId(), "修复人须为项目成员");
        }
        Bug upd = new Bug();
        upd.setId(id);
        upd.setVersion(b.getVersion());
        upd.setAssigneeId(dto.getAssigneeId());
        bugMapper.updateById(upd);
    }

    // ============================== 附件 ==============================

    public Long uploadAttachment(Long bugId, MultipartFile file) {
        Bug b = mustGetEditableBug(bugId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "附件文件不能为空");
        }
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf('.'));
        }
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, "bug");
            Files.createDirectories(dir);
            Path dest = dir.resolve(stored);
            Files.copy(file.getInputStream(), dest);
        } catch (IOException e) {
            throw new BizException(ResultCode.ERROR, "附件保存失败: " + e.getMessage());
        }
        BugAttachment a = new BugAttachment();
        a.setBugId(bugId);
        a.setFileName(origName);
        a.setFilePath("/bug/" + stored);
        a.setFileSize(file.getSize());
        a.setUploadTime(LocalDateTime.now());
        attachmentMapper.insert(a);
        return a.getId();
    }

    public List<BugAttachmentVO> listAttachments(Long bugId) {
        mustGetBugAsMember(bugId);
        List<BugAttachment> list = attachmentMapper.selectList(
                Wrappers.<BugAttachment>lambdaQuery()
                        .eq(BugAttachment::getBugId, bugId)
                        .orderByDesc(BugAttachment::getUploadTime));
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = list.stream().map(BugAttachment::getCreateBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = loadMap(userIds, userMapper::selectByIds, User::getId);
        return list.stream().map(a -> {
            BugAttachmentVO vo = new BugAttachmentVO();
            BeanUtils.copyProperties(a, vo);
            vo.setUploaderName(displayName(userMap.get(a.getCreateBy())));
            return vo;
        }).toList();
    }

    /**
     * 附件元数据（用于下载 header + 可见性校验）。
     */
    public BugAttachmentVO getAttachmentMeta(Long bugId, Long attachId) {
        mustGetBugAsMember(bugId);
        BugAttachment a = attachmentMapper.selectById(attachId);
        if (a == null || !bugId.equals(a.getBugId())) {
            throw new BizException(ResultCode.NOT_FOUND, "附件不存在");
        }
        BugAttachmentVO vo = new BugAttachmentVO();
        BeanUtils.copyProperties(a, vo);
        vo.setUploaderName(displayName(userMapper.selectById(a.getCreateBy())));
        return vo;
    }

    /**
     * 加载附件文件流（下载用）。
     */
    public org.springframework.core.io.Resource loadAttachmentResource(Long attachId) {
        BugAttachment a = attachmentMapper.selectById(attachId);
        if (a == null) {
            throw new BizException(ResultCode.NOT_FOUND, "附件不存在");
        }
        Path file = Paths.get(uploadDir, a.getFilePath().replaceFirst("^/+", ""));
        if (!Files.exists(file)) {
            throw new BizException(ResultCode.NOT_FOUND, "附件文件已丢失");
        }
        return new FileSystemResource(file);
    }

    // ============================== 私有：校验与工具 ==============================

    private Bug mustGetBugAsMember(Long id) {
        Bug b = bugMapper.selectById(id);
        if (b == null) {
            throw new BizException(ResultCode.NOT_FOUND, "缺陷不存在");
        }
        ensureMember(b.getProjectId());
        return b;
    }

    /**
     * 编辑/删除/上传附件：仅提交人或项目负责人。
     */
    private Bug mustGetEditableBug(Long id) {
        Bug b = mustGetBugAsMember(id);
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不参与项目业务");
        }
        if (SecurityUtils.hasRole(RoleCode.OWNER.name())) {
            return b;
        }
        Long uid = SecurityUtils.currentUserId();
        if (!uid.equals(b.getReporterId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅提交人或项目负责人可操作");
        }
        return b;
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

    private void ensureMemberIs(Long userId, Long projectId, String msg) {
        Long cnt = projectMemberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (cnt == null || cnt == 0L) {
            throw new BizException(ResultCode.BAD_REQUEST, msg);
        }
    }

    private void validateSprintInProject(Long sprintId, Long projectId) {
        Sprint s = sprintMapper.selectById(sprintId);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "迭代不存在");
        }
        if (!s.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "迭代与缺陷不属于同一项目");
        }
    }

    private void validateRequirementInProject(Long requirementId, Long projectId) {
        Requirement r = requirementMapper.selectById(requirementId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!r.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "需求与缺陷不属于同一项目");
        }
    }

    private void validateTaskInProject(Long taskId, Long projectId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在");
        }
        if (!t.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "任务与缺陷不属于同一项目");
        }
    }

    private String parseSeverity(String severity) {
        if (!StringUtils.hasText(severity)) {
            return BugSeverity.NORMAL.name();
        }
        try {
            return BugSeverity.valueOf(severity.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知严重程度: " + severity);
        }
    }

    private String parsePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return TaskPriority.MEDIUM.name();
        }
        try {
            return TaskPriority.valueOf(priority.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知优先级: " + priority);
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
