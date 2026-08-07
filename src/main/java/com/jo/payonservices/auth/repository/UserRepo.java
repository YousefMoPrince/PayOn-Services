package com.jo.payonservices.auth.repository;

import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.auth.model.users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<users, UUID> {
    Optional<users> findByUserName(String userName);
    Optional<users> findByEmail(String email);
    Optional<users> findByRole(Role role);
    List<users> findByPhoneNumberIn(List<String> phoneNumber);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
}
