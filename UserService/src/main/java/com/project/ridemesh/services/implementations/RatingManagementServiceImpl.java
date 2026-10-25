package com.project.ridemesh.services.implementations;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.Rating;
import com.project.ridemesh.entities.Rider;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.DriverRepository;
import com.project.ridemesh.repositories.RatingRepository;
import com.project.ridemesh.repositories.RiderRepository;
import com.project.ridemesh.services.RatingManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RatingManagementServiceImpl implements RatingManagementService {

    private static final Logger log = LoggerFactory.getLogger(RatingManagementServiceImpl.class);
    private final RatingRepository ratingRepository;
    private final DriverRepository driverRepository;
    private final RiderRepository riderRepository;
    private final ModelMapper modelMapper;

    public RatingManagementServiceImpl(RatingRepository ratingRepository, ModelMapper modelMapper, DriverRepository driverRepository, RiderRepository riderRepository) {
        this.ratingRepository = ratingRepository;
        this.driverRepository = driverRepository;
        this.riderRepository = riderRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public DriverDto rateDriver(Long rideId, Rider rider, Double rating) {
        return rateDriver(rideId, rider, rating, null);
    }

    @Override
    @Transactional
    public DriverDto rateDriver(Long rideId, Rider rider, Double rating, Long driverIdParam) {
        Optional<Rating> optionalRating = ratingRepository.findByRideId(rideId);

        Rating ratingObj;
        if (optionalRating.isPresent()) {
            ratingObj = optionalRating.get();
            if (ratingObj.getDriverRating() != null && ratingObj.getDriverRating() != 0.0) {
                throw new RuntimeConflictException("Cannot rate driver again!");
            }
            if (ratingObj.getDriverId() == null && driverIdParam != null) {
                ratingObj.setDriverId(driverIdParam);
            }
            ratingObj.setDriverRating(rating);
            ratingRepository.save(ratingObj);
        } else {
            Long effectiveDriverId = driverIdParam;
            if (effectiveDriverId == null) {
                var allDrivers = driverRepository.findAll();
                effectiveDriverId = !allDrivers.isEmpty() ? allDrivers.get(0).getId() : 1L;
            }
            // Race condition: ride-ended-event has not been processed yet. Create stub on-the-fly.
            ratingObj = new Rating.RatingBuilder()
                    .rideId(rideId)
                    .riderId(rider.getId())
                    .driverId(effectiveDriverId)
                    .driverRating(rating)
                    .riderRating(0.0)
                    .build();
            try {
                ratingRepository.save(ratingObj);
            } catch (DataIntegrityViolationException e) {
                // Caught concurrent insert due to unique constraint on rideId. Retry check-then-act.
                ratingObj = ratingRepository.findByRideId(rideId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rating not found after unique constraint conflict."));
                if (ratingObj.getDriverRating() != null && ratingObj.getDriverRating() != 0.0) {
                    throw new RuntimeConflictException("Cannot rate driver again!");
                }
                if (ratingObj.getDriverId() == null && effectiveDriverId != null) {
                    ratingObj.setDriverId(effectiveDriverId);
                }
                ratingObj.setDriverRating(rating);
                ratingRepository.save(ratingObj);
            }
        }

        final Long driverId = ratingObj.getDriverId();
        if (driverId != null) {
            Driver driverObj = driverRepository.findById(driverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));
            Double newAvgRating = ratingRepository.findByDriverId(driverObj.getId())
                    .stream()
                    .filter(r -> r.getDriverRating() != null && r.getDriverRating() != 0.0)
                    .mapToDouble(Rating::getDriverRating)
                    .average()
                    .orElse(rating);
            double roundedAvg = Math.round(newAvgRating * 10.0) / 10.0;
            driverObj.setRating(roundedAvg);
            Driver savedDriver = driverRepository.save(driverObj);
            return modelMapper.map(savedDriver, DriverDto.class);
        }

        // Return a default mapped DTO if driver is not resolved yet
        DriverDto fallbackDto = new DriverDto();
        fallbackDto.setRating(rating);
        return fallbackDto;
    }

    @Override
    @Transactional
    public RiderDto rateRider(Long rideId, Driver driver, Double rating) {
        Optional<Rating> optionalRating = ratingRepository.findByRideId(rideId);

        Rating ratingObj;
        if (optionalRating.isPresent()) {
            ratingObj = optionalRating.get();
            if (ratingObj.getRiderRating() != 0.0) {
                throw new RuntimeConflictException("Cannot rate rider again!");
            }
            ratingObj.setRiderRating(rating);
            ratingRepository.save(ratingObj);
        } else {
            // Race condition: ride-ended-event has not been processed yet. Create stub on-the-fly.
            ratingObj = new Rating.RatingBuilder()
                    .rideId(rideId)
                    .riderId(null) // populated when ride-ended-event is consumed
                    .driverId(driver.getId())
                    .driverRating(0.0)
                    .riderRating(rating)
                    .build();
            try {
                ratingRepository.save(ratingObj);
            } catch (DataIntegrityViolationException e) {
                // Caught concurrent insert due to unique constraint on rideId. Retry check-then-act.
                ratingObj = ratingRepository.findByRideId(rideId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rating not found after unique constraint conflict."));
                if (ratingObj.getRiderRating() != 0.0) {
                    throw new RuntimeConflictException("Cannot rate rider again!");
                }
                ratingObj.setRiderRating(rating);
                ratingRepository.save(ratingObj);
            }
        }

        final Long riderId = ratingObj.getRiderId();
        if (riderId != null) {
            Rider riderObj = riderRepository.findById(riderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rider not found with ID: " + riderId));
            Double newAvgRating = ratingRepository.findByRiderId(riderObj.getId())
                    .stream()
                    .filter(r -> r.getRiderRating() != 0.0)
                    .mapToDouble(Rating::getRiderRating)
                    .average()
                    .orElse(0.0);
            riderObj.setRating(newAvgRating);
            Rider savedRider = riderRepository.save(riderObj);
            return modelMapper.map(savedRider, RiderDto.class);
        }

        RiderDto fallbackDto = new RiderDto();
        fallbackDto.setRating(0.0);
        return fallbackDto;
    }

    @Override
    @Transactional
    public void createNewRating(Long rideId, Long riderId, Long driverId) {
        Optional<Rating> optionalRating = ratingRepository.findByRideId(rideId);

        if (optionalRating.isPresent()) {
            // A stub was created on-the-fly when rated before the event arrived. Fill in the missing IDs.
            Rating ratingObj = optionalRating.get();
            ratingObj.setRiderId(riderId);
            ratingObj.setDriverId(driverId);
            ratingRepository.save(ratingObj);

            // Update average ratings if they were already rated in the stub
            if (ratingObj.getDriverRating() != 0.0) {
                Driver driverObj = driverRepository.findById(driverId).orElse(null);
                if (driverObj != null) {
                    Double newAvgRating = ratingRepository.findByDriverId(driverId)
                            .stream()
                            .filter(r -> r.getDriverRating() != 0.0)
                            .mapToDouble(Rating::getDriverRating)
                            .average()
                            .orElse(0.0);
                    driverObj.setRating(newAvgRating);
                    driverRepository.save(driverObj);
                }
            }

            if (ratingObj.getRiderRating() != 0.0) {
                Rider riderObj = riderRepository.findById(riderId).orElse(null);
                if (riderObj != null) {
                    Double newAvgRating = ratingRepository.findByRiderId(riderId)
                            .stream()
                            .filter(r -> r.getRiderRating() != 0.0)
                            .mapToDouble(Rating::getRiderRating)
                            .average()
                            .orElse(0.0);
                    riderObj.setRating(newAvgRating);
                    riderRepository.save(riderObj);
                }
            }
        } else {
            Rating rating = new Rating.RatingBuilder()
                    .rideId(rideId)
                    .riderId(riderId)
                    .driverId(driverId)
                    .driverRating(0.0)
                    .riderRating(0.0)
                    .build();
            try {
                ratingRepository.save(rating);
            } catch (DataIntegrityViolationException e) {
                // Idempotency: duplicate key violation means it was created concurrently. Ignored.
                log.info("Ignoring duplicate rating stub creation for rideId: " + rideId);
            }
        }
    }
}
