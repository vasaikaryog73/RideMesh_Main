package com.project.ridemesh.controllers.driver;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.repositories.UserRepository;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.utils.GeometryUtil;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/drivers")
public class DriverInternalController {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public DriverInternalController(DriverRepository driverRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/nearby-available")
    public ResponseEntity<List<DriverDto>> getNearbyAvailableDrivers(@RequestBody(required = false) PointDto pickUpLocationDto, @RequestParam(defaultValue = "nearest") String strategy) {
        List<Driver> drivers = driverRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getAvailable()))
                .collect(Collectors.toList());
        
        List<DriverDto> driverDtos = drivers.stream()
                .map(driver -> modelMapper.map(driver, DriverDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(driverDtos);
    }

    @GetMapping("/matching")
    public ResponseEntity<List<DriverDto>> getMatchingDrivers(@RequestParam(required = false) String vehicleType, @RequestParam(required = false) List<String> districts) {
        List<Driver> drivers = driverRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getAvailable()))
                .collect(Collectors.toList());
        List<DriverDto> dtos = drivers.stream()
                .map(driver -> modelMapper.map(driver, DriverDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverDto> getDriverById(@PathVariable Long driverId) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(modelMapper.map(driver, DriverDto.class));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DriverDto> getDriverByUserId(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found for user ID: " + userId));
        return ResponseEntity.ok(modelMapper.map(driver, DriverDto.class));
    }
}
