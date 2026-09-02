package com.project.ridemesh.controllers.rider;

import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Rider;
import com.project.ridemesh.services.RiderService;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.repositories.UserRepository;
import com.project.ridemesh.repositories.RiderRepository;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/riders")
public class RiderInternalController {

    private final RiderService riderService;
    private final RiderRepository riderRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public RiderInternalController(RiderService riderService, RiderRepository riderRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.riderService = riderService;
        this.riderRepository = riderRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/{riderId}")
    public ResponseEntity<RiderDto> getRiderById(@PathVariable Long riderId) {
        try {
            Rider rider = riderService.getRiderById(riderId);
            return ResponseEntity.ok(modelMapper.map(rider, RiderDto.class));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RiderDto> getRiderByUserId(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
        Rider rider = riderRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found for user ID: " + userId));
        return ResponseEntity.ok(modelMapper.map(rider, RiderDto.class));
    }
}
