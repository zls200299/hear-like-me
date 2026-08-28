package com.szmirren.view;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.szmirren.common.JavaType;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author zhenghuisheng
 * @date : 2023/3/3
 */
public class Test {

    public static MongoDatabase returnMongoDatabase(){
        List<ServerAddress> adds = new ArrayList<>();
        //ServerAddress()两个参数分别为 服务器地址 和 端口
        //mongodb://bimmongo:bimmongo&2023@175.178.157.156:27011/articledb
        ServerAddress serverAddress = new ServerAddress("175.178.157.156", 27011);
        adds.add(serverAddress);

        //MongoCredential.createScramSha1Credential()三个参数分别为 用户名 数据库名称 密码
        MongoCredential mongoCredential = MongoCredential.createScramSha1Credential("bimmongo", "articledb", "bimmongo&2023".toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(b -> b.hosts(adds))
                .credential(mongoCredential)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        //连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("articledb");
        // 连接collection
        MongoCollection<Document> collection = mongoDatabase.getCollection("user");
        //查询第一个文档
        Document myDoc = collection.find().first();
        //得到文件内容 json串
        String json = myDoc.toJson();
        System.out.println(json);
        //返回连接数据库对象
        return mongoDatabase;
    }

    public static MongoDatabase getMongoDB(){
        MongoClient mongoClient = MongoClients.create("mongodb://1:1");
        MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
        MongoIterable<String> strings = mongoDatabase.listCollectionNames();
        System.out.println(strings.first());
        // 连接collection，如果集合存在，但是文档为空，那么该集合默认不显示，也会查询不到
        MongoCollection<Document> collection = mongoDatabase.getCollection("student");

        //查询第一个文档,默认第一条数据为完整数据，包含全部字段
        Document myDoc = collection.find().first();
        //得到文件内容 json串
        String json = myDoc.toJson();
        System.out.println(json);
        List<String> list = new ArrayList<>(myDoc.keySet());
        list.remove("_id");
        System.out.println("list集合的长度为：" + list.size());
        if (list != null || list.size() != 0){
            list.stream().forEach(param ->{
                String str = JavaType.mongoDbTypeToJavaType(myDoc.get(param));
                System.out.println(str);
            });
        }

        System.out.println(myDoc.get("age"));
        return mongoDatabase;
    }

    public static void main(String[] args) {
        Path path = Paths.get("./target/classes/template/");
        boolean exists = Files.exists(path);
        try {
            Stream<Path> list = Files.list(path);
            if (list != null){
                list.forEach(e-> System.out.println(e));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(path);
        System.out.println("打包数据" + Files.exists(Paths.get("target/jfx/app/template")));
        System.out.println("测试数据" + Files.exists(Paths.get("template/")));
    }
}
