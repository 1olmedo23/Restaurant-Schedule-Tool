package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Role;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.RoleRepository;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.AvailabilityRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Stream;


import java.util.*;

@Controller
@RequestMapping("/admin/users")
public class AdminController {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AssignmentRepository assignmentRepo;
    private final AvailabilityRepository availabilityRepo;

    public AdminController(AppUserRepository userRepo,
                           RoleRepository roleRepo,
                           PasswordEncoder encoder,
                           AssignmentRepository assignmentRepo,
                           AvailabilityRepository availabilityRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.assignmentRepo = assignmentRepo;
        this.availabilityRepo = availabilityRepo;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String created,
                       @RequestParam(required = false) String toggled,
                       @RequestParam(required = false) String deleted,
                       @RequestParam(required = false) String error) {
        model.addAttribute("active", "admin-users");

        // Managers first (ID desc), then non-managers (ID asc)
        List<AppUser> all = userRepo.findAll();

        var managers = all.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "MANAGER".equals(r.getName())))
                .sorted(Comparator.comparing(AppUser::getId))
                .toList();

        var others = all.stream()
                .filter(u -> u.getRoles().stream().noneMatch(r -> "MANAGER".equals(r.getName())))
                .sorted(Comparator.comparing(AppUser::getId))
                .toList();

        var ordered = Stream.concat(managers.stream(), others.stream()).toList();

        model.addAttribute("users", ordered);

        if (created != null) model.addAttribute("created", true);
        if (toggled != null) model.addAttribute("toggled", true);
        if (deleted != null) model.addAttribute("deleted", true);
        if (error != null) model.addAttribute("error", error);
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("active", "admin-users");
        model.addAttribute("roles", roleRepo.findAll());
        return "admin/users/new";
    }

    @PostMapping
    public String create(@RequestParam String username,
                         @RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam(name = "role") String roleName,   // single role
                         @RequestParam(defaultValue = "true") boolean enabled,
                         Model model) {

        if (userRepo.findByUsername(username).isPresent()) {
            model.addAttribute("active", "admin-users");
            model.addAttribute("roles", roleRepo.findAll());
            model.addAttribute("error", "Username already exists");
            return "admin/users/new";
        }

        Role role = roleRepo.findByName(roleName).orElse(null);
        if (role == null) {
            model.addAttribute("active", "admin-users");
            model.addAttribute("roles", roleRepo.findAll());
            model.addAttribute("error", "Invalid role selected");
            return "admin/users/new";
        }

        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setEnabled(enabled);
        u.setRoles(Set.of(role)); // exactly one role

        userRepo.save(u);
        return "redirect:/admin/users?created";
    }

    @PostMapping("/{id}/toggle")
    public String toggleEnabled(@PathVariable Long id, Authentication auth) {
        Optional<AppUser> opt = userRepo.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/users?error=User+not+found";
        AppUser u = opt.get();

        if (auth != null && u.getUsername().equalsIgnoreCase(auth.getName())) {
            return "redirect:/admin/users?error=You+cannot+disable+your+own+account";
        }

        boolean nextEnabled = !u.isEnabled();

        boolean isManager = u.getRoles().stream().anyMatch(r -> "MANAGER".equals(r.getName()));
        if (!nextEnabled && isManager) {
            long enabledManagers = userRepo.countByRoles_NameAndEnabled("MANAGER", true);
            if (enabledManagers <= 1) {
                return "redirect:/admin/users?error=Cannot+disable+the+last+enabled+manager";
            }
        }

        u.setEnabled(nextEnabled);
        userRepo.save(u);
        return "redirect:/admin/users?toggled";
    }

    @Transactional
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth) {
        Optional<AppUser> opt = userRepo.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/users?error=User+not+found";
        AppUser u = opt.get();

        if (auth != null && u.getUsername().equalsIgnoreCase(auth.getName())) {
            return "redirect:/admin/users?error=You+cannot+delete+your+own+account";
        }

        boolean isManager = u.getRoles().stream().anyMatch(r -> "MANAGER".equals(r.getName()));
        if (isManager) {
            long enabledManagers = userRepo.countByRoles_NameAndEnabled("MANAGER", true);
            if (u.isEnabled() && enabledManagers <= 1) {
                return "redirect:/admin/users?error=Cannot+delete+the+last+enabled+manager";
            }
        }

        assignmentRepo.deleteByEmployee(u);
        availabilityRepo.deleteByUser(u);

        userRepo.delete(u);
        return "redirect:/admin/users?deleted";
    }
}
