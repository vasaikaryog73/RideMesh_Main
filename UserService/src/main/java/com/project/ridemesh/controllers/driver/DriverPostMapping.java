package com.project.ridemesh.controllers.driver;

import com.project.ridemesh.dto.RatingDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.services.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver")
@Secured("ROLE_DRIVER")
public class DriverPostMapping {

    private final DriverService driverService;

    public DriverPostMapping(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/rateRider/{rideId}")
    public ResponseEntity<RiderDto> rateRider(@PathVariable Long rideId, @RequestBody RatingDto ratingDto){
        return ResponseEntity.ok(driverService.rateRider(rideId, ratingDto.getRating()));
    }
}
