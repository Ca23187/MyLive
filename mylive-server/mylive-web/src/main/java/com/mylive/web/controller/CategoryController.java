package com.mylive.web.controller;

import com.mylive.infra.jpa.entity.po.CategoryInfo;
import com.mylive.response.ResponseVo;
import com.mylive.service.category.CategoryInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryInfoService categoryInfoService;

    @PostMapping("/loadAllCategory")
    public ResponseVo<List<CategoryInfo>> loadAllCategory() {
        return ResponseVo.ok(categoryInfoService.getCategoryList4User());
    }
}
