
'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

// 서버에서 받아올 채팅방 DTO 타입
interface ChatRoomDto {
    roomId: string;
    name: string;
}

export default function Lobby() {
    const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
    const [newRoomName, setNewRoomName] = useState<string>('');
    const [username, setUsername] = useState<string | null>(null);
    const router = useRouter();

    useEffect(() => {
        // 로그인 시 저장했던 사용자 이름 가져오기 (표시용)
        const savedUsername = localStorage.getItem('chatUsername');
        setUsername(savedUsername);
        
        fetchRooms();
    }, []);

    // GET /chat/rooms API 호출
    const fetchRooms = async () => {
        try {
            const response = await fetch('/chat/rooms');
            if (response.status === 401 || response.status === 403) {
                // 인증되지 않은 경우 로그인 페이지로 이동
                router.push('/login');
                return;
            }
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data: ChatRoomDto[] = await response.json();
            setRooms(data);
        } catch (error) {
            console.error("Error fetching rooms:", error);
        }
    };

    // POST /chat/rooms API 호출
    const createRoom = async () => {
        if (!newRoomName.trim()) {
            alert('채팅방 이름을 입력해주세요.');
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

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            setNewRoomName('');
            await fetchRooms();
        } catch (error) {
            console.error("Error creating room:", error);
        }
    };

    const handleLogout = async () => {
        try {
            await fetch('/auth/logout', { method: 'POST' });
            localStorage.removeItem('chatUsername');
            router.push('/login');
        } catch (error) {
            console.error("Logout failed", error);
        }
    };

    return (
        <div className="container">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>Chat Lobby</h2>
                <button onClick={handleLogout} style={{ padding: '5px 10px', fontSize: '0.9em', backgroundColor: '#dc3545' }}>
                    로그아웃
                </button>
            </div>
            
            {username && <p style={{ textAlign: 'center', color: '#666' }}>환영합니다, <strong>{username}</strong>님!</p>}

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
                <h3>채팅방 목록</h3>
                <ul>
                    {rooms.map(room => (
                        <li key={room.roomId}>
                            <Link href={`/chat/${room.roomId}`}>
                                {room.name}
                            </Link>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}
