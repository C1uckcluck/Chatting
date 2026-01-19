package websocket.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.Member;
import websocket.demo.dto.NicknameChangeDto;
import websocket.demo.dto.PasswordChangeDto;
import websocket.demo.repository.MemberRepository;
import websocket.demo.service.MemberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("내 정보 조회 성공")
    void get_profile_success() throws Exception {
        memberService.signup("user", "pw", "nick");

        mockMvc.perform(get("/members/me").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.nickname").value("nick"));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void change_password_success() throws Exception {
        memberService.signup("user", "pw", "nick");

        PasswordChangeDto dto = new PasswordChangeDto("pw", "newpw", "newpw");

        mockMvc.perform(post("/members/me/password")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Member member = memberRepository.findByUsername("user").orElseThrow();
        assertThat(passwordEncoder.matches("newpw", member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 확인 불일치")
    void change_password_fail_mismatch() throws Exception {
        memberService.signup("user", "pw", "nick");

        PasswordChangeDto dto = new PasswordChangeDto("pw", "newpw", "wrong");

        mockMvc.perform(post("/members/me/password")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 변경 성공")
    void change_nickname_success() throws Exception {
        memberService.signup("user", "pw", "nick");

        NicknameChangeDto dto = new NicknameChangeDto("pw", "newnick");

        mockMvc.perform(post("/members/me/nickname")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Member member = memberRepository.findByUsername("user").orElseThrow();
        assertThat(member.getNickname()).isEqualTo("newnick");
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 비밀번호 오류")
    void change_nickname_fail_wrong_password() throws Exception {
        memberService.signup("user", "pw", "nick");

        NicknameChangeDto dto = new NicknameChangeDto("wrong", "newnick");

        mockMvc.perform(post("/members/me/nickname")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
