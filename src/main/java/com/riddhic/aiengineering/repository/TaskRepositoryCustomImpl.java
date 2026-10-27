package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.Task;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {

    private final EntityManager entityManager;

    public TaskRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Task> findTasksWithFilters(String status, String priority, Long assignedUserId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> countRoot = countQuery.from(Task.class);
        countQuery.select(cb.count(countRoot));
        
        List<Predicate> countPredicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            countPredicates.add(cb.equal(countRoot.get("status"), status));
        }
        if (priority != null && !priority.isBlank()) {
            countPredicates.add(cb.equal(countRoot.get("priority"), priority));
        }
        if (assignedUserId != null) {
            countPredicates.add(cb.equal(countRoot.get("assignedTo").get("id"), assignedUserId));
        }
        
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        
        long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // Results query with sort
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);
        
        List<Predicate> resultPredicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            resultPredicates.add(cb.equal(root.get("status"), status));
        }
        if (priority != null && !priority.isBlank()) {
            resultPredicates.add(cb.equal(root.get("priority"), priority));
        }
        if (assignedUserId != null) {
            resultPredicates.add(cb.equal(root.get("assignedTo").get("id"), assignedUserId));
        }
        
        if (!resultPredicates.isEmpty()) {
            query.where(cb.and(resultPredicates.toArray(new Predicate[0])));
        }
        
        // Apply sorting
        java.util.List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            if (order.isAscending()) {
                orders.add(cb.asc(root.get(order.getProperty())));
            } else {
                orders.add(cb.desc(root.get(order.getProperty())));
            }
        }
        
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        
        List<Task> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(results, pageable, total);
    }
}
