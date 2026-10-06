package com.microcommerce.userservice.service;

import com.microcommerce.userservice.dto.AuthenticationRequest;
import com.microcommerce.userservice.dto.AuthenticationResponse;
import com.microcommerce.userservice.dto.RegisterRequest;
import com.microcommerce.userservice.model.Role;
import com.microcommerce.userservice.model.User;
import com.microcommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();
        return buildResponse(repository.save(user));
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        return buildResponse(user);
    }

    private AuthenticationResponse buildResponse(User user) {
        // uid and role are read by the API gateway and forwarded to the
        // downstream services for ownership and admin checks.
        Map<String, Object> claims = Map.of(
                "uid", user.getId(),
                "role", user.getRole().name()
        );
        var jwtToken = jwtService.generateToken(claims, org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build());
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
