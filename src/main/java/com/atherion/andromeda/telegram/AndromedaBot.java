package com.atherion.andromeda.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class AndromedaBot extends TelegramLongPollingBot {

    private final TelegramBotProperties props;
    private final BotCommandHandler commandHandler;

    public AndromedaBot(TelegramBotProperties props, BotCommandHandler commandHandler) {
        super(props.getToken());
        this.props = props;
        this.commandHandler = commandHandler;
    }

    @Override
    public String getBotUsername() {
        return props.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text   = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();

        if (!text.startsWith("/")) return;

        Long telegramUserId = (update.getMessage().getFrom() != null)
                ? update.getMessage().getFrom().getId()
                : null;

        String response = commandHandler.handle(text, telegramUserId);
        if (response != null) {
            sendText(chatId, response);
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
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
