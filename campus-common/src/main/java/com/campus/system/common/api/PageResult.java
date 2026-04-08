package com.campus.system.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页数据包装类
 */
@Data
@Schema(description = "分页数据")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页条数")
    private Long pageSize;

    public PageResult() {
    }

    public PageResult(Long total, List<T> list, Long pageNum, Long pageSize) {
        this.total = total;
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
