"use client";

import { useParams } from "next/navigation";
import ChatRoomPanel from "../ChatRoomPanel";

export default function ChatRoom() {
    const params = useParams();
    const roomId = params.roomId as string;

    return <ChatRoomPanel roomId={roomId} variant="page" />;
}
