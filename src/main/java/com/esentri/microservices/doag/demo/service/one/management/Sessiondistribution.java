package com.esentri.microservices.doag.demo.service.one.management;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.ClusteredSessionStore;

public class Sessiondistribution extends AbstractVerticle {
  
  private static final String SESSION_REQUEST_TOPIC = "esentri.session.request";
  
  private ClusteredSessionStore clusteredSessionStore;
  
  private Logger logger;

  public void start (Future<Void> completionFuture) {
    
    logger = LoggerFactory.getLogger(Sessiondistribution.class);
    logger.info("Starting session service.");
    EventBus eventBus = vertx.eventBus();
    this.clusteredSessionStore = ClusteredSessionStore.create(this.vertx);
    
    eventBus.consumer(SESSION_REQUEST_TOPIC, this::sessionHandler);
    
    logger.info("Session service started successfully");
    completionFuture.complete();
  }
  
  private void sessionHandler (Message<String> msg) {
    
    logger.info("Request for new session id received.");
    JsonObject reply = new JsonObject().put("sessionId", this.clusteredSessionStore.createSession(1000000).id());
    logger.info("Answering imideatly with new session id. Payload is: " + reply.toString());
    msg.reply(reply.toString());
  }
  
}
