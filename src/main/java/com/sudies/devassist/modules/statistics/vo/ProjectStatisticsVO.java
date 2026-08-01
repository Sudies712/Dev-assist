package com.sudies.devassist.modules.statistics.vo;

import lombok.Data;

import java.util.List;

/**
 * 项目统计聚合（接口契约 §6）：项目概况(全局可见) + 当前项目各模块统计。
 * DistItem.name 为枚举英文码（status/priority/severity/result/aiType），count 为数量。
 */
@Data
public class ProjectStatisticsVO {

    private ProjectStat project;
    private RequirementStat requirement;
    private TaskStat task;
    private BugStat bug;
    private TestStat test;
    private AiStat ai;

    @Data
    public static class ProjectStat {
        private Integer total;
        private Integer inProgress;
        private List<DistItem> statusDist;
    }

    @Data
    public static class RequirementStat {
        private Long total;
        private Double doneRate;
        private List<DistItem> priorityDist;
    }

    @Data
    public static class TaskStat {
        private Long total;
        private Double doneRate;
        private List<DistItem> statusDist;
        private List<AssigneeItem> byAssignee;
    }

    @Data
    public static class BugStat {
        private Long total;
        private List<DistItem> severityDist;
        private List<DistItem> statusDist;
    }

    @Data
    public static class TestStat {
        private Long total;
        /**
         * 执行通过率（PASSED 执行数 / 总执行数）
         */
        private Double passRate;
        private List<DistItem> resultDist;
    }

    @Data
    public static class AiStat {
        private Long totalCalls;
        /**
         * 采纳率（FULL+PARTIAL）/ total
         */
        private Double adoptRate;
        private List<DistItem> byType;
    }

    @Data
    public static class DistItem {
        private String name;
        private Long value;

        public DistItem() {
        }

        public DistItem(String name, Long value) {
            this.name = name;
            this.value = value;
        }
    }

    @Data
    public static class AssigneeItem {
        private Long userId;
        private String realName;
        private Long count;
    }
}
