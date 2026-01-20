package websocket.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import websocket.demo.config.jwt.JwtTokenProvider;
import websocket.demo.domain.Member;
import websocket.demo.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class JsonLoginAuthenticationFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("nonPostMethods")
    @DisplayName("POST가 아니면 로그인 시도를 거부한다")
    void attemptAuthentication_requiresPost(String method) {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

        TestJsonLoginAuthenticationFilter filter = new TestJsonLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                memberRepository,
                jwtTokenProvider
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(request, response))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("Authentication method not supported");
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 프로필 정보를 응답에 포함한다")
    void successHandler_writesTokenAndProfile() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

        when(jwtTokenProvider.createToken("tester")).thenReturn("test-token");
        Member member = new Member();
        member.setUsername("tester");
        member.setNickname("tester-nick");
        when(memberRepository.findByUsername("tester")).thenReturn(java.util.Optional.of(member));

        TestJsonLoginAuthenticationFilter filter = new TestJsonLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                memberRepository,
                jwtTokenProvider
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"tester\",\"password\":\"pw\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        User principal = new User("tester", "pw", java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        filter.successHandler().onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        JsonNode payload = objectMapper.readTree(response.getContentAsString());
        assertThat(payload.path("success").asBoolean()).isTrue();
        assertThat(payload.path("data").path("username").asText()).isEqualTo("tester");
        assertThat(payload.path("data").path("nickname").asText()).isEqualTo("tester-nick");
        assertThat(payload.path("data").path("accessToken").asText()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("로그인 실패 시 401과 에러 메시지를 반환한다")
    void failureHandler_returnsUnauthorized() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

        TestJsonLoginAuthenticationFilter filter = new TestJsonLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                memberRepository,
                jwtTokenProvider
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.failureHandler().onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("bad credentials")
        );

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        JsonNode payload = objectMapper.readTree(response.getContentAsString());
        assertThat(payload.path("success").asBoolean()).isFalse();
        assertThat(payload.path("message").asText()).contains("bad credentials");
    }

    @Test
    @DisplayName("로그인 본문을 파싱해 인증 매니저에 위임한다")
    void attemptAuthentication_parsesBodyAndDelegates() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("tester", "pw"));

        TestJsonLoginAuthenticationFilter filter = new TestJsonLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                memberRepository,
                jwtTokenProvider
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"tester\",\"password\":\"pw\"}".getBytes(StandardCharsets.UTF_8));

        Authentication result = filter.attemptAuthentication(request, new MockHttpServletResponse());
        assertThat(result.getName()).isEqualTo("tester");
    }

    private static Stream<String> nonPostMethods() {
        return Arrays.stream(new String[] {"GET", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE"});
    }

    private static class TestJsonLoginAuthenticationFilter extends JsonLoginAuthenticationFilter {

        TestJsonLoginAuthenticationFilter(
                AuthenticationManager authenticationManager,
                ObjectMapper objectMapper,
                MemberRepository memberRepository,
                JwtTokenProvider jwtTokenProvider
        ) {
            super(authenticationManager, objectMapper, memberRepository, jwtTokenProvider);
        }

        public org.springframework.security.web.authentication.AuthenticationSuccessHandler successHandler() {
            return super.getSuccessHandler();
        }

        public org.springframework.security.web.authentication.AuthenticationFailureHandler failureHandler() {
            return super.getFailureHandler();
        }
    }
}
