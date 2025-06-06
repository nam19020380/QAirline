package org.example.controller;

import jakarta.validation.Valid;
import org.example.entity.User;
import org.example.payload.*;
import org.example.security.JwtUtils;
import org.example.service.UserService;
import org.example.serviceImpl.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserService userService;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            User user = userService.findByEmail(loginRequest.getEmail());
            JwtResponse jwtResponse = new JwtResponse(jwtUtils.generateJwtToken(user.getEmail()),
                    user.getId(),
                    user.getEmail(),
                    user.getEmail());
            return ResponseEntity.ok().body(jwtResponse);
        } catch(Exception e){
            return ResponseEntity.badRequest()
                    .body("Incorrect password or email");
        }
    }

    @PostMapping(value = "/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try{
            if (userService.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body("Error: Email is already in use!");
            }

            // Create new user's account
            User user = new User();
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(signUpRequest.getPassword());
            user.setName(signUpRequest.getEmail());

            userService.saveUser(user);

            return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
        }catch(Exception e){
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/forgetP")
    public ResponseEntity<?> forgetPassword(@Valid @RequestBody ForgetPasswordRequest forgetPasswordRequest) {
        if(userService.existsByEmail(forgetPasswordRequest.getEmail())){
            EmailService emailService = new EmailService();
            Long key = Math.round(Math.random() * (999999 - 100000 + 1) + 100000);
            String tempPassword = key.toString();
            User user = userService.findByEmail(forgetPasswordRequest.getEmail());
            user.setPassword(tempPassword);
            user.setForgotten(true);
            userService.saveUser(user);
            emailService.send(tempPassword, forgetPasswordRequest.getEmail());
            return new ResponseEntity<>("Get new password successfully", HttpStatus.OK);
        } else {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is not exist!");
        }
    }

    @PutMapping("/changeP")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(changePasswordRequest.getEmail(), changePasswordRequest.getOldPassword()));
            User user = userService.findByEmail(changePasswordRequest.getEmail());
            user.setPassword(changePasswordRequest.getNewPassword());
            user.setForgotten(false);
            userService.saveUser(user);
            return ResponseEntity.ok().body("Password change successfully!");
        } catch (Exception e){
            return ResponseEntity.badRequest()
                    .body("Server Error");
        }
    }
}
