package com.sudies.devassist.modules.bug.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷附件。表 bug_attachment 仅有 create_by/create_time/upload_time（无逻辑删除/更新审计），
 * 故不继承 BaseEntity；createBy/createTime 由 MetaObjectHandler 自动填充，uploadTime 手动设置。
 */
@Data
@TableName("bug_attachment")
public class BugAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bugId;

    private String fileName;

    /**
     * 相对存储路径，如 /bug/{uuid}.png；下载时拼接 upload.dir
     */
    private String filePath;

    private Long fileSize;

    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
