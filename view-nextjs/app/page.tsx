
'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';

// 서버에서 받아올 채팅방 DTO 타입
interface ChatRoomDto {
    roomId: string;
    name: string;
}

export default function Lobby() {
    const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
    const [newRoomName, setNewRoomName] = useState<string>('');

    // 컴포넌트가 마운트될 때 채팅방 목록을 불러옵니다.
    useEffect(() => {
        fetchRooms();
    }, []);

    // GET /chat/rooms API 호출
    const fetchRooms = async () => {
        try {
            const response = await fetch('http://localhost:8080/chat/rooms');
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
            const response = await fetch('http://localhost:8080/chat/rooms', {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: newRoomName,
            });

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            setNewRoomName('');
            // 새 방 생성 후 목록을 다시 불러옵니다.
            await fetchRooms();
        } catch (error) {
            console.error("Error creating room:", error);
        }
    };

    return (
        <div className="container">
            <h2>Chat Lobby</h2>

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

            <div id="room-list">
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
