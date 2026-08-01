package com.sudies.devassist.modules.document.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.annotation.DataScope;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.document.dto.DocumentQuery;
import com.sudies.devassist.modules.document.dto.UpdateDocumentDTO;
import com.sudies.devassist.modules.document.entity.Document;
import com.sudies.devassist.modules.document.entity.DocumentChunk;
import com.sudies.devassist.modules.document.mapper.DocumentChunkMapper;
import com.sudies.devassist.modules.document.mapper.DocumentMapper;
import com.sudies.devassist.modules.document.vo.DocumentVO;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目文档管理：上传、编辑、删除（连带清理 Qdrant 向量 + 文件）、重解析。
 * <p>上传后异步解析向量化（{@link DocumentParseTask}）。查询走 {@link DataScope} 按项目隔离。
 * 写操作（upload/update/delete/reparse）权限码收敛到项目负责人；读/下载对项目成员开放。
 */
@Service
public class DocumentService {

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private DocumentChunkMapper chunkMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DocumentParseTask parseTask;

    @Value("${dev-assist.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${qdrant.host:127.0.0.1}")
    private String qdrantHost;

    @Value("${qdrant.rest-port:6333}")
    private int qdrantRestPort;

    @Value("${qdrant.collection:dev_assist_chunks}")
    private String qdrantCollection;

    // ============================== 查询 ==============================

    @DataScope("project_id")
    public PageResult<DocumentVO> page(DocumentQuery q) {
        Page<Document> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<Document>lambdaQuery();
        if (q.getProjectId() != null) {
            w.eq(Document::getProjectId, q.getProjectId());
        }
        if (StringUtils.hasText(q.getType())) {
            w.eq(Document::getType, q.getType());
        }
        if (StringUtils.hasText(q.getParseStatus())) {
            w.eq(Document::getParseStatus, q.getParseStatus());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(Document::getName, q.getKeyword());
        }
        w.orderByDesc(Document::getCreateTime);
        documentMapper.selectPage(page, w);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    @DataScope("project_id")
    public DocumentVO detail(Long id) {
        Document d = documentMapper.selectById(id);
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文档不存在或无权访问");
        }
        return enrich(List.of(d)).get(0);
    }

    private List<DocumentVO> enrich(List<Document> docs) {
        if (docs.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectByIds(
                        docs.stream().map(Document::getUploaderId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<DocumentVO> vos = new ArrayList<>(docs.size());
        for (Document d : docs) {
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(d, vo);
            User u = userMap.get(d.getUploaderId());
            if (u != null) {
                vo.setUploaderName(StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername());
            }
            vos.add(vo);
        }
        return vos;
    }

    // ============================== 上传 ==============================

    public Long upload(MultipartFile file, Long projectId, String type, String description) {
        ensureMember(projectId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf('.'));
        }
        String stored = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, "document");
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(stored));
        } catch (IOException e) {
            throw new BizException(ResultCode.ERROR, "文件保存失败: " + e.getMessage());
        }
        Document d = new Document();
        d.setProjectId(projectId);
        d.setName(origName);
        d.setType(StringUtils.hasText(type) ? type.toUpperCase() : "OTHER");
        d.setDescription(description);
        d.setFilePath("/document/" + stored);
        d.setFileSize(file.getSize());
        d.setParseStatus("UNPARSED");
        d.setUploaderId(SecurityUtils.currentUserId());
        documentMapper.insert(d);
        // 异步解析向量化（跨 bean 调用，@Async 代理生效）
        parseTask.parse(d.getId());
        return d.getId();
    }

    // ============================== 编辑/删除/重解析 ==============================

    public void update(Long id, UpdateDocumentDTO dto) {
        Document d = mustGetOwnedDocument(id);
        Document upd = new Document();
        upd.setId(id);
        if (StringUtils.hasText(dto.getType())) {
            upd.setType(dto.getType().toUpperCase());
        }
        if (dto.getDescription() != null) {
            upd.setDescription(dto.getDescription());
        }
        documentMapper.updateById(upd);
    }

    @Transactional
    public void delete(Long id) {
        Document d = mustGetOwnedDocument(id);
        // 删 Qdrant 向量（按 document_id filter）
        deleteVectorsByDocument(id);
        // 删 chunks
        chunkMapper.delete(Wrappers.<DocumentChunk>lambdaQuery().eq(DocumentChunk::getDocumentId, id));
        // 删文件
        try {
            Path f = Paths.get(uploadDir, d.getFilePath().replaceFirst("^/+", ""));
            Files.deleteIfExists(f);
        } catch (IOException ignored) {
        }
        documentMapper.deleteById(id);
    }

    /**
     * 重解析：清旧切片与向量，重新摄入。
     */
    public void reparse(Long id) {
        Document d = mustGetOwnedDocument(id);
        deleteVectorsByDocument(id);
        chunkMapper.delete(Wrappers.<DocumentChunk>lambdaQuery().eq(DocumentChunk::getDocumentId, id));
        Document upd = new Document();
        upd.setId(id);
        upd.setParseStatus("UNPARSED");
        documentMapper.updateById(upd);
        parseTask.parse(id);
    }

    public List<DocumentChunk> listChunks(Long id) {
        mustGetOwnedDocument(id);
        return chunkMapper.selectList(Wrappers.<DocumentChunk>lambdaQuery()
                .eq(DocumentChunk::getDocumentId, id)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    /**
     * 供下载：返回文档实体（含 filePath）。
     */
    public Document getForDownload(Long id) {
        Document d = documentMapper.selectById(id);
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文档不存在");
        }
        ensureMember(d.getProjectId());
        return d;
    }

    /**
     * 供下载：解析文档物理路径。
     */
    public Path resolveFilePath(Long id) {
        Document d = getForDownload(id);
        return Paths.get(uploadDir, d.getFilePath().replaceFirst("^/+", ""));
    }

    // ============================== 私有 ==============================

    private Document mustGetOwnedDocument(Long id) {
        Document d = documentMapper.selectById(id);
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文档不存在");
        }
        ensureMember(d.getProjectId());
        return d;
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

    /**
     * 调 Qdrant REST 按 document_id 删除所有点。
     */
    private void deleteVectorsByDocument(Long documentId) {
        try {
            RestClient client = RestClient.builder().baseUrl("http://" + qdrantHost + ":" + qdrantRestPort).build();
            String body = "{\"filter\":{\"must\":[{\"key\":\"document_id\",\"match\":{\"value\":"
                    + documentId + "}}]}}";
            client.post().uri("/collections/{c}/points/delete?wait=true", qdrantCollection)
                    .body(body).retrieve().toBodilessEntity();
        } catch (Exception ignored) {
            // 向量删除失败不阻断文档删除流程
        }
    }
}
