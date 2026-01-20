package websocket.demo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import websocket.demo.domain.Member;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("사용자 저장 후 username으로 조회한다")
    void saveAndFindMember() {
        // given
        Member member = new Member("testUser", "password", "testNick");

        // when
        memberRepository.save(member);
        Optional<Member> foundMember = memberRepository.findByUsername("testUser");

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getNickname()).isEqualTo("testNick");
    }

    @Test
    @DisplayName("username 목록으로 여러 사용자 조회한다")
    void findByUsernameInReturnsMatchingMembers() {
        Member first = new Member("user1", "pw1", "nick1");
        Member second = new Member("user2", "pw2", "nick2");
        memberRepository.save(first);
        memberRepository.save(second);

        List<Member> found = memberRepository.findByUsernameIn(List.of("user1", "user2", "missing"));

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Member::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");
    }
}
