package com.mylive.admin.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.infra.jpa.entity.po.CategoryInfo;
import com.mylive.response.ResponseVo;
import com.mylive.service.category.CategoryInfoService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@RequiresLogin
public class CategoryController {

    private final CategoryInfoService categoryInfoService;

    @PostMapping("/loadCategory")
    public ResponseVo<List<CategoryInfo>> loadDataList() {
        return ResponseVo.ok(categoryInfoService.getCategoryList());
    }

    @PostMapping("/saveCategory")
    public ResponseVo<Void> saveCategory(@NotNull Integer parentCategoryId,
                                   Integer categoryId,
                                   @NotBlank String categoryCode,
                                   @NotBlank String categoryName,
                                   String icon,
                                   String background) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setParentCategoryId(parentCategoryId);
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setCategoryCode(categoryCode.trim());
        categoryInfo.setCategoryName(categoryName.trim());
        categoryInfo.setIcon(icon);
        categoryInfo.setBackground(background);
        categoryInfoService.saveCategory(categoryInfo);
        return ResponseVo.ok();
    }

    @PostMapping("/delCategory")
    public ResponseVo<Void> delCategory(@NotNull Integer categoryId) {
        categoryInfoService.delCategory(categoryId);
        return ResponseVo.ok();
    }

    @PostMapping("/reorder")
    public ResponseVo<Void> reorder(@NotBlank String categoryIds) {
        categoryInfoService.reorder(categoryIds);
        return ResponseVo.ok();
    }
}
