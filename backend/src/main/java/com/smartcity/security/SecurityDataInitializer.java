package com.smartcity.security;

import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:${ADMIN_USERNAME:admin}}")
    private String adminUsername;

    @Value("${app.admin.email:${ADMIN_EMAIL:admin@civicpulse.gov}}")
    private String adminEmail;

    @Value("${app.admin.password:${ADMIN_PASSWORD:Admin@CivicPulse2026}}")
    private String adminPassword;

    @Value("${app.admin.fullname:${ADMIN_FULLNAME:System Administrator}}")
    private String adminFullName;

    public SecurityDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Department trafficDept = departmentRepository != null ? departmentRepository.findByCode("TRAFFIC").orElse(null) : null;
        Team trafficTeam = teamRepository != null ? teamRepository.findByCode("TRAFFIC_OPS").orElse(null) : null;

        initOrUpdateUser(adminUsername, adminEmail, adminPassword, adminFullName, "ADMIN", null, null);
        initOrUpdateUser("operator", "operator@civicpulse.gov", adminPassword, "Command Operator", "COMMAND_OPERATOR", null, null);
        initOrUpdateUser("officer_traffic", "traffic@civicpulse.gov", adminPassword, "Traffic Department Officer", "DEPARTMENT_OFFICER", trafficDept, null);
        initOrUpdateUser("field_worker", "field@civicpulse.gov", adminPassword, "Field Operations Crew", "FIELD_WORKER", trafficDept, trafficTeam);
        initOrUpdateUser("citizen", "citizen@smartcity.gov", adminPassword, "Citizen Resident", "CITIZEN", null, null);
    }

    private void initOrUpdateUser(String username, String email, String password, String fullName, String roleName, Department department, Team team) {
        Role role = roleRepository.findByName(roleName).orElseGet(() ->
                roleRepository.saveAndFlush(new Role(roleName, roleName + " role")));

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            existing = userRepository.findByEmail(email);
        }
        if (existing.isEmpty()) {
            User u = new User(
                    username,
                    email,
                    passwordEncoder.encode(password),
                    fullName,
                    "1000",
                    role,
                    department,
                    team
            );
            u.setRoleName(roleName);
            u.setEnabled(true);
            u.setActive(true);
            u.setIsMobileVerified(true);
            u.setMobileNumber("+919876543210");
            userRepository.save(u);
            log.info("Initialized role account '{}' with role '{}'", username, roleName);
        } else {
            User u = existing.get();
            boolean needsUpdate = false;
            if (!Boolean.TRUE.equals(u.getIsMobileVerified())) {
                u.setIsMobileVerified(true);
                u.setMobileNumber("+919876543210");
                needsUpdate = true;
            }
            if (u.getRole() == null || !roleName.equals(u.getRoleName())) {
                u.setRole(role);
                u.setRoleName(roleName);
                needsUpdate = true;
            }
            if (department != null && (u.getDepartment() == null || !department.getId().equals(u.getDepartment().getId()))) {
                u.setDepartment(department);
                needsUpdate = true;
            }
            if (team != null && (u.getTeam() == null || !team.getId().equals(u.getTeam().getId()))) {
                u.setTeam(team);
                needsUpdate = true;
            }
            if (!Boolean.TRUE.equals(u.getEnabled()) || !Boolean.TRUE.equals(u.getActive())) {
                u.setEnabled(true);
                u.setActive(true);
                needsUpdate = true;
            }
            if (u.getPasswordHash() == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
                u.setPasswordHash(passwordEncoder.encode(password));
                needsUpdate = true;
            }
            if (needsUpdate) {
                userRepository.save(u);
                log.info("Synchronized affiliations/status for demo account '{}'", username);
            }
        }
    }
}
