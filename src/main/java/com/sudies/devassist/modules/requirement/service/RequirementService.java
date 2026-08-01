package com.sudies.devassist.modules.requirement.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.RequirementPriority;
import com.sudies.devassist.common.enums.RequirementStatus;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.dto.*;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.entity.RequirementReview;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.requirement.mapper.RequirementReviewMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * 需求管理。list/detail 标 {@link DataScope} 自动按 project_id 隔离；
 * 状态流转走 {@link RequirementStatus#next()}；评审流转写评审记录。
 */
@Service
public class RequirementService {

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private RequirementReviewMapper reviewMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    /**
     * 需求列表：@DataScope 自动追加 project_id IN (当前用户可见项目)
     */
    @DataScope("project_id")
    public PageResult<Requirement> page(RequirementQuery q) {
        Page<Requirement> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Requirement>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(Requirement::getProjectId, q.getProjectId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(Requirement::getStatus, q.getStatus());
        }
        if (StringUtils.hasText(q.getPriority())) {
            w.eq(Requirement::getPriority, q.getPriority());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(Requirement::getTitle, q.getKeyword());
        }
        w.orderByDesc(Requirement::getPriority).orderByDesc(Requirement::getCreateTime);
        requirementMapper.selectPage(page, w);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public Requirement detail(Long id) {
        Requirement r = requirementMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在或无权访问");
        }
        return r;
    }

    public Long create(CreateRequirementDTO dto) {
        ensureMember(dto.getProjectId());
        Long uid = SecurityUtils.currentUserId();
        Requirement r = new Requirement();
        r.setProjectId(dto.getProjectId());
        r.setTitle(dto.getTitle());
        r.setDescription(dto.getDescription());
        r.setType(dto.getType());
        r.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : RequirementPriority.MEDIUM.name());
        r.setStatus(RequirementStatus.PENDING_REVIEW.name());
        r.setEstimatedEffort(dto.getEstimatedEffort());
        r.setCreatorId(uid);
        requirementMapper.insert(r);
        return r.getId();
    }

    public void update(Long id, UpdateRequirementDTO dto) {
        Requirement r = requirementMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        ensureMember(r.getProjectId());
        Requirement upd = new Requirement();
        upd.setId(id);
        upd.setVersion(r.getVersion());
        upd.setTitle(dto.getTitle());
        upd.setDescription(dto.getDescription());
        upd.setType(dto.getType());
        upd.setPriority(dto.getPriority());
        upd.setEstimatedEffort(dto.getEstimatedEffort());
        requirementMapper.updateById(upd);
    }

    /**
     * 删除：已排期/开发中/测试中/已完成 不可删（只能关闭）
     */
    public void delete(Long id) {
        Requirement r = requirementMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        ensureMember(r.getProjectId());
        RequirementStatus s = RequirementStatus.valueOf(r.getStatus());
        if (EnumSet.of(RequirementStatus.SCHEDULED, RequirementStatus.DEVELOPING,
                RequirementStatus.TESTING, RequirementStatus.DONE).contains(s)) {
            throw new BizException(ResultCode.BAD_REQUEST, "已排期/开发中的需求不可删除，请改用关闭");
        }
        requirementMapper.deleteById(id);
    }

    public void changeStatus(Long id, RequirementStatusChangeDTO dto) {
        Requirement r = requirementMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        ensureMember(r.getProjectId());
        RequirementStatus current = RequirementStatus.valueOf(r.getStatus());
        RequirementStatus target;
        try {
            target = RequirementStatus.valueOf(dto.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知状态: " + dto.getTargetStatus());
        }
        if (!current.next().contains(target)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_CHANGE);
        }
        // 评审流转：PENDING_REVIEW → CONFIRMED/CLOSED，记录评审
        if (current == RequirementStatus.PENDING_REVIEW && StringUtils.hasText(dto.getReviewResult())) {
            RequirementReview review = new RequirementReview();
            review.setRequirementId(id);
            review.setReviewerId(SecurityUtils.currentUserId());
            review.setResult(dto.getReviewResult());
            review.setOpinion(dto.getReviewOpinion());
            review.setReviewTime(LocalDateTime.now());
            reviewMapper.insert(review);
        }
        Requirement upd = new Requirement();
        upd.setId(id);
        upd.setVersion(r.getVersion());
        upd.setStatus(target.name());
        requirementMapper.updateById(upd);
    }

    public List<RequirementReview> listReviews(Long requirementId) {
        return reviewMapper.selectList(Wrappers.<RequirementReview>lambdaQuery()
                .eq(RequirementReview::getRequirementId, requirementId)
                .orderByDesc(RequirementReview::getReviewTime));
    }

    public void addReview(Long requirementId, CreateReviewDTO dto) {
        Requirement r = requirementMapper.selectById(requirementId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "需求不存在");
        }
        ensureMember(r.getProjectId());
        RequirementReview review = new RequirementReview();
        review.setRequirementId(requirementId);
        review.setReviewerId(SecurityUtils.currentUserId());
        review.setResult(dto.getResult());
        review.setOpinion(dto.getOpinion());
        review.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(review);
    }

    /**
     * 校验当前用户是该项目的成员（ADMIN 放行）
     */
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
