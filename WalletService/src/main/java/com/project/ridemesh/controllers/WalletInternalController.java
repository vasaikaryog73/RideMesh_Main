package com.project.ridemesh.controllers;

import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.Wallet;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.PaymentRepository;
import com.project.ridemesh.services.PaymentService;
import com.project.ridemesh.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wallet/internal")
public class WalletInternalController {

    private final WalletService walletService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    public WalletInternalController(WalletService walletService, PaymentService paymentService, PaymentRepository paymentRepository, ModelMapper modelMapper) {
        this.walletService = walletService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<WalletDto> getWalletBalanceInternal(@PathVariable Long userId) {
        Wallet wallet;
        try {
            wallet = walletService.findByUserId(userId);
        } catch (ResourceNotFoundException e) {
            wallet = walletService.createNewWallet(userId);
        }
        WalletDto walletDto = modelMapper.map(wallet, WalletDto.class);
        return ResponseEntity.ok(walletDto);
    }

    @PostMapping("/settle-ride")
    public ResponseEntity<Map<String, Object>> settleRidePayment(@RequestBody Map<String, Object> req) {
        Long rideId = ((Number) req.get("rideId")).longValue();
        Long riderUserId = ((Number) req.get("riderUserId")).longValue();
        Long driverUserId = ((Number) req.get("driverUserId")).longValue();
        Double fare = ((Number) req.get("fare")).doubleValue();
        String paymentMethod = (String) req.get("paymentMethod");

        if (paymentRepository.findByRideId(rideId).isPresent()) {
            Map<String, Object> res = new HashMap<>();
            res.put("status", "ALREADY_SETTLED");
            return ResponseEntity.ok(res);
        }

        try {
            Payment payment = paymentService.createNewPayment(rideId, driverUserId, riderUserId, fare, paymentMethod);
            paymentService.processPayment(payment);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("status", "ERROR");
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        return ResponseEntity.ok(res);
    }
}
