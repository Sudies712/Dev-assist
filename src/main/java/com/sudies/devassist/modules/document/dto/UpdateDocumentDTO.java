package com.sudies.devassist.modules.document.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDocumentDTO {

    /**
     * REQUIREMENT/DESIGN/API/TEST/MEETING/STANDARD/SPRINT_SUMMARY/PROJECT_SUMMARY/OTHER
     */
    private String type;

    @Size(max = 1024, message = "描述过长")
    private String description;
}
