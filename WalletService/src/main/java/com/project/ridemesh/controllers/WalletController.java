package com.project.ridemesh.controllers;

import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.entities.Wallet;
import com.project.ridemesh.entities.enums.TransactionMethod;
import com.project.ridemesh.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.project.ridemesh.exceptions.ResourceNotFoundException;

import java.util.Map;

import com.project.ridemesh.dto.RazorpayOrderDto;
import org.springframework.beans.factory.annotation.Value;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.repositories.PaymentRepository;
import java.util.HashMap;

@RestController
@RequestMapping("/wallet")
@Secured({"ROLE_RIDER", "ROLE_DRIVER"})
public class WalletController {

    private final WalletService walletService;
    private final ModelMapper modelMapper;
    private final PaymentRepository paymentRepository;
    private final com.project.ridemesh.services.PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public WalletController(WalletService walletService, ModelMapper modelMapper, PaymentRepository paymentRepository, com.project.ridemesh.services.PaymentService paymentService) {
        this.walletService = walletService;
        this.modelMapper = modelMapper;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }



    @GetMapping("/balance")
    public ResponseEntity<WalletDto> getWalletBalance() {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Wallet wallet;
        try {
            wallet = walletService.findByUserId(userDto.getId());
        } catch (ResourceNotFoundException e) {
            wallet = walletService.createNewWallet(userDto.getId());
        }
        WalletDto walletDto = modelMapper.map(wallet, WalletDto.class);
        walletDto.setUser(userDto);
        return ResponseEntity.ok(walletDto);
    }

    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderDto> createOrder(@RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        try {
            int amountInPaise = (int) Math.round(amount * 100);
            String payload = String.format("{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"receipt_txn_%d\"}", amountInPaise, System.currentTimeMillis());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes("UTF-8")))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String body = response.body();
                String orderId = body.split("\"id\":\"")[1].split("\"")[0];
                return ResponseEntity.ok(new RazorpayOrderDto(orderId, amountInPaise, "INR", keyId));
            } else {
                throw new RuntimeException("Failed to create Razorpay Order: status " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Razorpay API: " + e.getMessage(), e);
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<WalletDto> verifyPayment(@RequestBody Map<String, String> request) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orderId = request.get("razorpayOrderId");
        String paymentId = request.get("razorpayPaymentId");
        String signature = request.get("razorpaySignature");
        String amountStr = request.get("amount");

        if (orderId == null || paymentId == null || signature == null || amountStr == null) {
            throw new IllegalArgumentException("Missing payment verification details.");
        }

        double amount = Double.parseDouble(amountStr);

        if (!verifySignature(orderId, paymentId, signature, keySecret)) {
            throw new IllegalArgumentException("Invalid signature verification failed.");
        }

        Wallet wallet = walletService.addMoneyToWallet(
                userDto.getId(),
                amount,
                paymentId,
                null,
                TransactionMethod.BANK
        );

        WalletDto walletDto = modelMapper.map(wallet, WalletDto.class);
        walletDto.setUser(userDto);
        return ResponseEntity.ok(walletDto);
    }

    @PostMapping("/create-withdraw-order")
    public ResponseEntity<RazorpayOrderDto> createWithdrawOrder(@RequestBody Map<String, Double> request) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        
        Wallet wallet = walletService.findByUserId(userDto.getId());
        if (wallet.getBalance() - amount < 500.0) {
            double maxWithdrawable = Math.max(0.0, wallet.getBalance() - 500.0);
            throw new IllegalArgumentException("Cannot withdraw. A minimum balance of ₹500 must be maintained in your wallet. Maximum withdrawable: ₹" + String.format("%.2f", maxWithdrawable));
        }

        try {
            int amountInPaise = (int) Math.round(amount * 100);
            String payload = String.format("{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"receipt_withdraw_%d\"}", amountInPaise, System.currentTimeMillis());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes("UTF-8")))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String body = response.body();
                String orderId = body.split("\"id\":\"")[1].split("\"")[0];
                return ResponseEntity.ok(new RazorpayOrderDto(orderId, amountInPaise, "INR", keyId));
            } else {
                throw new RuntimeException("Failed to create Razorpay Order for withdrawal: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Razorpay API: " + e.getMessage(), e);
        }
    }

    @PostMapping("/verify-withdraw")
    public ResponseEntity<WalletDto> verifyWithdraw(@RequestBody Map<String, String> request) {
        UserDto userDto = (UserDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orderId = request.get("razorpayOrderId");
        String paymentId = request.get("razorpayPaymentId");
        String signature = request.get("razorpaySignature");
        String amountStr = request.get("amount");

        if (orderId == null || paymentId == null || signature == null || amountStr == null) {
            throw new IllegalArgumentException("Missing withdrawal verification details.");
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        Wallet currentWallet = walletService.findByUserId(userDto.getId());
        if (currentWallet.getBalance() - amount < 500.0) {
            double maxWithdrawable = Math.max(0.0, currentWallet.getBalance() - 500.0);
            throw new IllegalArgumentException("Cannot withdraw. A minimum balance of ₹500 must be maintained in your wallet. Maximum withdrawable: ₹" + String.format("%.2f", maxWithdrawable));
        }

        if (!verifySignature(orderId, paymentId, signature, keySecret)) {
            throw new IllegalArgumentException("Invalid signature verification failed.");
        }

        Wallet updatedWallet = walletService.subtractMoneyFromWallet(
                userDto.getId(),
                amount,
                paymentId,
                null,
                TransactionMethod.BANK
        );

        WalletDto walletDto = modelMapper.map(updatedWallet, WalletDto.class);
        walletDto.setUser(userDto);
        return ResponseEntity.ok(walletDto);
    }

    @PostMapping("/pay-ride-upi")
    public ResponseEntity<Map<String, Object>> payRideUpi(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("razorpayOrderId");
        String paymentId = (String) request.get("razorpayPaymentId");
        String signature = (String) request.get("razorpaySignature");
        Double amount = request.get("amount") != null ? Double.parseDouble(request.get("amount").toString()) : null;
        Long rideId = request.get("rideId") != null ? Long.parseLong(request.get("rideId").toString()) : null;
        Long driverUserId = request.get("driverUserId") != null ? Long.parseLong(request.get("driverUserId").toString()) : null;

        if (orderId == null || paymentId == null || signature == null || amount == null || rideId == null || driverUserId == null) {
            throw new IllegalArgumentException("Missing required UPI payment details.");
        }

        if (!verifySignature(orderId, paymentId, signature, keySecret)) {
            throw new IllegalArgumentException("Invalid Razorpay signature verification failed.");
        }

        // Credit driver's wallet with the fare directly
        walletService.addMoneyToWallet(
                driverUserId,
                amount,
                paymentId,
                rideId,
                TransactionMethod.RIDE
        );

        // Record or update payment as CONFIRMED
        Payment payment = paymentRepository.findByRideId(rideId).orElseGet(() -> {
            Payment p = new Payment();
            p.setRideId(rideId);
            p.setDriverId(driverUserId);
            p.setAmount(amount);
            p.setPaymentMethod(PaymentMethod.UPI);
            return p;
        });
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        payment.setPaymentMethod(PaymentMethod.UPI);
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "UPI Payment successful. Driver wallet credited ₹" + String.format("%.2f", amount));
        response.put("paymentId", paymentId);
        response.put("rideId", rideId);
        return ResponseEntity.ok(response);
    }

    private boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        try {
            String data = orderId + "|" + paymentId;
            javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
