package com.sudies.devassist.modules.sprint.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.RequirementStatus;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.enums.SprintStatus;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.sprint.dto.*;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.entity.SprintRequirement;
import com.sudies.devassist.modules.sprint.mapper.SprintMapper;
import com.sudies.devassist.modules.sprint.mapper.SprintRequirementMapper;
import com.sudies.devassist.modules.sprint.vo.SprintProgressVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 迭代管理：CRUD、状态机、需求规划（联动需求 SCHEDULED↔CONFIRMED）、进度、总结。
 */
@Service
public class SprintService {

    @Resource
    private SprintMapper sprintMapper;

    @Resource
    private SprintRequirementMapper sprintRequirementMapper;

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @DataScope("project_id")
    public PageResult<Sprint> page(SprintQuery q) {
        Page<Sprint> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Sprint>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(Sprint::getProjectId, q.getProjectId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(Sprint::getStatus, q.getStatus());
        }
        w.orderByDesc(Sprint::getCreateTime);
        sprintMapper.selectPage(page, w);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public Sprint detail(Long id) {
        Sprint s = sprintMapper.selectById(id);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "迭代不存在或无权访问");
        }
        return s;
    }

    public Long create(CreateSprintDTO dto) {
        ensureMember(dto.getProjectId());
        Sprint s = new Sprint();
        s.setProjectId(dto.getProjectId());
        s.setName(dto.getName());
        s.setGoal(dto.getGoal());
        s.setStartDate(dto.getStartDate());
        s.setEndDate(dto.getEndDate());
        s.setStatus(SprintStatus.NOT_STARTED.name());
        sprintMapper.insert(s);
        return s.getId();
    }

    public void update(Long id, UpdateSprintDTO dto) {
        Sprint s = mustGetOwnedSprint(id);
        Sprint upd = new Sprint();
        upd.setId(id);
        upd.setVersion(s.getVersion());
        // 进行中的迭代仅允许改目标
        if (SprintStatus.IN_PROGRESS.name().equals(s.getStatus())) {
            upd.setGoal(dto.getGoal());
        } else {
            upd.setName(dto.getName());
            upd.setGoal(dto.getGoal());
        }
        sprintMapper.updateById(upd);
    }

    /**
     * 仅未开始迭代可删除
     */
    public void delete(Long id) {
        Sprint s = mustGetOwnedSprint(id);
        if (!SprintStatus.NOT_STARTED.name().equals(s.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅未开始的迭代可删除");
        }
        sprintMapper.deleteById(id);
    }

    public void changeStatus(Long id, String target) {
        Sprint s = mustGetOwnedSprint(id);
        SprintStatus current = SprintStatus.valueOf(s.getStatus());
        SprintStatus targetStatus;
        try {
            targetStatus = SprintStatus.valueOf(target);
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知状态: " + target);
        }
        if (!current.next().contains(targetStatus)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_CHANGE);
        }
        Sprint upd = new Sprint();
        upd.setId(id);
        upd.setVersion(s.getVersion());
        upd.setStatus(target);
        if (targetStatus == SprintStatus.COMPLETED) {
            upd.setActualEndDate(java.time.LocalDate.now());
        }
        sprintMapper.updateById(upd);
    }

    public void saveSummary(Long id, SprintSummaryDTO dto) {
        Sprint s = mustGetOwnedSprint(id);
        Sprint upd = new Sprint();
        upd.setId(id);
        upd.setVersion(s.getVersion());
        upd.setSummary(dto.getSummary());
        sprintMapper.updateById(upd);
    }

    /**
     * 加入需求：校验同项目 + 需求 CONFIRMED → SCHEDULED
     */
    @Transactional
    public void planRequirement(Long sprintId, ScheduleRequirementDTO dto) {
        Sprint s = mustGetOwnedSprint(sprintId);
        Requirement r = requirementMapper.selectById(dto.getRequirementId());
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!r.getProjectId().equals(s.getProjectId())) {
            throw new BizException(ResultCode.BAD_REQUEST, "需求与迭代不属于同一项目");
        }
        if (!RequirementStatus.CONFIRMED.name().equals(r.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有「已确认」的需求可加入迭代，当前状态: " + r.getStatus());
        }
        SprintRequirement sr = new SprintRequirement();
        sr.setSprintId(sprintId);
        sr.setRequirementId(r.getId());
        sr.setOperatorId(SecurityUtils.currentUserId());
        sr.setAddTime(LocalDateTime.now());
        sprintRequirementMapper.insert(sr);

        Requirement rup = new Requirement();
        rup.setId(r.getId());
        rup.setVersion(r.getVersion());
        rup.setStatus(RequirementStatus.SCHEDULED.name());
        requirementMapper.updateById(rup);
    }

    /**
     * 移出需求：校验需求 SCHEDULED（未开发）→ CONFIRMED
     */
    @Transactional
    public void removeRequirement(Long sprintId, Long requirementId) {
        Sprint s = mustGetOwnedSprint(sprintId);
        Requirement r = requirementMapper.selectById(requirementId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!RequirementStatus.SCHEDULED.name().equals(r.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅已排期且未开始开发的需求可移出");
        }
        SprintRequirement sr = sprintRequirementMapper.selectList(
                        Wrappers.<SprintRequirement>lambdaQuery()
                                .eq(SprintRequirement::getSprintId, sprintId)
                                .eq(SprintRequirement::getRequirementId, requirementId)
                                .isNull(SprintRequirement::getRemoveTime))
                .stream().findFirst().orElse(null);
        if (sr != null) {
            SprintRequirement srUpd = new SprintRequirement();
            srUpd.setId(sr.getId());
            srUpd.setRemoveTime(LocalDateTime.now());
            sprintRequirementMapper.updateById(srUpd);
        }
        Requirement rup = new Requirement();
        rup.setId(r.getId());
        rup.setVersion(r.getVersion());
        rup.setStatus(RequirementStatus.CONFIRMED.name());
        requirementMapper.updateById(rup);
    }

    /**
     * 迭代内的需求列表
     */
    public List<Requirement> listRequirements(Long sprintId) {
        Sprint s = mustGetOwnedSprint(sprintId);
        List<Long> reqIds = sprintRequirementMapper.selectList(
                        Wrappers.<SprintRequirement>lambdaQuery()
                                .eq(SprintRequirement::getSprintId, sprintId)
                                .isNull(SprintRequirement::getRemoveTime))
                .stream().map(SprintRequirement::getRequirementId).toList();
        if (reqIds.isEmpty()) {
            return List.of();
        }
        return requirementMapper.selectByIds(reqIds);
    }

    public SprintProgressVO progress(Long sprintId) {
        Sprint s = mustGetOwnedSprint(sprintId);
        List<Long> reqIds = sprintRequirementMapper.selectList(
                        Wrappers.<SprintRequirement>lambdaQuery()
                                .eq(SprintRequirement::getSprintId, sprintId)
                                .isNull(SprintRequirement::getRemoveTime))
                .stream().map(SprintRequirement::getRequirementId).toList();
        SprintProgressVO vo = new SprintProgressVO();
        vo.setRequirementTotal(reqIds.size());
        if (!reqIds.isEmpty()) {
            long done = requirementMapper.selectCount(Wrappers.<Requirement>lambdaQuery()
                    .in(Requirement::getId, reqIds)
                    .in(Requirement::getStatus, RequirementStatus.DONE.name(), RequirementStatus.CLOSED.name()));
            vo.setRequirementDone(done);
            vo.setRequirementDoneRate(reqIds.size() == 0 ? 0 : (double) done / reqIds.size());
        }
        return vo;
    }

    private Sprint mustGetOwnedSprint(Long id) {
        Sprint s = sprintMapper.selectById(id);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "迭代不存在");
        }
        ensureMember(s.getProjectId());
        return s;
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
