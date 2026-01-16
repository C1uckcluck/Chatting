'use client';

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { useParams } from 'next/navigation'; // Import useParams
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

// 메시지 타입을 정의합니다.
interface ChatMessage {
    type: 'ENTER' | 'TALK' | 'LEAVE';
    sender: string;
    content: string;
    sendAt: string;
    unreadCount: number;
}

export default function ChatRoom() {
    const params = useParams();
    const router = useRouter();
    const roomId = params.roomId as string;

    const [roomName, setRoomName] = useState<string>('');
    const [messageInput, setMessageInput] = useState<string>('');
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [sender, setSender] = useState<string>('');

    const clientRef = useRef<Client | null>(null);
    const subscriptionRef = useRef<StompSubscription | null>(null);

    const effectRan = useRef(false);

    // 컴포넌트 마운트 시 사용자 이름과 방 정보를 가져옵니다.
    useEffect(() => {
        const savedUsername = localStorage.getItem('chatUsername');
        if (!savedUsername) {
            alert('사용자 이름이 설정되지 않았습니다. 로비로 돌아갑니다.');
            router.push('/');
            return;
        }
        setSender(savedUsername);

        if (!roomId) return;

        const fetchRoomData = async () => {
            try {
                // Fetch room name
                const nameResponse = await fetch(`http://localhost:8080/chat/rooms/${roomId}`);
                if (nameResponse.ok) {
                    const roomData = await nameResponse.json();
                    setRoomName(roomData.name);
                }

                // Fetch message history
                const messagesResponse = await fetch(`http://localhost:8080/chat/rooms/${roomId}/messages`);
                if (messagesResponse.ok) {
                    const history = await messagesResponse.json();
                    setMessages(history);
                }

                // Mark messages as read
                await fetch(`http://localhost:8080/chat/rooms/${roomId}/read`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'text/plain',
                    },
                    body: savedUsername,
                });

            } catch (error) {
                console.error('Error fetching room data:', error);
            }
        };

        fetchRoomData();
    }, [roomId, router]);

    // WebSocket 연결 로직
    useEffect(() => {
        // sender가 설정된 후에만 연결을 시도합니다. 또는 effectRan.current가 true인 경우
        if (!sender || !roomId || effectRan.current === true) {
            return;
        }

        // 1. STOMP 클라이언트 생성 및 연결
        const client = new Client({
            webSocketFactory: () => new SockJS("http://localhost:8080/ws-stomp"),
            connectHeaders: {
                username: sender,
            },
            debug: (str) => console.log(new Date(), str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        clientRef.current = client;

        client.onConnect = (frame) => {
            console.log('STOMP 연결 성공:', frame);

            const subscriptionDestination = `/sub/${roomId}`;
            subscriptionRef.current = client.subscribe(subscriptionDestination, (message: IMessage) => {
                const receivedMessage: ChatMessage = JSON.parse(message.body);
                setMessages(prevMessages => [...prevMessages, receivedMessage]);
            });
            console.log(`구독 시작: ${subscriptionDestination}`);
        };

        client.onStompError = (frame) => {
            console.error('브로커 오류:', frame);
        };

        client.activate();
        effectRan.current = true;

        // 컴포넌트 언마운트 시 연결 해제
        return () => {
            if (clientRef.current && clientRef.current.connected) {
                console.log('Disconnecting STOMP client...');
                if (subscriptionRef.current) {
                    subscriptionRef.current.unsubscribe();
                }
                clientRef.current.deactivate();
            }
        };
    }, [roomId, sender]);

    const sendMessage = () => {
        if (messageInput.trim() && clientRef.current && clientRef.current.connected) {
            const destination = `/pub/${roomId}`;
            const chatMessage = {
                type: 'TALK',
                sender: sender,
                content: messageInput,
                sendAt: '', // 서버에서 설정
                unreadCount: 0 // 서버에서 설정
            };

            clientRef.current.publish({
                destination: destination,
                body: JSON.stringify(chatMessage),
            });
            setMessageInput('');
        } else {
            alert('메시지를 입력하거나 연결 상태를 확인하세요.');
        }
    };

    const messagesEndRef = useRef<HTMLDivElement | null>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    return (
        <div className="container">
            <div style={{ marginBottom: '10px' }}>
                <Link href="/">← 로비로 돌아가기</Link>
            </div>
            <h3 id="roomTitle">{roomName || '채팅방 로딩 중...'}</h3>
            <ul id="messages">
                {messages.map((msg, index) => {
                    const isSentByMe = msg.sender === sender;
                    const isSystemMessage = msg.type !== 'TALK';

                    if (isSystemMessage) {
                        return (
                            <li key={index} className="system">
                                <span>{msg.content}</span>
                            </li>
                        );
                    }

                    return (
                        <li key={index} className={isSentByMe ? 'sent' : 'received'}>
                            <div className="message-body">
                                {!isSentByMe && <div className="sender">{msg.sender}</div>}
                                <div className="message-line">
                                    {isSentByMe && msg.unreadCount > 0 && (
                                        <div className="timestamp" style={{ color: '#fbc02d' }}>
                                            {msg.unreadCount}
                                        </div>
                                    )}
                                    {isSentByMe && (
                                        <div className="timestamp">
                                            <span className="timestamp-oval">{msg.sendAt}</span>
                                        </div>
                                    )}
                                    <div className="content">{msg.content}</div>
                                    {!isSentByMe && (
                                        <div className="timestamp">
                                            <span className="timestamp-oval">{msg.sendAt}</span>
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
                    onKeyUp={(e) => e.key === 'Enter' && sendMessage()}
                />
                <button id="sendButton" onClick={sendMessage}>전송</button>
            </div>
        </div>
    );
}