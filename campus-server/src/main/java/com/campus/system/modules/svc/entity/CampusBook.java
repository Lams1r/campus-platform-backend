package com.campus.system.modules.svc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图书信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus_book")
@Schema(description = "图书信息")
public class CampusBook extends BaseEntity {

    /** 书名 */
    @Schema(description = "书名")
    private String bookName;

    /** 作者 */
    @Schema(description = "作者")
    private String author;

    /** ISBN编号 */
    @Schema(description = "ISBN编号")
    private String isbn;

    /** 出版社 */
    @Schema(description = "出版社")
    private String publisher;

    /** 分类 */
    @Schema(description = "分类")
    private String category;

    /** 馆藏总量 */
    @Schema(description = "馆藏总量")
    private Integer totalCount;

    /** 可借数量 */
    @Schema(description = "可借数量")
    private Integer availableCount;

    /** 存放位置 */
    @Schema(description = "存放位置")
    private String location;
}
