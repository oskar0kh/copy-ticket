package copy_ticket.copy_ticket.service.security;

import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // 'users' 테이블 조회를 위한 UserRepository 객체 생성
    private final UserRepository userRepository;

    // Spring이 위에서 생성한 userRepository 객체에 'UserRepository' Bean을 주입해줌 (DI)
    //    -> userRepository 객체로 'users' 테이블 CRUD 작업 수행
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * loadUserByUsername()
     *  - 로그인 시, 사용자가 입력한 ID/PW랑 'users' 테이블의 ID/PW 비교하는 메서드
    */
    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        
        // 1. 'users' 테이블에서 사용자 아이디(id)로 User 엔티티 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

        // 2. 조회된 사용자의 ID/PW를, Spring Security의 UserDetails 객체로 변환하여 반환
        //   -> Spring Security가 사용자가 입력한 ID/PW랑 UserDetails 객체의 ID/PW 비교하여 인증 처리
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
