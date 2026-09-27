package com.payflow.backend.controller;

import com.payflow.backend.entity.User;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.repository.UserRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.payflow.backend.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;

    @GetMapping("/resolve")
    public ResponseEntity<Map<String, String>> resolveUser(
            Authentication authentication,
            @RequestParam(required = false) String upi,
            @RequestParam(required = false) String phone) {

        if ((upi == null && phone == null) || (upi != null && phone != null)) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Wallet> walletOpt = Optional.empty();

        if (upi != null) {
            walletOpt = walletRepository.findByUpiHandle(upi);
        } else if (phone != null) {
            Optional<User> userOpt = userRepository.findByPhone(phone);
            if (userOpt.isPresent()) {
                walletOpt = walletRepository.findByUserId(userOpt.get().getId());
            }
        }

        if (walletOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String callerId = (String) authentication.getPrincipal();
        if (walletOpt.get().getUserId().toString().equals(callerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot send money to yourself."));
        }

        Map<String, String> response = new HashMap<>();
        response.put("walletId", walletOpt.get().getId().toString());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> softDeleteUser(Authentication authentication) {
        String userIdStr = (String) authentication.getPrincipal();
        userService.softDeleteUser(java.util.UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
