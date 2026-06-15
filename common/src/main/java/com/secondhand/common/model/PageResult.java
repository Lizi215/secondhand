package com.secondhand.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;

    public PageResult() {
        this.records = Collections.emptyList();
    }

    public PageResult(List<T> records, long total, long page, long size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (size > 0) ? (total + size - 1) / size : 0;
    }
}
