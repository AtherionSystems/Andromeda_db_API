package com.atherion.andromeda;

import com.atherion.andromeda.telegram.AndromedaBot;
import com.atherion.andromeda.telegram.BotCommandHandler;
import com.atherion.andromeda.telegram.TelegramBotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AndromedaBotTest {

    @Mock
    private TelegramBotProperties props;

    @Mock
    private BotCommandHandler commandHandler;

    private AndromedaBot bot;

    @BeforeEach
    void setUp() {
        when(props.getToken()).thenReturn("fake-token");
        lenient().when(props.getUsername()).thenReturn("AndromedaBot");
        lenient().when(commandHandler.handle(anyString(), any())).thenReturn(null);
        lenient().when(commandHandler.handle(eq("/ping"),             any())).thenReturn("Pong! Andromeda API is up and running.");
        lenient().when(commandHandler.handle(eq("/ping@AndromedaBot"), any())).thenReturn("Pong! Andromeda API is up and running.");
        lenient().when(commandHandler.handle(eq("/health"),           any())).thenReturn("Status: OK\nService: Andromeda Backend API\nBot: Connected");

        bot = spy(new AndromedaBot(props, commandHandler));
        lenient().doNothing().when(bot).sendText(anyString(), anyString());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Update buildUpdate(String text, long chatId) {
        User from = new User();
        from.setId(999L);
        from.setFirstName("Test");

        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setText(text);
        message.setChat(chat);
        message.setFrom(from);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    // ── /ping ──────────────────────────────────────────────────────────────────

    @Test
    void ping_command_sendsPongResponse() {
        bot.onUpdateReceived(buildUpdate("/ping", 100L));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendText(eq("100"), textCaptor.capture());
        assertTrue(textCaptor.getValue().toLowerCase().contains("pong"));
    }

    @Test
    void ping_commandWithUsername_sendsPongResponse() {
        bot.onUpdateReceived(buildUpdate("/ping@AndromedaBot", 200L));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendText(eq("200"), textCaptor.capture());
        assertTrue(textCaptor.getValue().toLowerCase().contains("pong"));
    }

    // ── /health ────────────────────────────────────────────────────────────────

    @Test
    void health_command_sendsStatusOkResponse() {
        bot.onUpdateReceived(buildUpdate("/health", 300L));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendText(eq("300"), textCaptor.capture());
        assertTrue(textCaptor.getValue().contains("OK"));
    }

    // ── unknown / empty ────────────────────────────────────────────────────────

    @Test
    void unknownCommand_doesNotSendAnything() {
        bot.onUpdateReceived(buildUpdate("/unknown", 400L));

        verify(bot, never()).sendText(anyString(), anyString());
    }

    @Test
    void updateWithNoMessage_doesNotSendAnything() {
        Update update = new Update(); // message is null
        bot.onUpdateReceived(update);

        verify(bot, never()).sendText(anyString(), anyString());
    }

    @Test
    void updateWithNoText_doesNotSendAnything() {
        Chat chat = new Chat();
        chat.setId(500L);

        Message message = new Message();
        message.setChat(chat);
        // no text set

        Update update = new Update();
        update.setMessage(message);

        bot.onUpdateReceived(update);

        verify(bot, never()).sendText(anyString(), anyString());
    }
}
