package com.sudies.devassist.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页响应：{@code { list, total, pageSize, currentPage }}。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageSize;
    private long currentPage;

    public static <T> PageResult<T> of(List<T> list, long total, long pageSize, long currentPage) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.pageSize = pageSize;
        r.currentPage = currentPage;
        return r;
    }
}
