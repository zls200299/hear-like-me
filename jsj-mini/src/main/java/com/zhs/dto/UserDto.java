package com.zhs.dto;

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
@ApiModel(value = "UserDto")
public class UserDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "用户昵称")
        private String nickname;

        @ApiModelProperty(value = "头像地址")
        private String avatar;

        @ApiModelProperty(value = "个人简介")
        private String bio;

        @ApiModelProperty(value = "状态：0禁用 1正常", required = true)
        private Integer status;

        @ApiModelProperty(value = "来源：1后台创建 2手机号注册 3微信小程序 4微信公众号/网页 5邮箱注册", required = true)
        private Integer sourceType;

        @ApiModelProperty(value = "注册时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date registerTime;

        @ApiModelProperty(value = "最后活跃时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date lastActiveTime;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "逻辑删除：0未删 1已删", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
