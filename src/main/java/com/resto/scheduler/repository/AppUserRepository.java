package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByRoles_Name(String name);

    // Safety checks for last-enabled-manager protection
    long countByRoles_NameAndEnabled(String name, boolean enabled);
}
