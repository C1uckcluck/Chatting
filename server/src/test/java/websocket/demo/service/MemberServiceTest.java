package websocket.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.Member;
import websocket.demo.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 성공 - 실제 DB 저장 및 비밀번호 암호화 확인")
    void signup_success() {
        // given
        String username = "user";
        String password = "pw";
        String nickname = "nick";

        // when
        Long memberId = memberService.signup(username, password, nickname);

        // then
        Member savedMember = memberRepository.findById(memberId).orElseThrow();
        assertThat(savedMember.getUsername()).isEqualTo(username);
        assertThat(savedMember.getNickname()).isEqualTo(nickname);
        assertThat(savedMember.getPassword()).isNotEqualTo(password); // 암호화 되었으므로 원문과 달라야 함
        assertThat(savedMember.getPassword()).startsWith("$2a$"); // BCrypt 패턴 확인
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자명")
    void signup_fail_duplicate_username() {
        // given
        String username = "user";
        memberService.signup(username, "pw1", "nick1");

        // when & then
        assertThatThrownBy(() -> memberService.signup(username, "pw2", "nick2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 사용자입니다.");
    }
}
