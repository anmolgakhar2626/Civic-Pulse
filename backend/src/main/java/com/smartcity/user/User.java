package com.smartcity.user;

import com.smartcity.department.Department;
import com.smartcity.department.Team;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "is_mobile_verified")
    private Boolean isMobileVerified = false;

    @Column(name = "genuine_complaint_count")
    private Integer genuineComplaintCount = 0;

    @Column(name = "is_verified_citizen")
    private Boolean isVerifiedCitizen = false;

    @Column(name = "role", length = 50, nullable = false)
    private String roleName = "CITIZEN";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User() {}

    public User(String username, String email, String passwordHash, String fullName, String phone,
                Role role, Department department, Team team) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        if (role != null) {
            this.roleName = role.getName();
            this.roles.add(role);
        } else {
            this.roleName = "CITIZEN";
        }
        this.department = department;
        this.team = team;
        this.enabled = true;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public User(String fullName, String email, String passwordHash, String roleName) {
        this.fullName = fullName;
        this.email = email;
        this.username = email;
        this.passwordHash = passwordHash;
        this.roleName = roleName != null ? roleName : "CITIZEN";
        this.role = new Role(this.roleName, this.roleName + " role");
        this.roles.add(this.role);
        this.enabled = true;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Override
    public String getPassword() { return passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRoleName() {
        if (roleName != null && !roleName.isBlank()) {
            return roleName;
        }
        if (role != null && role.getName() != null) {
            return role.getName();
        }
        return "CITIZEN";
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
        if (this.role == null && roleName != null) {
            this.role = new Role(roleName, roleName + " role");
        }
    }

    public Role getRole() {
        if (role == null && roleName != null) {
            this.role = new Role(roleName, roleName + " role");
        }
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.roleName = role.getName();
            this.roles.add(role);
        }
    }

    public Set<Role> getRoles() {
        if (roles.isEmpty() && role != null) {
            roles.add(role);
        } else if (roles.isEmpty() && roleName != null) {
            roles.add(new Role(roleName, roleName + " role"));
        }
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
        if (!this.roles.isEmpty() && this.role == null) {
            this.role = this.roles.iterator().next();
            if (this.role != null) {
                this.roleName = this.role.getName();
            }
        }
    }

    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
            if (this.role == null) {
                this.role = role;
                this.roleName = role.getName();
            }
        }
    }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        this.active = enabled;
    }

    public Boolean getActive() { return active != null ? active : enabled; }
    public void setActive(Boolean active) {
        this.active = active;
        this.enabled = active;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Spring Security UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        String primaryRole = getRoleName();
        if (primaryRole != null) {
            String roleAuthority = primaryRole.startsWith("ROLE_") ? primaryRole : "ROLE_" + primaryRole;
            authorities.add(new SimpleGrantedAuthority(roleAuthority));
        }
        if (roles != null) {
            for (Role r : roles) {
                if (r != null && r.getName() != null) {
                    String roleAuthority = r.getName().startsWith("ROLE_") ? r.getName() : "ROLE_" + r.getName();
                    authorities.add(new SimpleGrantedAuthority(roleAuthority));
                }
            }
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled) && Boolean.TRUE.equals(this.active);
    }

    public String getMobileNumber() { return mobileNumber != null ? mobileNumber : phone; }
    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
        if (this.phone == null) {
            this.phone = mobileNumber;
        }
    }

    public Boolean getIsMobileVerified() { return Boolean.TRUE.equals(isMobileVerified); }
    public boolean isMobileVerified() { return Boolean.TRUE.equals(isMobileVerified); }
    public void setIsMobileVerified(Boolean isMobileVerified) { this.isMobileVerified = isMobileVerified; }

    public Integer getGenuineComplaintCount() { return genuineComplaintCount != null ? genuineComplaintCount : 0; }
    public void setGenuineComplaintCount(Integer genuineComplaintCount) { this.genuineComplaintCount = genuineComplaintCount; }

    public Boolean getIsVerifiedCitizen() { return Boolean.TRUE.equals(isVerifiedCitizen); }
    public boolean isVerifiedCitizen() { return Boolean.TRUE.equals(isVerifiedCitizen); }
    public void setIsVerifiedCitizen(Boolean isVerifiedCitizen) { this.isVerifiedCitizen = isVerifiedCitizen; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
