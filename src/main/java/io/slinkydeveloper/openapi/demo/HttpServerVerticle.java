package io.slinkydeveloper.openapi.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {

  HttpServer server;

  @Override
  public void start(Future future) {
    OpenAPI3RouterFactory.create(this.vertx, "spec.yaml", openAPI3RouterFactoryAsyncResult -> {
      if (openAPI3RouterFactoryAsyncResult.succeeded()) {
        OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

        // Enable automatic response when ValidationException is thrown
        routerFactory.setOptions(new RouterFactoryOptions()
            .setMountResponseContentTypeHandler(true)
        );

        // Add routes handlers
        routerFactory.addHandlerByOperationId("getTransactionsList", routingContext -> {
          RequestParameters requestParameters = routingContext.get("parsedParameters");
          JsonObject message = mapFiltersParameters(requestParameters);
          vertx.eventBus().send("transactions.demo/list", message, messageAsyncResult -> {
            routingContext
                .response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(messageAsyncResult.result().body().toString());
          });
        });

        routerFactory.addHandlerByOperationId("putTransaction", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters");
          if (!params.body().getJsonObject().containsKey("batch"))
            vertx.eventBus().send("transactions.demo/add", params.body().getJsonObject());
          else
            vertx.eventBus().send("transactions.demo/addMultiple", params.body().getJsonObject().getJsonArray("batch"));
          routingContext.response().setStatusCode(200).setStatusMessage("OK").end();
        });

        routerFactory.addHandlerByOperationId("calculateSum", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters");
          JsonObject message = new JsonObject()
              .put("from", params.queryParameter("from").getString())
              .put("to", params.queryParameter("to").getString());
          vertx.eventBus().send("transactions.demo/calculate", message, messageAsyncResult ->
              routingContext
                  .response()
                  .setStatusCode(200)
                  .putHeader("Content-Type", "application/json")
                  .end(messageAsyncResult.result().body().toString())
          );
        });

        // Generate the router
        Router router = routerFactory.getRouter();
        router.errorHandler(500, rc -> rc.response().setStatusCode(500).end(rc.failure() + "\n" + Arrays.toString(rc.failure().getStackTrace())));
        server = vertx.createHttpServer(new HttpServerOptions().setPort(3000).setHost("localhost"));
        server.requestHandler(router).listen(httpServerAsyncResult -> {
          if (httpServerAsyncResult.succeeded())
            System.out.println("HTTPServerVerticle started! Listening on port " + httpServerAsyncResult.result().actualPort());
          else
            System.out.println(httpServerAsyncResult.cause());
        });
        future.complete();
      } else {
        // Something went wrong during router factory initialization
        future.fail(openAPI3RouterFactoryAsyncResult.cause());
      }
    });
  }

  private JsonObject mapFiltersParameters(RequestParameters parameters) {
    JsonObject filters = new JsonObject();
    filters.put("from", parameters.queryParameter("from") != null ? parameters.queryParameter("from").toJson() : new JsonArray());
    filters.put("to", parameters.queryParameter("to") != null ? parameters.queryParameter("to").toJson() : new JsonArray());
    filters.put("message", parameters.queryParameter("message") != null ? parameters.queryParameter("message").toJson() : new JsonArray());
    return filters;
  }

  @Override
  public void stop() {
    this.server.close();
  }

}
