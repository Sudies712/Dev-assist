package com.sudies.devassist.modules.bug.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.bug.dto.*;
import com.sudies.devassist.modules.bug.service.BugService;
import com.sudies.devassist.modules.bug.vo.BugAttachmentVO;
import com.sudies.devassist.modules.bug.vo.BugVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "缺陷管理")
@RestController
@RequestMapping("/api/bugs")
public class BugController {

    @Resource
    private BugService bugService;

    @Operation(summary = "缺陷列表（DataScope 按项目隔离）")
    @SaCheckPermission("bug:read")
    @GetMapping
    public Result<PageResult<BugVO>> page(BugQuery q) {
        return Result.success(bugService.page(q));
    }

    @Operation(summary = "缺陷详情")
    @SaCheckPermission("bug:read")
    @GetMapping("/{id}")
    public Result<BugVO> detail(@PathVariable Long id) {
        return Result.success(bugService.detail(id));
    }

    @Operation(summary = "提交缺陷（提交人=当前用户）")
    @SaCheckPermission("bug:submit")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateBugDTO dto) {
        return Result.success(bugService.create(dto));
    }

    @Operation(summary = "编辑缺陷（提交人或项目负责人）")
    @SaCheckPermission("bug:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateBugDTO dto) {
        bugService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除缺陷（仅待确认/已拒绝，提交人或项目负责人）")
    @SaCheckPermission("bug:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bugService.delete(id);
        return Result.success();
    }

    @Operation(summary = "缺陷状态变更（开发修复 / 测试驱动状态机）")
    @SaCheckPermission(value = {"bug:change_status", "bug:update"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody @Valid BugStatusChangeDTO dto) {
        bugService.changeStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "分配修复人（仅项目负责人）")
    @SaCheckPermission("bug:assign")
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody @Valid AssignBugDTO dto) {
        bugService.assign(id, dto);
        return Result.success();
    }

    @Operation(summary = "上传缺陷附件")
    @SaCheckPermission("bug:update")
    @PostMapping("/{id}/attachments")
    public Result<Long> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(bugService.uploadAttachment(id, file));
    }

    @Operation(summary = "附件列表")
    @SaCheckPermission("bug:read")
    @GetMapping("/{id}/attachments")
    public Result<List<BugAttachmentVO>> listAttachments(@PathVariable Long id) {
        return Result.success(bugService.listAttachments(id));
    }

    @Operation(summary = "下载附件")
    @SaCheckPermission("bug:read")
    @GetMapping("/{bugId}/attachments/{attachId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long bugId, @PathVariable Long attachId) {
        BugAttachmentVO meta = bugService.getAttachmentMeta(bugId, attachId);
        org.springframework.core.io.Resource body = bugService.loadAttachmentResource(attachId);
        String encoded = URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(meta.getFileSize() != null ? meta.getFileSize() : -1)
                .body(body);
    }
}
