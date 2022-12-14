package com.vasileck.scannotification.service;

import com.vasileck.scannotification.event.ErrorEvent;
import com.vasileck.scannotification.event.ResultEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    @Getter
    private final String botUsername;
    @Getter
    private final String botToken;
    private final Set<Long> activeChatId = new HashSet<>();


    private final ScannerWebsite scannerWebsite;

    public TelegramBot(
            TelegramBotsApi telegramBotsApi,
            @Value("${telegram-bot.name}") String botUsername,
            @Value("${telegram-bot.token}") String botToken,
            ScannerWebsite scannerWebsite) throws TelegramApiException {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.scannerWebsite = scannerWebsite;
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage().getChatId();
        activeChatId.add(chatId);
//        if (update.hasMessage()) {
//            chatId = update.getMessage().getChatId();
//        } else if (update.hasChannelPost()) {
//            chatId = update.getChannelPost().getChatId();
//        } else if (update.hasCallbackQuery()) {
//            chatId = update.getCallbackQuery().getMessage().getChatId();
//        }
        Message requestMessage = update.getMessage();
        if (requestMessage.getText().contains("/start")) {
            sendMsg(chatId, """
                    ???????????? ????????????:
                    /search <url_web_site> - ?????? ???????????? ????????????
                    /stop - ?????? ?????????????????? ????????????""");
        } else if (requestMessage.getText().contains("/stop")) {
            scannerWebsite.stopSearch(chatId);
        } else if (requestMessage.getText().contains("/search")) {
            String[] texts = requestMessage.getText().split(" ");
            if (texts.length == 2) {
                scannerWebsite.startSearch(chatId, texts[1]);
                sendMsg(chatId, "?????????? ??????????");
            } else if (texts.length < 2) {
                sendMsg(chatId, "???? ???????? ???????????????????? URL, ???????????? ?????? ???????????? ???? ????????????");
            } else {
                sendMsg(chatId, "???? ???????? ???????????????????? URL, ?? ???????????? ???????? ??????????????");
            }
        } else {
            sendMsg(chatId, """
                    ?? ???? ?????????? ??????????????, ???????????? ????????????:
                    /search <url_web_site> - ?????? ???????????? ????????????
                    /stop - ?????? ?????????????????? ????????????""");

        }
    }

    @EventListener
    public void sendResult(ResultEvent resultEvent) {
        StringBuilder resultMessage = new StringBuilder();
        resultEvent.getResult()
                .forEach((key, value) -> resultMessage.append("id:").append(key).append(", ").append(value).append("\n"));
        sendMsg(resultEvent.getChatId(), "The search new results from " + resultEvent.getMessage() + "\n" + resultMessage);
    }

    @EventListener
    public void sendError(ErrorEvent errorEvent) {
        var msg = String.format("Search was finished: %s", errorEvent.getMessage());
        log.error(msg);
        sendMsg(errorEvent.getChatId(), errorEvent.getMessage());
        activeChatId.remove(errorEvent.getChatId());
    }

    @PreDestroy
    public void doSomethingAfterStartup() {
        activeChatId.forEach(chatId -> sendMsg(chatId, "Server to searching was by stopped"));
    }

    private void sendMsg(Long chatId, String msg) {
        SendMessage response = new SendMessage();
        response.setChatId(chatId.toString());
        response.setText(msg);
        try {
            execute(response);
        } catch (TelegramApiException e) {
            log.error("Error send result: " + e.getMessage(), e);
        }
    }
}
