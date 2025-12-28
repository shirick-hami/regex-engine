package com.rickm.regex.engine.matcher;


import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.exception.BacktrackLimitExceededException;
import com.rickm.regex.engine.parser.AstNode;
import com.rickm.regex.engine.parser.BasicRegexParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the BacktrackingMatcher.
 */
@DisplayName("BacktrackingMatcher Tests")
class BacktrackingMatcherTest {
    
    private static final long DEFAULT_BACKTRACK_LIMIT = 100000;
    private static final long DEFAULT_TIMEOUT = 30000;
    
    private MatchResult matchFull(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        BacktrackingMatcher matcher = new BacktrackingMatcher(ast, DEFAULT_BACKTRACK_LIMIT, DEFAULT_TIMEOUT);
        return matcher.matchFull(input);
    }
    
    private MatchResult find(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        BacktrackingMatcher matcher = new BacktrackingMatcher(ast, DEFAULT_BACKTRACK_LIMIT, DEFAULT_TIMEOUT);
        return matcher.find(input);
    }
    
    private MatchResult findAll(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        BacktrackingMatcher matcher = new BacktrackingMatcher(ast, DEFAULT_BACKTRACK_LIMIT, DEFAULT_TIMEOUT);
        return matcher.findAll(input);
    }
    
    @Nested
    @DisplayName("Literal Matching Tests")
    class LiteralTests {
        
        @Test
        @DisplayName("Match single character")
        void testSingleChar() {
            MatchResult result = matchFull("a", "a");
            assertTrue(result.isMatched());
            assertEquals("a", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Single character no match")
        void testSingleCharNoMatch() {
            MatchResult result = matchFull("a", "b");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Match word")
        void testWord() {
            MatchResult result = matchFull("hello", "hello");
            assertTrue(result.isMatched());
            assertEquals("hello", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Word partial match fails full match")
        void testPartialWordFails() {
            MatchResult result = matchFull("hello", "hello world");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Find word in text")
        void testFindWord() {
            MatchResult result = find("hello", "say hello world");
            assertTrue(result.isMatched());
            assertEquals(4, result.getStartIndex());
            assertEquals(9, result.getEndIndex());
            assertEquals("hello", result.getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("Tab Character (\\t) Tests")
    class TabTests {
        
        @Test
        @DisplayName("Match tab character")
        void testMatchTab() {
            MatchResult result = matchFull("\\t", "\t");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Tab does not match space")
        void testTabNotSpace() {
            MatchResult result = matchFull("\\t", " ");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Find tab in string")
        void testFindTab() {
            MatchResult result = find("\\t", "hello\tworld");
            assertTrue(result.isMatched());
            assertEquals(5, result.getStartIndex());
        }
        
        @Test
        @DisplayName("Multiple tabs")
        void testMultipleTabs() {
            MatchResult result = matchFull("\\t\\t", "\t\t");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Whitespace (\\s) Tests")
    class WhitespaceTests {
        
        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", "\n", "\r"})
        @DisplayName("Match various whitespace characters")
        void testMatchWhitespace(String ws) {
            MatchResult result = matchFull("\\s", ws);
            assertTrue(result.isMatched(), "Should match: " + ws.getBytes()[0]);
        }
        
        @Test
        @DisplayName("Whitespace does not match letter")
        void testWhitespaceNotLetter() {
            MatchResult result = matchFull("\\s", "a");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Find whitespace in text")
        void testFindWhitespace() {
            MatchResult result = find("\\s", "hello world");
            assertTrue(result.isMatched());
            assertEquals(5, result.getStartIndex());
            assertEquals(" ", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Multiple whitespace with +")
        void testMultipleWhitespace() {
            MatchResult result = matchFull("\\s+", "   ");
            assertTrue(result.isMatched());
            assertEquals(3, result.getMatchedText().length());
        }
    }
    
    @Nested
    @DisplayName("Character Class [abc] Tests")
    class CharClassTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"a", "b", "c"})
        @DisplayName("Match characters in class")
        void testMatchInClass(String ch) {
            MatchResult result = matchFull("[abc]", ch);
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Character not in class")
        void testNotInClass() {
            MatchResult result = matchFull("[abc]", "d");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Character class with quantifier")
        void testClassWithQuantifier() {
            MatchResult result = matchFull("[abc]+", "abcabc");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Negated Character Class [^abc] Tests")
    class NegatedCharClassTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"d", "e", "x", "1"})
        @DisplayName("Match characters NOT in negated class")
        void testMatchNotInClass(String ch) {
            MatchResult result = matchFull("[^abc]", ch);
            assertTrue(result.isMatched());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"a", "b", "c"})
        @DisplayName("Characters in negated class do not match")
        void testInNegatedClass(String ch) {
            MatchResult result = matchFull("[^abc]", ch);
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Negated class with range")
        void testNegatedRange() {
            MatchResult result = matchFull("[^a-z]", "5");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Negated range fails for char in range")
        void testNegatedRangeFails() {
            MatchResult result = matchFull("[^a-z]", "m");
            assertFalse(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Character Range [a-z] Tests")
    class CharRangeTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"a", "m", "z"})
        @DisplayName("Match characters in range")
        void testMatchInRange(String ch) {
            MatchResult result = matchFull("[a-z]", ch);
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Uppercase not in lowercase range")
        void testCaseNotInRange() {
            MatchResult result = matchFull("[a-z]", "A");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Multiple ranges [a-zA-Z]")
        void testMultipleRanges() {
            assertTrue(matchFull("[a-zA-Z]", "a").isMatched());
            assertTrue(matchFull("[a-zA-Z]", "Z").isMatched());
            assertFalse(matchFull("[a-zA-Z]", "5").isMatched());
        }
        
        @Test
        @DisplayName("Digit range [0-9]")
        void testDigitRange() {
            assertTrue(matchFull("[0-9]", "5").isMatched());
            assertFalse(matchFull("[0-9]", "a").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Star Quantifier (*) Tests")
    class StarTests {
        
        @Test
        @DisplayName("Star matches zero occurrences")
        void testStarZero() {
            MatchResult result = matchFull("a*", "");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Star matches one occurrence")
        void testStarOne() {
            MatchResult result = matchFull("a*", "a");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Star matches many occurrences")
        void testStarMany() {
            MatchResult result = matchFull("a*", "aaaaa");
            assertTrue(result.isMatched());
            assertEquals("aaaaa", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Star with following literal")
        void testStarWithFollowing() {
            MatchResult result = matchFull("a*b", "aaab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Star greedy behavior")
        void testStarGreedy() {
            MatchResult result = find("a*", "aaa");
            assertTrue(result.isMatched());
            assertEquals("aaa", result.getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("Plus Quantifier (+) Tests")
    class PlusTests {
        
        @Test
        @DisplayName("Plus requires at least one")
        void testPlusRequiresOne() {
            MatchResult result = matchFull("a+", "");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Plus matches one")
        void testPlusOne() {
            MatchResult result = matchFull("a+", "a");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Plus matches many")
        void testPlusMany() {
            MatchResult result = matchFull("a+", "aaaaa");
            assertTrue(result.isMatched());
            assertEquals("aaaaa", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Plus with character class")
        void testPlusWithClass() {
            MatchResult result = matchFull("[a-z]+", "hello");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Question Mark Quantifier (?) Tests")
    class QuestionTests {
        
        @Test
        @DisplayName("Question matches zero")
        void testQuestionZero() {
            MatchResult result = matchFull("a?", "");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Question matches one")
        void testQuestionOne() {
            MatchResult result = matchFull("a?", "a");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Question does not match two")
        void testQuestionNotTwo() {
            MatchResult result = matchFull("a?", "aa");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Optional in pattern")
        void testOptionalPattern() {
            assertTrue(matchFull("colou?r", "color").isMatched());
            assertTrue(matchFull("colou?r", "colour").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Alternation (|) Tests")
    class AlternationTests {
        
        @Test
        @DisplayName("Match first alternative")
        void testFirstAlternative() {
            MatchResult result = matchFull("a|b", "a");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Match second alternative")
        void testSecondAlternative() {
            MatchResult result = matchFull("a|b", "b");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Neither alternative matches")
        void testNoAlternativeMatch() {
            MatchResult result = matchFull("a|b", "c");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Word alternation")
        void testWordAlternation() {
            assertTrue(matchFull("cat|dog", "cat").isMatched());
            assertTrue(matchFull("cat|dog", "dog").isMatched());
            assertFalse(matchFull("cat|dog", "bird").isMatched());
        }
        
        @Test
        @DisplayName("Multiple alternatives")
        void testMultipleAlternatives() {
            assertTrue(matchFull("a|b|c", "a").isMatched());
            assertTrue(matchFull("a|b|c", "b").isMatched());
            assertTrue(matchFull("a|b|c", "c").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Escape Character (\\x) Tests")
    class EscapeTests {
        
        @ParameterizedTest
        @CsvSource({
            "\\*, *",
            "\\+, +",
            "\\?, ?",
            "\\., .",
            "\\|, |",
            "\\(, (",
            "\\), )",
            "\\[, [",
            "\\], ]",
            "\\\\, \\"
        })
        @DisplayName("Escaped special characters match literally")
        void testEscapedSpecials(String pattern, String expected) {
            MatchResult result = matchFull(pattern, expected);
            assertTrue(result.isMatched(), "Pattern " + pattern + " should match " + expected);
        }
        
        @Test
        @DisplayName("Escaped dot matches only dot")
        void testEscapedDot() {
            assertTrue(matchFull("\\.", ".").isMatched());
            assertFalse(matchFull("\\.", "a").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Any Character (.) Tests")
    class AnyCharTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"a", "1", "!", " ", "\t", "α"})
        @DisplayName("Dot matches various characters")
        void testDotMatches(String ch) {
            MatchResult result = matchFull(".", ch);
            assertTrue(result.isMatched(), "Dot should match: " + ch);
        }
        
        @Test
        @DisplayName("Dot does not match newline")
        void testDotNotNewline() {
            MatchResult result = matchFull(".", "\n");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Dot with quantifier")
        void testDotWithQuantifier() {
            MatchResult result = matchFull(".+", "hello");
            assertTrue(result.isMatched());
            assertEquals("hello", result.getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("Group Tests")
    class GroupTests {
        
        @Test
        @DisplayName("Simple group")
        void testSimpleGroup() {
            MatchResult result = matchFull("(ab)", "ab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Group with quantifier")
        void testGroupWithQuantifier() {
            MatchResult result = matchFull("(ab)+", "ababab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Group with alternation")
        void testGroupWithAlternation() {
            assertTrue(matchFull("(cat|dog)s", "cats").isMatched());
            assertTrue(matchFull("(cat|dog)s", "dogs").isMatched());
        }
        
        @Test
        @DisplayName("Nested groups")
        void testNestedGroups() {
            MatchResult result = matchFull("((a)+)", "aaa");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {
        
        @Test
        @DisplayName("Find all words")
        void testFindAllWords() {
            MatchResult result = findAll("[a-z]+", "hello world foo");
            assertTrue(result.isMatched());
            assertEquals(3, result.getAllMatches().size());
            assertEquals("hello", result.getAllMatches().get(0).getMatchedText());
            assertEquals("world", result.getAllMatches().get(1).getMatchedText());
            assertEquals("foo", result.getAllMatches().get(2).getMatchedText());
        }
        
        @Test
        @DisplayName("Find all digits")
        void testFindAllDigits() {
            MatchResult result = findAll("[0-9]+", "Order 123 and Order 456");
            assertTrue(result.isMatched());
            assertEquals(2, result.getAllMatches().size());
            assertEquals("123", result.getAllMatches().get(0).getMatchedText());
            assertEquals("456", result.getAllMatches().get(1).getMatchedText());
        }
        
        @Test
        @DisplayName("Find all with positions")
        void testFindAllPositions() {
            MatchResult result = findAll("ab", "ab ab ab");
            assertTrue(result.isMatched());
            assertEquals(3, result.getAllMatches().size());
            assertEquals(0, result.getAllMatches().get(0).getStartIndex());
            assertEquals(3, result.getAllMatches().get(1).getStartIndex());
            assertEquals(6, result.getAllMatches().get(2).getStartIndex());
        }
    }
    
    @Nested
    @DisplayName("Complex Pattern Tests")
    class ComplexPatternTests {
        
        @Test
        @DisplayName("Identifier pattern")
        void testIdentifier() {
            String pattern = "[a-zA-Z_][a-zA-Z0-9_]*";
            assertTrue(matchFull(pattern, "myVar").isMatched());
            assertTrue(matchFull(pattern, "_private").isMatched());
            assertTrue(matchFull(pattern, "var123").isMatched());
            assertFalse(matchFull(pattern, "123var").isMatched());
        }
        
        @Test
        @DisplayName("Simple email-like pattern")
        void testEmailLike() {
            String pattern = "[a-z]+@[a-z]+\\.[a-z]+";
            assertTrue(matchFull(pattern, "user@example.com").isMatched());
            assertFalse(matchFull(pattern, "invalid").isMatched());
        }
        
        @Test
        @DisplayName("HTTP/HTTPS pattern")
        void testHttpPattern() {
            String pattern = "(http|https)://[a-z]+";
            assertTrue(matchFull(pattern, "http://example").isMatched());
            assertTrue(matchFull(pattern, "https://example").isMatched());
            assertFalse(matchFull(pattern, "ftp://example").isMatched());
        }
        
        @Test
        @DisplayName("Decimal number pattern")
        void testDecimalNumber() {
            String pattern = "[0-9]+\\.[0-9]+";
            assertTrue(matchFull(pattern, "3.14").isMatched());
            assertTrue(matchFull(pattern, "123.456").isMatched());
            assertFalse(matchFull(pattern, "123").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Backtracking Behavior Tests")
    class BacktrackingTests {
        
        @Test
        @DisplayName("Backtracking in star quantifier")
        void testStarBacktracking() {
            // Pattern a*a requires backtracking
            MatchResult result = matchFull("a*a", "aaa");
            assertTrue(result.isMatched());
            assertTrue(result.getBacktrackCount() > 0);
        }
        
        @Test
        @DisplayName("Backtracking in alternation")
        void testAlternationBacktracking() {
            // Should try first alternative, fail, then try second
            MatchResult result = matchFull("ab|ac", "ac");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Backtrack limit enforcement")
        void testBacktrackLimit() {
            // Pattern that causes excessive backtracking
            AstNode ast = new BasicRegexParser("a*a*a*a*a*b").parse();
            BacktrackingMatcher matcher = new BacktrackingMatcher(ast, 100, 30000);
            
            assertThrows(BacktrackLimitExceededException.class,
                    () -> matcher.matchFull("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaac"));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty pattern matches empty string")
        void testEmptyPatternEmptyString() {
            MatchResult result = matchFull("", "");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Empty input with star")
        void testEmptyInputStar() {
            MatchResult result = matchFull("a*", "");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Unicode input")
        void testUnicodeInput() {
            MatchResult result = matchFull(".", "α");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Very long match")
        void testLongMatch() {
            String longInput = "a".repeat(1000);
            MatchResult result = matchFull("a+", longInput);
            assertTrue(result.isMatched());
            assertEquals(1000, result.getMatchedText().length());
        }
    }
}
