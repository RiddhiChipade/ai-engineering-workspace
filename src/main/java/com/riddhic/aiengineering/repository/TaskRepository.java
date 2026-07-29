package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
