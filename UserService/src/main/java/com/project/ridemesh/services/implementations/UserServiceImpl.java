package com.project.ridemesh.services.implementations;

import com.project.ridemesh.dto.UpdateProfileDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.repositories.UserRepository;
import com.project.ridemesh.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public User getUserFromId(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(()-> new AuthenticationCredentialsNotFoundException("User not found!"));
    }

    @Override
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileDto updateProfileDto) {
        User user = getUserFromId(userId);
        if (updateProfileDto.getName() != null && !updateProfileDto.getName().trim().isEmpty()) {
            user.setName(updateProfileDto.getName().trim());
        }
        if (updateProfileDto.getProfilePictureUrl() != null && !updateProfileDto.getProfilePictureUrl().trim().isEmpty()) {
            user.setProfilePictureUrl(updateProfileDto.getProfilePictureUrl().trim());
        }
        if (updateProfileDto.getPhoneNumber() != null && !updateProfileDto.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(updateProfileDto.getPhoneNumber().trim());
        }
        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserDto.class);
    }
}
