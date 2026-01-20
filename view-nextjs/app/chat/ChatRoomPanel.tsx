"use client";

import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";

interface ChatMessage {
    type:
        | "ENTER"
        | "TALK"
        | "LEAVE"
        | "IMAGE"
        | "READ_UPDATE"
        | "PRESENCE_UPDATE";
    id?: number | null;
    sender: string;
    content: string;
    imageUrl?: string | null;
    sendAt: string;
    unreadCount: number;
    updates?: { messageId: number; unreadCount: number }[];
    username?: string;
    nickname?: string;
    online?: boolean;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string | null;
}

interface Participant {
    username: string;
    nickname: string;
    online: boolean;
}

type ChatRoomPanelProps = {
    roomId: string;
    variant?: "page" | "panel";
    onLeave?: () => void;
};

export default function ChatRoomPanel({
    roomId,
    variant = "page",
    onLeave,
}: ChatRoomPanelProps) {
    const router = useRouter();

    const [roomName, setRoomName] = useState<string>("");
    const [messageInput, setMessageInput] = useState<string>("");
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [sender, setSender] = useState<string>("");
    const [selfNicknames, setSelfNicknames] = useState<string[]>([]);
    const [participants, setParticipants] = useState<Participant[]>([]);

    const clientRef = useRef<Client | null>(null);
    const subscriptionRef = useRef<StompSubscription | null>(null);
    const readDebounceTimer = useRef<NodeJS.Timeout | null>(null);
    const pendingReadUsername = useRef<string | null>(null);

    const getAuthHeaders = () => {
        const token = localStorage.getItem("chatAccessToken");
        return token ? { Authorization: `Bearer ${token}` } : {};
    };

    const handleAuthError = (response: Response) => {
        if (response.status === 401 || response.status === 403) {
            router.push("/login");
            return true;
        }
        return false;
    };

    useEffect(() => {
        const savedUsername = localStorage.getItem("chatUsername");
        const savedNickname = localStorage.getItem("chatNickname");
        const rawHistory = localStorage.getItem("chatNicknameHistory");
        const history = rawHistory ? (JSON.parse(rawHistory) as string[]) : [];
        if (!savedUsername) {
            alert("사용자 정보가 없습니다. 로비로 돌아갑니다.");
            router.push("/");
            return;
        }
        setSender(savedNickname || savedUsername);
        setSelfNicknames(
            [savedNickname, savedUsername, ...history].filter(
                (value): value is string => Boolean(value),
            ),
        );
    }, [router]);

    useEffect(() => {
        setRoomName("");
        setMessages([]);
        setParticipants([]);
        setMessageInput("");
    }, [roomId]);

    useEffect(() => {
        if (!roomId) return;

        const fetchRoomData = async () => {
            try {
                const enterResponse = await fetch(`/chat/rooms/${roomId}/enter`, {
                    method: "POST",
                    headers: {
                        ...getAuthHeaders(),
                    },
                });
                if (handleAuthError(enterResponse)) {
                    return;
                }

                const nameResponse = await fetch(`/chat/rooms/${roomId}`, {
                    headers: {
                        ...getAuthHeaders(),
                    },
                });
                if (handleAuthError(nameResponse)) {
                    return;
                }
                if (nameResponse.ok) {
                    const payload: ApiResponse<{ name: string }> | null =
                        await nameResponse.json().catch(() => null);
                    if (payload?.success && payload.data?.name) {
                        setRoomName(payload.data.name);
                    }
                }

                const messagesResponse = await fetch(
                    `/chat/rooms/${roomId}/messages`,
                    {
                        headers: {
                            ...getAuthHeaders(),
                        },
                    },
                );
                if (handleAuthError(messagesResponse)) {
                    return;
                }
                if (messagesResponse.ok) {
                    const payload: ApiResponse<ChatMessage[]> | null =
                        await messagesResponse.json().catch(() => null);
                    if (payload?.success) {
                        setMessages(payload.data || []);
                    }
                }

                const participantsResponse = await fetch(
                    `/chat/rooms/${roomId}/participants`,
                    {
                        headers: {
                            ...getAuthHeaders(),
                        },
                    },
                );
                if (handleAuthError(participantsResponse)) {
                    return;
                }
                if (participantsResponse.ok) {
                    const payload: ApiResponse<Participant[]> | null =
                        await participantsResponse.json().catch(() => null);
                    if (payload?.success) {
                        setParticipants(payload.data || []);
                    }
                }

                const savedUsername = localStorage.getItem("chatUsername");
                if (savedUsername) {
                    scheduleRead(savedUsername);
                }
            } catch (error) {
                console.error("Error fetching room data:", error);
            }
        };

        fetchRoomData();
    }, [roomId]);

    const markAsRead = async (username: string) => {
        await fetch(`/chat/rooms/${roomId}/read`, {
            method: "POST",
            headers: {
                "Content-Type": "text/plain",
                ...getAuthHeaders(),
            },
            body: username,
        });
    };

    const scheduleRead = (username: string) => {
        pendingReadUsername.current = username;
        if (readDebounceTimer.current) {
            clearTimeout(readDebounceTimer.current);
        }
        readDebounceTimer.current = setTimeout(() => {
            const pending = pendingReadUsername.current;
            pendingReadUsername.current = null;
            if (pending) {
                markAsRead(pending).catch((error) =>
                    console.error("Read update failed", error),
                );
            }
        }, 3000);
    };

    useEffect(() => {
        if (!sender || !roomId) {
            return;
        }

        const accessToken = localStorage.getItem("chatAccessToken");
        const client = new Client({
            webSocketFactory: () =>
                new SockJS("http://localhost:8080/ws-stomp"),
            connectHeaders: {
                username: localStorage.getItem("chatUsername") || sender,
                nickname: sender,
                ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
            },
            debug: (str) => console.log(new Date(), str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        clientRef.current = client;

        client.onConnect = (frame) => {
            console.log("STOMP connected:", frame);

            const subscriptionDestination = `/sub/${roomId}`;
            subscriptionRef.current = client.subscribe(
                subscriptionDestination,
                (message: IMessage) => {
                    const receivedMessage: ChatMessage = JSON.parse(
                        message.body,
                    );
                    if (
                        receivedMessage.type === "READ_UPDATE" &&
                        receivedMessage.updates
                    ) {
                        const updateMap = new Map(
                            receivedMessage.updates.map((u) => [
                                u.messageId,
                                u.unreadCount,
                            ]),
                        );
                        setMessages((prevMessages) =>
                            prevMessages.map((msg) =>
                                msg.id && updateMap.has(msg.id)
                                    ? {
                                          ...msg,
                                          unreadCount:
                                              updateMap.get(msg.id) ??
                                              msg.unreadCount,
                                      }
                                    : msg,
                            ),
                        );
                        return;
                    }
                    if (
                        receivedMessage.type === "PRESENCE_UPDATE" &&
                        receivedMessage.username
                    ) {
                        setParticipants((prev) => {
                            const exists = prev.some(
                                (p) => p.username === receivedMessage.username,
                            );
                            if (exists) {
                                return prev.map((p) =>
                                    p.username === receivedMessage.username
                                        ? {
                                              ...p,
                                              online:
                                                  receivedMessage.online ??
                                                  p.online,
                                              nickname:
                                                  receivedMessage.nickname ??
                                                  p.nickname,
                                          }
                                        : p,
                                );
                            }
                            return [
                                ...prev,
                                {
                                    username: receivedMessage.username,
                                    nickname:
                                        receivedMessage.nickname ??
                                        receivedMessage.username,
                                    online: receivedMessage.online ?? false,
                                },
                            ];
                        });
                        return;
                    }
                    setMessages((prevMessages) => [
                        ...prevMessages,
                        receivedMessage,
                    ]);

                    const savedUsername = localStorage.getItem("chatUsername");
                    const isSelf =
                        selfNicknames.includes(receivedMessage.sender) ||
                        receivedMessage.sender === sender;
                    if (
                        savedUsername &&
                        !isSelf &&
                        (receivedMessage.type === "TALK" ||
                            receivedMessage.type === "IMAGE")
                    ) {
                        scheduleRead(savedUsername);
                    }
                },
            );
            console.log(`Subscribed: ${subscriptionDestination}`);
        };

        client.onStompError = (frame) => {
            console.error("Broker error:", frame);
        };

        client.activate();

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
            }
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
            if (readDebounceTimer.current) {
                clearTimeout(readDebounceTimer.current);
                readDebounceTimer.current = null;
                pendingReadUsername.current = null;
            }
        };
    }, [roomId, sender, selfNicknames]);

    const sendMessage = () => {
        if (
            messageInput.trim() &&
            clientRef.current &&
            clientRef.current.connected
        ) {
            const destination = `/pub/${roomId}`;
            const chatMessage = {
                id: null,
                type: "TALK",
                sender: sender,
                content: messageInput,
                imageUrl: null,
                sendAt: "",
                unreadCount: 0,
            };

            clientRef.current.publish({
                destination: destination,
                body: JSON.stringify(chatMessage),
            });
            setMessageInput("");
        } else {
            alert("메시지를 입력하고 연결 상태를 확인해 주세요.");
        }
    };

    const handleLeaveRoom = async () => {
        const confirmed = confirm("정말로 채팅방을 나가시겠습니까?");
        if (!confirmed) return;

        try {
            const response = await fetch(`/chat/rooms/${roomId}/leave`, {
                method: "POST",
                headers: {
                    ...getAuthHeaders(),
                },
            });
            if (handleAuthError(response)) {
                return;
            }
            const payload: ApiResponse<null> | null = await response
                .json()
                .catch(() => null);
            if (!response.ok || !payload?.success) {
                const message =
                    payload?.message || "채팅방 나가기에 실패했습니다.";
                alert(message);
                return;
            }
            if (onLeave) {
                onLeave();
            } else {
                router.push("/");
            }
        } catch (error) {
            console.error("Leave room error:", error);
            alert("채팅방 나가기 중 오류가 발생했습니다.");
        }
    };

    const handleImageUpload = async (
        event: React.ChangeEvent<HTMLInputElement>,
    ) => {
        const file = event.target.files?.[0];
        if (!file) return;
        if (file.size > 1 * 1024 * 1024) {
            alert("1MB 이하의 이미지만 업로드할 수 있습니다.");
            event.target.value = "";
            return;
        }
        if (!clientRef.current || !clientRef.current.connected) {
            alert("연결 상태를 확인해 주세요.");
            return;
        }

        try {
            const formData = new FormData();
            formData.append("file", file);
            const response = await fetch(`/chat/rooms/${roomId}/images`, {
                method: "POST",
                headers: {
                    ...getAuthHeaders(),
                },
                body: formData,
            });
            if (handleAuthError(response)) {
                return;
            }
            const payloadText = await response.text();
            let payload: ApiResponse<string> | null = null;
            try {
                payload = payloadText
                    ? (JSON.parse(payloadText) as ApiResponse<string>)
                    : null;
            } catch {
                payload = null;
            }
            if (!response.ok || !payload?.success || !payload.data) {
                console.error("Upload failed", {
                    status: response.status,
                    body: payloadText,
                });
                throw new Error(
                    payload?.message || "이미지 업로드에 실패했습니다.",
                );
            }

            clientRef.current.publish({
                destination: `/pub/${roomId}`,
                body: JSON.stringify({
                    id: null,
                    type: "IMAGE",
                    sender: sender,
                    content: "",
                    imageUrl: payload.data,
                    sendAt: "",
                    unreadCount: 0,
                }),
            });
        } catch (error) {
            console.error("Image upload error:", error);
            alert("이미지 업로드 중 오류가 발생했습니다.");
        } finally {
            event.target.value = "";
        }
    };

    const messagesEndRef = useRef<HTMLDivElement | null>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    useEffect(() => {
        if (!roomId) return;
        const frame = requestAnimationFrame(() => {
            scrollToBottom();
        });
        return () => cancelAnimationFrame(frame);
    }, [roomId]);

    const wrapperClass = variant === "panel" ? "chat-panel" : "container";

    return (
        <div className={wrapperClass}>
            {variant === "page" && (
                <div
                    style={{
                        marginBottom: "10px",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                    }}
                >
                    <Link className="ghost-button" href="/">
                        로비로 돌아가기
                    </Link>
                    <button className="ghost-button" onClick={handleLeaveRoom}>
                        채팅방 나가기
                    </button>
                </div>
            )}

            {variant === "panel" && (
                <div className="chat-panel-header">
                    <div className="chat-panel-title">
                        {roomName || "채팅방을 불러오는 중..."}
                    </div>
                    <button className="ghost-button" onClick={handleLeaveRoom}>
                        채팅방 나가기
                    </button>
                </div>
            )}

            {variant === "page" && (
                <h3 id="roomTitle">{roomName || "채팅방을 불러오는 중..."}</h3>
            )}

            <div className="chat-panel-body">
                <ul id="messages">
                    {messages.map((msg, index) => {
                        const isSentByMe =
                            selfNicknames.includes(msg.sender) ||
                            msg.sender === sender;
                        const isSystemMessage =
                            msg.type === "ENTER" || msg.type === "LEAVE";

                        if (isSystemMessage) {
                            return (
                                <li key={index} className="system">
                                    <span>{msg.content}</span>
                                </li>
                            );
                        }

                        return (
                            <li
                                key={index}
                                className={isSentByMe ? "sent" : "received"}
                            >
                                <div className="message-body">
                                    {!isSentByMe && (
                                        <div className="sender">
                                            {msg.sender}
                                        </div>
                                    )}
                                    <div className="message-line">
                                        {isSentByMe && msg.unreadCount > 0 && (
                                            <div
                                                className="timestamp"
                                                style={{ color: "#fbc02d" }}
                                            >
                                                {msg.unreadCount}
                                            </div>
                                        )}
                                        {isSentByMe && (
                                            <div className="timestamp">
                                                <span className="timestamp-oval">
                                                    {msg.sendAt}
                                                </span>
                                            </div>
                                        )}
                                        {msg.type === "TALK" && (
                                            <div className="content">
                                                {msg.content}
                                            </div>
                                        )}
                                        {msg.type === "IMAGE" &&
                                            msg.imageUrl && (
                                                <img
                                                    className="content"
                                                    src={msg.imageUrl}
                                                    alt="첨부 이미지"
                                                    style={{
                                                        maxWidth: "240px",
                                                        maxHeight: "240px",
                                                        objectFit: "cover",
                                                        borderRadius: "8px",
                                                    }}
                                                />
                                            )}
                                        {!isSentByMe && msg.unreadCount > 0 && (
                                            <div
                                                className="timestamp"
                                                style={{ color: "#fbc02d" }}
                                            >
                                                {msg.unreadCount}
                                            </div>
                                        )}
                                        {!isSentByMe && (
                                            <div className="timestamp">
                                                <span className="timestamp-oval">
                                                    {msg.sendAt}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </li>
                        );
                    })}
                    <div ref={messagesEndRef} />
                </ul>

                <div className="input-group">
                    <input
                        type="text"
                        id="messageInput"
                        placeholder="메시지를 입력하세요"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyUp={(e) => e.key === "Enter" && sendMessage()}
                    />
                    <button id="sendButton" onClick={sendMessage}>
                        전송
                    </button>
                </div>
                <div style={{ marginTop: "8px" }}>
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleImageUpload}
                    />
                </div>
            </div>

            <div
                style={{
                    marginTop: "12px",
                    border: "1px solid #eee",
                    borderRadius: "12px",
                    padding: "12px",
                    background: "#fff",
                }}
            >
                <div
                    style={{
                        fontSize: "14px",
                        fontWeight: 600,
                        marginBottom: "10px",
                    }}
                >
                    참여자
                </div>
                <div
                    style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "8px",
                    }}
                >
                    {participants.map((participant) => (
                        <div
                            key={participant.username}
                            style={{
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "space-between",
                                gap: "8px",
                            }}
                        >
                            <span style={{ fontSize: "14px" }}>
                                {participant.nickname}
                            </span>
                            <span
                                style={{
                                    width: "8px",
                                    height: "8px",
                                    borderRadius: "50%",
                                    display: "inline-block",
                                    backgroundColor: participant.online
                                        ? "#2e7d32"
                                        : "#111",
                                }}
                            />
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
