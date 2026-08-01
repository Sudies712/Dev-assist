package com.sudies.devassist.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudies.devassist.common.enums.AiDraftStatus;
import com.sudies.devassist.common.enums.AiRecordStatus;
import com.sudies.devassist.common.enums.DraftTarget;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.ai.dto.DraftCreateDTO;
import com.sudies.devassist.modules.ai.dto.DraftItem;
import com.sudies.devassist.modules.ai.entity.AiDraft;
import com.sudies.devassist.modules.ai.entity.AiRecord;
import com.sudies.devassist.modules.ai.mapper.AiDraftMapper;
import com.sudies.devassist.modules.ai.mapper.AiRecordMapper;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.task.dto.CreateTaskDTO;
import com.sudies.devassist.modules.task.service.TaskService;
import com.sudies.devassist.modules.testcase.dto.CreateTestCaseDTO;
import com.sudies.devassist.modules.testcase.service.TestCaseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 草稿：创建（POST /drafts）、确认（PUT /confirm，事务落业务表）、丢弃。
 * <p>草稿确认按 targetModule 分发（关键流程 §3.2）：
 * TASK/TESTCASE 调对应 Service.create 自动落表；REQUIREMENT/BUG/SUMMARY 标记 ADOPTED 供手动采纳。
 * ai_record.status 随草稿状态滚动（UNADOPTED→PARTIAL→FULL）。
 */
@Service
public class AiDraftService {

    @Resource
    private AiDraftMapper draftMapper;

    @Resource
    private AiRecordMapper recordMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private TaskService taskService;

    @Resource
    private TestCaseService testCaseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<Long> createDrafts(DraftCreateDTO dto) {
        ensureMember(dto.getProjectId());
        Long uid = SecurityUtils.currentUserId();
        List<Long> ids = new ArrayList<>();
        for (DraftItem item : dto.getItems()) {
            AiDraft d = new AiDraft();
            d.setAiRecordId(dto.getAiRecordId());
            d.setProjectId(dto.getProjectId());
            d.setTargetModule(item.getTargetModule());
            d.setTargetType(item.getTargetType());
            d.setDraftContent(toJson(item));
            d.setStatus(AiDraftStatus.PENDING_CONFIRM.name());
            d.setCreatorId(uid);
            draftMapper.insert(d);
            ids.add(d.getId());
        }
        refreshRecordStatus(dto.getAiRecordId());
        return ids;
    }

    /**
     * 确认采纳：事务落业务表 + 草稿 ADOPTED。返回生成的业务实体 id（非创建型落点为 null）。
     */
    @Transactional
    public Long confirm(Long draftId) {
        AiDraft d = draftMapper.selectById(draftId);
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND, "草稿不存在");
        }
        ensureMember(d.getProjectId());
        if (AiDraftStatus.ADOPTED.name().equals(d.getStatus())) {
            return null; // 幂等
        }
        if (!AiDraftStatus.PENDING_CONFIRM.name().equals(d.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "草稿状态不允许确认: " + d.getStatus());
        }
        DraftItem item = fromJson(d.getDraftContent());
        Long targetId = applyDraft(d, item);
        d.setStatus(AiDraftStatus.ADOPTED.name());
        d.setConfirmTime(LocalDateTime.now());
        draftMapper.updateById(d);
        refreshRecordStatus(d.getAiRecordId());
        return targetId;
    }

    @Transactional
    public void discard(Long draftId) {
        AiDraft d = draftMapper.selectById(draftId);
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND, "草稿不存在");
        }
        ensureMember(d.getProjectId());
        d.setStatus(AiDraftStatus.DISCARDED.name());
        draftMapper.updateById(d);
        refreshRecordStatus(d.getAiRecordId());
    }

    public List<AiDraft> listByRecord(Long recordId) {
        return draftMapper.selectList(Wrappers.<AiDraft>lambdaQuery()
                .eq(AiDraft::getAiRecordId, recordId)
                .orderByAsc(AiDraft::getCreateTime));
    }

    // ============================== 草稿分发 ==============================

    private Long applyDraft(AiDraft d, DraftItem item) {
        DraftTarget t = DraftTarget.valueOf(d.getTargetModule());
        return switch (t) {
            case TASK -> applyTask(d, item);
            case TESTCASE -> applyTestCase(d, item);
            // REQUIREMENT/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY：标记 ADOPTED，用户手动采纳（补充/总结类）
            default -> null;
        };
    }

    private Long applyTask(AiDraft d, DraftItem item) {
        if (item.getSprintId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "任务草稿需指定归属迭代（sprintId）");
        }
        CreateTaskDTO dto = new CreateTaskDTO();
        dto.setProjectId(d.getProjectId());
        dto.setSprintId(item.getSprintId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getContent());
        dto.setPriority(item.getPriority());
        return taskService.create(dto);
    }

    private Long applyTestCase(AiDraft d, DraftItem item) {
        CreateTestCaseDTO dto = new CreateTestCaseDTO();
        dto.setProjectId(d.getProjectId());
        dto.setTitle(item.getTitle());
        dto.setSteps(item.getContent());
        dto.setExpectedResult(item.getContent());
        dto.setPriority(item.getPriority());
        return testCaseService.create(dto);
    }

    // ============================== 状态滚动 ==============================

    private void refreshRecordStatus(Long recordId) {
        AiRecord rec = recordMapper.selectById(recordId);
        if (rec == null) {
            return;
        }
        List<AiDraft> drafts = draftMapper.selectList(Wrappers.<AiDraft>lambdaQuery()
                .eq(AiDraft::getAiRecordId, recordId));
        if (drafts.isEmpty()) {
            return;
        }
        boolean anyAdopted = drafts.stream().anyMatch(x -> AiDraftStatus.ADOPTED.name().equals(x.getStatus()));
        boolean allClosed = drafts.stream().allMatch(x ->
                AiDraftStatus.ADOPTED.name().equals(x.getStatus()) || AiDraftStatus.DISCARDED.name().equals(x.getStatus()));
        String status = allClosed ? AiRecordStatus.FULL.name()
                : (anyAdopted ? AiRecordStatus.PARTIAL.name() : AiRecordStatus.UNADOPTED.name());
        rec.setStatus(status);
        if (allClosed && anyAdopted && rec.getAdoptTime() == null) {
            rec.setAdoptTime(LocalDateTime.now());
        }
        recordMapper.updateById(rec);
    }

    private String toJson(DraftItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (Exception e) {
            throw new BizException(ResultCode.ERROR, "草稿序列化失败");
        }
    }

    private DraftItem fromJson(String json) {
        try {
            return objectMapper.readValue(json, DraftItem.class);
        } catch (Exception e) {
            throw new BizException(ResultCode.ERROR, "草稿内容损坏");
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
}
