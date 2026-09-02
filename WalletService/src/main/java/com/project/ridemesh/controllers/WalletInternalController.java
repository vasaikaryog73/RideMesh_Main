package com.project.ridemesh.controllers;

import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.entities.Wallet;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet/internal")
public class WalletInternalController {

    private final WalletService walletService;
    private final ModelMapper modelMapper;

    public WalletInternalController(WalletService walletService, ModelMapper modelMapper) {
        this.walletService = walletService;
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
}
