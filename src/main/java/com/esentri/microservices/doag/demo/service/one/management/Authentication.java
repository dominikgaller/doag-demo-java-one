package com.esentri.microservices.doag.demo.service.one.management;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;


public class Authentication extends AbstractVerticle {
  
  private static final String LOGIN_REQUEST_TOPIC = "esentri.login.request";

  private static final String LOGOUT_REQUEST_TOPIC = "esentri.logout";
  
  private static final String SHARED_DBNAME = "users";
  
  private JDBCClient jdbcClient;
  
  private EventBus eventBus;
  
  private Logger logger;
  
  public void start (Future<Void> completionFuture) {
    
    logger = LoggerFactory.getLogger(Authentication.class);
    logger.info("Starting authentication service.");
    this.eventBus = vertx.eventBus();
    this.jdbcClient = JDBCClient.createShared(vertx, new JsonObject(), SHARED_DBNAME);
    eventBus.consumer(LOGIN_REQUEST_TOPIC, this::loginHandler);
    eventBus.consumer(LOGOUT_REQUEST_TOPIC, this::logoutHandler);
    logger.info("Authentication services started successfully.");
    completionFuture.complete();
  }
  
  private void loginHandler (Message<String> msg) {
    
    logger.info("Request for login received.");
    logger.info("Payload for login request is: " + msg.body());
    JsonObject loginCreds = new JsonObject(msg.body());
    String sessionId = extractSessionId(loginCreds);
    if (loginCreds.containsKey("username") && loginCreds.containsKey("password")) {
      String name = loginCreds.getString("username");
      
      Future<Boolean> exists = Future.factory.future();
      exists.setHandler(new AuthorizeUserHandler(jdbcClient, eventBus, sessionId, loginCreds));
      userExists(name, exists);
    } else {
      throw new IllegalArgumentException("Credential combination is wrong");
    }
    
  }
  
  private void userExists (String name, Future<Boolean> exists) {
    
    this.jdbcClient.getConnection(conn -> {
      if (conn.failed()) {
        throw new IllegalStateException(conn.cause().getMessage());
      }
      SQLConnection connection = conn.result();
      checkUsername(connection, name, exists);
    });
  }
  
  private void checkUsername (SQLConnection conn, String name, Future<Boolean> exists) {
    
    String query = "SELECT Name FROM User WHERE Name = ?";
    JsonArray params = new JsonArray().add(name);
    conn.queryWithParams(query, params, rs -> {
      if (rs.failed()) {
        throw new RuntimeException(rs.cause().getMessage());
      }
      int count = rs.result().getResults().size();
      if (count > 0) {
        exists.complete(true);
      } else {
        exists.complete(false);
      }
      conn.close();
    });
  }
  
  private void logoutHandler (Message<String> msg) {
    
    logger.info("Request for logout received.");
    logger.info("Payload for logout request is: " + msg.body());
    JsonObject json = new JsonObject().put("logout", true);
    logger.info(json.toString());
    msg.reply(json.toString());
  }
  
  private String extractSessionId (JsonObject json) {
    
    if (json.containsKey("sessionId")) {
      return json.getString("sessionId");
    } else {
      throw new IllegalArgumentException("Message contains no sessionId");
    }
  }
  
}
