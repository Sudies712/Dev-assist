package com.sudies.devassist.modules.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.enums.TaskPriority;
import com.sudies.devassist.common.enums.TaskStatus;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.mapper.SprintMapper;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.task.dto.*;
import com.sudies.devassist.modules.task.entity.Task;
import com.sudies.devassist.modules.task.entity.TaskComment;
import com.sudies.devassist.modules.task.entity.WorkLog;
import com.sudies.devassist.modules.task.mapper.TaskCommentMapper;
import com.sudies.devassist.modules.task.mapper.TaskMapper;
import com.sudies.devassist.modules.task.mapper.WorkLogMapper;
import com.sudies.devassist.modules.task.vo.BurndownPointVO;
import com.sudies.devassist.modules.task.vo.TaskCommentVO;
import com.sudies.devassist.modules.task.vo.TaskVO;
import com.sudies.devassist.modules.task.vo.WorkLogVO;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务管理：CRUD、状态机（{@link TaskStatus#next()}）、分配、评论、工作记录。
 * <p>写权限边界（SRS §12.1）：项目负责人任意；开发人员仅自己负责的任务；测试人员仅 READY_FOR_TEST 流转。
 * 查询走 {@link DataScope} 按项目隔离。退回原因写入 task_comment 留审计痕迹。
 */
@Service
public class TaskService {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskCommentMapper commentMapper;

    @Resource
    private WorkLogMapper workLogMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private SprintMapper sprintMapper;

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private UserMapper userMapper;

    // ============================== 查询 ==============================

    @DataScope("project_id")
    public PageResult<TaskVO> page(TaskQuery q) {
        Page<Task> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Task>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(Task::getProjectId, q.getProjectId());
        }
        if (q.getSprintId() != null) {
            w.eq(Task::getSprintId, q.getSprintId());
        }
        if (q.getRequirementId() != null) {
            w.eq(Task::getRequirementId, q.getRequirementId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(Task::getStatus, q.getStatus());
        }
        if (q.getAssigneeId() != null) {
            w.eq(Task::getAssigneeId, q.getAssigneeId());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(Task::getTitle, q.getKeyword());
        }
        w.orderByDesc(Task::getCreateTime);
        taskMapper.selectPage(page, w);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public TaskVO detail(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在或无权访问");
        }
        return enrich(List.of(t)).get(0);
    }

    /**
     * 批量补充关联展示字段（负责人/创建人/需求标题/迭代名），避免 N+1。
     */
    private List<TaskVO> enrich(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> sprintIds = new HashSet<>();
        Set<Long> reqIds = new HashSet<>();
        for (Task t : tasks) {
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
            if (t.getCreateBy() != null) {
                userIds.add(t.getCreateBy());
            }
            if (t.getSprintId() != null) {
                sprintIds.add(t.getSprintId());
            }
            if (t.getRequirementId() != null) {
                reqIds.add(t.getRequirementId());
            }
        }
        Map<Long, User> userMap = loadMap(userIds, ids -> userMapper.selectByIds(ids), User::getId);
        Map<Long, Sprint> sprintMap = loadMap(sprintIds, ids -> sprintMapper.selectByIds(ids), Sprint::getId);
        Map<Long, Requirement> reqMap = loadMap(reqIds, ids -> requirementMapper.selectByIds(ids), Requirement::getId);

        List<TaskVO> vos = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            TaskVO vo = new TaskVO();
            BeanUtils.copyProperties(t, vo);
            vo.setAssigneeName(displayName(userMap.get(t.getAssigneeId())));
            vo.setCreatorName(displayName(userMap.get(t.getCreateBy())));
            if (t.getRequirementId() != null) {
                Requirement r = reqMap.get(t.getRequirementId());
                if (r != null) {
                    vo.setRequirementTitle(r.getTitle());
                }
            }
            if (t.getSprintId() != null) {
                Sprint s = sprintMap.get(t.getSprintId());
                if (s != null) {
                    vo.setSprintName(s.getName());
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    // ============================== 创建/编辑/删除 ==============================

    public Long create(CreateTaskDTO dto) {
        ensureMember(dto.getProjectId());
        validateSprintInProject(dto.getSprintId(), dto.getProjectId());
        if (dto.getRequirementId() != null) {
            validateRequirementInProject(dto.getRequirementId(), dto.getProjectId());
        }
        if (dto.getAssigneeId() != null) {
            ensureMemberIs(dto.getAssigneeId(), dto.getProjectId(), "负责人须为项目成员");
        }
        Task t = new Task();
        t.setProjectId(dto.getProjectId());
        t.setSprintId(dto.getSprintId());
        t.setRequirementId(dto.getRequirementId());
        t.setTitle(dto.getTitle());
        t.setDescription(dto.getDescription());
        t.setPriority(parsePriority(dto.getPriority()));
        t.setStatus(TaskStatus.TODO.name());
        t.setAssigneeId(dto.getAssigneeId());
        t.setDeadline(dto.getDeadline());
        t.setEstimatedHours(dto.getEstimatedHours());
        taskMapper.insert(t);
        return t.getId();
    }

    public void update(Long id, UpdateTaskDTO dto) {
        Task t = mustGetWritableTask(id);
        Task upd = new Task();
        upd.setId(id);
        upd.setVersion(t.getVersion());
        if (StringUtils.hasText(dto.getTitle())) {
            upd.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            upd.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            upd.setPriority(parsePriority(dto.getPriority()));
        }
        upd.setDeadline(dto.getDeadline());
        upd.setEstimatedHours(dto.getEstimatedHours());
        upd.setActualHours(dto.getActualHours());
        taskMapper.updateById(upd);
    }

    /**
     * 仅待处理/已关闭的任务可删除。
     */
    public void delete(Long id) {
        Task t = mustGetWritableTask(id);
        if (!TaskStatus.TODO.name().equals(t.getStatus())
                && !TaskStatus.CLOSED.name().equals(t.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅待处理或已关闭的任务可删除");
        }
        taskMapper.deleteById(id);
    }

    // ============================== 状态机 ==============================

    @Transactional
    public void changeStatus(Long id, TaskStatusChangeDTO dto) {
        Task t = mustGetTaskAsMember(id);
        TaskStatus current;
        TaskStatus target;
        try {
            current = TaskStatus.valueOf(t.getStatus());
            target = TaskStatus.valueOf(dto.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知任务状态: " + dto.getTargetStatus());
        }

        // 写边界：测试人员（仅 task:advance_test）只能处理待测试任务；其余收敛到 owner/assignee
        if (!SecurityUtils.hasPermission("task:change_status")) {
            if (current != TaskStatus.READY_FOR_TEST) {
                throw new BizException(ResultCode.FORBIDDEN, "测试人员仅可处理「待测试」任务");
            }
            if (target != TaskStatus.DONE && target != TaskStatus.IN_PROGRESS) {
                throw new BizException(ResultCode.BAD_REQUEST, "待测试任务仅可流转至已完成或退回进行中");
            }
        } else {
            ensureWritable(t);
        }

        if (!current.next().contains(target)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_CHANGE);
        }
        // IN_PROGRESS → TODO 退回须填原因
        if (current == TaskStatus.IN_PROGRESS && target == TaskStatus.TODO
                && !StringUtils.hasText(dto.getReason())) {
            throw new BizException(ResultCode.BAD_REQUEST, "退回须填写原因");
        }

        Task upd = new Task();
        upd.setId(id);
        upd.setVersion(t.getVersion());
        upd.setStatus(target.name());
        if (target == TaskStatus.DONE) {
            upd.setDoneTime(LocalDateTime.now());
        }
        taskMapper.updateById(upd);

        // 退回原因写入评论（审计痕迹）：开发退回 / 测试不通过退回
        boolean isRollback = (current == TaskStatus.IN_PROGRESS && target == TaskStatus.TODO)
                || (current == TaskStatus.READY_FOR_TEST && target == TaskStatus.IN_PROGRESS);
        if (isRollback && StringUtils.hasText(dto.getReason())) {
            TaskComment c = new TaskComment();
            c.setTaskId(id);
            c.setUserId(SecurityUtils.currentUserId());
            c.setContent("【退回原因】" + dto.getReason());
            commentMapper.insert(c);
        }
    }

    // ============================== 分配 ==============================

    public void assign(Long id, AssignTaskDTO dto) {
        Task t = mustGetTaskAsMember(id);
        if (!SecurityUtils.hasRole(RoleCode.OWNER.name())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅项目负责人可分配任务");
        }
        if (dto.getAssigneeId() != null) {
            ensureMemberIs(dto.getAssigneeId(), t.getProjectId(), "负责人须为项目成员");
        }
        Task upd = new Task();
        upd.setId(id);
        upd.setVersion(t.getVersion());
        upd.setAssigneeId(dto.getAssigneeId());
        taskMapper.updateById(upd);
    }

    // ============================== 评论 ==============================

    public Long addComment(Long taskId, CreateCommentDTO dto) {
        mustGetTaskAsMember(taskId);
        TaskComment c = new TaskComment();
        c.setTaskId(taskId);
        c.setUserId(SecurityUtils.currentUserId());
        c.setContent(dto.getContent());
        commentMapper.insert(c);
        return c.getId();
    }

    public List<TaskCommentVO> listComments(Long taskId) {
        mustGetTaskAsMember(taskId);
        List<TaskComment> list = commentMapper.selectList(
                Wrappers.<TaskComment>lambdaQuery()
                        .eq(TaskComment::getTaskId, taskId)
                        .orderByAsc(TaskComment::getCreateTime));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = loadUsers(list.stream().map(TaskComment::getUserId));
        return list.stream().map(c -> {
            TaskCommentVO vo = new TaskCommentVO();
            BeanUtils.copyProperties(c, vo);
            fillUser(vo, userMap.get(c.getUserId()));
            return vo;
        }).toList();
    }

    // ============================== 工作记录 ==============================

    public Long addWorkLog(Long taskId, CreateWorkLogDTO dto) {
        Task t = mustGetWritableTask(taskId);
        WorkLog w = new WorkLog();
        w.setTaskId(taskId);
        w.setUserId(SecurityUtils.currentUserId());
        w.setSprintId(t.getSprintId());
        w.setContent(dto.getContent());
        w.setHours(dto.getHours());
        w.setLogDate(dto.getLogDate());
        workLogMapper.insert(w);
        return w.getId();
    }

    public List<WorkLogVO> listWorkLogs(Long taskId) {
        mustGetTaskAsMember(taskId);
        List<WorkLog> list = workLogMapper.selectList(
                Wrappers.<WorkLog>lambdaQuery()
                        .eq(WorkLog::getTaskId, taskId)
                        .orderByDesc(WorkLog::getLogDate));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = loadUsers(list.stream().map(WorkLog::getUserId));
        return list.stream().map(w -> {
            WorkLogVO vo = new WorkLogVO();
            BeanUtils.copyProperties(w, vo);
            fillUser(vo, userMap.get(w.getUserId()));
            return vo;
        }).toList();
    }

    // ============================== 燃尽图 ==============================

    /**
     * 燃尽图（SRS §5.4.4）：纵轴=剩余任务数。按迭代开始日到 min(今天, 结束日) 逐日统计，
     * remaining=总任务数-截至当日完成数；ideal=按天均匀递减到 0 的理想线。
     */
    public List<BurndownPointVO> burndown(Long sprintId) {
        Sprint s = sprintMapper.selectById(sprintId);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "迭代不存在");
        }
        List<Task> tasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getSprintId, sprintId));
        int total = tasks.size();

        LocalDate start = s.getStartDate();
        LocalDate end = s.getEndDate();
        // 无起止日期时退化为单点（当前剩余）
        if (start == null || end == null) {
            List<BurndownPointVO> single = new ArrayList<>(1);
            BurndownPointVO p = new BurndownPointVO();
            p.setDate(LocalDate.now());
            p.setRemaining(remainingAt(tasks, LocalDate.now()));
            p.setIdeal(0);
            single.add(p);
            return single;
        }
        LocalDate today = LocalDate.now();
        LocalDate last = end.isBefore(today) ? end : today;
        if (last.isBefore(start)) {
            last = start;
        }

        long span = start.until(end).getDays();
        List<BurndownPointVO> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(last); d = d.plusDays(1)) {
            BurndownPointVO p = new BurndownPointVO();
            p.setDate(d);
            p.setRemaining(remainingAt(tasks, d));
            // 理想线：从 total 线性降到 0
            long elapsed = start.until(d).getDays();
            int ideal = span <= 0 ? 0 : (int) Math.round(total * (1.0 - (double) elapsed / span));
            p.setIdeal(Math.max(ideal, 0));
            points.add(p);
        }
        return points;
    }

    private int remainingAt(List<Task> tasks, LocalDate date) {
        int done = 0;
        for (Task t : tasks) {
            if (t.getDoneTime() != null && !t.getDoneTime().toLocalDate().isAfter(date)) {
                done++;
            }
        }
        return tasks.size() - done;
    }

    // ============================== 供 ProjectService 调用 ==============================

    /**
     * 统计项目下某成员的未完成任务数（移除成员前校验，SRS §5.2.2）。
     */
    public long countUndoneTasks(Long projectId, Long userId) {
        return taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId)
                .eq(Task::getAssigneeId, userId)
                .in(Task::getStatus,
                        TaskStatus.TODO.name(),
                        TaskStatus.IN_PROGRESS.name(),
                        TaskStatus.READY_FOR_TEST.name()));
    }

    // ============================== 私有：校验与工具 ==============================

    /**
     * 取任务 + 校验当前用户是项目成员（不含写边界）。
     */
    private Task mustGetTaskAsMember(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在");
        }
        ensureMember(t.getProjectId());
        return t;
    }

    /**
     * 取任务 + 写边界（owner 任意 / developer 仅自己负责的）。
     */
    private Task mustGetWritableTask(Long id) {
        Task t = mustGetTaskAsMember(id);
        ensureWritable(t);
        return t;
    }

    private void ensureWritable(Task t) {
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不参与项目业务");
        }
        if (SecurityUtils.hasRole(RoleCode.OWNER.name())) {
            return;
        }
        // DEVELOPER / 其他：仅自己负责的任务（未分配的任务 assigneeId 为 null，不可操作）
        Long uid = SecurityUtils.currentUserId();
        if (!uid.equals(t.getAssigneeId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅可操作自己负责的任务");
        }
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
            throw new BizException(ResultCode.BAD_REQUEST, "迭代与任务不属于同一项目");
        }
    }

    private void validateRequirementInProject(Long requirementId, Long projectId) {
        Requirement r = requirementMapper.selectById(requirementId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!r.getProjectId().equals(projectId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "需求与任务不属于同一项目");
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

    private void fillUser(TaskCommentVO vo, User u) {
        if (u != null) {
            vo.setUserName(u.getUsername());
            vo.setRealName(u.getRealName());
        }
    }

    private void fillUser(WorkLogVO vo, User u) {
        if (u != null) {
            vo.setUserName(u.getUsername());
            vo.setRealName(u.getRealName());
        }
    }

    private Map<Long, User> loadUsers(java.util.stream.Stream<Long> idStream) {
        Set<Long> ids = idStream.filter(Objects::nonNull).collect(Collectors.toSet());
        return loadMap(ids, userMapper::selectByIds, User::getId);
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
