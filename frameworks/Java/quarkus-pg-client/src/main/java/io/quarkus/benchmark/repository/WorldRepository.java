package io.quarkus.benchmark.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.benchmark.model.World;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

@ApplicationScoped
public class WorldRepository {

    @Inject
    PgClients clients;

    public CompletionStage<World> find(int id) {
        return clients.getClient().preparedQuery("SELECT * FROM World WHERE id = $1", Tuple.of(id))
                .thenApply(rowset -> {
                    Row row = rowset.iterator().next();
                    return new World(row.getInteger(0), row.getInteger(1));
                });
    }

    public CompletionStage<Void> update(World[] worlds) {
        Arrays.sort(worlds);
        List<Tuple> args = new ArrayList<>(worlds.length);
        for(World world : worlds) {
            args.add(Tuple.of(world.getId(), world.getRandomNumber()));
        }
        return clients.getPool().preparedBatch("UPDATE World SET randomNumber = $2 WHERE id = $1", args)
                .thenApply(v -> null);
    }
}
