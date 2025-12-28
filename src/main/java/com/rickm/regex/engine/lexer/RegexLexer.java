package com.rickm.regex.engine.lexer;

import java.util.List;

public interface RegexLexer {
    List<Token> tokenize();
    int getPosition();
    String getPattern();
}
