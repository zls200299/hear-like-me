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
@ApiModel(value = "UserLoginSessionResp")
public class UserLoginSessionResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "用户ID")
        private Long userId;

        @ApiModelProperty(value = "登录 Token", required = true)
        private String token;

        @ApiModelProperty(value = "登录时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date loginTime;

        @ApiModelProperty(value = "过期时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date expireTime;

        @ApiModelProperty(value = "设备类型：MINIPROGRAM/WEB/OTHER")
        private String deviceType;

        @ApiModelProperty(value = "客户端IP")
        private String clientIp;

        @ApiModelProperty(value = "状态：0失效 1有效", required = true)
        private Integer status;

        @ApiModelProperty(value = "删除标志：0未删除 1已删除", required = true)
        private Integer isDelete;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
