package com.project.ridemesh.controllers.driver;

import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RideRequestDto;
import com.project.ridemesh.dto.RideStartDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.services.DriverRideService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/driver")
@Secured("ROLE_DRIVER")
public class DriverRideController {

    private final DriverRideService driverRideService;
    private final UserServiceClient userServiceClient;

    public DriverRideController(DriverRideService driverRideService, UserServiceClient userServiceClient) {
        this.driverRideService = driverRideService;
        this.userServiceClient = userServiceClient;
    }

    @PostMapping("/acceptRide/{rideRequestId}")
    public ResponseEntity<RideDto> acceptRide(@PathVariable Long rideRequestId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.acceptRide(rideRequestId, driver.getId()));
    }

    @PostMapping("/declineRide/{rideRequestId}")
    public ResponseEntity<Void> declineRide(@PathVariable Long rideRequestId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        driverRideService.declineRide(rideRequestId, driver.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/startRide/{rideId}")
    public ResponseEntity<RideDto> startRide(@PathVariable Long rideId, @RequestBody RideStartDto rideStartDto) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.startRide(rideId, rideStartDto.getOtp(), driver.getId()));
    }

    @PostMapping("/endRide/{rideId}")
    public ResponseEntity<RideDto> endRide(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.endRide(rideId, driver.getId()));
    }

    @PostMapping("/cancelRide/{rideId}")
    public ResponseEntity<RideDto> cancelRide(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.cancelRide(rideId, driver.getId()));
    }

    @GetMapping("/getMyRides")
    public ResponseEntity<Page<RideDto>> getAllMyRides(@RequestParam(defaultValue = "0") Integer pageOffset,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize, Sort.by(Sort.Direction.DESC, "createdTime"));
        return ResponseEntity.ok(driverRideService.getAllMyRides(driver.getId(), pageRequest));
    }

    @GetMapping("/pendingRides")
    public ResponseEntity<List<RideRequestDto>> getPendingRidesNearDriver() {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.getPendingRidesNearDriver(driver.getId()));
    }

    @PostMapping("/completeRide/{rideId}")
    public ResponseEntity<RideDto> completeRide(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.completeRide(rideId, driver.getId()));
    }

    @GetMapping("/activeRide")
    public ResponseEntity<RideDto> getActiveRide() {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.getActiveRide(driver.getId()));
    }

    @PostMapping("/confirmCashReceived/{rideId}")
    public ResponseEntity<RideDto> confirmCashReceived(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DriverDto driver = userServiceClient.getDriverByUserId(userDto.getId());
        if (driver == null) {
            throw new ResourceNotFoundException("Driver profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(driverRideService.confirmCashReceived(rideId, driver.getId()));
    }
}
