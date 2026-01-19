package websocket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.domain.Member;
import websocket.demo.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(String username, String password, String nickname) {
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        Member member = new Member(username, encodedPassword, nickname);
        return memberRepository.save(member).getId();
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String newPasswordConfirm) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호가 서로 일치하지 않습니다.");
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public void changeNickname(String username, String currentPassword, String newNickname) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }

        member.setNickname(newNickname);
        memberRepository.save(member);
    }
}
