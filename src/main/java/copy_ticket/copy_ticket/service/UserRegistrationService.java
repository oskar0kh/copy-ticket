package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.SignupRequestDto;
import copy_ticket.copy_ticket.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(SignupRequestDto request) {

        // 활성 사용자 기준 ID 중복 체크 (soft delete된 계정은 재사용 허용)
        if (userRepository.existsByIdAndDeletedAtIsNull(request.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 사용자가 입력한 PW를 BCrypt로 해싱(암호화) -> DB에 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 엔티티 생성 : ID, 해싱된 PW, 이름
        User user = User.createForSignup(
            request.getId(),
                encodedPassword,
                request.getName()
        );

        // 'users' 테이블에 User 엔티티 저장
        userRepository.save(user);
    }
}
