package ${content.controller.classPackage};

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


import ${content.service.classPackage}.${content.service.className};
import ${content.entity.classPackage}.${content.entity.className};
import ${content.createDto.classPackage}.${content.createDto.className};

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */
@RestController
@RequestMapping("${'/' + content.entity.tableName?replace('_','/')}")
@Api(value = "")
@Slf4j
public class ${content.controller.className} {

    @Resource
    private ${content.service.className}  ${content.service.className?uncap_first};

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<${content.entity.className}>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<${content.entity.className}> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<${content.entity.className}> lambda = new QueryWrapper<${content.entity.className}>().lambda();
        //此处可以拼条件
        lambda.eq(${content.entity.className}::getIsDelete,0);
        IPage<${content.entity.className}> pages =  ${content.service.className?uncap_first}.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<${content.entity.className}> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<${content.entity.className}> wrapper = new QueryWrapper<${content.entity.className}>()
                            .lambda().eq(${content.entity.className}::getId,id).eq(${content.entity.className}::getIsDelete,0);
        return R.ok(${content.service.className?uncap_first}.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<${content.entity.className}> query = new QueryWrapper<${content.entity.className}>().lambda().eq(${content.entity.className}::getId, id).eq(${content.entity.className}::getIsDelete, 0);
        ${content.entity.className} ${content.entity.className?uncap_first} = ${content.service.className?uncap_first}.getOne(query);
        if(ObjectUtils.isEmpty(${content.entity.className?uncap_first})) throw new ServiceException("该数据不存在或者已经被删除");
        ${content.entity.className?uncap_first}.setIsDelete(1);
        ${content.service.className?uncap_first}.updateById(${content.entity.className?uncap_first});
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ${content.createDto.className} ${content.createDto.className?uncap_first}){
        if (CollectionUtils.isEmpty(${content.createDto.className?uncap_first}.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<${content.entity.className}> list = new ArrayList<>();
        ${content.createDto.className?uncap_first}.getIdList().stream().forEach(id ->{
        ${content.entity.className} ${content.entity.className?uncap_first} = ${content.service.className?uncap_first}.getById(id);
            if (ObjectUtils.isEmpty(${content.entity.className?uncap_first})) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == ${content.entity.className?uncap_first}.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            ${content.entity.className?uncap_first}.setIsDelete(1);
            list.add(${content.entity.className?uncap_first});
        });
        ${content.service.className?uncap_first}.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody ${content.createDto.className} ${content.createDto.className?uncap_first}){
        return ${content.service.className?uncap_first}.addOrUpdate(${content.createDto.className?uncap_first});
    }

}
