package com.sudies.devassist.modules.task.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 燃尽图数据点。纵轴=剩余任务数（SRS §5.4.4）。
 * remaining 为实际剩余；ideal 为理想剩余（按天均匀递减到 0），供前端绘理想线。
 */
@Data
public class BurndownPointVO {

    private LocalDate date;

    /**
     * 实际剩余任务数
     */
    private Integer remaining;

    /**
     * 理想剩余任务数
     */
    private Integer ideal;
}
