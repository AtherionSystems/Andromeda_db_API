package com.atherion.andromeda;

import com.atherion.andromeda.telegram.AndromedaBot;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AndromedaBotTest {

    @Mock
    private TelegramBotProperties props;

    private AndromedaBot bot;

    @BeforeEach
    void setUp() {
        when(props.getToken()).thenReturn("fake-token");
        when(props.getUsername()).thenReturn("AndromedaBot");

        bot = spy(new AndromedaBot(props));
        doNothing().when(bot).sendText(anyString(), anyString());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Update buildUpdate(String text, long chatId) {
        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setText(text);
        message.setChat(chat);

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
