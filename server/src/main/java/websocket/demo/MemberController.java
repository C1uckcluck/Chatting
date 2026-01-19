package websocket.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import websocket.demo.domain.Member;
import websocket.demo.dto.MemberProfileDto;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.NicknameChangeDto;
import websocket.demo.dto.PasswordChangeDto;
import websocket.demo.repository.MemberRepository;
import websocket.demo.service.MemberService;

import java.security.Principal;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileDto>> getProfile(Principal principal) {
        Member member = memberRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(ApiResponse.success(new MemberProfileDto(member.getUsername(), member.getNickname())));
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody PasswordChangeDto changeDto, Principal principal) {
        memberService.changePassword(
                principal.getName(),
                changeDto.currentPassword(),
                changeDto.newPassword(),
                changeDto.newPasswordConfirm()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> changeNickname(@RequestBody NicknameChangeDto changeDto, Principal principal) {
        memberService.changeNickname(
                principal.getName(),
                changeDto.currentPassword(),
                changeDto.newNickname()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
