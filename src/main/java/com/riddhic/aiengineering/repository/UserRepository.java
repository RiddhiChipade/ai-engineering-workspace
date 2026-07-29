package com.riddhic.aiengineering.repository;

import com.riddhic.aiengineering.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
