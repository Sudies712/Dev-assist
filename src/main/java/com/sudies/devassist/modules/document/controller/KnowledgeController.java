package com.sudies.devassist.modules.document.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.document.dto.AskDTO;
import com.sudies.devassist.modules.document.service.KnowledgeService;
import com.sudies.devassist.modules.document.vo.KnowledgeAnswerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库问答")
@RestController
@RequestMapping("/api/kb")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @Operation(summary = "知识库问答（RAG，按项目隔离检索）")
    @SaCheckPermission("kb:ask")
    @PostMapping("/ask")
    public Result<KnowledgeAnswerVO> ask(@RequestBody @Valid AskDTO dto) {
        return Result.success(knowledgeService.ask(dto.getProjectId(), dto.getQuestion()));
    }
}
