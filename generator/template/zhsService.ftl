package ${content.service.classPackage};

<#assign tableReMarks = "">
<#list content.entity.attrs as item>
    <#if item.field == "id">
        <#if content.table.remarks?? && content.table.remarks != "">
            <#assign tableReMarks = content.table.remarks>
        <#elseif item.remarks?? && item.remarks != "">
            <#assign tableReMarks = item.remarks>
        <#else>
            <#assign tableReMarks = content.entity.tableName>
        </#if>
        <#break>
    </#if>
</#list>

import ${content.entity.classPackage}.${content.entity.className};
import ${content.createDto.classPackage}.${content.createDto.className};

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */

public interface ${content.service.className} extends IService<${content.entity.className}> {
    R addOrUpdate(${content.createDto.className} ${content.createDto.className?uncap_first});
}
