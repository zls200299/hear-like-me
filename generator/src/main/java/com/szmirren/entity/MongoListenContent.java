package com.szmirren.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhenghuisheng
 * @date : 2023/2/27
 */
public class MongoListenContent {
    /** SqlAssist类的包名 */
    private String classPackage;
    /** SqlAssist类的类型 */
    private String className;


    /**
     * 初始化
     */
    public MongoListenContent() {
        super();
    }

    /**
     * 通过包名与类名初始化
     *
     * @param classPackage
     * @param className
     */
    public MongoListenContent(String classPackage, String className) {
        super();
        this.classPackage = classPackage;
        this.className = className ;
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


    @Override
    public String toString() {
        return "SQLContent [classPackage=" + classPackage + ", className=" + className +  "]";
    }
}
