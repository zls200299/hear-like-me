package ${content.mongoDto.classPackage};

import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.io.Serializable;


/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */

@Data
@ApiModel
public class ${content.mongoDto.className} implements Serializable {
<#list content.mongoDto.attList as item>
    <#if item.column == "id">
    private String ${item.column};

    <#elseif item.type = "java.util.Date">
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    <#if item.column == "createTime">
    @TableField(fill = FieldFill.INSERT_UPDATE)
    </#if>
    <#if item.column == "updateTime">
    @TableField(fill = FieldFill.UPDATE)
    </#if>
    private Date ${item.column};
    <#else>
    private ${item.type} ${item.column};
    </#if>
</#list>
}
