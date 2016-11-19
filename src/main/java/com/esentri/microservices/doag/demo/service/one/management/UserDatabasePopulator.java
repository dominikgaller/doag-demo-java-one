package com.esentri.microservices.doag.demo.service.one.management;

import com.esentri.microservices.doag.demo.service.one.IdeRunner;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class UserDatabasePopulator {
  
  private Vertx vertxInstance;
  
  private String dbName;
  
  private static final String USERS_DATA_INFORMATION_RESOURCE_NAME = "users.json";
  
  UserDatabasePopulator (Vertx vertx, String dbname) {
    
    this.setVertxInstance(vertx);
    this.setDBName(dbname);
  }
  
  void populateDatabase () {
    
    initDBClient();
  }
  
  private void initDBClient () {
    
    final JDBCClient client = JDBCClient.createShared(this.vertxInstance, createConfig(), this.dbName);
    client.getConnection(conn -> {
      if (conn.failed()) {
        throw new IllegalStateException(conn.cause().getMessage());
      }
      setUpDB(conn);
    });
  }
  
  private void setUpDB (AsyncResult<SQLConnection> conn) {
    
    createUserDB(conn);
    insertUsers(conn);
  }
  
  private void createUserDB (AsyncResult<SQLConnection> conn) {
    
    final SQLConnection connection = conn.result();
    connection.execute("create table user(id integer identity primary key, name varchar(255), password varchar(255))", res -> {
      if (res.failed()) {
        System.out.println("cannot create table user");
        throw new IllegalStateException(conn.cause().getMessage());
      }
      connection.close();
    });
  }
  
  private void insertUsers (AsyncResult<SQLConnection> conn) {
    
    JsonArray arr = readJsonArrayFile(USERS_DATA_INFORMATION_RESOURCE_NAME);
    arr.forEach(elem -> {
      JsonObject val = new JsonObject(elem.toString());
      insertUser(conn, val.getString("name"), val.getString("password"));
    });
  }
  
  private void insertUser (AsyncResult<SQLConnection> conn, String name, String password) {
    
    SQLConnection connection = conn.result();
    connection.execute("insert into user (name, password) values ('" + name + "','" + password + "')", res -> {
      if (res.failed()) {
        throw new IllegalStateException(res.cause().getMessage());
      }
      connection.close();
    });
  }
  
  private JsonArray readJsonArrayFile (String path) {
    
    String content = readDataFile(path);
    return new JsonArray(content);
  }
  
  private JsonObject createConfig () {
    
    return new JsonObject().put("url", "jdbc:hsqldb:mem:" + this.dbName + "?shutdown=false")
            .put("driver_class", "org.hsqldb.jdbcDriver").put("max_pool_size", 30);
  }
  
  private void setVertxInstance (Vertx vertxInstance) {
    
    this.vertxInstance = vertxInstance;
  }
  
  private void setDBName (String dbName) {
    
    this.dbName = dbName;
  }
  
  private static String readDataFile (String filename) {
    
    ClassLoader classLoader = IdeRunner.class.getClassLoader();
    try (InputStream in = classLoader.getResourceAsStream(filename);
         BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String str;
      StringBuilder sb = new StringBuilder(8192);
      while ((str = r.readLine()) != null) {
        sb.append(str);
      }
      return sb.toString();
    } catch (IOException ioe) {
      throw new IllegalStateException("Error while reading input config");
    }
  }
  
}
