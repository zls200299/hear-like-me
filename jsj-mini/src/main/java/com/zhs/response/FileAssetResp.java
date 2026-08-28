package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 *
 * @author 
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "FileAssetResp")
public class FileAssetResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "文件所属用户；未登录/公共素材可为空")
        private Long ownerUserId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "派生文件的上游文件ID")
        private Long parentAssetId;

        @ApiModelProperty(value = "AUDIO_SOURCE/AUDIO_NORMALIZED/AUDIO_OUTPUT/SAMPLE_AUDIO/READ_IMAGE/READ_AUDIO/READ_AUDIO_PROCESSED/CONTENT_IMAGE/OTHER", required = true)
        private String assetType;

        @ApiModelProperty(value = "LOCAL/COS/OSS/S3", required = true)
        private String storageProvider;

        @ApiModelProperty(value = "对象存储 Bucket；LOCAL 时为空字符串", required = true)
        private String bucketName;

        @ApiModelProperty(value = "对象存储 Key 或服务器本地相对路径", required = true)
        private String objectKey;

        @ApiModelProperty(value = "原始文件名")
        private String originalFilename;

        @ApiModelProperty(value = "扩展名，不含点")
        private String fileExt;

        @ApiModelProperty(value = "MIME 类型")
        private String mimeType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "文件大小（字节）")
        private Long fileSize;

        @ApiModelProperty(value = "SHA-256")
        private String sha256;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "音频时长（毫秒）")
        private Long durationMs;

        @ApiModelProperty(value = "音频采样率 Hz")
        private Integer sampleRate;

        @ApiModelProperty(value = "音频声道数")
        private Integer audioChannels;

        @ApiModelProperty(value = "PCM 位深")
        private Integer bitDepth;

        @ApiModelProperty(value = "编码：pcm_s16le/aac/mp3 等")
        private String audioCodec;

        @ApiModelProperty(value = "图片宽度 px")
        private Integer imageWidth;

        @ApiModelProperty(value = "图片高度 px")
        private Integer imageHeight;

        @ApiModelProperty(value = "PRIVATE/PUBLIC", required = true)
        private String accessMode;

        @ApiModelProperty(value = "ACTIVE/EXPIRED/DELETED", required = true)
        private String status;

        @ApiModelProperty(value = "临时文件自动过期时间；NULL=长期保存")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date expireTime;

        @ApiModelProperty(value = "软删除时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date deleteTime;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "逻辑删除：0否 1是", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
