package ${content.dao.classPackage};

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
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * ${tableReMarks} 的dao
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */
@Mapper
@Repository
public interface ${content.dao.className} extends BaseMapper<${content.entity.className}> {

}
