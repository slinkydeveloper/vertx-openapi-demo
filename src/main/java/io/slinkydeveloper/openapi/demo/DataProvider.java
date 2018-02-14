package io.slinkydeveloper.openapi.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public class DataProvider {

    List<Transaction> transactions;

    DataProvider() {
        transactions = new ArrayList<>();
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<Transaction> getFilteredTransactions(Predicate<Transaction> p) {
        return transactions.stream().filter(p).collect(Collectors.toList());
    }

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }

    public void addTransactions(Collection<Transaction> t) {
        transactions.addAll(t);
    }
}
