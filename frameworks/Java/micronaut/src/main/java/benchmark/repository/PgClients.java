package benchmark.repository;


import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import io.vertx.sqlclient.SqlClient;

class PgClients {
    private final Iterator<SqlClient> iterator;

    PgClients(Collection<SqlClient> clients) {
        iterator = Stream.generate(() -> clients).flatMap(Collection::stream).iterator();
    }

    synchronized SqlClient getOne() {
        return iterator.next();
    }
}