package com.zhs.controller.api;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.readaloud.ReadAloudCategoryApiResp;
import com.zhs.response.readaloud.ReadAloudItemApiResp;
import com.zhs.service.ReadAloudMiniApiService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序点读 API
 */
@RestController
@RequestMapping("/api/read-aloud")
@NoLoginRequest
public class ReadAloudApiController {

    @Resource
    private ReadAloudMiniApiService readAloudMiniApiService;

    @GetMapping("/categories")
    public R<List<ReadAloudCategoryApiResp>> listCategories() {
        return R.ok(readAloudMiniApiService.listEnabledCategories());
    }

    @GetMapping("/items")
    public R<List<ReadAloudItemApiResp>> listItems(@RequestParam("categoryId") Long categoryId) {
        return R.ok(readAloudMiniApiService.listPublishedItems(categoryId));
    }
}
