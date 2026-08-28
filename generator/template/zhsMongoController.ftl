package ${content.mongoController.classPackage};

import ${content.mongoDto.classPackage}.${content.mongoDto.className};


import com.zhs.util.R;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.zhs.exception.ServiceException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Date;
import java.util.List;



/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */

@Data
@Api(value = "")
@RestController
@RequestMapping("${'/'+content.mongoController.collectionName}Mongo")
public class ${content.mongoController.className} implements Serializable {
    @Resource
    private MongoTemplate mongoTemplate;

    @ApiOperation(value = "插入或者更新数据")
    @PostMapping("/insertOrUpdate")
    public R insertOrUpdate(@RequestBody ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}){
        Long id = ${content.mongoDto.className?uncap_first}.getId();
        //新增数据时，给id默认值
        if (id == null) {
            ${content.mongoDto.className?uncap_first}.setId(IdWorker.getId());
            ${content.mongoDto.className?uncap_first}.setIsDelete(0);
            ${content.mongoDto.className?uncap_first}.setCreateTime(new Date());
            ${content.mongoDto.className?uncap_first}.setUpdateTime(new Date());
        }
        mongoTemplate.save(${content.mongoDto.className?uncap_first});
        return R.ok();
    }

    @ApiOperation(value = "批量插入数据")
    @PostMapping("/insertList")
    public R insertList(@RequestBody ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}){
        List<${content.mongoDto.className}> ${content.mongoDto.className?uncap_first}List = ${content.mongoDto.className?uncap_first}.get${content.mongoDto.className}List();
        ${content.mongoDto.className?uncap_first}List.stream().forEach(p -> {
            p.setId(IdWorker.getId());
            p.setIsDelete(0);
            p.setCreateTime(new Date());
            p.setUpdateTime(new Date());
        });
        mongoTemplate.insertAll(${content.mongoDto.className?uncap_first}List);
        return R.ok("数据批量插入成功");
    }

    @ApiOperation(value = "根据id查询数据文档信息")
    @GetMapping("/getById")
    public R getById(@RequestParam("id") String id){
        //根据id获取文档
        ${content.mongoDto.className} ${content.mongoDto.className?uncap_first} = mongoTemplate.findById(Long.parseLong(id), ${content.mongoDto.className}.class);
        return R.ok(${content.mongoDto.className?uncap_first});
    }

    @ApiOperation(value = "分页查询文档信息")
    @GetMapping("/getByPage")
    public R getByPage(@RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
                       @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //拼接条件
        Query query = new Query();
        //过滤掉被逻辑删除的数据
        query.addCriteria(Criteria.where("isDelete").is(0));
        //构建分页工具
        Pageable pageable = PageRequest.of(currentPage-1, pageSize);
        query.with(pageable);
        //默认通过创建时间降序排序
        query.with(Sort.by(Sort.Order.desc("createTime")));
        //获取全部的文档，query条件为空默认查所有
        List<${content.mongoDto.className}> pageList = mongoTemplate.find(query, ${content.mongoDto.className}.class);
        long total = mongoTemplate.count(query, ${content.mongoDto.className}.class);
        Page<${content.mongoDto.className}> page = new PageImpl(pageList,pageable,total);
        return R.ok(page);
    }

    @ApiOperation(value = "根据id逻辑删除删除文档")
    @PostMapping("/deleteByUpdate/{id}")
    public R deleteIdByUpdate(@PathVariable("id") String id){
        ${content.mongoDto.className} ${content.mongoDto.className?uncap_first} = mongoTemplate.findById(Long.parseLong(id),${content.mongoDto.className}.class);
        if (ObjectUtils.isEmpty(${content.mongoDto.className?uncap_first})) throw new ServiceException("id为" + id + "的数据不存在");
        Query query = new Query(Criteria.where("id").is(Long.parseLong(id)));
        Update update = new Update();
        update.set("isDelete",1).set("updateTime",new Date());
        //更新
        mongoTemplate.updateFirst(query,update,${content.mongoDto.className}.class);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "批量逻辑删除数据")
    @PostMapping("/deleteByUpdateIds")
    public R deleteByUpdateIds(@RequestBody ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}){
        if (CollectionUtils.isEmpty(${content.mongoDto.className?uncap_first}.getIdList())) throw new ServiceException("要删除的id不能为空!");
            ${content.mongoDto.className?uncap_first}.getIdList().stream().forEach(id ->{
            ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}Param = mongoTemplate.findById(Long.parseLong(id), ${content.mongoDto.className}.class);
            if (ObjectUtils.isEmpty(${content.mongoDto.className?uncap_first}Param)) throw new ServiceException("id为" + id + "的数据不存在");
            Query query = new Query(Criteria.where("id").is(Long.parseLong(id)));
            Update update = new Update();
            update.set("isDelete",1).set("updateTime",new Date());
            //更新
            mongoTemplate.updateFirst(query,update,${content.mongoDto.className}.class);
        });
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "根据id删除文档")
    @PostMapping("/delete/{id}")
    public R deleteById(@PathVariable("id") String id){
        ${content.mongoDto.className} ${content.mongoDto.className?uncap_first} = mongoTemplate.findById(Long.parseLong(id), ${content.mongoDto.className}.class);
        if (ObjectUtils.isEmpty(${content.mongoDto.className?uncap_first})) throw new ServiceException("id为" + id + "的数据不存在");
        //根据id删除
        mongoTemplate.remove(${content.mongoDto.className?uncap_first});
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}){
        if (CollectionUtils.isEmpty(${content.mongoDto.className?uncap_first}.getIdList())) throw new ServiceException("要删除的id不能为空!");
        ${content.mongoDto.className?uncap_first}.getIdList().stream().forEach(id ->{
            ${content.mongoDto.className} ${content.mongoDto.className?uncap_first}Param = mongoTemplate.findById(Long.parseLong(id), ${content.mongoDto.className}.class);
            if (ObjectUtils.isEmpty(${content.mongoDto.className?uncap_first}Param)) throw new ServiceException("id为" + id + "的数据不存在");
            mongoTemplate.remove(${content.mongoDto.className?uncap_first}Param);
        });
        return R.ok("数据删除成功");
    }
}