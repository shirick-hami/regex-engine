package com.rickm.regex.engine.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the RegexLexer (Tokenizer).
 */
@DisplayName("RegexLexer Tests")
class BasicRegexLexerTest {
    
    @Nested
    @DisplayName("Basic Token Tests")
    class BasicTokenTests {
        
        @Test
        @DisplayName("Tokenize single literal")
        void testSingleLiteral() {
            RegexLexer lexer = new BasicRegexLexer("a");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(2, tokens.size()); // literal + EOF
            assertEquals(TokenType.LITERAL, tokens.get(0).getType());
            assertEquals('a', tokens.get(0).getValue());
            assertEquals(TokenType.EOF, tokens.get(1).getType());
        }
        
        @Test
        @DisplayName("Tokenize multiple literals")
        void testMultipleLiterals() {
            RegexLexer lexer = new BasicRegexLexer("abc");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(4, tokens.size()); // 3 literals + EOF
            assertEquals('a', tokens.get(0).getValue());
            assertEquals('b', tokens.get(1).getValue());
            assertEquals('c', tokens.get(2).getValue());
        }
        
        @Test
        @DisplayName("Empty pattern produces only EOF")
        void testEmptyPattern() {
            RegexLexer lexer = new BasicRegexLexer("");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(1, tokens.size());
            assertEquals(TokenType.EOF, tokens.get(0).getType());
        }
    }
    
    @Nested
    @DisplayName("Special Character Tests")
    class SpecialCharacterTests {
        
        @Test
        @DisplayName("Tokenize dot")
        void testDot() {
            RegexLexer lexer = new BasicRegexLexer(".");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.DOT, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize star")
        void testStar() {
            RegexLexer lexer = new BasicRegexLexer("*");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.STAR, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize plus")
        void testPlus() {
            RegexLexer lexer = new BasicRegexLexer("+");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.PLUS, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize question")
        void testQuestion() {
            RegexLexer lexer = new BasicRegexLexer("?");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.QUESTION, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize pipe")
        void testPipe() {
            RegexLexer lexer = new BasicRegexLexer("|");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.PIPE, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize parentheses")
        void testParentheses() {
            RegexLexer lexer = new BasicRegexLexer("()");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.LPAREN, tokens.get(0).getType());
            assertEquals(TokenType.RPAREN, tokens.get(1).getType());
        }
        
        @Test
        @DisplayName("Tokenize brackets")
        void testBrackets() {
            RegexLexer lexer = new BasicRegexLexer("[]");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.LBRACKET, tokens.get(0).getType());
            assertEquals(TokenType.RBRACKET, tokens.get(1).getType());
        }
        
        @Test
        @DisplayName("Tokenize caret")
        void testCaret() {
            RegexLexer lexer = new BasicRegexLexer("^");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.CARET, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize hyphen")
        void testHyphen() {
            RegexLexer lexer = new BasicRegexLexer("-");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.HYPHEN, tokens.get(0).getType());
        }
    }
    
    @Nested
    @DisplayName("Escape Sequence Tests")
    class EscapeSequenceTests {
        
        @Test
        @DisplayName("Tokenize tab escape")
        void testTabEscape() {
            RegexLexer lexer = new BasicRegexLexer("\\t");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.TAB, tokens.get(0).getType());
            assertEquals('\t', tokens.get(0).getValue());
        }
        
        @Test
        @DisplayName("Tokenize whitespace escape")
        void testWhitespaceEscape() {
            RegexLexer lexer = new BasicRegexLexer("\\s");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.WHITESPACE, tokens.get(0).getType());
        }
        
        @Test
        @DisplayName("Tokenize newline escape")
        void testNewlineEscape() {
            RegexLexer lexer = new BasicRegexLexer("\\n");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.ESCAPED_CHAR, tokens.get(0).getType());
            assertEquals('\n', tokens.get(0).getValue());
        }
        
        @Test
        @DisplayName("Tokenize carriage return escape")
        void testCarriageReturnEscape() {
            RegexLexer lexer = new BasicRegexLexer("\\r");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.ESCAPED_CHAR, tokens.get(0).getType());
            assertEquals('\r', tokens.get(0).getValue());
        }
        
        @Test
        @DisplayName("Tokenize escaped backslash")
        void testEscapedBackslash() {
            RegexLexer lexer = new BasicRegexLexer("\\\\");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.ESCAPED_CHAR, tokens.get(0).getType());
            assertEquals('\\', tokens.get(0).getValue());
        }
        
        @Test
        @DisplayName("Tokenize escaped special characters")
        void testEscapedSpecials() {
            RegexLexer lexer = new BasicRegexLexer("\\*\\+\\?\\.");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(5, tokens.size()); // 4 escaped + EOF
            assertEquals('*', tokens.get(0).getValue());
            assertEquals('+', tokens.get(1).getValue());
            assertEquals('?', tokens.get(2).getValue());
            assertEquals('.', tokens.get(3).getValue());
        }
        
        @Test
        @DisplayName("Trailing backslash treated as literal")
        void testTrailingBackslash() {
            RegexLexer lexer = new BasicRegexLexer("a\\");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(3, tokens.size()); // a, \, EOF
            assertEquals(TokenType.LITERAL, tokens.get(0).getType());
            assertEquals(TokenType.LITERAL, tokens.get(1).getType());
            assertEquals('\\', tokens.get(1).getValue());
        }
    }
    
    @Nested
    @DisplayName("Token Position Tests")
    class PositionTests {
        
        @Test
        @DisplayName("Token positions are correct")
        void testPositions() {
            RegexLexer lexer = new BasicRegexLexer("abc");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(0, tokens.get(0).getPosition());
            assertEquals(1, tokens.get(1).getPosition());
            assertEquals(2, tokens.get(2).getPosition());
            assertEquals(3, tokens.get(3).getPosition()); // EOF
        }
        
        @Test
        @DisplayName("Escape sequence positions")
        void testEscapePositions() {
            RegexLexer lexer = new BasicRegexLexer("a\\tb");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(0, tokens.get(0).getPosition()); // a
            assertEquals(1, tokens.get(1).getPosition()); // \t
            assertEquals(3, tokens.get(2).getPosition()); // b
        }
    }
    
    @Nested
    @DisplayName("Complex Pattern Tests")
    class ComplexPatternTests {
        
        @Test
        @DisplayName("Tokenize character class pattern")
        void testCharClassPattern() {
            RegexLexer lexer = new BasicRegexLexer("[a-z]+");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(7, tokens.size());
            assertEquals(TokenType.LBRACKET, tokens.get(0).getType());
            assertEquals(TokenType.LITERAL, tokens.get(1).getType());
            assertEquals(TokenType.HYPHEN, tokens.get(2).getType());
            assertEquals(TokenType.LITERAL, tokens.get(3).getType());
            assertEquals(TokenType.RBRACKET, tokens.get(4).getType());
            assertEquals(TokenType.PLUS, tokens.get(5).getType());
            assertEquals(TokenType.EOF, tokens.get(6).getType());
        }
        
        @Test
        @DisplayName("Tokenize negated character class")
        void testNegatedCharClass() {
            RegexLexer lexer = new BasicRegexLexer("[^abc]");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(TokenType.LBRACKET, tokens.get(0).getType());
            assertEquals(TokenType.CARET, tokens.get(1).getType());
            assertEquals(TokenType.LITERAL, tokens.get(2).getType());
        }
        
        @Test
        @DisplayName("Tokenize alternation pattern")
        void testAlternation() {
            RegexLexer lexer = new BasicRegexLexer("a|b");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(4, tokens.size());
            assertEquals(TokenType.LITERAL, tokens.get(0).getType());
            assertEquals(TokenType.PIPE, tokens.get(1).getType());
            assertEquals(TokenType.LITERAL, tokens.get(2).getType());
        }
        
        @Test
        @DisplayName("Tokenize grouped pattern")
        void testGroupedPattern() {
            RegexLexer lexer = new BasicRegexLexer("(ab)+");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(6, tokens.size());
            assertEquals(TokenType.LPAREN, tokens.get(0).getType());
            assertEquals(TokenType.LITERAL, tokens.get(1).getType());
            assertEquals(TokenType.LITERAL, tokens.get(2).getType());
            assertEquals(TokenType.RPAREN, tokens.get(3).getType());
            assertEquals(TokenType.PLUS, tokens.get(4).getType());
        }
    }
    
    @Nested
    @DisplayName("Unicode Tests")
    class UnicodeTests {
        
        @Test
        @DisplayName("Tokenize unicode characters")
        void testUnicode() {
            RegexLexer lexer = new BasicRegexLexer("αβγ");
            List<Token> tokens = lexer.tokenize();
            
            assertEquals(4, tokens.size());
            assertEquals('α', tokens.get(0).getValue());
            assertEquals('β', tokens.get(1).getValue());
            assertEquals('γ', tokens.get(2).getValue());
        }
        
        @Test
        @DisplayName("Tokenize emoji")
        void testEmoji() {
            RegexLexer lexer = new BasicRegexLexer("a😀b");
            List<Token> tokens = lexer.tokenize();
            
            // Note: emoji handling depends on Java's char handling
            assertTrue(tokens.size() >= 3);
        }
    }
}
