package io.quarkus.benchmark.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import io.quarkus.benchmark.model.World;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

@ApplicationScoped
public class WorldRepository {

    @Inject
    PgPool pool;

    public CompletionStage<World> find(int id) {
        return pool.preparedQuery("SELECT * FROM World WHERE id = $1", Tuple.of(id))
                .thenApply(rowset -> {
                    switch(rowset.size()) {
                    case 1: {
                        Row row = rowset.iterator().next();
                        return new World(row.getInteger(0), row.getInteger(1));
                    }
                    default: throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }
                });
    }

    public CompletionStage<Void> update(World world) {
        return pool.preparedQuery("UPDATE World SET randomNumber = $2 WHERE id = $1", Tuple.of(world.getId(), world.getRandomNumber()))
                .thenApply(v -> null);
    }
}
