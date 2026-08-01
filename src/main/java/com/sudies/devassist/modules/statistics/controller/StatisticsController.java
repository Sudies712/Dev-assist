package com.sudies.devassist.modules.statistics.controller;

import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.statistics.service.StatisticsService;
import com.sudies.devassist.modules.statistics.vo.MemberLoadVO;
import com.sudies.devassist.modules.statistics.vo.ProjectStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计仪表盘（接口契约 §6）。
 * <p>burndown 已在 SprintController（/api/sprints/{id}/burndown）。本控制器提供项目统计聚合 + 成员负载。
 * 权限：项目读（所有角色可见所属项目统计），由 service ensureMember 校验。
 */
@Tag(name = "统计仪表盘")
@RestController
@RequestMapping("/api/projects")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @Operation(summary = "项目统计聚合（项目概况 + 需求/任务/Bug/测试/AI 分布）")
    @GetMapping("/{id}/statistics")
    public Result<ProjectStatisticsVO> statistics(@PathVariable Long id) {
        return Result.success(statisticsService.statistics(id));
    }

    @Operation(summary = "项目成员负载（任务/Bug/完成数）")
    @GetMapping("/{id}/member-load")
    public Result<List<MemberLoadVO>> memberLoad(@PathVariable Long id) {
        return Result.success(statisticsService.memberLoad(id));
    }
}
