package com.project.ridemesh.controllers.rider;

import com.project.ridemesh.clients.UserServiceClient;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RideRequestDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.services.RiderRideService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rider")
@Secured("ROLE_RIDER")
public class RiderRideController {

    private final RiderRideService riderRideService;
    private final UserServiceClient userServiceClient;

    public RiderRideController(RiderRideService riderRideService, UserServiceClient userServiceClient) {
        this.riderRideService = riderRideService;
        this.userServiceClient = userServiceClient;
    }

    @PostMapping("/requestRide")
    public ResponseEntity<RideRequestDto> requestRide(@RequestBody RideRequestDto rideRequestDto) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(riderRideService.requestRide(rideRequestDto, rider.getId()));
    }

    @PostMapping("/cancelRide/{rideId}")
    public ResponseEntity<RideDto> cancelRide(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(riderRideService.cancelRide(rideId, rider.getId()));
    }

    @PostMapping("/cancelRequest/{requestId}")
    public ResponseEntity<Void> cancelRideRequest(@PathVariable Long requestId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        riderRideService.cancelRideRequest(requestId, rider.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getMyRides")
    public ResponseEntity<Page<RideDto>> getAllMyRides(@RequestParam(defaultValue = "0") Integer pageOffset,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize, Sort.by(Sort.Direction.DESC, "createdTime"));
        return ResponseEntity.ok(riderRideService.getAllMyRides(rider.getId(), pageRequest));
    }

    @PostMapping("/completeRide/{rideId}")
    public ResponseEntity<RideDto> completeRide(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(riderRideService.completeRide(rideId, rider.getId()));
    }

    @GetMapping("/activeRide")
    public ResponseEntity<RideDto> getActiveRide() {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(riderRideService.getActiveRide(rider.getId()));
    }

    @PostMapping("/confirmCashPaid/{rideId}")
    public ResponseEntity<RideDto> confirmCashPaid(@PathVariable Long rideId) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RiderDto rider = userServiceClient.getRiderByUserId(userDto.getId());
        if (rider == null) {
            throw new ResourceNotFoundException("Rider profile not found for user: " + userDto.getId());
        }
        return ResponseEntity.ok(riderRideService.confirmCashPaid(rideId, rider.getId()));
    }

    @PostMapping("/finalizeUpi/{rideId}")
    public ResponseEntity<RideDto> finalizeUpiPayment(@PathVariable Long rideId) {
        return ResponseEntity.ok(riderRideService.finalizeUpiPayment(rideId));
    }

    @PostMapping(value = {"/rateRide/{rideId}", "/completeRide/{rideId}/rate"})
    public ResponseEntity<RideDto> rateRide(@PathVariable Long rideId, 
                                            @RequestParam(required = false) Double rating,
                                            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Double effectiveRating = rating;
        if (effectiveRating == null && body != null && body.get("rating") != null) {
            effectiveRating = Double.parseDouble(body.get("rating").toString());
        }
        if (effectiveRating == null) {
            effectiveRating = 5.0;
        }
        return ResponseEntity.ok(riderRideService.rateRide(rideId, effectiveRating));
    }
}
