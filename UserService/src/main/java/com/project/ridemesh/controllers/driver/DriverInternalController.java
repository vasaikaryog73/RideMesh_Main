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
                .filter(d -> isVehicleTypeMatch(d.getVehicleType(), vehicleType))
                .filter(d -> {
                    if (districts == null || districts.isEmpty()) return true;
                    return districts.stream().anyMatch(reqDist -> isDistrictMatch(d.getDistrict(), reqDist));
                })
                .collect(Collectors.toList());
        List<DriverDto> dtos = drivers.stream()
                .map(driver -> modelMapper.map(driver, DriverDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private boolean isDistrictMatch(String driverDistrict, String reqDistrict) {
        if (driverDistrict == null || driverDistrict.isBlank() || reqDistrict == null || reqDistrict.isBlank()) {
            return true;
        }
        String d1 = cleanDistrictName(driverDistrict);
        String d2 = cleanDistrictName(reqDistrict);

        if (d1.equals(d2)) return true;
        if (d1.contains(d2) || d2.contains(d1)) return true;

        if ((d1.contains("mumbai") && d2.contains("mumbai")) ||
            (d1.contains("bengaluru") && d2.contains("bengaluru")) ||
            (d1.contains("bangalore") && (d2.contains("bangalore") || d2.contains("bengaluru"))) ||
            (d1.contains("delhi") && d2.contains("delhi"))) {
            return true;
        }

        return false;
    }

    private String cleanDistrictName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\b(district|dist|city|suburban|rural|urban)\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private boolean isVehicleTypeMatch(String driverVehicleType, String requestedVehicleType) {
        if (requestedVehicleType == null || requestedVehicleType.isBlank()) {
            return true;
        }
        if (driverVehicleType == null || driverVehicleType.isBlank()) {
            return false;
        }
        String d = driverVehicleType.trim().toLowerCase();
        String r = requestedVehicleType.trim().toLowerCase();

        if (d.equals(r)) {
            return true;
        }

        // Scooter and scooty alias
        if ((d.equals("scooter") || d.equals("scooty")) && (r.equals("scooter") || r.equals("scooty"))) {
            return true;
        }

        // Auto / Rickshaw alias
        if ((d.contains("rickshaw") || d.contains("auto")) && (r.contains("rickshaw") || r.contains("auto"))) {
            return true;
        }

        return false;
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
