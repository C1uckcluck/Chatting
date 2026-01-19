# AGENTS.md

## 프로젝트 개요
- server: Spring Boot 3 + WebSocket(STOMP) 기반의 채팅 백엔드
- view-nextjs: Next.js 15(React 19) 기반의 채팅 프런트엔드
- REST 응답은 공통 포맷(`ApiResponse`) 사용, 이미지 업로드/읽음 카운트/접속 상태 제공

## 디렉토리 구조
- server/: Spring Boot 앱, JPA/MySQL, WebSocket/STOMP, 보안 설정
- view-nextjs/: Next.js App Router, 로그인/회원가입/로비/채팅방 UI
- db/: MySQL 데이터 볼륨(로컬)
- server/uploads/: 채팅 이미지 업로드 저장 경로(로컬, Git 제외)

## 실행 방법(로컬)
- DB: `server/docker-compose.yml`로 MySQL 8 컨테이너 실행(3307 -> 3306 매핑)
- server: Java 17 필요, `./gradlew bootRun` (기본 8080)
- view-nextjs: `npm install` 후 `npm run dev` (기본 3000)

## 주요 엔드포인트(REST)
- 인증: `POST /auth/signup`, `POST /auth/login`, `POST /auth/logout`
- 채팅방: `GET /chat/rooms`, `POST /chat/rooms`(text/plain 본문), `GET /chat/rooms/{roomId}`
- 메시지: `GET /chat/rooms/{roomId}/messages`, `POST /chat/rooms/{roomId}/read`(text/plain username)
- 참여자: `GET /chat/rooms/{roomId}/participants`
- 이미지 업로드: `POST /chat/rooms/{roomId}/images`(multipart/form-data, file)

## WebSocket/STOMP 흐름
- 엔드포인트: `/ws-stomp` (SockJS 사용)
- 발행: `/pub/{roomId}`
- 구독: `/sub/{roomId}`
- 클라이언트는 connectHeaders로 `username`/`nickname` 전달
- 메시지는 서버에서 저장 후 브로드캐스트
- 추가 이벤트: `READ_UPDATE`, `PRESENCE_UPDATE`

## 프런트 프록시 및 통신 규칙
- `view-nextjs/next.config.ts`에서 `/auth/*`, `/chat/rooms/*`, `/members/*`, `/uploads/*`는 `http://localhost:8080`로 rewrite
- 채팅방 페이지는 메시지/읽음 처리 요청을 `http://localhost:8080`으로 직접 호출
- 로그인 성공 시 `localStorage`에 `chatUsername`, `chatNickname` 저장

## 보안/데이터 설정
- 서버는 세션 기반 인증, `/auth/**`, `/ws-stomp/**`, `/uploads/**`는 인증 없이 접근 가능
- 업로드 경로: `server/src/main/resources/application.properties`의 `app.upload.dir` (기본 `uploads`)
- DB 연결: `server/src/main/resources/application.properties`에서 MySQL `chatdb` 사용
- JPA `ddl-auto=update`

## 테스트
- 서버 테스트: `server/src/test/java/**`
- 실행: `./gradlew test`

## 응답 포맷(ApiResponse)
```json
{
  "success": true,
  "data": {},
  "message": null
}
```

## 읽음/접속 상태 개요
- unreadCount는 메시지마다 현재 읽지 않은 인원 수(방 참여자 기준)
- 읽음 처리: `/chat/rooms/{roomId}/read` 호출 시 서버가 해당 구간 메시지 unreadCount 감소 후 `READ_UPDATE` 브로드캐스트
- 접속 상태: WebSocket 구독/해제 이벤트로 `PRESENCE_UPDATE` 브로드캐스트
