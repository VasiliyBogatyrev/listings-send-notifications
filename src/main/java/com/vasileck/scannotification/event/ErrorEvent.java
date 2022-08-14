package com.vasileck.scannotification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ErrorEvent extends ApplicationEvent {
    private final Long chatId;
    private final String message;

    public ErrorEvent(Long chatId, String message) {
        super(message);
        this.chatId = chatId;
        this.message = message;
    }
}
