package com.zhs.response.readaloud;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class ReadAloudItemApiResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    private String itemCode;

    private String title;

    private String subtitle;

    private String imageUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long audioAssetId;

    private String audioUrl;
}
