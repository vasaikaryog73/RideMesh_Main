package com.project.ridemesh.controllers;

import com.project.ridemesh.dto.RatingDto;
import com.project.ridemesh.repositories.RatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ratings")
public class RatingInternalController {

    private final RatingRepository ratingRepository;

    public RatingInternalController(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<RatingDto> getRatingByRideId(@PathVariable Long rideId) {
        return ratingRepository.findByRideId(rideId)
                .map(r -> {
                    RatingDto dto = new RatingDto();
                    dto.setRideId(r.getRideId());
                    dto.setDriverId(r.getDriverId());
                    dto.setDriverRating(r.getDriverRating());
                    dto.setRiderRating(r.getRiderRating());
                    dto.setRating(r.getDriverRating());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
