package io.quarkus.benchmark.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.benchmark.model.Fortune;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.pgclient.Row;

@ApplicationScoped
public class FortuneRepository {

    @Inject
    PgPool pgPool;

    public CompletionStage<List<Fortune>> findAll() {
        return pgPool.preparedQuery("SELECT * FROM Fortune")
                .thenApply(rowset -> {
                    List<Fortune> ret = new ArrayList<>(rowset.size());
                    for(Row r : rowset.getDelegate()) {
                        ret.add(new Fortune(r.getInteger("id"), r.getString("message")));
                    }
                    return ret;
                });
    }
}
