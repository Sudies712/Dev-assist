package com.sudies.devassist.modules.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsDTO {

    private List<Long> permissionIds;
}
