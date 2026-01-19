package websocket.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.domain.Member;
import websocket.demo.dto.LoginDto;
import websocket.demo.dto.LoginResponseDto;
import websocket.demo.dto.SignupDto;
import websocket.demo.repository.MemberRepository;
import websocket.demo.service.MemberService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupDto signupDto) {
        memberService.signup(signupDto.username(), signupDto.password(), signupDto.nickname());
        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(loginDto.username(), loginDto.password());

        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        Member member = memberRepository.findByUsername(loginDto.username())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(new LoginResponseDto(member.getUsername(), member.getNickname()));
    }
}
