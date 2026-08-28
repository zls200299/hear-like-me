package com.szmirren.models;

/**
 * @author zhenghuisheng
 * @date : 2023/3/7
 */
public class MongoTypeDto {
    private String column;
    private String type;

    public MongoTypeDto(String column, String type) {
        this.column = column;
        this.type = type;
    }

    public MongoTypeDto(){

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
