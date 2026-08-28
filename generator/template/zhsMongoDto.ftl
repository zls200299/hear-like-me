package ${content.mongoDto.classPackage};

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ${content.author!}
 * @since ${content.now!}
 */

@Document("${content.mongoDto.tableName}") //对应集合中的某个文档
@Data
@ApiModel
public class ${content.mongoDto.className} implements Serializable {
<#list content.mongoDto.attList as item>
    <#if item.column == "_id">
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id; //映射中的_id
    <#elseif item.type == "Date">
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field
    private Date ${item.column};
    <#else>
    @Field
    private ${item.type} ${item.column};
    </#if>
</#list>

//=====================自定义字段，不会加到mongodb中====================

    @Transient
    private List<${content.mongoDto.className}> ${content.mongoDto.className?uncap_first}List;

    @Transient
    private List<String> idList;
}
