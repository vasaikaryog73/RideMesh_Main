package com.project.ridemesh.controllers.rider;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RatingDto;
import com.project.ridemesh.services.RiderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rider")
@Secured("ROLE_RIDER")
public class RiderPostMapping {

    private final RiderService riderService;

    public RiderPostMapping(RiderService riderService) {
        this.riderService = riderService;
    }

    @PostMapping("/rateDriver/{rideId}")
    public ResponseEntity<DriverDto> rateDriver(@PathVariable Long rideId, @RequestBody RatingDto ratingDto){
        return ResponseEntity.ok(riderService.rateDriver(rideId, ratingDto.getRating()));
    }
}
