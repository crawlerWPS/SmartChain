package com.scfs.common.core;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页查询入参
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码从1开始")
    private int page = 1;

    @Min(value = 1, message = "每页大小至少1")
    @Max(value = 100, message = "每页大小最大100")
    private int size = 20;

    /** 排序字段 */
    private String sortBy;

    /** 排序方向 ASC/DESC */
    private String sortOrder = "DESC";

    /** 关键词搜索 */
    private String keyword;

    public long offset() {
        return (long) (page - 1) * size;
    }
}
