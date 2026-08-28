package com.szmirren.entity;

import com.szmirren.models.MongoTypeDto;

import java.util.List;
import java.util.Map;

/**
 * @author zhenghuisheng
 * @date : 2023/2/27
 */
public class MongoDtoContent {
    /** SqlAssist类的包名 */
    private String classPackage;
    /** SqlAssist类的类型 */
    private String className;

    private List<MongoTypeDto> attList;

    private String tableName;

    /**
     * 初始化
     */
    public MongoDtoContent() {
        super();
    }

    /**
     * 通过包名与类名初始化
     *
     * @param classPackage
     * @param className
     */
    public MongoDtoContent(String classPackage, String className ,List<MongoTypeDto> attrs,String tableName) {
        super();
        this.classPackage = classPackage;
        this.className = className ;
        this.attList = attrs;
        this.tableName = tableName;
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

    public List<MongoTypeDto> getAttList() {
        return attList;
    }

    public void setAttList(List<MongoTypeDto> attList) {
        this.attList = attList;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
