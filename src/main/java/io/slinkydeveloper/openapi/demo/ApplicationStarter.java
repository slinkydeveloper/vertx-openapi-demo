package io.slinkydeveloper.openapi.demo;

import io.vertx.core.Vertx;

public class ApplicationStarter {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new DataVerticle());
    vertx.deployVerticle(new HttpServerVerticle());
  }

}
