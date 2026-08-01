package com.sudies.devassist.modules.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReviewDTO {

    /**
     * PASS / REJECT
     */
    @NotBlank(message = "评审结果不能为空")
    private String result;

    private String opinion;
}
