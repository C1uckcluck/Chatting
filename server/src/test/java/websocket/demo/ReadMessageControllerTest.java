package websocket.demo;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import websocket.demo.service.ChatMessageService;
import websocket.demo.service.LastReadTimestampService;
import websocket.demo.service.MessageBroadcastService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReadMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LastReadTimestampService lastReadTimestampService;

    @MockBean
    private ChatMessageService chatMessageService;

    @MockBean
    private MessageBroadcastService messageBroadcastService;

    @Test
    @DisplayName("markAsRead uses authenticated user and accepts empty body")
    void mark_as_read_uses_authenticated_user() throws Exception {
        when(lastReadTimestampService.updateLastReadTimestamp(eq("room-1"), eq("alice")))
                .thenReturn(null);
        when(chatMessageService.decrementUnreadCounts(eq("room-1"), eq("alice"), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(post("/chat/rooms/room-1/read").with(user("alice")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(lastReadTimestampService).updateLastReadTimestamp("room-1", "alice");
        verify(chatMessageService).decrementUnreadCounts("room-1", "alice", null);
        verify(messageBroadcastService, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("markAsRead requires authentication")
    void mark_as_read_requires_authentication() throws Exception {
        mockMvc.perform(post("/chat/rooms/room-1/read").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
