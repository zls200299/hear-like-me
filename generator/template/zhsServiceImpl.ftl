package ${content.serviceImpl.classPackage};

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

import ${content.entity.classPackage}.${content.entity.className};
import ${content.dao.classPackage}.${content.dao.className};
import ${content.service.classPackage}.${content.service.className};
import ${content.createDto.classPackage}.${content.createDto.className};


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */
@Service
@Slf4j
public class ${content.serviceImpl.className} extends ServiceImpl< ${content.dao.className}, ${content.entity.className}> implements ${content.service.className} {

    @Resource
    private ${content.dao.className} ${content.dao.className?uncap_first};

    @Override
    public R addOrUpdate(${content.createDto.className} ${content.createDto.className?uncap_first}) {
        if (BeanUtil.isEmpty(${content.createDto.className?uncap_first})) throw new ServiceException("数据不能为空");
        ${content.entity.className} ${content.entity.className?uncap_first} = new ${content.entity.className}();
        BeanUtil.copyProperties(${content.createDto.className?uncap_first},${content.entity.className?uncap_first});
        if (${content.createDto.className?uncap_first}.getId() == null){
            ${content.dao.className?uncap_first}.insert(${content.entity.className?uncap_first});
            return R.ok("数据插入成功");
        }else {
            ${content.dao.className?uncap_first}.updateById(${content.entity.className?uncap_first});
            return R.ok("数据更新成功");
        }
    }
}