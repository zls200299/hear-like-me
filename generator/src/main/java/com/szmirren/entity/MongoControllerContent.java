package com.szmirren.entity;

import com.szmirren.models.MongoTypeDto;

import java.util.List;

/**
 * @author zhenghuisheng
 * @date : 2023/2/27
 */
public class MongoControllerContent {
    /** SqlAssist类的包名 */
    private String classPackage;
    /** SqlAssist类的类型 */
    private String className;

    private String collectionName;

    /**
     * 初始化
     */
    public MongoControllerContent() {
        super();
    }

    /**
     * 通过包名与类名初始化
     *
     * @param classPackage
     * @param className
     */
    public MongoControllerContent(String classPackage, String className,String collectionName) {
        super();
        this.classPackage = classPackage;
        this.className = className ;
        this.collectionName = collectionName;
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
        return "SQLContent [classPackage=" + classPackage + ", className=" + className + "]";
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
