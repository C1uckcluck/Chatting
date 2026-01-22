'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { apiUrl } from './lib/api';

interface ChatRoomDto {
    roomId: string;
    name: string;
    maxCapacity?: number | null;
    currentCount?: number | null;
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
    const [newRoomName, setNewRoomName] = useState<string>('');
    const [newRoomMaxCapacity, setNewRoomMaxCapacity] = useState<string>('');
    const [username, setUsername] = useState<string | null>(null);
    const [roomsPage, setRoomsPage] = useState(0);
    const [roomsTotalPages, setRoomsTotalPages] = useState(0);
    const router = useRouter();

    const getAuthHeaders = () => {
        const token = localStorage.getItem('chatAccessToken');
        return token ? { Authorization: `Bearer ${token}` } : {};
    };

    useEffect(() => {
        const savedUsername = localStorage.getItem('chatUsername');
        setUsername(savedUsername);
        fetchRooms(0);
        fetchMyRooms();
    }, []);

    const fetchRooms = async (page: number) => {
        try {
            const response = await fetch(apiUrl(`/chat/rooms?page=${page}&size=8`), {
                headers: {
                    ...getAuthHeaders(),
                },
            });
            if (response.status === 401 || response.status === 403) {
                router.push('/login');
                return;
            }
            const payload: ApiResponse<PagedResponse<ChatRoomDto>> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Network response was not ok');
            }
            setRooms(payload.data?.items || []);
            setRoomsPage(payload.data?.page ?? 0);
            setRoomsTotalPages(payload.data?.totalPages ?? 0);
        } catch (error) {
            console.error('Error fetching rooms:', error);
        }
    };

    const fetchMyRooms = async () => {
        try {
            const response = await fetch(apiUrl('/chat/rooms/my'), {
                headers: {
                    ...getAuthHeaders(),
                },
            });
            if (response.status === 401 || response.status === 403) {
                router.push('/login');
                return;
            }
            const payload: ApiResponse<ChatRoomDto[]> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Network response was not ok');
            }
            setMyRooms(payload.data || []);
        } catch (error) {
            console.error('Error fetching my rooms:', error);
        }
    };

    const createRoom = async () => {
        if (!newRoomName.trim()) {
            alert('채팅방 이름을 입력해 주세요.');
            return;
        }
        const parsedMaxCapacity = Number.parseInt(newRoomMaxCapacity, 10);
        if (!Number.isFinite(parsedMaxCapacity) || parsedMaxCapacity < 1) {
            alert('최소 참가 인원은 1명입니다.');
            return;
        }

        try {
            const response = await fetch(apiUrl('/chat/rooms'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders(),
                },
                body: JSON.stringify({
                    name: newRoomName.trim(),
                    maxCapacity: parsedMaxCapacity,
                }),
            });

            if (response.status === 401 || response.status === 403) {
                router.push('/login');
                return;
            }

            const payload: ApiResponse<ChatRoomDto> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Network response was not ok');
            }
            setNewRoomName('');
            setNewRoomMaxCapacity('');
            await fetchRooms(0);
            await fetchMyRooms();
        } catch (error) {
            console.error('Error creating room:', error);
        }
    };

    const handleLogout = async () => {
        try {
            const response = await fetch(apiUrl('/auth/logout'), { method: 'POST' });
            const payload: ApiResponse<null> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Logout failed');
            }
            localStorage.removeItem('chatUsername');
            localStorage.removeItem('chatNickname');
            localStorage.removeItem('chatNicknameHistory');
            localStorage.removeItem('chatAccessToken');
            router.push('/login');
        } catch (error) {
            console.error('Logout failed', error);
        }
    };

    return (
        <div className="container">
            <div className="lobby-shell">
                <header className="lobby-header">
                    <div>
                        <div className="lobby-eyebrow">Live rooms</div>
                        <h2>채팅 로비</h2>
                    </div>
                    <div className="lobby-actions">
                        <Link className="ghost-button" href="/profile">내 정보</Link>
                        <button className="ghost-button" onClick={handleLogout}>
                            로그아웃
                        </button>
                    </div>
                </header>

                {username && (
                    <p className="lobby-greeting">
                        안녕하세요, <strong>{username}</strong>님. 지금 바로 대화를 시작하세요.
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
                            onKeyUp={(e) => e.key === 'Enter' && createRoom()}
                        />
                        <input
                            type="number"
                            min={1}
                            placeholder="참여가능 인원수를 입력해주세요"
                            value={newRoomMaxCapacity}
                            onChange={(e) =>
                                setNewRoomMaxCapacity(e.target.value)
                            }
                        />
                        <button onClick={createRoom}>방 만들기</button>
                    </div>
                </section>

                <section className="lobby-grid">
                    <div className="room-panel">
                        <div className="room-panel-header">
                            <h3>참여중인 채팅방</h3>
                            <span className="room-count">{myRooms.length}</span>
                        </div>
                        <ul className="room-list">
                            {myRooms.length === 0 && (
                                <li className="room-empty">참여중인 채팅방이 없습니다.</li>
                            )}
                            {myRooms.map((room) => (
                                <li key={room.roomId} className="room-item">
                                    <Link className="room-link" href={`/chat/${room.roomId}`}>
                                        <span className="room-name">{room.name}</span>
                                        <span className="room-meta">
                                            {room.currentCount ?? 0} / {room.maxCapacity ?? "-"}
                                        </span>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div className="room-panel room-panel-accent">
                        <div className="room-panel-header">
                            <h3>전체 채팅방</h3>
                            <span className="room-count">{rooms.length}</span>
                        </div>
                        <ul className="room-list">
                            {rooms.length === 0 && (
                                <li className="room-empty">아직 생성된 채팅방이 없습니다.</li>
                            )}
                            {rooms.map((room) => (
                                <li key={room.roomId} className="room-item">
                                    <Link className="room-link" href={`/chat/${room.roomId}`}>
                                        <span className="room-name">{room.name}</span>
                                        <span className="room-meta">
                                            {room.currentCount ?? 0} / {room.maxCapacity ?? "-"}
                                        </span>
                                    </Link>
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
                                {roomsTotalPages === 0 ? 0 : roomsPage + 1} / {roomsTotalPages || 1}
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
                </section>
            </div>
        </div>
    );
}
