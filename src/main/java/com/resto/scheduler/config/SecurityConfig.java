package com.resto.scheduler.config;

import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/login", "/css/**", "/webjars/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("MANAGER")
                    .requestMatchers("/manager/**").hasRole("MANAGER")
                    .requestMatchers("/employee/**").hasAnyRole("EMPLOYEE","MANAGER")
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/post-login", true)
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .permitAll()
            );
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  public UserDetailsService userDetailsService(AppUserRepository users) {
    return username -> {
      AppUser u = users.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
      return new User(
              u.getUsername(),
              u.getPassword(),
              u.isEnabled(),
              true, true, true,
              u.getRoles().stream()
                      .map(Role::getName)
                      .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                      .collect(Collectors.toSet())
      );
    };
  }
}
