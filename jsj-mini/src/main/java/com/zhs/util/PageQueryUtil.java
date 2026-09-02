package com.zhs.util;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一分页查询。当 MyBatis-Plus 分页插件未生效时（total=0 却返回全表），自动降级为 count + 内存切片。
 */
public final class PageQueryUtil {

    private PageQueryUtil() {
    }

    public static <T> Map<String, Object> queryPage(
            IService<T> service,
            long currentPage,
            long pageSize,
            Wrapper<T> wrapper) {
        long current = currentPage > 0 ? currentPage : 1;
        long size = pageSize > 0 ? pageSize : 10;

        Page<T> page = new Page<>(current, size);
        IPage<T> mpResult = service.page(page, wrapper);

        long total = mpResult.getTotal();
        List<T> records = mpResult.getRecords();

        if (needsManualPagination(mpResult, size)) {
            total = service.count(wrapper);
            List<T> all = service.list(wrapper);
            int from = (int) Math.min((current - 1) * size, all.size());
            int to = (int) Math.min(from + size, all.size());
            records = from < to ? all.subList(from, to) : List.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", current);
        result.put("size", size);
        result.put("pages", size > 0 ? (total + size - 1) / size : 0);
        return result;
    }

    private static <T> boolean needsManualPagination(IPage<T> page, long size) {
        List<T> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return false;
        }
        return page.getTotal() == 0 || records.size() > size;
    }
}
