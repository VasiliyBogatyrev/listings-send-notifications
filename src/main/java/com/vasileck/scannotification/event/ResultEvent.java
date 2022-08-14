package com.vasileck.scannotification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class ResultEvent extends ApplicationEvent {
    private final String message;
    private final Long chatId;
    private final Map<String, String> result;

    public ResultEvent(String message, Long chatId, Map<String, String> result) {
        super(result);
        this.message = message;
        this.chatId = chatId;
        this.result = result;
    }
}
