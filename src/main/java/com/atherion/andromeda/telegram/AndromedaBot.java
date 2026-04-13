package com.atherion.andromeda.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class AndromedaBot extends TelegramLongPollingBot {

    private final TelegramBotProperties props;

    public AndromedaBot(TelegramBotProperties props) {
        super(props.getToken());
        this.props = props;
    }

    @Override
    public String getBotUsername() {
        return props.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();

        if ("/ping".equals(text) || ("/ping@" + props.getUsername()).equals(text)) {
            sendText(chatId, "Pong! Andromeda API is up and running.");
        } else if ("/health".equals(text)) {
            sendText(chatId, "Status: OK\nService: Andromeda Backend API\nBot: Connected");
        }
    }

    public void sendText(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            // log or rethrow as needed
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
