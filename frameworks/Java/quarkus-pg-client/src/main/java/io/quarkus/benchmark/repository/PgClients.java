package io.quarkus.benchmark.repository;


import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import io.vertx.axle.pgclient.PgPool;
import io.vertx.axle.sqlclient.SqlClient;

class PgClients {
    private final Iterator<SqlClient> clientIterator;
    private final Iterator<PgPool> poolIterator;

    PgClients(Collection<SqlClient> clients, Collection<PgPool> pools) {
        clientIterator = Stream.generate(() -> clients).flatMap(Collection::stream).iterator();
        poolIterator = Stream.generate(() -> pools).flatMap(Collection::stream).iterator();
    }
    
    // for ArC
    public PgClients() {
        clientIterator = null;
        poolIterator = null;
    }

    synchronized SqlClient getClient() {
        return clientIterator.next();
    }

    synchronized PgPool getPool() {
        return poolIterator.next();
    }
}