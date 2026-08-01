package com.sudies.devassist.modules.document.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.document.dto.DocumentQuery;
import com.sudies.devassist.modules.document.dto.UpdateDocumentDTO;
import com.sudies.devassist.modules.document.entity.Document;
import com.sudies.devassist.modules.document.entity.DocumentChunk;
import com.sudies.devassist.modules.document.service.DocumentService;
import com.sudies.devassist.modules.document.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@Tag(name = "文档管理")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @Operation(summary = "文档列表（DataScope 按项目隔离）")
    @SaCheckPermission("document:read")
    @GetMapping
    public Result<PageResult<DocumentVO>> page(DocumentQuery q) {
        return Result.success(documentService.page(q));
    }

    @Operation(summary = "文档详情")
    @SaCheckPermission("document:read")
    @GetMapping("/{id}")
    public Result<DocumentVO> detail(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    @Operation(summary = "上传文档（异步解析向量化）")
    @SaCheckPermission("document:upload")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam Long projectId,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String description) {
        return Result.success(documentService.upload(file, projectId, type, description));
    }

    @Operation(summary = "编辑文档（类型/描述）")
    @SaCheckPermission("document:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateDocumentDTO dto) {
        documentService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除文档（连带清理向量与文件）")
    @SaCheckPermission("document:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重新解析文档")
    @SaCheckPermission("document:reparse")
    @PostMapping("/{id}/reparse")
    public Result<Void> reparse(@PathVariable Long id) {
        documentService.reparse(id);
        return Result.success();
    }

    @Operation(summary = "下载文档")
    @SaCheckPermission("document:download")
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        Document d = documentService.getForDownload(id);
        Path file = documentService.resolveFilePath(id);
        String encoded = URLEncoder.encode(d.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(d.getFileSize() != null ? d.getFileSize() : -1)
                .body(new FileSystemResource(file));
    }

    @Operation(summary = "文档切片列表（解析结果）")
    @SaCheckPermission("document:read")
    @GetMapping("/{id}/chunks")
    public Result<List<DocumentChunk>> listChunks(@PathVariable Long id) {
        return Result.success(documentService.listChunks(id));
    }
}
