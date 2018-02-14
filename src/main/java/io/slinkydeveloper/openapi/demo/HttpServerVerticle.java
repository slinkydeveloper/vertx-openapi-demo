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

import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {

    HttpServer server;

    @Override
    public void start(Future future) {
        OpenAPI3RouterFactory.create(this.vertx, getClass().getResource("/spec.yaml").getFile(), openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                // Enable automatic response when ValidationException is thrown
                routerFactory.setOptions(new RouterFactoryOptions()
                        .setMountValidationFailureHandler(true)
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


                // Generate the router
                Router router = routerFactory.getRouter();
                server = vertx.createHttpServer(new HttpServerOptions().setPort(3000).setHost("localhost"));
                server.requestHandler(router::accept).listen(httpServerAsyncResult -> {
                    if (httpServerAsyncResult.succeeded())
                        System.out.println("Listening on port " + httpServerAsyncResult.result().actualPort());
                    else
                        System.out.println(httpServerAsyncResult.cause());
                });
                future.complete();
            } else {
                // Something went wrong during router factory initialization
                Throwable exception = openAPI3RouterFactoryAsyncResult.cause();
                System.err.println(exception);
                System.exit(1);
            }
        });
    }

    // SO BORING MANUAL MAPPING
    private JsonObject mapFiltersParameters(RequestParameters parameters) {
        JsonObject filters = new JsonObject();
        mapFiltersArray(parameters, filters, "from");
        mapFiltersArray(parameters, filters, "to");
        mapFiltersArray(parameters, filters, "message");
        return filters;
    }

    private void mapFiltersArray(RequestParameters parameters, JsonObject filters, String filterName) {
        if (parameters.queryParameter(filterName) != null) {
            filters.put(filterName, new JsonArray(
                    parameters.queryParameter(filterName).getArray().stream().map(RequestParameter::getString).collect(Collectors.toList())
            ));
        }
    }

    @Override
    public void stop() {
        this.server.close();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new DataVerticle());
        vertx.deployVerticle(new HttpServerVerticle());
    }

}
