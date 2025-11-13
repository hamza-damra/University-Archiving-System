package com.alqude.edu.ArchiveSystem.dto.auth;

import com.alqude.edu.ArchiveSystem.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String departmentName;
    
    public JwtResponse(String token, Long id, String email, String firstName, String lastName, Role role, String departmentName) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.departmentName = departmentName;
    }
}
