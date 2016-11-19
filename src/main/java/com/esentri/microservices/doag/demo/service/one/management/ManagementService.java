package com.esentri.microservices.doag.demo.service.one.management;

import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.ArrayList;
import java.util.List;

public class ManagementService extends AbstractVerticle {
  
  private final HazelcastUtil hazelcastUtil = new HazelcastUtil();
  
  private Logger logger;
  
  private Vertx vertx;
  
  public ManagementService() {
  
    System.out.println("calling default constructor");
    
  }
  
  public void start (Future<Void> completionFuture) {
    
    logger = LoggerFactory.getLogger(ManagementService.class);
    Future<HazelcastInstance> hazelcastInstanceFuture = Future.future();
    hazelcastInstanceFuture.setHandler(ref -> clusterCreatedHandler(ref, completionFuture));
    hazelcastUtil.createHazelcastCluster(hazelcastInstanceFuture);
  }

  
  private void initializeManagementService (Future<Void> startFuture) {
    
    UserDatabasePopulator udp = new UserDatabasePopulator(this.vertx, "users");
    udp.populateDatabase();
    System.out.println("database populated");
    
    deployVerticles(startFuture);
    
  }
  
  private List<String> getVerticlesToDeployReference() {
    List<String> verticlesToDeployReference = new ArrayList<>();
    verticlesToDeployReference.add(Authentication.class.getName());
    verticlesToDeployReference.add(Sessiondistribution.class.getName());
    return verticlesToDeployReference;
  }
  
  private void deployVerticles(Future<Void> completionFuture) {
    List<Future> completionListVerticles = deployAllVerticles(getVerticlesToDeployReference());
    CompositeFuture.all(completionListVerticles).setHandler(asyncResult -> {
      if(asyncResult.succeeded()) {
        completionFuture.complete();
        logger.info("Management services started successfully.");
        return;
      }
      completionFuture.fail("Could not all verticles deploy2.");
    });
    
    
    this.vertx.deployVerticle(Authentication.class.getName());
    this.vertx.deployVerticle(Sessiondistribution.class.getName());
  }
  
  private List<Future> deployAllVerticles(List<String> verticlesToDeployReference) {
    List<Future> completionListVerticles = new ArrayList<>();
    verticlesToDeployReference.forEach(verticleToDeploy -> {
      
      Future<Void> verticleDeploymentCompletionFuture = Future.future();
      completionListVerticles.add(verticleDeploymentCompletionFuture);
      this.vertx.deployVerticle(verticleToDeploy, onCompletion -> verticleDeploymentCompletionFuture.complete());
    });
    return completionListVerticles;
  }
  
  private void clusterCreatedHandler (AsyncResult<HazelcastInstance> hazelcastInstanceAsyncResult, Future<Void>
           completionFuture) {
    
    if (!hazelcastInstanceAsyncResult.succeeded()) {
      throw new IllegalStateException("Error while creating hazelcast cluster");
    }
    
    ClusterManager clusterManager = new HazelcastClusterManager(hazelcastInstanceAsyncResult.result());
    
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setClusterManager(clusterManager);
    Vertx.clusteredVertx(vertxOptions, res -> {
      if (res.succeeded()) {
        this.vertx = res.result();
        initializeManagementService(completionFuture);
      } else {
        completionFuture.fail("Failed: " + res.cause());
      }
    });
  }
  
}
