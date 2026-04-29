package com.atherion.andromeda.telegram;

import com.atherion.andromeda.services.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class AndromedaBot extends TelegramLongPollingBot {

    private final TelegramBotProperties props;
    private final BotCommandHandler commandHandler;
    private final AiService aiService;
    private final AiIntentRouter intentRouter;

    public AndromedaBot(TelegramBotProperties props,
                        BotCommandHandler commandHandler,
                        AiService aiService,
                        AiIntentRouter intentRouter) {
        super(props.getToken());
        this.props = props;
        this.commandHandler = commandHandler;
        this.aiService = aiService;
        this.intentRouter = intentRouter;
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
        Long telegramUserId = update.getMessage().getFrom() != null
                ? update.getMessage().getFrom().getId() : null;

        String response;

        if (text.startsWith("/")) {
            // Standard slash-command path
            response = commandHandler.handle(text, telegramUserId);

        } else if (text.toLowerCase().contains("guacamole")) {
            // Debug / AI passthrough: any guacamole mention goes straight to the AI
            response = handleDirectAiQuestion(text);

        } else if (aiService.isEnabled()) {
            // Natural language: let the AI figure out the intent and route it
            response = intentRouter.route(text, telegramUserId);
            if (response == null) {
                response = "I didn't understand that. Use /help to see available commands, " +
                           "or ask me something naturally (e.g. \"show me project 3 tasks\").";
            }

        } else {
            return; // AI disabled and no slash command — ignore
        }

        if (response != null) {
            sendText(chatId, response);
        }
    }

    /**
     * Forward a free-form question directly to the AI and return the answer.
     * Used for the guacamole debug path and any similar passthrough questions.
     */
    private String handleDirectAiQuestion(String question) {
        if (!aiService.isEnabled()) return "AI is currently disabled.";
        String answer = aiService.chat(
                "You are a helpful assistant. Answer any question the user asks clearly and concisely.",
                question);
        return answer != null ? answer : "The AI did not respond. Please try again.";
    }

    public void sendText(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
