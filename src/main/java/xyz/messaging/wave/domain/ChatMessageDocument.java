package xyz.messaging.wave.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "chat_messages", createIndex = false)
@Setting(settingPath = "elasticsearch/settings.json")
public class ChatMessageDocument {
    @Id
    private String id; // ChatMessage.id.toString()

    @Field(type = FieldType.Keyword)
    private String roomId;

    @Field(type = FieldType.Keyword)
    private String sender;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer"),
        otherFields = {
            @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "ngram_analyzer")
        }
    )
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
