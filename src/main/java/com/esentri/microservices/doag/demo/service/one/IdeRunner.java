package com.esentri.microservices.doag.demo.service.one;

import com.esentri.microservices.doag.demo.service.one.management.ManagementService;
import io.vertx.core.Vertx;

public class IdeRunner {
  
  public static void main(String[] args) {
  
    Vertx.vertx().deployVerticle(ManagementService.class.getName(), completionFuture -> {
      if(!completionFuture.succeeded()) {
        throw new RuntimeException("Error while deploying " + ManagementService.class.getName());
      }
      System.out.println("Startup routine complete.");
    });
  }
}
