<#--package ${content.swagger.classPackage};-->

<#--import io.swagger.annotations.Api;-->
<#--import org.springframework.context.annotation.Bean;-->
<#--import org.springframework.beans.factory.annotation.Value;-->
<#--import org.springframework.context.annotation.Configuration;-->
<#--import springfox.documentation.builders.ApiInfoBuilder;-->
<#--import springfox.documentation.builders.PathSelectors;-->
<#--import springfox.documentation.builders.RequestHandlerSelectors;-->
<#--import springfox.documentation.service.ApiInfo;-->
<#--import springfox.documentation.spi.DocumentationType;-->
<#--import springfox.documentation.spring.web.plugins.Docket;-->
<#--import springfox.documentation.swagger2.annotations.EnableSwagger2;-->
<#--import io.github.swagger2markup.Swagger2MarkupConfig;-->
<#--import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;-->
<#--import io.github.swagger2markup.Swagger2MarkupConverter;-->
<#--import io.github.swagger2markup.markup.builder.MarkupLanguage;-->
<#--import io.github.swagger2markup.GroupBy;-->
<#--import io.github.swagger2markup.Language;-->
<#--import org.junit.jupiter.api.Test;-->
<#--import java.net.URL;-->
<#--import java.nio.file.Paths;-->

<#--/**-->
<#--* Swagger使用的配置文件 该文件在代码生成时不会覆盖-->
<#--* @author ${content.author!}-->
<#--* @since ${content.now!}-->
<#--*/-->

<#--@Configuration-->
<#--@EnableSwagger2-->
<#--public class ${content.swagger.className} {-->

<#--    @Value("${content.swagger.port}")-->
<#--    public Integer port;-->

<#--    @Bean-->
<#--    public Docket createRestApi(){-->
<#--        return new Docket(DocumentationType.SWAGGER_2)-->
<#--            .apiInfo(apiInfo())-->
<#--            .select()-->
<#--            .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))-->
<#--            .paths(PathSelectors.any())-->
<#--            .build();-->
<#--    }-->

<#--    //基本信息的配置，信息会在api文档上显示-->
<#--    // 浏览器访问地址：http://localhost:端口号/swagger-ui.html-->
<#--    private ApiInfo apiInfo(){-->
<#--        return new ApiInfoBuilder()-->
<#--            .title("模型引擎服务接口文档")-->
<#--            .description("模型引擎服务接口文档")-->
<#--            .termsOfServiceUrl("http://localhost:" + port + "/swagger")-->
<#--            .version("1.0")-->
<#--            .build();-->

<#--    }-->

<#--    /**-->
<#--    * 导出接口文档到docs目录下的markdown文件中,需要手动配置端口号-->
<#--    * @throws Exception-->
<#--    */-->
<#--    @Test-->
<#--    public void generateMarkdownDocsToFile() throws Exception {-->
<#--        //输出Markdown到单文件-->
<#--        Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder()-->
<#--            .withMarkupLanguage(MarkupLanguage.MARKDOWN)-->
<#--            .withOutputLanguage(Language.ZH)-->
<#--            .withPathsGroupedBy(GroupBy.TAGS)-->
<#--            .withGeneratedExamples()-->
<#--            .withoutInlineSchema()-->
<#--            .build();-->

<#--    Swagger2MarkupConverter.from(new URL("http://localhost:8883/v2/api-docs"))-->
<#--            .withConfig(config)-->
<#--            .build()-->
<#--            .toFile(Paths.get("src/main/resources/docs/markdown"));-->
<#--    }-->
<#--}-->
