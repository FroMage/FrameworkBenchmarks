package io.quarkus.benchmark.repository.hibernate.reactive;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import io.quarkus.benchmark.model.hibernate.reactive.Fortune;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class FortuneRepository extends BaseRepository {

    public Uni<List<Fortune>> findAll() {
        return inSession(s -> {
            CriteriaBuilder criteriaBuilder = sf.getCriteriaBuilder();
            CriteriaQuery<Fortune> fortuneQuery = criteriaBuilder.createQuery(Fortune.class);
            Root<Fortune> from = fortuneQuery.from(Fortune.class);
            fortuneQuery.select(from);
            return s.createQuery(fortuneQuery).getResultList();
        });
    }
}
