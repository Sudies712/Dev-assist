package com.sudies.devassist.modules.document.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.document.vo.KnowledgeAnswerVO;
import com.sudies.devassist.modules.document.vo.KnowledgeSourceVO;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 RAG 问答（关键流程 §4.2）：问题向量化 → Qdrant 检索（强制 project_id 隔离）→
 * 命中为空直接回"未找到"不臆造；命中则拼上下文交 DeepSeek 生成。
 * <p>检索用 Qdrant <b>REST API</b>（/points/search）而非 langchain4j EmbeddingStore.search ——
 * 后者依赖的 gRPC client 1.13 与 server 1.18 search 响应 protobuf 不兼容（写入兼容、读取不兼容）。
 * content 存储在 payload 的 text_segment 字段（由 langchain4j-qdrant 写入）。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private static final int TOP_K = 5;
    private static final double MIN_SCORE = 0.5;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private ChatModel chatModel;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Value("${qdrant.host:127.0.0.1}")
    private String host;

    @Value("${qdrant.rest-port:6333}")
    private int restPort;

    @Value("${qdrant.collection:dev_assist_chunks}")
    private String collection;

    public KnowledgeAnswerVO ask(Long projectId, String question) {
        ensureMember(projectId);
        float[] vec = embeddingModel.embed(question).content().vector();
        StringBuilder vecJson = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) {
                vecJson.append(',');
            }
            vecJson.append(vec[i]);
        }
        vecJson.append(']');
        String body = "{\"vector\":" + vecJson + ",\"limit\":" + TOP_K + ",\"score_threshold\":" + MIN_SCORE
                + ",\"filter\":{\"must\":[{\"key\":\"project_id\",\"match\":{\"value\":" + projectId + "}}]},"
                + "\"with_payload\":true}";

        List<KnowledgeSourceVO> sources = new ArrayList<>();
        StringBuilder ctx = new StringBuilder();
        try {
            RestClient client = RestClient.builder().baseUrl("http://" + host + ":" + restPort).build();
            @SuppressWarnings({"unchecked", "rawtypes"})
            java.util.Map<String, Object> root = client.post().uri("/collections/{c}/points/search", collection)
                    .header("Content-Type", "application/json")
                    .body(body).retrieve().body(java.util.Map.class);
            Object resultObj = root.get("result");
            List<?> points = resultObj instanceof List ? (List<?>) resultObj : List.of();
            int idx = 1;
            for (Object item : points) {
                java.util.Map<String, Object> p = (java.util.Map<String, Object>) item;
                java.util.Map<String, Object> payload = (java.util.Map<String, Object>) p.get("payload");
                if (payload == null) {
                    continue;
                }
                String text = String.valueOf(payload.getOrDefault("text_segment", ""));
                String src = String.valueOf(payload.getOrDefault("source_name", ""));
                if (text.isEmpty()) {
                    continue;
                }
                ctx.append("【片段").append(idx++).append("】").append(text).append("\n\n");
                KnowledgeSourceVO s = new KnowledgeSourceVO();
                s.setSourceName(src);
                s.setSnippet(text.length() > 120 ? text.substring(0, 120) + "…" : text);
                sources.add(s);
            }
        } catch (Exception e) {
            log.error("知识库检索失败 projectId={}", projectId, e);
            KnowledgeAnswerVO vo = new KnowledgeAnswerVO();
            vo.setAnswer("知识库检索失败：" + e.getMessage());
            vo.setSources(List.of());
            return vo;
        }

        KnowledgeAnswerVO vo = new KnowledgeAnswerVO();
        if (sources.isEmpty()) {
            vo.setAnswer("未在当前项目知识库中找到相关文档。");
            vo.setSources(List.of());
            return vo;
        }
        String prompt = "你是项目知识库助手。请严格依据以下项目文档片段回答问题，"
                + "若文档无相关信息请直接说明，不要编造。\n\n文档片段：\n" + ctx + "\n问题：" + question;
        String answer = chatModel.chat(prompt);
        vo.setAnswer(answer);
        vo.setSources(sources);
        return vo;
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
