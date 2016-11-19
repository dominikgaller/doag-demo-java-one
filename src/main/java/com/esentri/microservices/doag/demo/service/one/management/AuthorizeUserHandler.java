package com.esentri.microservices.doag.demo.service.one.management;


import com.esentri.microservices.doag.demo.service.one.management.entities.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;

class AuthorizeUserHandler implements Handler<AsyncResult<Boolean>> {
  
  private static final String LOGIN_REPLY_TOPIC = "esentri.login.reply";
  
  private String usersAssignedSessionId;
  
  private JsonObject loginCredentials;
  
  private JDBCClient jdbcClient;
  
  private EventBus eventBus;
  
  private Logger logger;
  
  AuthorizeUserHandler (JDBCClient client, EventBus eb, String sessionId, JsonObject loginCreds) {
    
    logger = LoggerFactory.getLogger(AuthorizeUserHandler.class);
    this.usersAssignedSessionId = sessionId;
    this.loginCredentials = loginCreds;
    this.eventBus = eb;
    this.jdbcClient = client;
  }
  
  @Override
  public void handle (AsyncResult<Boolean> exists) {
    
    JsonObject reply = new JsonObject();
    if (exists.succeeded() && exists.result()) {
      logger.info("User exists. Checking credentials.");
      Future<User> userFut = Future.factory.future();
      userFut.setHandler(new UserObjectHandler(usersAssignedSessionId));
      authorizeUser(loginCredentials, userFut);
    } else {
      reply.put("loggedIn", false);
      logger.info("User Login was not successful, because user does not exist. "
              + "Answernig with payload: " + reply.toString());
      eventBus.send(LOGIN_REPLY_TOPIC + ":" + usersAssignedSessionId, reply.toString());
    }
  }
  
  private void authorizeUser (JsonObject loginCreds, Future<User> fut) {
    
    this.jdbcClient.getConnection(conn -> {
      if (conn.failed()) {
        throw new IllegalArgumentException(conn.cause().getMessage());
      }
      SQLConnection connection = conn.result();
      getUser(connection, loginCreds, fut);
    });
  }
  
  private void getUser (SQLConnection connection, JsonObject loginCreds, Future<User> fut) {
    
    String query = "SELECT Id, Name, Password FROM User WHERE Name =? AND Password =?";
    JsonArray params = new JsonArray().add(loginCreds.getString("username"))
            .add(loginCreds.getString("password"));
    connection.queryWithParams(query, params, rs -> {
      if (rs.succeeded()) {
        ResultSet resultSet = rs.result();
        List<JsonArray> results = resultSet.getResults();
        if (results.size() > 1) {
          logger.info("There is no user with this username and password");
          fut.fail("Wrong password or name.");
        } else {
          for (JsonArray row : results) {
            fut.complete(new User(row.getLong(0), row.getString(1), row.getString(2)));
          }
        }
      } else {
        throw new RuntimeException(rs.cause().getMessage());
      }
    });
  }
  
  private class UserObjectHandler implements Handler<AsyncResult<User>> {
    
    private String sessionId;
    
    private UserObjectHandler (String sessionId) {
      
      this.sessionId = sessionId;
    }
    
    @Override
    public void handle (AsyncResult<User> user) {
      
      JsonObject reply = new JsonObject();
      if (user.succeeded()) {
        User u = user.result();
        reply.put("loggedIn", true).put("user", userToReply(u));
        logger.info("Login was successful. Answering with payload: " + reply.toString());
      } else {
        reply.put("loggedIn", false);
        logger.info("User login was not successful, because user entered wrong password. "
                + "Answering request with: " + reply.toString());
      }
      System.out.println(sessionId);
      eventBus.send(LOGIN_REPLY_TOPIC + ":" + sessionId, reply.toString());
      
    }
    
    private JsonObject userToReply (User u) {
      
      return new JsonObject().put("name", u.getName()).put("id", u.getId());
    }
  }
}
