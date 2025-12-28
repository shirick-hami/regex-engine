package com.rickm.regex.engine.lexer;

import lombok.Getter;

@Getter
public class Token {
    private final TokenType type;
    private final char value;
    private final int position;

    public Token(TokenType type, char value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("Token{type=%s, value='%c', pos=%d}", type, value, position);
    }
}
