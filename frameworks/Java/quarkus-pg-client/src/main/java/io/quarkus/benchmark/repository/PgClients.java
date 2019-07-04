package io.quarkus.benchmark.repository;


import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgPool;

class PgClients {
    private final Iterator<PgClient> clientIterator;
    private final Iterator<PgPool> poolIterator;

    PgClients(Collection<PgClient> clients, Collection<PgPool> pools) {
        clientIterator = Stream.generate(() -> clients).flatMap(Collection::stream).iterator();
        poolIterator = Stream.generate(() -> pools).flatMap(Collection::stream).iterator();
    }
    
    // for ArC
    public PgClients() {
        clientIterator = null;
        poolIterator = null;
    }

    synchronized PgClient getClient() {
        return clientIterator.next();
    }

    synchronized PgPool getPool() {
        return poolIterator.next();
    }
}