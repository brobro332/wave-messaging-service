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