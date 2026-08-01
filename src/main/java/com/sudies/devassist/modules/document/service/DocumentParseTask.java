package com.sudies.devassist.modules.document.service;

import com.sudies.devassist.modules.document.entity.Document;
import com.sudies.devassist.modules.document.entity.DocumentChunk;
import com.sudies.devassist.modules.document.mapper.DocumentChunkMapper;
import com.sudies.devassist.modules.document.mapper.DocumentMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档异步摄入任务（关键流程 §4.1）：文本提取 → 切片 → 向量化 → 入 Qdrant → 回写 document_chunk。
 * <p>独立 bean，由 {@link DocumentService} 跨实例调用，确保 {@code @Async} 代理生效。
 * 文本提取当前支持 MD/TXT（直读）；其他类型记 FAILED（后续 Tika 扩展）。
 */
@Component
public class DocumentParseTask {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseTask.class);

    /**
     * 切片大小/重叠（SRS §4.1：300–500 字，重叠 50）
     */
    private static final int CHUNK_SIZE = 400;
    private static final int OVERLAP = 50;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private DocumentChunkMapper chunkMapper;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${dev-assist.upload.dir:./uploads}")
    private String uploadDir;

    @Async
    public void parse(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            return;
        }
        markStatus(documentId, "PARSING", null);
        try {
            String text = extractText(doc);
            List<String> chunks = chunkText(text);
            if (chunks.isEmpty()) {
                markStatus(documentId, "PARSED", null);
                return;
            }
            // 构造 TextSegment（带 metadata：project_id / document_id / source_name）
            List<TextSegment> segments = new ArrayList<>(chunks.size());
            for (String c : chunks) {
                segments.add(TextSegment.from(c,
                        new dev.langchain4j.data.document.Metadata()
                                .put("project_id", doc.getProjectId())
                                .put("document_id", doc.getId())
                                .put("source_name", StringUtils.hasText(doc.getName()) ? doc.getName() : "")));
            }
            Response<List<Embedding>> resp = embeddingModel.embedAll(segments);
            List<Embedding> embeddings = resp.content();
            for (int i = 0; i < segments.size(); i++) {
                String vectorId = embeddingStore.add(embeddings.get(i), segments.get(i));
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setProjectId(doc.getProjectId());
                chunk.setContent(chunks.get(i));
                chunk.setVectorId(vectorId);
                chunk.setChunkIndex(i);
                chunkMapper.insert(chunk);
            }
            markStatus(documentId, "PARSED", null);
        } catch (Exception e) {
            log.error("文档解析失败 docId={}", documentId, e);
            markStatus(documentId, "FAILED", e.getMessage());
        }
    }

    private String extractText(Document doc) throws IOException {
        Path file = Paths.get(uploadDir, doc.getFilePath().replaceFirst("^/+", ""));
        String name = doc.getName() == null ? "" : doc.getName().toLowerCase();
        if (name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".markdown")) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("暂不支持该文件类型解析（当前仅 MD/TXT）: " + doc.getName());
    }

    /**
     * 按字符数切片，重叠 OVERLAP。
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        text = text.trim();
        if (text.length() <= CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = end - OVERLAP;
        }
        return chunks;
    }

    private void markStatus(Long id, String status, String error) {
        Document upd = new Document();
        upd.setId(id);
        upd.setParseStatus(status);
        // error 暂不落库（schema 无错误字段），仅日志
        documentMapper.updateById(upd);
    }
}
