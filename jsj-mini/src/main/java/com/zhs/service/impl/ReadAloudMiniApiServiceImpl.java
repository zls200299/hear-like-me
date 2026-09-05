package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.exception.ServiceException;
import com.zhs.model.ReadAloudCategory;
import com.zhs.model.ReadAloudItem;
import com.zhs.response.readaloud.ReadAloudCategoryApiResp;
import com.zhs.response.readaloud.ReadAloudItemApiResp;
import com.zhs.service.IReadAloudCategoryService;
import com.zhs.service.IReadAloudItemService;
import com.zhs.service.ReadAloudMiniApiService;
import com.zhs.service.storage.LocalFileStorageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReadAloudMiniApiServiceImpl implements ReadAloudMiniApiService {

    private static final Map<String, String> CATEGORY_ICON_MAP = Map.of(
            "daily", "home",
            "fruit", "leaf",
            "animal", "bird",
            "transport", "phone",
            "words", "chat",
            "life", "home",
            "nature", "leaf",
            "city", "phone",
            "animals", "bird"
    );

    @Resource
    private IReadAloudCategoryService readAloudCategoryService;

    @Resource
    private IReadAloudItemService readAloudItemService;

    @Resource
    private LocalFileStorageService localFileStorageService;

    @Override
    public List<ReadAloudCategoryApiResp> listEnabledCategories() {
        List<ReadAloudCategory> list = readAloudCategoryService.list(new LambdaQueryWrapper<ReadAloudCategory>()
                .eq(ReadAloudCategory::getIsDelete, 0)
                .eq(ReadAloudCategory::getEnabled, 1)
                .orderByAsc(ReadAloudCategory::getSortOrder)
                .orderByAsc(ReadAloudCategory::getId));
        return list.stream().map(this::toCategory).toList();
    }

    @Override
    public List<ReadAloudItemApiResp> listPublishedItems(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException("categoryId 不能为空");
        }
        ReadAloudCategory category = readAloudCategoryService.getOne(new LambdaQueryWrapper<ReadAloudCategory>()
                .eq(ReadAloudCategory::getId, categoryId)
                .eq(ReadAloudCategory::getIsDelete, 0)
                .eq(ReadAloudCategory::getEnabled, 1));
        if (category == null) {
            throw new ServiceException("分类不存在或未启用");
        }

        List<ReadAloudItem> list = readAloudItemService.list(new LambdaQueryWrapper<ReadAloudItem>()
                .eq(ReadAloudItem::getIsDelete, 0)
                .eq(ReadAloudItem::getStatus, "PUBLISHED")
                .eq(ReadAloudItem::getCategoryId, categoryId)
                .isNotNull(ReadAloudItem::getProcessedAudioAssetId)
                .orderByAsc(ReadAloudItem::getSortOrder)
                .orderByAsc(ReadAloudItem::getId));
        return list.stream().map(this::toItem).toList();
    }

    private ReadAloudCategoryApiResp toCategory(ReadAloudCategory category) {
        ReadAloudCategoryApiResp resp = new ReadAloudCategoryApiResp();
        resp.setId(category.getId());
        resp.setCategoryCode(category.getCategoryCode());
        resp.setName(category.getNameCn());
        resp.setCaption(StringUtils.hasText(category.getNameEn()) ? category.getNameEn() : "");
        resp.setIcon(resolveCategoryIcon(category.getCategoryCode()));
        if (category.getCoverAssetId() != null) {
            resp.setCoverUrl(localFileStorageService.buildPreviewUrl(category.getCoverAssetId()));
        }
        return resp;
    }

    private ReadAloudItemApiResp toItem(ReadAloudItem item) {
        ReadAloudItemApiResp resp = new ReadAloudItemApiResp();
        resp.setId(item.getId());
        resp.setCategoryId(item.getCategoryId());
        resp.setItemCode(item.getItemCode());
        resp.setTitle(item.getTitleCn());
        resp.setSubtitle(StringUtils.hasText(item.getDescriptionCn()) ? item.getDescriptionCn() : "");
        if (item.getImageAssetId() != null) {
            resp.setImageUrl(localFileStorageService.buildPreviewUrl(item.getImageAssetId()));
        }
        resp.setAudioAssetId(item.getProcessedAudioAssetId());
        if (item.getProcessedAudioAssetId() != null) {
            resp.setAudioUrl(localFileStorageService.buildPreviewUrl(item.getProcessedAudioAssetId()));
        }
        return resp;
    }

    private String resolveCategoryIcon(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return "chat";
        }
        String key = categoryCode.trim().toLowerCase(Locale.ROOT);
        return CATEGORY_ICON_MAP.getOrDefault(key, "chat");
    }
}
