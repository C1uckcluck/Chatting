'use client';

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { useParams } from 'next/navigation'; // Import useParams
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';

// 메시지 타입을 정의합니다.
interface ChatMessage {
    type: 'ENTER' | 'TALK' | 'LEAVE';
    sender: string;
    content: string;
    sendAt: string;
}

export default function ChatRoom() {
    const params = useParams(); // Get params
    const roomId = params.roomId as string; // Get roomId from params

    const [roomName, setRoomName] = useState<string>('');
    const [messageInput, setMessageInput] = useState<string>('');
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [sender] = useState<string>("User_" + Math.floor(Math.random() * 1000));

    const clientRef = useRef<Client | null>(null);
    const subscriptionRef = useRef<StompSubscription | null>(null);

    const effectRan = useRef(false);

    // Fetch room name when component mounts
    useEffect(() => {
        if (!roomId) return;
        const fetchRoomName = async () => {
            try {
                const response = await fetch(`http://localhost:8080/chat/rooms/${roomId}`);
                if (response.ok) {
                    const roomData = await response.json();
                    setRoomName(roomData.name);
                } else {
                    console.error('Failed to fetch room name');
                }
            } catch (error) {
                console.error('Error fetching room name:', error);
            }
        };
        fetchRoomName();
    }, [roomId]);

    useEffect(() => {
        if (effectRan.current === true || !roomId) {
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
                console.log(receivedMessage)
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
    }, [roomId]);

    const sendMessage = () => {
        if (messageInput.trim() && clientRef.current && clientRef.current.connected) {
            const destination = `/pub/${roomId}`;
            const chatMessage: ChatMessage = {
                type: 'TALK',
                sender: sender,
                content: messageInput,
                sendAt: '',
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