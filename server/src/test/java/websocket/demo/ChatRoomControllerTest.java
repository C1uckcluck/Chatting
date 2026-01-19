package websocket.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import websocket.demo.domain.ChatRoomEntity;
import websocket.demo.domain.ChatRoomMemberEntity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("참여중인 채팅방 목록 조회 성공")
    void get_my_rooms_success() throws Exception {
        ChatRoomEntity room = new ChatRoomEntity("room-1", "Study Room");
        entityManager.persist(room);
        entityManager.persist(new ChatRoomMemberEntity(room, "user"));
        entityManager.flush();

        mockMvc.perform(get("/chat/rooms/my").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value("room-1"))
                .andExpect(jsonPath("$[0].name").value("Study Room"));
    }

    @Test
    @DisplayName("채팅방 나가기 시 참여 목록에서 제외")
    void leave_room_removes_membership() throws Exception {
        ChatRoomEntity room = new ChatRoomEntity("room-2", "Game Room");
        entityManager.persist(room);
        entityManager.persist(new ChatRoomMemberEntity(room, "user"));
        entityManager.flush();

        mockMvc.perform(post("/chat/rooms/room-2/leave").with(user("user")).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/chat/rooms/my").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("채팅방 입장 시 참여 목록에 포함")
    void enter_room_adds_membership() throws Exception {
        ChatRoomEntity room = new ChatRoomEntity("room-3", "Music Room");
        entityManager.persist(room);
        entityManager.flush();

        mockMvc.perform(post("/chat/rooms/room-3/enter").with(user("user")).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/chat/rooms/my").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value("room-3"));
    }
}
