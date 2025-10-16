package ua.foxminded.university.testutil;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Component
@Transactional
public class TestDataInitializer {

	@PersistenceContext
    private EntityManager em;
	
    public <T> List<T> persistAll(T... entities) {
        if (entities == null || entities.length == 0) return List.of();
        for (T e : entities) em.persist(e);

        return Arrays.asList(entities);
    }
    
    public void clear() {
    	em.clear();
    }
}
