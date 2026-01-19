'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

interface ChatRoomDto {
    roomId: string;
    name: string;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string | null;
}

export default function Lobby() {
    const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
    const [myRooms, setMyRooms] = useState<ChatRoomDto[]>([]);
    const [newRoomName, setNewRoomName] = useState<string>('');
    const [username, setUsername] = useState<string | null>(null);
    const router = useRouter();

    useEffect(() => {
        const savedUsername = localStorage.getItem('chatUsername');
        setUsername(savedUsername);
        fetchRooms();
        fetchMyRooms();
    }, []);

    const fetchRooms = async () => {
        try {
            const response = await fetch('/chat/rooms');
            if (response.status === 401 || response.status === 403) {
                router.push('/login');
                return;
            }
            const payload: ApiResponse<ChatRoomDto[]> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Network response was not ok');
            }
            setRooms(payload.data || []);
        } catch (error) {
            console.error('Error fetching rooms:', error);
        }
    };

    const fetchMyRooms = async () => {
        try {
            const response = await fetch('/chat/rooms/my');
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

        try {
            const response = await fetch('/chat/rooms', {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: newRoomName,
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
            await fetchRooms();
            await fetchMyRooms();
        } catch (error) {
            console.error('Error creating room:', error);
        }
    };

    const handleLogout = async () => {
        try {
            const response = await fetch('/auth/logout', { method: 'POST' });
            const payload: ApiResponse<null> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'Logout failed');
            }
            localStorage.removeItem('chatUsername');
            localStorage.removeItem('chatNickname');
            localStorage.removeItem('chatNicknameHistory');
            router.push('/login');
        } catch (error) {
            console.error('Logout failed', error);
        }
    };

    return (
        <div className="container">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>채팅 로비</h2>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <Link className="ghost-button" href="/profile">내 정보</Link>
                    <button className="ghost-button" onClick={handleLogout}>
                        로그아웃
                    </button>
                </div>
            </div>

            {username && (
                <p style={{ textAlign: 'center', color: '#666' }}>
                    안녕하세요, <strong>{username}</strong>님.
                </p>
            )}

            <div className="input-group">
                <input
                    type="text"
                    placeholder="새 채팅방 이름"
                    value={newRoomName}
                    onChange={(e) => setNewRoomName(e.target.value)}
                    onKeyUp={(e) => e.key === 'Enter' && createRoom()}
                />
                <button onClick={createRoom}>방 만들기</button>
            </div>

            <div id="room-list" style={{ marginTop: '20px' }}>
                <h3>참여중인 채팅방</h3>
                <ul>
                    {myRooms.length === 0 && <li>참여중인 채팅방이 없습니다.</li>}
                    {myRooms.map((room) => (
                        <li key={room.roomId}>
                            <Link href={`/chat/${room.roomId}`}>{room.name}</Link>
                        </li>
                    ))}
                </ul>
                <h3 style={{ marginTop: '16px' }}>전체 채팅방</h3>
                <ul>
                    {rooms.map((room) => (
                        <li key={room.roomId}>
                            <Link href={`/chat/${room.roomId}`}>{room.name}</Link>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
