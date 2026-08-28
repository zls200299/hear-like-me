package ${content.mongoListen.classPackage};

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;


/**
 * 在创建集合时，过滤掉 _class字段
 * @author ${content.author!}
 * @since ${content.now!}
 */


@Configuration
public class ${content.mongoListen.className} implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    MongoTemplate oneMongoTemplate;

    private static final String TYPEKEY = "_class";

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        MongoConverter converter = oneMongoTemplate.getConverter();
        if (converter.getTypeMapper().isTypeKey(TYPEKEY)) {
            ((MappingMongoConverter) converter).setTypeMapper(new DefaultMongoTypeMapper(null));
        }
    }
}
