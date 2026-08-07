package com.jo.payonservices.auth.service;

import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
@Autowired
private UserRepo userRepo;
@Autowired
private PasswordEncoder passwordEncoder;
    public void getAdmin() {
    users admin= userRepo.findByRole(Role.ADMIN).orElseThrow(()->new RuntimeException("Admin not found"));
    String hashedPassword = passwordEncoder.encode(admin.getPassword());
    admin.setPassword(hashedPassword);
    userRepo.save(admin);
    }
}
