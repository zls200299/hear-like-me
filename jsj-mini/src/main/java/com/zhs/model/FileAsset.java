package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;


/**
 * 统一文件资源表
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class FileAsset implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "文件所属用户；未登录/公共素材可为空")
    private Long ownerUserId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "派生文件的上游文件ID")
    private Long parentAssetId;


    @ApiModelProperty(value = "AUDIO_SOURCE/AUDIO_NORMALIZED/AUDIO_OUTPUT/SAMPLE_AUDIO/READ_IMAGE/READ_AUDIO/READ_AUDIO_PROCESSED/CONTENT_IMAGE/OTHER")
    private String assetType;

    @ApiModelProperty(value = "LOCAL/COS/OSS/S3")
    private String storageProvider;

    @ApiModelProperty(value = "对象存储 Bucket；LOCAL 时为空字符串")
    private String bucketName;

    @ApiModelProperty(value = "对象存储 Key 或服务器本地相对路径")
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

    @ApiModelProperty(value = "PRIVATE/PUBLIC")
    private String accessMode;

    @ApiModelProperty(value = "ACTIVE/EXPIRED/DELETED")
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
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "逻辑删除：0否 1是")
    private Integer isDelete;


//===========================自定义字段=================================

}
