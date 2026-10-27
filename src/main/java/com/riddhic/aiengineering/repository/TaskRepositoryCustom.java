package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskRepositoryCustom {
    Page<Task> findTasksWithFilters(String status, String priority, Long assignedUserId, Pageable pageable);
}
