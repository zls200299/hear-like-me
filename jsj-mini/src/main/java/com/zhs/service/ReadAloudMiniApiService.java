package com.zhs.service;

import com.zhs.response.readaloud.ReadAloudCategoryApiResp;
import com.zhs.response.readaloud.ReadAloudItemApiResp;

import java.util.List;

public interface ReadAloudMiniApiService {

    List<ReadAloudCategoryApiResp> listEnabledCategories();

    List<ReadAloudItemApiResp> listPublishedItems(Long categoryId);
}
