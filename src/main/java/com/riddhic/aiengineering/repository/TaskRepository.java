package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.Task;
import com.riddhic.aiengineering.enums.TaskStatus;
import com.riddhic.aiengineering.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    
    Page<Task> findByPriority(Priority priority, Pageable pageable);
    
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);
    
    Page<Task> findByStatusAndPriority(TaskStatus status, Priority priority, Pageable pageable);
    
    Page<Task> findByStatusAndAssignedToId(TaskStatus status, Long userId, Pageable pageable);
    
    Page<Task> findByPriorityAndAssignedToId(Priority priority, Long userId, Pageable pageable);
    
    Page<Task> findByStatusAndPriorityAndAssignedToId(TaskStatus status, Priority priority, Long userId, Pageable pageable);
}
