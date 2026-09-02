package com.project.ridemesh.controllers.driver;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.services.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/driver")
@Secured("ROLE_DRIVER")
public class DriverGetMapping {

    private final DriverService driverService;

    public DriverGetMapping(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/getMyProfile")
    public ResponseEntity<DriverDto> getDriverProfile(){
        return ResponseEntity.ok(driverService.getDriverProfile());
    }
}
