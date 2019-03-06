package io.slinkydeveloper.openapi.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public class DataVerticle extends AbstractVerticle {

  DataProvider dataProvider;

  private Predicate<Transaction> constructFilterPredicate(JsonObject filter) {
    List<Predicate<Transaction>> predicates = new ArrayList<>();
    if (filter.containsKey("from")) {
      predicates.add(transaction -> filter.getJsonArray("from").contains(transaction.getFrom()));
    }
    if (filter.containsKey("to")) {
      predicates.add(transaction -> filter.getJsonArray("to").contains(transaction.getTo()));
    }
    if (filter.containsKey("message")) {
      predicates.add(transaction -> filter.getJsonArray("message").stream().filter(o -> ((String) o).contains(transaction.getMessage())).count() > 0);
    }
    // Elegant predicates combination
    return predicates.stream().reduce(transaction -> true, Predicate::and);
  }

  @Override
  public void start() {
    dataProvider = new DataProvider();
    EventBus eventBus = vertx.eventBus();

    eventBus.consumer("transactions.demo/add").handler(objectMessage -> {
      JsonObject json = (JsonObject) objectMessage.body();
      dataProvider.addTransaction(json.mapTo(Transaction.class));
    });

    eventBus.consumer("transactions.demo/addMultiple").handler(objectMessage -> {
      JsonArray jsonArray = (JsonArray) objectMessage.body();
      dataProvider.addTransactions(
          jsonArray
              .stream()
              // We can safely cast to JsonObject because schemas combined with
              // validator guarantees that this is an array of objects
              .map(obj -> ((JsonObject) obj).mapTo(Transaction.class))
              .collect(Collectors.toList())
      );
    });

    eventBus.consumer("transactions.demo/list").handler(objectMessage -> {
      objectMessage.reply(
          Json.encode(
              dataProvider.getFilteredTransactions(
                  constructFilterPredicate((JsonObject) objectMessage.body())
              )
          )
      );
    });

    eventBus.consumer("transactions.demo/calculate").handler(objectMessage -> {
      JsonObject payload = (JsonObject) objectMessage.body();
      objectMessage.reply(
          dataProvider.calculateSum(payload.getString("from"), payload.getString("to"))
      );
    });

    System.out.println("DataVerticle started!");

  }

}
