package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.AuthUserResponseDto;
import copy_ticket.copy_ticket.dto.LoginRequestDto;
import copy_ticket.copy_ticket.dto.SignupRequestDto;
import copy_ticket.copy_ticket.dto.WithdrawRequestDto;
import copy_ticket.copy_ticket.service.AuthService;
import copy_ticket.copy_ticket.service.UserRegistrationService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthApiController {

    private final UserRegistrationService userRegistrationService;
    private final AuthService authService;

    public AuthApiController(
            UserRegistrationService userRegistrationService,
            AuthService authService
    ) {
        this.userRegistrationService = userRegistrationService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto signupRequest) {
        try {
            userRegistrationService.register(signupRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthUserResponseDto responseDto = authService.login(loginRequest, request, response);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        authService.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<AuthUserResponseDto> currentUser(Authentication authentication) {
        AuthUserResponseDto responseDto = authService.currentUser(authentication);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<Map<String, String>> withdraw(
            @Valid @RequestBody WithdrawRequestDto withdrawRequest,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        authService.withdraw(withdrawRequest, request, response, authentication);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }
}
