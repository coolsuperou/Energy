package org.example.back.dto;

import lombok.Data;
import org.example.back.entity.enums.UserRole;



@Data
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


}
