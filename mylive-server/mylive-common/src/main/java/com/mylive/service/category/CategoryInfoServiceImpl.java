package com.mylive.service.category;

import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.po.CategoryInfo;
import com.mylive.infra.jpa.repository.CategoryInfoRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.mapstruct.CategoryInfoMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.BasicStorageService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryInfoServiceImpl implements CategoryInfoService {
    private final CategoryInfoRepository repo;
    private final CategoryInfoMapper categoryInfoMapper;
    private final RedisComponent redisComponent;
    private final BasicStorageService storageService;
    private final VideoInfoRepository videoInfoRepository;

    // NOTE: Category 默认只实现了两级分类
    @Override
    public List<CategoryInfo> getCategoryList() {
        // 1. 查出所有分类（按父ID + 排序）
        List<CategoryInfo> allList = repo.findAllByOrderByParentCategoryIdAscOrderNumAsc();

        // 2. 按 parentId 分组
        Map<Integer, List<CategoryInfo>> groupMap = allList.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getParentCategoryId() == null ? 0 : item.getParentCategoryId()
                ));

        // 3. 取一级分类（parentCategoryId = 0）
        List<CategoryInfo> rootList = groupMap.getOrDefault(0, new ArrayList<>());

        // 4. 给每个一级分类塞 children
        for (CategoryInfo parent : rootList) {
            List<CategoryInfo> children = groupMap.get(parent.getCategoryId());
            if (children != null) {
                parent.setChildren(children);
            }
        }

        return rootList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategory(CategoryInfo bean) {
        CategoryInfo codeExists = repo.findByCategoryCode(bean.getCategoryCode());
        if (codeExists != null &&
                !Objects.equals(bean.getCategoryId(), codeExists.getCategoryId())) {
            throw new BusinessException("Category code already exists");
        }

        if (bean.getCategoryId() == null) {
            int maxOrder = repo.findMaxOrder(bean.getParentCategoryId());
            bean.setOrderNum(maxOrder + 1);
            repo.save(bean);
        } else {
            CategoryInfo entity = repo.findById(bean.getCategoryId())
                    .orElseThrow(() -> new BusinessException("Category not found"));

            categoryInfoMapper.updateFromDto(bean, entity);
            repo.save(entity);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.saveCategoryList(getCategoryList());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delCategory(Integer categoryId) {
        boolean existsVideo = videoInfoRepository.existsByCategoryIdOrParentCategoryId(categoryId, categoryId);
        if (existsVideo) {
            throw new BusinessException("There are videos in this category, so you cannot delete it");
        }

        CategoryInfo categoryInfo = repo.findByCategoryId(categoryId);
        if (categoryInfo == null) {
            return;
        }
        String icon = categoryInfo.getIcon();
        String background = categoryInfo.getBackground();
        repo.deleteByCategoryIdOrParentCategoryId(categoryId, categoryId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.saveCategoryList(getCategoryList());
                if (icon != null) {
                    try {
                        storageService.delete(icon);
                    } catch (Exception e) {
                        log.error("Failed to delete icon, categoryId = {}", categoryId, e);
                    }
                }
                if (background != null) {
                    try {
                        storageService.delete(background);
                    } catch (Exception e) {
                        log.error("Failed to delete background, categoryId = {}", categoryId, e);
                    }
                }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(String categoryIds) {
        List<Integer> ids = StringTools.parseDelimitedDistinctList(categoryIds, ",")
                .stream()
                .map(Integer::valueOf)
                .toList();

        List<CategoryInfo> list = repo.findAllById(ids);

        if (ids.size() != list.size()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        Map<Integer, CategoryInfo> map = list.stream().collect(
                Collectors.toMap(CategoryInfo::getCategoryId, e -> e));

        int order = 1;
        for (Integer id : ids) {
            CategoryInfo category = map.get(id);
            if (category != null) {
                category.setOrderNum(order++);
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.saveCategoryList(getCategoryList());
            }
        });
    }

    @Override
    public List<CategoryInfo> getCategoryList4User() {
        List<CategoryInfo> categoryInfoList = redisComponent.getCategoryList();
        if (categoryInfoList != null) {
            return categoryInfoList;
        }
        List<CategoryInfo> dbList = getCategoryList();
        redisComponent.saveCategoryList(dbList);
        return dbList;
    }
}
