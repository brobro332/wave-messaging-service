# 🌊 wave-messaging-service

> **DATT Ecosystem** - Real-time Location-Aware Distributed Messaging Microservice  
> WebSocket(STOMP), Redis Pub/Sub, Kafka 및 PostgreSQL 기반의 0.001초 실시간 분산 메시징 마이크로서비스 백엔드

---

## 🛠️ Tech Stack

- **Core**: Java 17, Spring Boot 3.3.2, Gradle
- **Real-time Messaging**: WebSocket (STOMP), SockJS
- **In-Memory Pub/Sub**: Redis (DB 1번 영역 사용)
- **Persistence Queue**: Apache Kafka
- **Database**: PostgreSQL (`datt_wave` DB) & Spring Data JPA

---

## 🚀 로컬 실행 방법

### 1. 전제 조건 (공유 인프라 기동)
`datt-platform` 메인 인프라(PostgreSQL, Redis, Kafka)가 기동되어 있어야 합니다.

### 2. 프로젝트 빌드 및 실행
```bash
./gradlew bootRun
```
* **서버 포트**: `http://localhost:8081`
* **WebSocket 엔드포인트**: `ws://localhost:8081/ws-stomp`

---

## 🧪 실시간 웹소켓 채팅 테스트 방법

1. 브라우저에서 루트 디렉토리의 `test-chat.html` 파일을 엽니다. (탭 2개 띄우기)
2. 탭 1: 닉네임 `유저A`, 탭 2: 닉네임 `유저B` 입력 후 입장.
3. 실시간 0.001초 분산 채팅 전송 테스트 실행!
