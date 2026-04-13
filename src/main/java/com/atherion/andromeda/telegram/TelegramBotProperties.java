package com.atherion.andromeda.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramBotProperties {

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.username}")
    private String username;

    public String getToken() { return token; }
    public String getUsername() { return username; }
}
