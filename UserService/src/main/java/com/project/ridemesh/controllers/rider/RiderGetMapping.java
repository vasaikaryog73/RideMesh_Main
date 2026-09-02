package com.project.ridemesh.controllers.rider;

import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.services.RiderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rider")
@Secured("ROLE_RIDER")
public class RiderGetMapping {

    private final RiderService riderService;

    public RiderGetMapping(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping("/getMyProfile")
    public ResponseEntity<RiderDto> getRiderProfile(){
        return ResponseEntity.ok(riderService.getRiderProfile());
    }
}
