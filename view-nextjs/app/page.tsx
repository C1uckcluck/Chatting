"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import ChatRoomPanel from "./chat/ChatRoomPanel";

interface ChatRoomDto {
    roomId: string;
    name: string;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string | null;
}

interface PagedResponse<T> {
    items: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export default function Lobby() {
    const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
    const [myRooms, setMyRooms] = useState<ChatRoomDto[]>([]);
    const [newRoomName, setNewRoomName] = useState<string>("");
    const [username, setUsername] = useState<string | null>(null);
    const [roomsPage, setRoomsPage] = useState(0);
    const [roomsTotalPages, setRoomsTotalPages] = useState(0);
    const [selectedRoom, setSelectedRoom] = useState<ChatRoomDto | null>(null);
    const router = useRouter();

    const getAuthHeaders = () => {
        const token = localStorage.getItem("chatAccessToken");
        return token ? { Authorization: `Bearer ${token}` } : {};
    };

    useEffect(() => {
        const savedUsername = localStorage.getItem("chatUsername");
        setUsername(savedUsername);
        fetchRooms(0);
        fetchMyRooms();
    }, []);

    const fetchRooms = async (page: number) => {
        try {
            const response = await fetch(`/chat/rooms?page=${page}&size=8`, {
                headers: {
                    ...getAuthHeaders(),
                },
            });
            if (response.status === 401 || response.status === 403) {
                router.push("/login");
                return;
            }
            const payload: ApiResponse<PagedResponse<ChatRoomDto>> | null =
                await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(
                    payload?.message || "Network response was not ok",
                );
            }
            setRooms(payload.data?.items || []);
            setRoomsPage(payload.data?.page ?? 0);
            setRoomsTotalPages(payload.data?.totalPages ?? 0);
        } catch (error) {
            console.error("Error fetching rooms:", error);
        }
    };

    const fetchMyRooms = async () => {
        try {
            const response = await fetch("/chat/rooms/my", {
                headers: {
                    ...getAuthHeaders(),
                },
            });
            if (response.status === 401 || response.status === 403) {
                router.push("/login");
                return;
            }
            const payload: ApiResponse<ChatRoomDto[]> | null = await response
                .json()
                .catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(
                    payload?.message || "Network response was not ok",
                );
            }
            setMyRooms(payload.data || []);
        } catch (error) {
            console.error("Error fetching my rooms:", error);
        }
    };

    const createRoom = async () => {
        if (!newRoomName.trim()) {
            alert("채팅방 이름을 입력해 주세요.");
            return;
        }

        try {
            const response = await fetch("/chat/rooms", {
                method: "POST",
                headers: {
                    "Content-Type": "text/plain",
                    ...getAuthHeaders(),
                },
                body: newRoomName,
            });

            if (response.status === 401 || response.status === 403) {
                router.push("/login");
                return;
            }

            const payload: ApiResponse<ChatRoomDto> | null = await response
                .json()
                .catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(
                    payload?.message || "Network response was not ok",
                );
            }
            setNewRoomName("");
            await fetchRooms(0);
            await fetchMyRooms();
            if (payload?.data) {
                setSelectedRoom(payload.data);
            }
        } catch (error) {
            console.error("Error creating room:", error);
        }
    };

    const handleRoomSelect = (room: ChatRoomDto) => {
        setSelectedRoom(room);
        if (typeof window !== "undefined") {
            const isCompact = window.matchMedia("(max-width: 1100px)").matches;
            if (isCompact) {
                router.push(`/chat/${room.roomId}`);
            }
        }
    };

    const handleLogout = async () => {
        try {
            const response = await fetch("/auth/logout", { method: "POST" });
            const payload: ApiResponse<null> | null = await response
                .json()
                .catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || "Logout failed");
            }
            localStorage.removeItem("chatUsername");
            localStorage.removeItem("chatNickname");
            localStorage.removeItem("chatNicknameHistory");
            localStorage.removeItem("chatAccessToken");
            router.push("/login");
        } catch (error) {
            console.error("Logout failed", error);
        }
    };

    return (
        <div className="lobby-frame">
            <aside className="lobby-rail">
                <div className="lobby-rail-top">
                    <div className="lobby-rail-brand" />
                </div>
                <nav className="lobby-rail-icons">
                    <span className="lobby-rail-dot is-active" />
                    <span className="lobby-rail-dot" />
                    <span className="lobby-rail-dot" />
                    <span className="lobby-rail-dot" />
                </nav>
                <div className="lobby-rail-footer">
                    <span className="lobby-rail-avatar" />
                </div>
            </aside>

            <section className="lobby-panel">
                <header className="lobby-panel-header">
                    <div>
                        <div className="lobby-panel-title">
                            <span className="lobby-panel-title-text">채팅</span>
                            <span className="lobby-panel-title-caret">v</span>
                        </div>
                        <div className="lobby-panel-subtitle">Live rooms</div>
                    </div>
                    <div className="lobby-panel-actions">
                        <Link className="ghost-button" href="/profile">
                            내 정보
                        </Link>
                        <button className="ghost-button" onClick={handleLogout}>
                            로그아웃
                        </button>
                    </div>
                </header>

                {username && (
                    <p className="lobby-panel-greeting">
                        {username}님, 지금 대화를 시작하세요.
                    </p>
                )}

                <section className="lobby-create">
                    <div className="lobby-create-title">새 채팅방</div>
                    <div className="input-group">
                        <input
                            type="text"
                            placeholder="새 채팅방 이름"
                            value={newRoomName}
                            onChange={(e) => setNewRoomName(e.target.value)}
                            onKeyUp={(e) => e.key === "Enter" && createRoom()}
                        />
                        <button onClick={createRoom}>방 만들기</button>
                    </div>
                </section>

                <div className="lobby-list">
                    <div className="lobby-list-section">
                        <div className="lobby-list-header">
                            <span>참여중</span>
                            <span className="lobby-pill">{myRooms.length}</span>
                        </div>
                        <ul>
                            {myRooms.length === 0 && (
                                <li className="lobby-empty">
                                    참여중인 채팅방이 없습니다.
                                </li>
                            )}
                            {myRooms.map((room) => (
                                <li key={room.roomId}>
                                    <button
                                        type="button"
                                        className={`lobby-room ${selectedRoom?.roomId === room.roomId ? "is-active" : ""}`}
                                        onClick={() => handleRoomSelect(room)}
                                    >
                                        <span className="lobby-room-icon">
                                            chat
                                        </span>
                                        <span className="lobby-room-name">
                                            {room.name}
                                        </span>
                                        <span className="lobby-room-meta">
                                            바로가기
                                        </span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="lobby-list-section">
                        <div className="lobby-list-header">
                            <span>전체 채팅방</span>
                            <span className="lobby-pill">{rooms.length}</span>
                        </div>
                        <ul>
                            {rooms.length === 0 && (
                                <li className="lobby-empty">
                                    아직 생성된 채팅방이 없습니다.
                                </li>
                            )}
                            {rooms.map((room) => (
                                <li key={room.roomId}>
                                    <button
                                        type="button"
                                        className={`lobby-room ${selectedRoom?.roomId === room.roomId ? "is-active" : ""}`}
                                        onClick={() => handleRoomSelect(room)}
                                    >
                                        <span className="lobby-room-icon">
                                            #
                                        </span>
                                        <span className="lobby-room-name">
                                            {room.name}
                                        </span>
                                        <span className="lobby-room-meta">
                                            입장
                                        </span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                        <div className="room-pagination">
                            <button
                                className="ghost-button"
                                disabled={roomsPage <= 0}
                                onClick={() => fetchRooms(roomsPage - 1)}
                            >
                                이전
                            </button>
                            <span className="room-page-info">
                                {roomsTotalPages === 0 ? 0 : roomsPage + 1} /{" "}
                                {roomsTotalPages || 1}
                            </span>
                            <button
                                className="ghost-button"
                                disabled={roomsPage + 1 >= roomsTotalPages}
                                onClick={() => fetchRooms(roomsPage + 1)}
                            >
                                다음
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            <main className="lobby-main">
                <div className="lobby-main-top">
                    <div className="lobby-main-title">
                        <span className="lobby-main-title-text">
                            {selectedRoom?.name || "채팅방 선택"}
                        </span>
                        <span className="lobby-panel-title-caret">v</span>
                    </div>
                    <div className="lobby-main-actions">
                        <button className="ghost-button">검색</button>
                        <button className="ghost-button">새 창</button>
                    </div>
                </div>
                {selectedRoom ? (
                    <ChatRoomPanel
                        roomId={selectedRoom.roomId}
                        variant="panel"
                        onLeave={() => setSelectedRoom(null)}
                    />
                ) : (
                    <div className="lobby-empty-state">
                        <div className="lobby-empty-bubble">
                            <span className="lobby-empty-dot" />
                            <span className="lobby-empty-dot" />
                            <span className="lobby-empty-dot" />
                        </div>
                        <p>목록에서 채팅방을 선택하세요.</p>
                    </div>
                )}
            </main>
        </div>
    );
}
