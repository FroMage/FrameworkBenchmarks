package io.quarkus.benchmark.repository.hibernate.reactive;

import java.util.function.Function;

import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public class BaseRepository {
    @Inject
    protected Mutiny.SessionFactory sf;

    public <T> Uni<T> inSession(Function<Mutiny.Session, Uni<T>> work){
        return sf.openSession().flatMap(session -> {
            return work.apply(session)
                    .onItemOrFailure().invoke((w, t) -> session.close());
        });
    }

}
