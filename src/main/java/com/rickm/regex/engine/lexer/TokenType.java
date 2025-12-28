package com.rickm.regex.engine.lexer;

public enum TokenType {
    LITERAL,        // Regular character
    DOT,            // . (any character)
    STAR,           // * (zero or more)
    PLUS,           // + (one or more)
    QUESTION,       // ? (zero or one)
    PIPE,           // | (alternation)
    LPAREN,         // ( (group start)
    RPAREN,         // ) (group end)
    LBRACKET,       // [ (char class start)
    RBRACKET,       // ] (char class end)
    CARET,          // ^ (negation in char class)
    HYPHEN,         // - (range in char class)
    BACKSLASH,      // \ (escape)
    TAB,            // \t
    WHITESPACE,     // \s
    ESCAPED_CHAR,   // \x (escaped character)
    EOF             // End of pattern
}
