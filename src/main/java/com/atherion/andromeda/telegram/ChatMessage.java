package com.atherion.andromeda.telegram;

/**
 * Represents a single turn in a conversation: either a user message or an
 * assistant response. Stored in ConversationSession for multi-turn AI context.
 */
public record ChatMessage(String role, String content) {}
