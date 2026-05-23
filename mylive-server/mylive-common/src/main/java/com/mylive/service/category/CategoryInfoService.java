package com.mylive.service.category;

import com.mylive.infra.jpa.entity.po.CategoryInfo;

import java.util.List;

public interface CategoryInfoService {
    List<CategoryInfo> getCategoryList();

    void saveCategory(CategoryInfo categoryInfo);

    void delCategory(Integer categoryId);

    void reorder(String categoryIds);

    List<CategoryInfo> getCategoryList4User();
}
