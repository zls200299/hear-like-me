package ${content.entity.classPackage};

<#assign tableReMarks = "">
<#list content.entity.attrs as item>
    <#if item.field == "id">
        <#if content.table.remarks?? && content.table.remarks != "">
            <#assign tableReMarks = content.table.remarks>
        <#elseif item.remarks?? && item.remarks != "">
            <#assign tableReMarks = item.remarks>
        <#else>
            <#assign tableReMarks = content.entity.tableName + "实体类">
        </#if>
    </#if>
</#list>

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
 * ${tableReMarks}
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */

@Data
@ApiModel
public class ${content.entity.className} implements Serializable {
//===========================数据库字段================================
    <#list content.entity.attrs as item>

    <#if item.field == "id">
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long ${item.field};

    <#elseif item.javaType = "java.util.Date">
    @ApiModelProperty(value = "${item.remarks}")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    <#if item.field == "createTime">
    @TableField(fill = FieldFill.INSERT)
    </#if>
    <#if item.field == "updateTime">
    @TableField(fill = FieldFill.INSERT_UPDATE)
    </#if>
    private Date ${item.field};

    <#elseif item.javaType = "Long">
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "${item.remarks}")
    private Long ${item.field};

    <#else>
    @ApiModelProperty(value = "${item.remarks}")
    private ${item.javaType} ${item.field};
    </#if>
    </#list>


//===========================自定义字段=================================

}
