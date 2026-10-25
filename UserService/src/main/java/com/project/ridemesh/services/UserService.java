package com.project.ridemesh.services;

import com.project.ridemesh.dto.UpdateProfileDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User getUserFromId(Long userId);
    UserDto updateProfile(Long userId, UpdateProfileDto updateProfileDto);
}
