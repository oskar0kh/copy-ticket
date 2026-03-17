package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.AuthUserResponseDto;
import copy_ticket.copy_ticket.dto.LoginRequestDto;
import copy_ticket.copy_ticket.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public AuthUserResponseDto login(
            LoginRequestDto loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            // 2. AuthenticationManager의 authenticate() 메서드 호출 -> 미검증 토큰 인증 시도
            //   -> 내부적으로 CustomUserDetailsService의 loadUserByUsername() 메서드 실행해서 DB에서 사용자 조회 및 인증 처리
            //      UserDetailsService를 호출해 DB에서 유저를 찾고, PasswordEncoder로 비밀번호가 맞는지 대조
            //      인증 성공하면, 인증된 사용자 정보가 담긴 Authentication 객체 반환
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated( // 1. 사용자가 입력한 ID/PW 담은 '미검증 토큰' 생성
                            loginRequest.getId(),
                            loginRequest.getPassword()
                    )
            );

            // 3. 빈 SecurityContext 객체 생성 (서버 메모리에 저장)
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();

            // SecurityContext 객체에 인증된 Authentication 객체 저장
            context.setAuthentication(authentication);

            // SecurityContext 객체를 현재 요청을 처리중인 Thread 전용 저장소에 보관(ThreadLocal)
            // -> 이후 같은 요청 범위 내의 Service, Repository 등에서 SecurityContextHolder를 통해 ThreadLoacl에서 꺼내 쓸 수 있음
            //    (이 요청 안에서 다른 서비스들을 거칠때, 현재 로그인중인 사용자가 누군지 바로 확인 가능)
            securityContextHolderStrategy.setContext(context);

            // 4. SecurityContext 객체를 HTTP 세션에 저장 -> 이후 요청에서 세션 통해 SecurityContext 불러와 인증 정보 확인 가능
            securityContextRepository.saveContext(context, request, response);

            // 5. 인증 성공했으면, DB에서 users 테이블 조회해서 response DTO로 변환하여 반환
            User user = userRepository.findById(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다."));

            return AuthUserResponseDto.from(user);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    public AuthUserResponseDto currentUser(Authentication authentication) {
        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다."));

        return AuthUserResponseDto.from(user);
    }
}
