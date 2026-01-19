package websocket.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import websocket.demo.domain.Member;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
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
}
