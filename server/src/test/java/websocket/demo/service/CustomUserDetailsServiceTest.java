package websocket.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.Member;
import websocket.demo.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("username으로 사용자 정보를 조회한다")
    void loadUserByUsername() {
        memberRepository.save(new Member("user1", "hashed", "nick"));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user1");

        assertThat(userDetails.getUsername()).isEqualTo("user1");
        assertThat(userDetails.getPassword()).isEqualTo("hashed");
        assertThat(userDetails.getAuthorities()).extracting("authority")
                .contains("ROLE_USER");
    }

    @Test
    @DisplayName("없는 username 조회 시 예외가 발생한다")
    void loadUserByUsernameNotFound() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
