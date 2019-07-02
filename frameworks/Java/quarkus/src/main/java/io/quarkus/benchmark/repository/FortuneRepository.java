package io.quarkus.benchmark.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import io.quarkus.benchmark.model.Fortune;

@ApplicationScoped
public class FortuneRepository {

    @Inject
    EntityManager em;

    public List<Fortune> findAll() {
        Query query = em.createQuery("SELECT f FROM Fortune f");
        return query.getResultList();
    }
}
