package com.sudies.devassist.modules.ai.controller;

import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.ai.dto.AiAskDTO;
import com.sudies.devassist.modules.ai.dto.DraftCreateDTO;
import com.sudies.devassist.modules.ai.entity.AiDraft;
import com.sudies.devassist.modules.ai.entity.AiRecord;
import com.sudies.devassist.modules.ai.service.AiDraftService;
import com.sudies.devassist.modules.ai.service.AiService;
import com.sudies.devassist.modules.ai.vo.AiResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 助手与草稿（接口契约 §5）。
 * <p>权限按助手动态校验（service ensurePermission: ai:&lt;type&gt;），故 controller 不加静态 @SaCheckPermission。
 */
@Tag(name = "AI 助手")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @Resource
    private AiDraftService aiDraftService;

    @Operation(summary = "AI 助手生成（需求分析/任务拆解/用例生成/Bug分析/迭代总结/项目总结）")
    @PostMapping("/{assistant}")
    public Result<AiResultVO> generate(@PathVariable String assistant, @RequestBody @Valid AiAskDTO dto) {
        return Result.success(aiService.generate(assistant, dto));
    }

    @Operation(summary = "创建草稿（勾选 AI 建议生成草稿，待二次确认）")
    @PostMapping("/drafts")
    public Result<List<Long>> createDrafts(@RequestBody @Valid DraftCreateDTO dto) {
        return Result.success(aiDraftService.createDrafts(dto));
    }

    @Operation(summary = "确认草稿（事务落业务表：TASK/TESTCASE 自动创建）")
    @PutMapping("/drafts/{id}/confirm")
    public Result<Long> confirm(@PathVariable Long id) {
        return Result.success(aiDraftService.confirm(id));
    }

    @Operation(summary = "丢弃草稿")
    @DeleteMapping("/drafts/{id}")
    public Result<Void> discard(@PathVariable Long id) {
        aiDraftService.discard(id);
        return Result.success();
    }

    @Operation(summary = "项目 AI 生成记录")
    @GetMapping("/records")
    public Result<List<AiRecord>> listRecords(@RequestParam Long projectId) {
        return Result.success(aiService.listRecords(projectId));
    }

    @Operation(summary = "AI 记录下的草稿列表")
    @GetMapping("/records/{id}/drafts")
    public Result<List<AiDraft>> listDrafts(@PathVariable Long id) {
        return Result.success(aiDraftService.listByRecord(id));
    }
}
