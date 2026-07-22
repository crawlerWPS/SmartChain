package com.scfs.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应 - 对应 RFC 3.1 分页约定
 *
 * <pre>
 * {
 *   "list": [...],
 *   "total": 100,
 *   "page": 1,
 *   "size": 20
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total, 1, list == null ? 0 : list.size());
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(java.util.Collections.emptyList(), 0L, page, size);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(java.util.Collections.emptyList(), 0L, 1, 20);
    }
}
