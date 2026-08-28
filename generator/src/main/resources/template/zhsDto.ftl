package ${content.createDto.classPackage};

<#assign tableReMarks = "">
<#list content.entity.attrs as item>
    <#if item.field == "id">
        <#if content.table.remarks?? && content.table.remarks != "">
            <#assign tableReMarks = content.table.remarks >
        <#elseif item.remarks?? && item.remarks != "">
            <#assign tableReMarks = item.remarks>
        <#else>
            <#assign tableReMarks = content.entity.tableName>
        </#if>
        <#break>
    </#if>
</#list>
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
 * @author ${content.author!}
 * @since ${content.now!}
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "${content.createDto.className}")
public class ${content.createDto.className} implements Serializable {
        //表字段
<#list content.createDto.attrs as item>
    <#if item.field == "id">
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long ${item.field};

    <#elseif item.javaType = "java.util.Date">
        @ApiModelProperty(value = "${item.remarks}")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date ${item.field};

    <#elseif item.javaType = "Long">
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "${item.remarks}")
        private Long ${item.field};

    <#else>
        @ApiModelProperty(value = "${item.remarks!}"<#if item.nullable=false>, required = true</#if>)
        private ${item.javaType} ${item.field};

    </#if>
</#list>
        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
