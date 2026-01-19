package websocket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import websocket.demo.domain.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);

    List<Member> findByUsernameIn(List<String> usernames);
}
