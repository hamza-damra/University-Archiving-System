package com.alquds.edu.ArchiveSystem.mapper.user;

import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;

import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserResponse;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "professorId", ignore = true)
    @Mapping(target = "courseAssignments", ignore = true)
    @Mapping(target = "documentSubmissions", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    User toEntity(UserCreateRequest request);
    
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    UserResponse toResponse(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "professorId", ignore = true)
    @Mapping(target = "courseAssignments", ignore = true)
    @Mapping(target = "documentSubmissions", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
    
    @Named("departmentIdToDepartment")
    default Department departmentIdToDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = new Department();
        department.setId(departmentId);
        return department;
    }
}
