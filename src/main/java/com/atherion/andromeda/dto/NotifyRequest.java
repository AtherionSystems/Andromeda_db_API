package com.atherion.andromeda.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotifyRequest {
    private String chatId;
    private String context;
}
