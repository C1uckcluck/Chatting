package websocket.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.LoginDto;
import websocket.demo.dto.LoginResponseDto;
import websocket.demo.repository.MemberRepository;

public class JsonLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;

    public JsonLoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            ObjectMapper objectMapper,
            MemberRepository memberRepository
    ) {
        this.objectMapper = objectMapper;
        this.memberRepository = memberRepository;
        setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("/auth/login");
        setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        setAuthenticationSuccessHandler(this::handleSuccess);
        setAuthenticationFailureHandler(this::handleFailure);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        LoginDto loginDto;
        try {
            loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);
        } catch (IOException ex) {
            throw new AuthenticationServiceException("Invalid login request.", ex);
        }

        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(loginDto.username(), loginDto.password());
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private void handleSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        var member = memberRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.success(new LoginResponseDto(member.getUsername(), member.getNickname()))
        );
    }

    private void handleFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(exception.getMessage()));
    }
}
