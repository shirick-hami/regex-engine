package com.rickm.regex.engine.parser;

import com.rickm.regex.engine.exception.RegexParseException;
import com.rickm.regex.engine.lexer.BasicRegexLexer;
import com.rickm.regex.engine.lexer.Token;
import com.rickm.regex.engine.lexer.TokenType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive descent parser for regular expressions.
 *
 * Implements the following grammar with proper precedence (descending order):
 *
 * 1. Groups: (EXPR)
 * 2. Atoms: literals, character classes, escape sequences
 * 3. Quantifiers: *, +, ?
 * 4. Concatenation: implicit sequence
 * 5. Alternation: |
 *
 * Grammar:
 * <pre>
 * expr        ::= concat ('|' concat)*
 * concat      ::= quantified+
 * quantified  ::= atom ('*' | '+' | '?')?
 * atom        ::= literal | escape | '.' | charClass | '(' expr ')'
 * charClass   ::= '[' '^'? charClassItems ']'
 * charClassItems ::= (charRange | char)+
 * charRange   ::= char '-' char
 * </pre>
 */
public class BasicRegexParser implements RegexParser{

    private final String pattern;
    private final List<Token> tokens;
    private int tokenIndex;

    /**
     * Creates a new parser for the given pattern.
     *
     * @param pattern the regex pattern to parse
     */
    public BasicRegexParser(String pattern) {
        this.pattern = pattern;
        this.tokens = new BasicRegexLexer(pattern).tokenize();
        this.tokenIndex = 0;
    }

    /**
     * Parses the pattern and returns the AST root.
     *
     * @return the root node of the AST
     * @throws RegexParseException if parsing fails
     */
    public AstNode parse() {
        if (pattern.isEmpty()) {
            // Empty pattern matches empty string
            return AstNode.literal('\0');
        }

        AstNode ast = parseExpr();

        // Ensure we consumed all tokens
        if (!isAtEnd()) {
            throw new RegexParseException(
                    "Unexpected token: " + currentToken().getType(),
                    pattern, currentToken().getPosition());
        }

        return ast;
    }

    /**
     * Parses an expression (handles alternation at lowest precedence).
     * expr ::= concat ('|' concat)*
     */
    private AstNode parseExpr() {
        AstNode left = parseConcat();

        while (match(TokenType.PIPE)) {
            AstNode right = parseConcat();
            left = AstNode.alternation(left, right);
        }

        return left;
    }

    /**
     * Parses a concatenation of quantified atoms.
     * concat ::= quantified+
     */
    private AstNode parseConcat() {
        AstNode result = null;

        while (!isAtEnd() && !check(TokenType.PIPE) && !check(TokenType.RPAREN)) {
            AstNode quantified = parseQuantified();
            if (quantified == null) {
                break;
            }

            if (result == null) {
                result = quantified;
            } else {
                result = AstNode.concat(result, quantified);
            }
        }

        if (result == null) {
            // Empty concatenation - matches empty string
            return AstNode.literal('\0');
        }

        return result;
    }

    /**
     * Parses an atom with optional quantifier.
     * quantified ::= atom ('*' | '+' | '?')?
     */
    private AstNode parseQuantified() {
        AstNode atom = parseAtom();

        if (atom == null) {
            return null;
        }

        if (match(TokenType.STAR)) {
            return AstNode.quantifier(NodeType.STAR, atom);
        } else if (match(TokenType.PLUS)) {
            return AstNode.quantifier(NodeType.PLUS, atom);
        } else if (match(TokenType.QUESTION)) {
            return AstNode.quantifier(NodeType.QUESTION, atom);
        }

        return atom;
    }

    /**
     * Parses an atomic expression.
     * atom ::= literal | escape | '.' | charClass | '(' expr ')'
     */
    private AstNode parseAtom() {
        // Check for EOF or tokens that shouldn't start an atom
        if (isAtEnd() || check(TokenType.RPAREN) ||
                check(TokenType.PIPE) ||
                check(TokenType.STAR) ||
                check(TokenType.PLUS) ||
                check(TokenType.QUESTION)) {
            return null;
        }

        Token token = currentToken();

        switch (token.getType()) {
            case LITERAL:
                advance();
                return AstNode.literal(token.getValue());

            case DOT:
                advance();
                return AstNode.anyChar();

            case TAB:
                advance();
                return AstNode.tab();

            case WHITESPACE:
                advance();
                return AstNode.whitespace();

            case ESCAPED_CHAR:
                advance();
                return AstNode.escaped(token.getValue());

            case LBRACKET:
                return parseCharClass();

            case LPAREN:
                return parseGroup();

            case CARET:
                // ^ outside of char class - treat as literal
                advance();
                return AstNode.literal('^');

            case HYPHEN:
                // - outside of char class - treat as literal
                advance();
                return AstNode.literal('-');

            case RBRACKET:
                // ] outside of char class - treat as literal
                advance();
                return AstNode.literal(']');

            default:
                throw new RegexParseException(
                        "Unexpected token: " + token.getType(),
                        pattern, token.getPosition());
        }
    }

    /**
     * Parses a grouped expression.
     * group ::= '(' expr ')'
     */
    private AstNode parseGroup() {
        int startPos = currentToken().getPosition();
        consume(TokenType.LPAREN, "Expected '('");

        AstNode inner = parseExpr();

        if (!match(TokenType.RPAREN)) {
            throw new RegexParseException("Unmatched '('", pattern, startPos);
        }

        return AstNode.group(inner);
    }

    /**
     * Parses a character class.
     * charClass ::= '[' '^'? charClassItems ']'
     */
    private AstNode parseCharClass() {
        int startPos = currentToken().getPosition();
        consume(TokenType.LBRACKET, "Expected '['");

        boolean negated = false;
        if (match(TokenType.CARET)) {
            negated = true;
        }

        Set<Character> chars = new HashSet<>();

        // First character - ] and - have special handling
        if (check(TokenType.RBRACKET) && chars.isEmpty()) {
            // [] is invalid, but []] means literal ]
            // We'll treat empty as error
            throw new RegexParseException("Empty character class", pattern, startPos);
        }

        while (!isAtEnd() && !check(TokenType.RBRACKET)) {
            parseCharClassItem(chars);
        }

        if (!match(TokenType.RBRACKET)) {
            throw new RegexParseException("Unmatched '['", pattern, startPos);
        }

        if (chars.isEmpty()) {
            throw new RegexParseException("Empty character class", pattern, startPos);
        }

        return AstNode.charClass(chars, negated);
    }

    /**
     * Parses a single item in a character class (character or range).
     */
    private void parseCharClassItem(Set<Character> chars) {
        char first = getCharClassChar();

        // Check for range
        if (check(TokenType.HYPHEN) && !isNextToken(TokenType.RBRACKET)) {
            advance(); // consume hyphen
            char last = getCharClassChar();

            if (last < first) {
                throw new RegexParseException(
                        "Invalid character range: " + first + "-" + last,
                        pattern, tokenIndex);
            }

            for (char c = first; c <= last; c++) {
                chars.add(c);
            }
        } else {
            chars.add(first);
        }
    }

    /**
     * Gets a single character for use in a character class.
     */
    private char getCharClassChar() {
        if (isAtEnd()) {
            throw new RegexParseException("Unexpected end in character class", pattern, tokenIndex);
        }

        Token token = currentToken();
        advance();

        switch (token.getType()) {
            case LITERAL:
            case ESCAPED_CHAR:
            case CARET:  // ^ as literal if not at start
            case HYPHEN: // - as literal at start or end
            case STAR:
            case PLUS:
            case QUESTION:
            case PIPE:
            case DOT:
            case LPAREN:
            case RPAREN:
                return token.getValue();
            case TAB:
                return '\t';
            case WHITESPACE:
                // In a character class, \s should add space-type characters
                // For simplicity, we'll return space and the caller should handle \s specially
                return ' ';
            default:
                throw new RegexParseException(
                        "Invalid character in character class: " + token.getType(),
                        pattern, token.getPosition());
        }
    }

    // ===== Helper methods =====

    /**
     * Checks if we've reached the end of tokens.
     */
    private boolean isAtEnd() {
        return tokenIndex >= tokens.size() ||
                tokens.get(tokenIndex).getType() == TokenType.EOF;
    }

    /**
     * Gets the current token without advancing.
     */
    private Token currentToken() {
        if (tokenIndex >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF
        }
        return tokens.get(tokenIndex);
    }

    /**
     * Checks if the current token is of the given type.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return currentToken().getType() == type;
    }

    /**
     * Checks if the next token is of the given type.
     */
    private boolean isNextToken(TokenType type) {
        if (tokenIndex + 1 >= tokens.size()) return type == TokenType.EOF;
        return tokens.get(tokenIndex + 1).getType() == type;
    }

    /**
     * Advances to the next token and returns the previous one.
     */
    private Token advance() {
        if (!isAtEnd()) {
            tokenIndex++;
        }
        return tokens.get(tokenIndex - 1);
    }

    /**
     * If the current token matches the given type, advance and return true.
     */
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Consume a token of the expected type, or throw an exception.
     */
    private void consume(TokenType type, String message) {
        if (!match(type)) {
            throw new RegexParseException(message, pattern,
                    isAtEnd() ? pattern.length() : currentToken().getPosition());
        }
    }
}
