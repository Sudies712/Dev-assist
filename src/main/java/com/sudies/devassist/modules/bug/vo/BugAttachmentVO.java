package com.sudies.devassist.modules.bug.vo;

import com.sudies.devassist.modules.bug.entity.BugAttachment;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BugAttachmentVO extends BugAttachment {

    /**
     * 上传人姓名
     */
    private String uploaderName;
}
