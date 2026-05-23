package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
public class CategoryInfo implements Serializable {


    /**
     * 自增分类ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 父级分类ID
     */
    private Integer parentCategoryId;

    /**
     * 图标
     */
    private String icon;

    /**
     * 背景图
     */
    private String background;

    /**
     * 排序号
     */
    private Integer orderNum;

    @Transient
    public List<CategoryInfo> children = new ArrayList<>();
}
