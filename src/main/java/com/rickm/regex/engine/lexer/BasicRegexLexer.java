package com.rickm.regex.engine.lexer;

import java.util.ArrayList;
import java.util.List;

public class BasicRegexLexer implements RegexLexer {
    private final String pattern;
    private int position;

    public BasicRegexLexer(String pattern) {
        this.pattern = pattern;
        this.position = 0;
    }

    /**
     * Tokenizes the entire pattern into a list of tokens.
     *
     * @return list of tokens
     */
    @Override
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < pattern.length()) {
            Token token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        tokens.add(new Token(TokenType.EOF, '\0', position));
        return tokens;
    }

    /**
     * Gets the next token from the pattern.
     *
     * @return the next token, or null if at end
     */
    private Token nextToken() {
        if (position >= pattern.length()) {
            return null;
        }

        char current = pattern.charAt(position);
        int tokenPos = position;

        switch (current) {
            case '\\':
                return handleEscape();
            case '.':
                position++;
                return new Token(TokenType.DOT, '.', tokenPos);
            case '*':
                position++;
                return new Token(TokenType.STAR, '*', tokenPos);
            case '+':
                position++;
                return new Token(TokenType.PLUS, '+', tokenPos);
            case '?':
                position++;
                return new Token(TokenType.QUESTION, '?', tokenPos);
            case '|':
                position++;
                return new Token(TokenType.PIPE, '|', tokenPos);
            case '(':
                position++;
                return new Token(TokenType.LPAREN, '(', tokenPos);
            case ')':
                position++;
                return new Token(TokenType.RPAREN, ')', tokenPos);
            case '[':
                position++;
                return new Token(TokenType.LBRACKET, '[', tokenPos);
            case ']':
                position++;
                return new Token(TokenType.RBRACKET, ']', tokenPos);
            case '^':
                position++;
                return new Token(TokenType.CARET, '^', tokenPos);
            case '-':
                position++;
                return new Token(TokenType.HYPHEN, '-', tokenPos);
            default:
                position++;
                return new Token(TokenType.LITERAL, current, tokenPos);
        }
    }

    /**
     * Handles escape sequences (\t, \s, \x).
     *
     * @return the appropriate token for the escape sequence
     */
    private Token handleEscape() {
        int tokenPos = position;
        position++; // Skip the backslash

        if (position >= pattern.length()) {
            // Trailing backslash - treat as literal
            return new Token(TokenType.LITERAL, '\\', tokenPos);
        }

        char escaped = pattern.charAt(position);
        position++;

        switch (escaped) {
            case 't':
                return new Token(TokenType.TAB, '\t', tokenPos);
            case 's':
                return new Token(TokenType.WHITESPACE, ' ', tokenPos);
            case 'n':
                return new Token(TokenType.ESCAPED_CHAR, '\n', tokenPos);
            case 'r':
                return new Token(TokenType.ESCAPED_CHAR, '\r', tokenPos);
            case '\\':
                return new Token(TokenType.ESCAPED_CHAR, '\\', tokenPos);
            case '.':
            case '*':
            case '+':
            case '?':
            case '|':
            case '(':
            case ')':
            case '[':
            case ']':
            case '^':
            case '-':
                return new Token(TokenType.ESCAPED_CHAR, escaped, tokenPos);
            default:
                // Unknown escape - treat as literal escaped character
                return new Token(TokenType.ESCAPED_CHAR, escaped, tokenPos);
        }
    }

    /**
     * Gets the current position in the pattern.
     *
     * @return current position
     */
    @Override
    public int getPosition() {
        return position;
    }

    /**
     * Gets the pattern being tokenized.
     *
     * @return the pattern string
     */
    @Override
    public String getPattern() {
        return pattern;
    }
}
