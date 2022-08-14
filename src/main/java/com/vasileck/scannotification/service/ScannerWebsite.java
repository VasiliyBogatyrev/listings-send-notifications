package com.vasileck.scannotification.service;

import com.vasileck.scannotification.event.ErrorEvent;
import com.vasileck.scannotification.event.ResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ScannerWebsite {
    @Value("${scanner.periodInSeq:60}")
    private Integer periodInSeq;

    private final Map<Long, Boolean> stopWork = new ConcurrentHashMap<>();
    private final ApplicationEventMulticaster applicationEventMulticaster;


    public ScannerWebsite(ApplicationEventMulticaster applicationEventMulticaster) {
        this.applicationEventMulticaster = applicationEventMulticaster;
    }

    @Async
    public void stopSearch(Long chatId) {
        stopWork.put(chatId, true);
    }

    @Async
    public void startSearch(Long chatId, String url) {
        stopWork.put(chatId, false);
        Set<String> foundResults = new HashSet<>();
        try {
            do {
                Map<String, String> newResult = new HashMap<>();
                Document document = Jsoup.connect(url).get();
                Elements found = document.getElementsByAttributeValueContaining("data-testid", "search-result_listing_");
                if (found.isEmpty()) {
                    applicationEventMulticaster.multicastEvent(new ErrorEvent(chatId, "Not found active results, check URL"));
                    return;
                }
                found.forEach(e -> {
                    String id = e.attributes().get("data-testid").replace("search-result_listing_", "");
                    if (!foundResults.contains(id)) {
                        String urlResult = "test";
                        newResult.put(id, urlResult);
                        foundResults.add(id);
                    }
                });
                if (!CollectionUtils.isEmpty(newResult)) {
                    applicationEventMulticaster.multicastEvent(new ResultEvent(url, chatId, newResult));
                }
                Thread.sleep(periodInSeq * 1000);
            }
            while (!stopWork.getOrDefault(chatId, true));
            log.info("Stop method for {}", chatId);
        } catch (Exception e) {
            applicationEventMulticaster.multicastEvent(new ErrorEvent(chatId, e.getMessage()));
        }
    }
}
