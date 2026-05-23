package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.CategoryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryInfoRepository extends JpaRepository<CategoryInfo, Integer> {
    List<CategoryInfo> findAllByOrderByParentCategoryIdAscOrderNumAsc();

    CategoryInfo findByCategoryCode(String categoryCode);

    @Query("select coalesce(max(c.orderNum), 0) from CategoryInfo c where c.parentCategoryId = :parentCategoryId")
    int findMaxOrder(Integer parentCategoryId);

    void deleteByCategoryIdOrParentCategoryId(Integer categoryId, Integer parentCategoryId);

    CategoryInfo findByCategoryId(Integer categoryId);
}
