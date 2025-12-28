package com.rickm.regex.engine.parser;

import com.rickm.regex.engine.exception.RegexParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the BasicRegexParser (AST Builder).
 */
@DisplayName("BasicRegexParser Tests")
public class BasicRegexParserTest {

    @Nested
    @DisplayName("Literal Character Tests")
    class LiteralTests {

        @Test
        @DisplayName("Single literal character")
        void testSingleLiteral() {
            RegexParser parser = new BasicRegexParser("a");
            AstNode ast = parser.parse();

            assertEquals(NodeType.LITERAL, ast.getType());
            assertEquals('a', ast.getCharacter());
        }

        @Test
        @DisplayName("Multiple literal characters (concatenation)")
        void testMultipleLiterals() {
            RegexParser parser = new BasicRegexParser("abc");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
            assertEquals(2, ast.getChildren().size());
        }

        @Test
        @DisplayName("Digits as literals")
        void testDigits() {
            RegexParser parser = new BasicRegexParser("123");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
        }

        @Test
        @DisplayName("Unicode characters")
        void testUnicode() {
            RegexParser parser = new BasicRegexParser("αβγ");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
        }
    }

    @Nested
    @DisplayName("Escape Sequence Tests")
    class EscapeTests {

        @Test
        @DisplayName("Tab escape \\t")
        void testTabEscape() {
            RegexParser parser = new BasicRegexParser("\\t");
            AstNode ast = parser.parse();

            assertEquals(NodeType.TAB, ast.getType());
            assertEquals('\t', ast.getCharacter());
        }

        @Test
        @DisplayName("Whitespace escape \\s")
        void testWhitespaceEscape() {
            RegexParser parser = new BasicRegexParser("\\s");
            AstNode ast = parser.parse();

            assertEquals(NodeType.WHITESPACE, ast.getType());
        }

        @Test
        @DisplayName("Escaped special characters")
        void testEscapedSpecials() {
            RegexParser parser = new BasicRegexParser("\\*\\+\\?");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
            AstNode first = ast.getFirstChild();
            assertEquals(NodeType.CONCAT, first.getType());
            assertEquals(NodeType.ESCAPED, first.getFirstChild().getType());
            assertEquals('*', first.getFirstChild().getCharacter());
        }

        @Test
        @DisplayName("Escaped backslash")
        void testEscapedBackslash() {
            RegexParser parser = new BasicRegexParser("\\\\");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ESCAPED, ast.getType());
            assertEquals('\\', ast.getCharacter());
        }

        @Test
        @DisplayName("Escaped dot")
        void testEscapedDot() {
            RegexParser parser = new BasicRegexParser("\\.");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ESCAPED, ast.getType());
            assertEquals('.', ast.getCharacter());
        }
    }

    @Nested
    @DisplayName("Character Class Tests")
    class CharClassTests {

        @Test
        @DisplayName("Simple character class [abc]")
        void testSimpleCharClass() {
            RegexParser parser = new BasicRegexParser("[abc]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CHAR_CLASS, ast.getType());
            assertFalse(ast.isNegated());
            assertTrue(ast.getCharSet().contains('a'));
            assertTrue(ast.getCharSet().contains('b'));
            assertTrue(ast.getCharSet().contains('c'));
            assertEquals(3, ast.getCharSet().size());
        }

        @Test
        @DisplayName("Negated character class [^abc]")
        void testNegatedCharClass() {
            RegexParser parser = new BasicRegexParser("[^abc]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.NEGATED_CHAR_CLASS, ast.getType());
            assertTrue(ast.isNegated());
            assertTrue(ast.getCharSet().contains('a'));
        }

        @Test
        @DisplayName("Character range [a-z]")
        void testCharRange() {
            RegexParser parser = new BasicRegexParser("[a-z]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CHAR_CLASS, ast.getType());
            assertEquals(26, ast.getCharSet().size());
            assertTrue(ast.getCharSet().contains('a'));
            assertTrue(ast.getCharSet().contains('m'));
            assertTrue(ast.getCharSet().contains('z'));
        }

        @Test
        @DisplayName("Multiple ranges [a-zA-Z]")
        void testMultipleRanges() {
            RegexParser parser = new BasicRegexParser("[a-zA-Z]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CHAR_CLASS, ast.getType());
            assertEquals(52, ast.getCharSet().size());
            assertTrue(ast.getCharSet().contains('a'));
            assertTrue(ast.getCharSet().contains('Z'));
        }

        @Test
        @DisplayName("Negated range [^a-z]")
        void testNegatedRange() {
            RegexParser parser = new BasicRegexParser("[^a-z]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.NEGATED_CHAR_CLASS, ast.getType());
            assertTrue(ast.isNegated());
            assertEquals(26, ast.getCharSet().size());
        }

        @Test
        @DisplayName("Digit range [0-9]")
        void testDigitRange() {
            RegexParser parser = new BasicRegexParser("[0-9]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CHAR_CLASS, ast.getType());
            assertEquals(10, ast.getCharSet().size());
        }

        @Test
        @DisplayName("Mixed range and literals [a-z0-9_]")
        void testMixedCharClass() {
            RegexParser parser = new BasicRegexParser("[a-z0-9_]");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CHAR_CLASS, ast.getType());
            assertEquals(37, ast.getCharSet().size()); // 26 + 10 + 1
        }
    }

    @Nested
    @DisplayName("Any Character (Dot) Tests")
    class DotTests {

        @Test
        @DisplayName("Single dot")
        void testSingleDot() {
            RegexParser parser = new BasicRegexParser(".");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ANY_CHAR, ast.getType());
        }

        @Test
        @DisplayName("Dot with other characters")
        void testDotWithLiterals() {
            RegexParser parser = new BasicRegexParser("a.b");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
        }
    }

    @Nested
    @DisplayName("Quantifier Tests")
    class QuantifierTests {

        @Test
        @DisplayName("Star quantifier *")
        void testStar() {
            RegexParser parser = new BasicRegexParser("a*");
            AstNode ast = parser.parse();

            assertEquals(NodeType.STAR, ast.getType());
            assertEquals(NodeType.LITERAL, ast.getFirstChild().getType());
            assertEquals('a', ast.getFirstChild().getCharacter());
        }

        @Test
        @DisplayName("Plus quantifier +")
        void testPlus() {
            RegexParser parser = new BasicRegexParser("a+");
            AstNode ast = parser.parse();

            assertEquals(NodeType.PLUS, ast.getType());
            assertEquals(NodeType.LITERAL, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Question mark quantifier ?")
        void testQuestion() {
            RegexParser parser = new BasicRegexParser("a?");
            AstNode ast = parser.parse();

            assertEquals(NodeType.QUESTION, ast.getType());
            assertEquals(NodeType.LITERAL, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Quantifier on character class")
        void testQuantifierOnCharClass() {
            RegexParser parser = new BasicRegexParser("[a-z]+");
            AstNode ast = parser.parse();

            assertEquals(NodeType.PLUS, ast.getType());
            assertEquals(NodeType.CHAR_CLASS, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Multiple quantifiers")
        void testMultipleQuantifiers() {
            RegexParser parser = new BasicRegexParser("a*b+c?");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
        }
    }

    @Nested
    @DisplayName("Alternation Tests")
    class AlternationTests {

        @Test
        @DisplayName("Simple alternation a|b")
        void testSimpleAlternation() {
            RegexParser parser = new BasicRegexParser("a|b");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ALTERNATION, ast.getType());
            assertEquals(NodeType.LITERAL, ast.getFirstChild().getType());
            assertEquals('a', ast.getFirstChild().getCharacter());
            assertEquals(NodeType.LITERAL, ast.getSecondChild().getType());
            assertEquals('b', ast.getSecondChild().getCharacter());
        }

        @Test
        @DisplayName("Multiple alternation a|b|c")
        void testMultipleAlternation() {
            RegexParser parser = new BasicRegexParser("a|b|c");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ALTERNATION, ast.getType());
        }

        @Test
        @DisplayName("Alternation with words cat|dog")
        void testWordAlternation() {
            RegexParser parser = new BasicRegexParser("cat|dog");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ALTERNATION, ast.getType());
            assertEquals(NodeType.CONCAT, ast.getFirstChild().getType());
            assertEquals(NodeType.CONCAT, ast.getSecondChild().getType());
        }
    }

    @Nested
    @DisplayName("Group Tests")
    class GroupTests {

        @Test
        @DisplayName("Simple group (ab)")
        void testSimpleGroup() {
            RegexParser parser = new BasicRegexParser("(ab)");
            AstNode ast = parser.parse();

            assertEquals(NodeType.GROUP, ast.getType());
            assertEquals(NodeType.CONCAT, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Group with quantifier (ab)+")
        void testGroupWithQuantifier() {
            RegexParser parser = new BasicRegexParser("(ab)+");
            AstNode ast = parser.parse();

            assertEquals(NodeType.PLUS, ast.getType());
            assertEquals(NodeType.GROUP, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Nested groups ((a))")
        void testNestedGroups() {
            RegexParser parser = new BasicRegexParser("((a))");
            AstNode ast = parser.parse();

            assertEquals(NodeType.GROUP, ast.getType());
            assertEquals(NodeType.GROUP, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Group with alternation (a|b)")
        void testGroupWithAlternation() {
            RegexParser parser = new BasicRegexParser("(a|b)");
            AstNode ast = parser.parse();

            assertEquals(NodeType.GROUP, ast.getType());
            assertEquals(NodeType.ALTERNATION, ast.getFirstChild().getType());
        }

        @Test
        @DisplayName("Complex group (a|b)*c")
        void testComplexGroup() {
            RegexParser parser = new BasicRegexParser("(a|b)*c");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
            assertEquals(NodeType.STAR, ast.getFirstChild().getType());
        }
    }

    @Nested
    @DisplayName("Precedence Tests")
    class PrecedenceTests {

        @Test
        @DisplayName("Quantifier binds tighter than concatenation: ab*")
        void testQuantifierPrecedence() {
            // ab* should be a(b*) not (ab)*
            RegexParser parser = new BasicRegexParser("ab*");
            AstNode ast = parser.parse();

            assertEquals(NodeType.CONCAT, ast.getType());
            assertEquals(NodeType.LITERAL, ast.getFirstChild().getType());
            assertEquals(NodeType.STAR, ast.getSecondChild().getType());
        }

        @Test
        @DisplayName("Concatenation binds tighter than alternation: ab|cd")
        void testConcatPrecedence() {
            // ab|cd should be (ab)|(cd) not a(b|c)d
            RegexParser parser = new BasicRegexParser("ab|cd");
            AstNode ast = parser.parse();

            assertEquals(NodeType.ALTERNATION, ast.getType());
            assertEquals(NodeType.CONCAT, ast.getFirstChild().getType());
            assertEquals(NodeType.CONCAT, ast.getSecondChild().getType());
        }

        @Test
        @DisplayName("Group overrides precedence: (ab)*")
        void testGroupPrecedence() {
            RegexParser parser = new BasicRegexParser("(ab)*");
            AstNode ast = parser.parse();

            assertEquals(NodeType.STAR, ast.getType());
            assertEquals(NodeType.GROUP, ast.getFirstChild().getType());
            assertEquals(NodeType.CONCAT, ast.getFirstChild().getFirstChild().getType());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorTests {

        @Test
        @DisplayName("Unmatched opening parenthesis")
        void testUnmatchedOpenParen() {
            RegexParser parser = new BasicRegexParser("(abc");

            assertThrows(RegexParseException.class, parser::parse);
        }

        @Test
        @DisplayName("Unmatched closing parenthesis")
        void testUnmatchedCloseParen() {
            RegexParser parser = new BasicRegexParser("abc)");

            assertThrows(RegexParseException.class, parser::parse);
        }

        @Test
        @DisplayName("Unmatched opening bracket")
        void testUnmatchedOpenBracket() {
            RegexParser parser = new BasicRegexParser("[abc");

            assertThrows(RegexParseException.class, parser::parse);
        }

        @Test
        @DisplayName("Empty character class")
        void testEmptyCharClass() {
            RegexParser parser = new BasicRegexParser("[]");

            assertThrows(RegexParseException.class, parser::parse);
        }

        @Test
        @DisplayName("Invalid character range")
        void testInvalidRange() {
            RegexParser parser = new BasicRegexParser("[z-a]");

            assertThrows(RegexParseException.class, parser::parse);
        }
    }

    @Nested
    @DisplayName("Complex Pattern Tests")
    class ComplexPatternTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "[a-zA-Z_][a-zA-Z0-9_]*",  // Identifier pattern
                "[0-9]+\\.[0-9]+",          // Decimal number
                "(http|https)://[a-z]+",    // Simple URL
                "[a-z]+@[a-z]+\\.[a-z]+",   // Simple email
                "a*b*c*",                    // Multiple stars
                "(a+b)+",                    // Nested quantifiers
                "([a-z]|[0-9])+",           // Alternation in group
                "\\s+[a-zA-Z]+\\s*"         // Whitespace and words
        })
        @DisplayName("Parse complex patterns without error")
        void testComplexPatterns(String pattern) {
            RegexParser parser = new BasicRegexParser(pattern);

            assertDoesNotThrow(parser::parse);
            assertNotNull(parser.parse());
        }
    }

    @Nested
    @DisplayName("Empty and Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty pattern")
        void testEmptyPattern() {
            RegexParser parser = new BasicRegexParser("");
            AstNode ast = parser.parse();

            // Empty pattern should match empty string
            assertNotNull(ast);
        }

        @Test
        @DisplayName("Single character")
        void testSingleChar() {
            RegexParser parser = new BasicRegexParser("x");
            AstNode ast = parser.parse();

            assertEquals(NodeType.LITERAL, ast.getType());
        }

        @Test
        @DisplayName("Only quantifier star")
        void testOnlyStarQuantifier() {
            // This is technically valid - matches at any position
            RegexParser parser = new BasicRegexParser("a*");
            AstNode ast = parser.parse();

            assertEquals(NodeType.STAR, ast.getType());
        }
    }
}
