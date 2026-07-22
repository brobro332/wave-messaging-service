package xyz.messaging.wave.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "chat_messages", createIndex = false)
public class ChatMessageDocument {
    @Id
    private String id; // ChatMessage.id.toString()

    @Field(type = FieldType.Keyword)
    private String roomId;

    @Field(type = FieldType.Keyword)
    private String sender;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String message;

    @Field(type = FieldType.Keyword)
    private String messageType;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    public static ChatMessageDocument from(ChatMessage msg) {
        return ChatMessageDocument.builder()
                .id(msg.getId().toString())
                .roomId(msg.getRoomId())
                .sender(msg.getSender())
                .message(msg.getMessage())
                .messageType(msg.getMessageType())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
