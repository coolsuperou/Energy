package org.example.back.dto;

import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserDTO {

    private Long id;
    private String username;
    private String name;
    private UserRole role;
    private String roleName;
    private String department;
    private String phone;
    private String email;
    private UserStatus status;
    private String employeeId;
    private String joinDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setRoleName(getRoleDisplayName(user.getRole()));
        dto.setDepartment(user.getDepartment());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        dto.setEmployeeId(generateEmployeeId(user));
        dto.setJoinDate(formatJoinDate(user.getCreatedAt()));
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    private static String getRoleDisplayName(UserRole role) {
        if (role == null) return "";
        switch (role) {
            case INSPECTOR: return "设备巡检员";
            case WORKSHOP: return "车间用户";
            case DISPATCHER: return "调度员";
            case MANAGER: return "经理";
            case ADMIN: return "管理员";
            default: return "";
        }
    }

    private static String generateEmployeeId(User user) {
        if (user.getId() == null) return "";
        String prefix = "";
        if (user.getRole() != null) {
            switch (user.getRole()) {
                case INSPECTOR: prefix = "INS"; break;
                case WORKSHOP: prefix = "WKS"; break;
                case DISPATCHER: prefix = "DIS"; break;
                case MANAGER: prefix = "MGR"; break;
                case ADMIN: prefix = "ADM"; break;
                default: prefix = "USR";
            }
        }
        return prefix + String.format("%04d", user.getId());
    }

    private static String formatJoinDate(LocalDateTime createdAt) {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
