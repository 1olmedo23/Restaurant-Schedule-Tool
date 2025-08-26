/**
package com.resto.scheduler.config;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Role;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
public class TestDataLoader {

    @Bean
    CommandLineRunner loadData(RoleRepository roleRepo, AppUserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            if (roleRepo.count() == 0) {
                roleRepo.saveAll(List.of(new Role("MANAGER"), new Role("EMPLOYEE")));
            }
            Role manager = roleRepo.findByName("MANAGER").orElseThrow();
            Role employee = roleRepo.findByName("EMPLOYEE").orElseThrow();

            if (userRepo.count() == 0) {
                // manager
                AppUser m = new AppUser();
                m.setUsername("manager1");
                m.setPassword(encoder.encode("password"));
                m.setFullName("Mandy Manager");
                m.setEmail("manager1@example.com");
                m.setEnabled(true);
                m.setRoles(Set.of(manager));
                userRepo.save(m);

                // employees
                for (String uname : List.of("alice","bob","carlos","sushi")) {
                    AppUser u = new AppUser();
                    u.setUsername(uname);
                    u.setPassword(encoder.encode("password"));
                    u.setFullName(Character.toUpperCase(uname.charAt(0)) + uname.substring(1) + " User");
                    u.setEmail(uname + "@example.com");
                    u.setEnabled(true);
                    u.setRoles(Set.of(employee));
                    userRepo.save(u);
                }
            }
        };
    }
}
 */