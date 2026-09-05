package com.zhs.response.readaloud;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class ReadAloudCategoryApiResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String categoryCode;

    private String name;

    private String caption;

    private String coverUrl;

    /** 前端本地 SVG 图标名（无封面时用） */
    private String icon;
}
