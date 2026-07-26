# 개발???�트 (v1.0)

## e26da3d ?�세 Javadoc 주석 추�?
- **목적**: wave-messaging-service???�체 Controller �?Service ?�래?�들???�??비즈?�스 로직(STOMP, Redis, Elasticsearch ????문서?�하???�키?�처 ?�름�?코드 가?�성??개선??
- **?�업 ?�일**:
  - `src/main/java/xyz/messaging/wave/controller/ChatController.java`
  - `src/main/java/xyz/messaging/wave/controller/ChatRoomController.java`
  - `src/main/java/xyz/messaging/wave/controller/WorkspaceController.java`
  - `src/main/java/xyz/messaging/wave/service/ChatRoomService.java`
  - `src/main/java/xyz/messaging/wave/service/WorkspaceService.java`
- **구체?�인 ?�업 ?�용**:
  - ?�래??�?메서???�벨???��? Javadoc 추�?.
  - STOMP ?�소�??�름, Redis Pub/Sub ??��, Elasticsearch ?�?�스??검??방식 ?�명.
  - ?�크?�페?�스 �?채팅�??�명주기?� ???��? 메시지(Unread Count) ?��? ?�산 로직 ?�명.

### c06998a TraceId 전파 및 기본 로깅 필터 도입
- **작업 파일**: src/main/java/xyz/messaging/global/logging/RequestLoggingFilter.java
- **작업 목적**: 분산 트레이싱을 위한 Trace ID 수용 및 자체 로깅 기능 신설
- **작업 내용**:
  - RequestLoggingFilter 신규 추가
  - HTTP 요청 헤더에서 X-Trace-Id를 추출하여 로깅 컨텍스트(MDC)에 등록
  - 헤더에 없으면 신규 생성, 요청 클라이언트 IP 수집 등 datt-platform과 동일한 규격의 로깅 구현
