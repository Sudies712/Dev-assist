package com.sudies.devassist.modules.document.vo;

import com.sudies.devassist.modules.document.entity.Document;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentVO extends Document {

    /**
     * 上传人姓名
     */
    private String uploaderName;
}
