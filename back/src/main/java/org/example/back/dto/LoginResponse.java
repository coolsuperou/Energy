package org.example.back.dto;

import org.example.back.entity.enums.UserRole;

public class LoginResponse {

    private Long userId;
    private String username;
    private String name;
    private UserRole role;
    private String department;
    private String redirectUrl;

    public LoginResponse(Long userId, String username, String name, UserRole role, 
                        String department, String redirectUrl) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.department = department;
        this.redirectUrl = redirectUrl;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
    public String getDepartment() { return department; }
    public String getRedirectUrl() { return redirectUrl; }
}
