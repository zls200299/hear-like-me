package com.szmirren.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhenghuisheng
 * @date : 2023/2/27
 */
public class SwaggerContent {
    /** SqlAssist类的包名 */
    private String classPackage;
    /** SqlAssist类的类型 */
    private String className;
    private String port;
    /** SqlAssist类的配置文件 */
    private Map<String, Object> item = new HashMap<>();

    /**
     * 初始化
     */
    public SwaggerContent() {
        super();
    }

    /**
     * 通过包名与类名初始化
     *
     * @param classPackage
     * @param className
     */
    public SwaggerContent(String classPackage, String className,String port) {
        super();
        this.classPackage = classPackage;
        this.className = className ;
        this.port = port;
    }




    public String getClassPackage() {
        return classPackage;
    }

    public void setClassPackage(String classPackage) {
        this.classPackage = classPackage;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Map<String, Object> getItem() {
        return item;
    }

    public void setItem(Map<String, Object> item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "SQLContent [classPackage=" + classPackage + ", className=" + className + ", item=" + item + "]";
    }
}
