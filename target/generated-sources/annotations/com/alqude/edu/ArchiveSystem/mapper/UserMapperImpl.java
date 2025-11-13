package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-13T03:02:16+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( request.getEmail() );
        user.setFirstName( request.getFirstName() );
        user.setLastName( request.getLastName() );
        user.setRole( request.getRole() );

        user.setIsActive( true );

        return user;
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        userResponse.setDepartmentName( userDepartmentName( user ) );
        userResponse.setCreatedAt( user.getCreatedAt() );
        userResponse.setEmail( user.getEmail() );
        userResponse.setFirstName( user.getFirstName() );
        userResponse.setId( user.getId() );
        userResponse.setIsActive( user.getIsActive() );
        userResponse.setLastName( user.getLastName() );
        userResponse.setRole( user.getRole() );
        userResponse.setUpdatedAt( user.getUpdatedAt() );

        return userResponse;
    }

    @Override
    public void updateEntity(UserUpdateRequest request, User user) {
        if ( request == null ) {
            return;
        }

        if ( request.getEmail() != null ) {
            user.setEmail( request.getEmail() );
        }
        if ( request.getFirstName() != null ) {
            user.setFirstName( request.getFirstName() );
        }
        if ( request.getIsActive() != null ) {
            user.setIsActive( request.getIsActive() );
        }
        if ( request.getLastName() != null ) {
            user.setLastName( request.getLastName() );
        }
    }

    private String userDepartmentName(User user) {
        if ( user == null ) {
            return null;
        }
        Department department = user.getDepartment();
        if ( department == null ) {
            return null;
        }
        String name = department.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
