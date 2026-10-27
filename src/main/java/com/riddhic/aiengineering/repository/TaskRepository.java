package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    
    Page<Task> findByPriority(Priority priority, Pageable pageable);
    
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);
    
    Page<Task> findByStatusAndPriority(TaskStatus status, Priority priority, Pageable pageable);
    
    Page<Task> findByStatusAndAssignedToId(TaskStatus status, Long userId, Pageable pageable);
    
    Page<Task> findByPriorityAndAssignedToId(Priority priority, Long userId, Pageable pageable);
    
    Page<Task> findByStatusAndPriorityAndAssignedToId(TaskStatus status, Priority priority, Long userId, Pageable pageable);
    
    // Search methods
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Task> searchByTitleOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.status = :status")
    Page<Task> searchByTitleOrDescriptionAndStatus(@Param("searchTerm") String searchTerm, @Param("status") TaskStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.priority = :priority")
    Page<Task> searchByTitleOrDescriptionAndPriority(@Param("searchTerm") String searchTerm, @Param("priority") Priority priority, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.assignedTo.id = :userId")
    Page<Task> searchByTitleOrDescriptionAndAssignedToId(@Param("searchTerm") String searchTerm, @Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.status = :status AND t.priority = :priority")
    Page<Task> searchByTitleOrDescriptionAndStatusAndPriority(@Param("searchTerm") String searchTerm, @Param("status") TaskStatus status, @Param("priority") Priority priority, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.status = :status AND t.assignedTo.id = :userId")
    Page<Task> searchByTitleOrDescriptionAndStatusAndAssignedToId(@Param("searchTerm") String searchTerm, @Param("status") TaskStatus status, @Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.priority = :priority AND t.assignedTo.id = :userId")
    Page<Task> searchByTitleOrDescriptionAndPriorityAndAssignedToId(@Param("searchTerm") String searchTerm, @Param("priority") Priority priority, @Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND t.status = :status AND t.priority = :priority AND t.assignedTo.id = :userId")
    Page<Task> searchByTitleOrDescriptionAndStatusAndPriorityAndAssignedToId(@Param("searchTerm") String searchTerm, @Param("status") TaskStatus status, @Param("priority") Priority priority, @Param("userId") Long userId, Pageable pageable);
}
