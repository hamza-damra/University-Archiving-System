package com.alquds.edu.ArchiveSystem.mapper.user;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import org.springframework.stereotype.Component;

@Component("userMapper")
public class UserMapperManual implements UserMapper {
    
    @Override
    public User toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setIsActive(true);
        
        return user;
    }
    
    @Override
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setRole(user.getRole());
        userResponse.setIsActive(user.getIsActive());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());
        
        if (user.getDepartment() != null) {
            userResponse.setDepartmentName(user.getDepartment().getName());
        }
        
        return userResponse;
    }
    
    @Override
    public void updateEntity(UserUpdateRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
    }
}
