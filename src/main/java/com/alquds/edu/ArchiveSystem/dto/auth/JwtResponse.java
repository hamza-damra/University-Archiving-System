package com.alquds.edu.ArchiveSystem.dto.auth;

import com.alquds.edu.ArchiveSystem.entity.auth.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private Long departmentId;
    private String departmentName;

    public JwtResponse(String token, Long id, String email, String firstName, String lastName, Role role,
            Long departmentId, String departmentName) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public JwtResponse(String token, String refreshToken, Long id, String email, String firstName, String lastName, Role role,
            Long departmentId, String departmentName) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }
}
