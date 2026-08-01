package com.sudies.devassist.modules.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.enums.ProjectRole;
import com.sudies.devassist.common.enums.ProjectStatus;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.project.dto.AddMemberDTO;
import com.sudies.devassist.modules.project.dto.CreateProjectDTO;
import com.sudies.devassist.modules.project.dto.ProjectQuery;
import com.sudies.devassist.modules.project.dto.UpdateProjectDTO;
import com.sudies.devassist.modules.project.entity.Project;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMapper;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.project.vo.ProjectMemberVO;
import com.sudies.devassist.modules.project.vo.ProjectVO;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.task.service.TaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目管理：CRUD、状态流转（{@link ProjectStatus#next()}）、成员管理。
 * <p>项目列表按当前用户可见过滤：ADMIN 全局；其余只能见自己创建或身为成员的项目。
 */
@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TaskService taskService;

    public PageResult<ProjectVO> page(ProjectQuery q) {
        Long uid = SecurityUtils.currentUserId();
        boolean isAdmin = SecurityUtils.hasRole(RoleCode.ADMIN.name());
        Page<Project> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Project>lambdaQuery();
        if (StringUtils.hasText(q.getName())) {
            w.like(Project::getName, q.getName());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(Project::getStatus, q.getStatus());
        }
        if (!isAdmin) {
            List<Long> memberPids = projectMemberMapper.selectList(
                            Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getUserId, uid))
                    .stream().map(ProjectMember::getProjectId).toList();
            w.and(ww -> {
                ww.eq(Project::getCreatorId, uid);
                if (!memberPids.isEmpty()) {
                    ww.or().in(Project::getId, memberPids);
                }
            });
        }
        w.orderByDesc(Project::getCreateTime);
        projectMapper.selectPage(page, w);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    public ProjectVO detail(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND, "项目不存在");
        }
        return enrich(List.of(p)).get(0);
    }

    /**
     * 补充创建人姓名（realName 优先，其次 username），供列表/详情展示
     */
    private List<ProjectVO> enrich(List<Project> projects) {
        if (projects.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectByIds(
                        projects.stream().map(Project::getCreatorId).filter(java.util.Objects::nonNull)
                                .distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return projects.stream().map(p -> {
            ProjectVO vo = new ProjectVO();
            org.springframework.beans.BeanUtils.copyProperties(p, vo);
            User u = userMap.get(p.getCreatorId());
            vo.setCreatorName(u != null && StringUtils.hasText(u.getRealName())
                    ? u.getRealName() : u != null ? u.getUsername() : null);
            return vo;
        }).toList();
    }

    @Transactional
    public Long create(CreateProjectDTO dto) {
        Long uid = SecurityUtils.currentUserId();
        Project p = new Project();
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setTechStack(dto.getTechStack());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        p.setStatus(ProjectStatus.NOT_STARTED.name());
        p.setCreatorId(uid);
        projectMapper.insert(p);
        // 创建人自动成为项目负责人（OWNER）
        ProjectMember m = new ProjectMember();
        m.setProjectId(p.getId());
        m.setUserId(uid);
        m.setProjectRole(ProjectRole.OWNER.name());
        projectMemberMapper.insert(m);
        return p.getId();
    }

    public void update(Long id, UpdateProjectDTO dto) {
        Project p = new Project();
        p.setId(id);
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setTechStack(dto.getTechStack());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        projectMapper.updateById(p);
    }

    public void changeStatus(Long id, String target) {
        Project p = detail(id);
        ProjectStatus current = ProjectStatus.valueOf(p.getStatus());
        ProjectStatus targetStatus;
        try {
            targetStatus = ProjectStatus.valueOf(target);
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知的项目状态: " + target);
        }
        if (!current.next().contains(targetStatus)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_CHANGE);
        }
        Project upd = new Project();
        upd.setId(id);
        upd.setVersion(p.getVersion());
        upd.setStatus(target);
        // 起止时间由状态变更自动记录（开始=NOT_STARTED→IN_PROGRESS 记 startDate；结束=→COMPLETED 记 endDate）
        if (current == ProjectStatus.NOT_STARTED && targetStatus == ProjectStatus.IN_PROGRESS) {
            upd.setStartDate(java.time.LocalDate.now());
        }
        if (targetStatus == ProjectStatus.COMPLETED) {
            upd.setEndDate(java.time.LocalDate.now());
        }
        projectMapper.updateById(upd);
    }

    public List<ProjectMemberVO> listMembers(Long projectId) {
        List<ProjectMember> members = projectMemberMapper.selectList(
                Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getJoinTime));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectByIds(
                        members.stream().map(ProjectMember::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(m -> {
            ProjectMemberVO vo = new ProjectMemberVO();
            vo.setId(m.getId());
            vo.setUserId(m.getUserId());
            User u = userMap.get(m.getUserId());
            vo.setRealName(u != null ? u.getRealName() : null);
            vo.setProjectRole(m.getProjectRole());
            vo.setJoinTime(m.getJoinTime());
            return vo;
        }).toList();
    }

    public void addMember(Long projectId, AddMemberDTO dto) {
        Long userCnt = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getId, dto.getUserId()));
        if (userCnt == null || userCnt == 0L) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long exist = projectMemberMapper.selectCount(
                Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId)
                        .eq(ProjectMember::getUserId, dto.getUserId()));
        if (exist != null && exist > 0L) {
            throw new BizException(ResultCode.BAD_REQUEST, "该用户已是项目成员");
        }
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setUserId(dto.getUserId());
        m.setProjectRole(dto.getProjectRole());
        projectMemberMapper.insert(m);
    }

    /**
     * 移除成员。校验该成员在该项目下无未完成任务（SRS §5.2.2）。
     */
    public void removeMember(Long projectId, Long userId) {
        long undone = taskService.countUndoneTasks(projectId, userId);
        if (undone > 0) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "该成员尚有 " + undone + " 个未完成任务，无法移除");
        }
        projectMemberMapper.delete(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    /**
     * 可添加的候选成员：系统启用用户中非本项目成员（SRS §5.2.2 从系统用户中选取）。
     * 返回简要字段，避免泄露 loginFailCount/lockUntil 等安全字段。
     */
    public List<com.sudies.devassist.modules.project.vo.UserBriefVO> listMemberCandidates(Long projectId) {
        List<Long> memberUserIds = projectMemberMapper.selectList(
                        Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId))
                .stream().map(ProjectMember::getUserId).toList();
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, "ENABLED")
                .notIn(!memberUserIds.isEmpty(), User::getId, memberUserIds)
                .orderByAsc(User::getRealName));
        return users.stream().map(u -> {
            com.sudies.devassist.modules.project.vo.UserBriefVO vo = new com.sudies.devassist.modules.project.vo.UserBriefVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            return vo;
        }).toList();
    }

    /**
     * 修改成员项目角色（SRS §5.2.2 设置成员角色）。
     */
    public void changeMemberRole(Long projectId, Long userId, String projectRole) {
        ProjectMember m = projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND, "该用户不是项目成员");
        }
        m.setProjectRole(projectRole);
        projectMemberMapper.updateById(m);
    }
}
