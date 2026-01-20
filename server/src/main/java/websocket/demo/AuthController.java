package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.SignupDto;
import websocket.demo.service.MemberService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupDto signupDto) {
        memberService.signup(signupDto.username(), signupDto.password(), signupDto.nickname());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
