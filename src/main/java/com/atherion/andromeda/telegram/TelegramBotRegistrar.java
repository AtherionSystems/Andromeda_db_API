package com.atherion.andromeda.telegram;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBotRegistrar {

    private final AndromedaBot bot;

    public TelegramBotRegistrar(AndromedaBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void registerBot() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
    }
}
