package com.rickm.regex.engine.matcher;

import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.parser.AstNode;
import com.rickm.regex.engine.parser.BasicRegexParser;
import com.rickm.regex.engine.parser.RegexParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the DFAMatcher.
 * Tests the DFA-based matching algorithm using subset construction.
 */
@DisplayName("DFAMatcher Tests")
class DFAMatcherTest {
    
    private static final long DEFAULT_TIMEOUT = 30000;
    
    private MatchResult matchFull(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        DFAMatcher matcher = new DFAMatcher(ast, DEFAULT_TIMEOUT);
        return matcher.matchFull(input);
    }
    
    private MatchResult find(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        DFAMatcher matcher = new DFAMatcher(ast, DEFAULT_TIMEOUT);
        return matcher.find(input);
    }
    
    private MatchResult findAll(String pattern, String input) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        DFAMatcher matcher = new DFAMatcher(ast, DEFAULT_TIMEOUT);
        return matcher.findAll(input);
    }
    
    private DFAMatcher createMatcher(String pattern) {
        AstNode ast = new BasicRegexParser(pattern).parse();
        return new DFAMatcher(ast, DEFAULT_TIMEOUT);
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
        
        @ParameterizedTest
        @CsvSource({
            "abc, abc, true",
            "abc, abd, false",
            "abc, ab, false",
            "ab, abc, false",
            "test, test, true",
            "test, Test, false"
        })
        @DisplayName("Various literal matching cases")
        void testLiteralMatching(String pattern, String input, boolean expected) {
            MatchResult result = matchFull(pattern, input);
            assertEquals(expected, result.isMatched());
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
        
        @Test
        @DisplayName("Tab with other characters")
        void testTabWithChars() {
            MatchResult result = matchFull("a\\tb", "a\tb");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Whitespace (\\s) Tests")
    class WhitespaceTests {
        
        @ParameterizedTest
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("Match whitespace characters")
        void testMatchWhitespace(String ws) {
            MatchResult result = matchFull("\\s", ws);
            assertTrue(result.isMatched(), "Should match whitespace");
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
        
        @Test
        @DisplayName("Mixed character class")
        void testMixedClass() {
            MatchResult result = matchFull("[aeiou]+", "aeiou");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Character class with digits")
        void testClassWithDigits() {
            MatchResult result = matchFull("[abc123]", "2");
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
        
        @Test
        @DisplayName("Negated class with quantifier")
        void testNegatedClassQuantifier() {
            MatchResult result = matchFull("[^a]+", "xyz123");
            assertTrue(result.isMatched());
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
        @DisplayName("Character outside range")
        void testOutsideRange() {
            MatchResult result = matchFull("[a-z]", "A");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Digit range")
        void testDigitRange() {
            MatchResult result = matchFull("[0-9]+", "12345");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Multiple ranges")
        void testMultipleRanges() {
            MatchResult result = matchFull("[a-zA-Z]+", "HelloWorld");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Range with individual chars")
        void testRangeWithChars() {
            MatchResult result = matchFull("[a-z_]+", "hello_world");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Star (*) Quantifier Tests")
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
        @DisplayName("Star matches multiple occurrences")
        void testStarMultiple() {
            MatchResult result = matchFull("a*", "aaaaa");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Star with following literal")
        void testStarWithLiteral() {
            MatchResult result = matchFull("a*b", "aaab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Star with character class")
        void testStarWithClass() {
            MatchResult result = matchFull("[ab]*", "abababab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Multiple star quantifiers")
        void testMultipleStars() {
            MatchResult result = matchFull("a*b*c*", "aabbcc");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Plus (+) Quantifier Tests")
    class PlusTests {
        
        @Test
        @DisplayName("Plus requires at least one")
        void testPlusRequiresOne() {
            MatchResult result = matchFull("a+", "");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Plus matches one occurrence")
        void testPlusOne() {
            MatchResult result = matchFull("a+", "a");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Plus matches multiple occurrences")
        void testPlusMultiple() {
            MatchResult result = matchFull("a+", "aaaaa");
            assertTrue(result.isMatched());
            assertEquals("aaaaa", result.getMatchedText());
        }
        
        @Test
        @DisplayName("Plus with following literal")
        void testPlusWithLiteral() {
            MatchResult result = matchFull("a+b", "aaab");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Plus with character class")
        void testPlusWithClass() {
            MatchResult result = matchFull("[0-9]+", "12345");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Question (?) Quantifier Tests")
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
        @DisplayName("Optional prefix")
        void testOptionalPrefix() {
            MatchResult result1 = matchFull("https?://test", "http://test");
            MatchResult result2 = matchFull("https?://test", "https://test");
            assertTrue(result1.isMatched());
            assertTrue(result2.isMatched());
        }
        
        @Test
        @DisplayName("Optional with character class")
        void testOptionalClass() {
            MatchResult result = matchFull("[+-]?[0-9]+", "-123");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Alternation (|) Tests")
    class AlternationTests {
        
        @Test
        @DisplayName("Simple alternation first option")
        void testAlternationFirst() {
            MatchResult result = matchFull("cat|dog", "cat");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Simple alternation second option")
        void testAlternationSecond() {
            MatchResult result = matchFull("cat|dog", "dog");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Alternation no match")
        void testAlternationNoMatch() {
            MatchResult result = matchFull("cat|dog", "bird");
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Multiple alternatives")
        void testMultipleAlternatives() {
            assertTrue(matchFull("a|b|c|d", "c").isMatched());
        }
        
        @Test
        @DisplayName("Alternation with longer patterns")
        void testAlternationLongerPatterns() {
            assertTrue(matchFull("hello|world|foo", "world").isMatched());
        }
        
        @Test
        @DisplayName("Alternation in group")
        void testAlternationInGroup() {
            assertTrue(matchFull("(cat|dog)s", "cats").isMatched());
            assertTrue(matchFull("(cat|dog)s", "dogs").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Escaped Characters Tests")
    class EscapedCharTests {
        
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
        
        @Test
        @DisplayName("Multiple escaped characters")
        void testMultipleEscaped() {
            MatchResult result = matchFull("\\[a\\]\\+", "[a]+");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Any Character (.) Tests")
    class AnyCharTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"a", "1", "!", " ", "\t"})
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
        
        @Test
        @DisplayName("Dot in pattern")
        void testDotInPattern() {
            MatchResult result = matchFull("a.c", "abc");
            assertTrue(result.isMatched());
        }
        
        @Test
        @DisplayName("Multiple dots")
        void testMultipleDots() {
            MatchResult result = matchFull("...", "abc");
            assertTrue(result.isMatched());
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
        
        @Test
        @DisplayName("Group star")
        void testGroupStar() {
            MatchResult result = matchFull("(ab)*", "");
            assertTrue(result.isMatched());
            
            MatchResult result2 = matchFull("(ab)*", "ababab");
            assertTrue(result2.isMatched());
        }
        
        @Test
        @DisplayName("Complex nested groups")
        void testComplexNestedGroups() {
            MatchResult result = matchFull("((ab)|(cd))+", "abcdab");
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
        
        @Test
        @DisplayName("No matches")
        void testNoMatches() {
            MatchResult result = findAll("[0-9]+", "no digits here");
            assertFalse(result.isMatched());
            assertTrue(result.getAllMatches().isEmpty());
        }
        
        @Test
        @DisplayName("Single character matches")
        void testSingleCharMatches() {
            MatchResult result = findAll("a", "banana");
            assertTrue(result.isMatched());
            assertEquals(3, result.getAllMatches().size());
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
        
        @Test
        @DisplayName("IP address-like pattern")
        void testIpAddressLike() {
            String pattern = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+";
            assertTrue(matchFull(pattern, "192.168.1.1").isMatched());
            assertFalse(matchFull(pattern, "192.168.1").isMatched());
        }
    }
    
    @Nested
    @DisplayName("DFA-Specific Behavior Tests")
    class DFABehaviorTests {
        
        @Test
        @DisplayName("DFA state count tracking")
        void testStateCount() {
            DFAMatcher matcher = createMatcher("[a-z]+");
            assertTrue(matcher.getStateCount() > 0, "DFA should have states");
        }
        
        @Test
        @DisplayName("DFA construction time tracking")
        void testConstructionTime() {
            DFAMatcher matcher = createMatcher("[a-zA-Z0-9]+");
            assertTrue(matcher.getDfaConstructionTimeMs() >= 0, "Construction time should be tracked");
        }
        
        @Test
        @DisplayName("DFA handles pathological patterns efficiently")
        void testPathologicalPattern() {
            // This pattern would cause exponential backtracking
            // but DFA handles it in linear time
            String pattern = "a*a*a*a*a*b";
            String input = "aaaaaaaaaaaaaaaaaaaac"; // 20 a's followed by c
            
            MatchResult result = matchFull(pattern, input);
            assertFalse(result.isMatched()); // Should not match
            // The key is it completes without timeout
        }
        
        @Test
        @DisplayName("DFA reuse for multiple matches")
        void testDFAReuse() {
            DFAMatcher matcher = createMatcher("[a-z]+");
            
            // Same DFA can be used for multiple inputs
            MatchResult result1 = matcher.matchFull("hello");
            MatchResult result2 = matcher.matchFull("world");
            MatchResult result3 = matcher.matchFull("123");
            
            assertTrue(result1.isMatched());
            assertTrue(result2.isMatched());
            assertFalse(result3.isMatched());
        }
        
        @Test
        @DisplayName("Very long input handling")
        void testLongInput() {
            String longInput = "a".repeat(10000);
            MatchResult result = matchFull("a+", longInput);
            assertTrue(result.isMatched());
            assertEquals(10000, result.getMatchedText().length());
        }
        
        @Test
        @DisplayName("DFA matching is O(n)")
        void testLinearTimeMatching() {
            DFAMatcher matcher = createMatcher("a+b");
            
            // Multiple sizes to show linear behavior
            for (int size : new int[]{100, 1000, 5000}) {
                String input = "a".repeat(size) + "b";
                MatchResult result = matcher.matchFull(input);
                assertTrue(result.isMatched());
            }
        }
        
        @Test
        @DisplayName("Complex alternation with many branches")
        void testManyBranches() {
            String pattern = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z";
            DFAMatcher matcher = createMatcher(pattern);
            
            for (char c = 'a'; c <= 'z'; c++) {
                assertTrue(matcher.matchFull(String.valueOf(c)).isMatched());
            }
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
        @DisplayName("Find at start of string")
        void testFindAtStart() {
            MatchResult result = find("hello", "hello world");
            assertTrue(result.isMatched());
            assertEquals(0, result.getStartIndex());
        }
        
        @Test
        @DisplayName("Find at end of string")
        void testFindAtEnd() {
            MatchResult result = find("world", "hello world");
            assertTrue(result.isMatched());
            assertEquals(6, result.getStartIndex());
        }
        
        @Test
        @DisplayName("Zero-width match")
        void testZeroWidthMatch() {
            MatchResult result = matchFull("a*", "");
            assertTrue(result.isMatched());
            assertEquals(0, result.getMatchedText().length());
        }
        
        @Test
        @DisplayName("Special characters in input")
        void testSpecialCharsInInput() {
            MatchResult result = matchFull("hello\\?", "hello?");
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Subset Construction Tests")
    class SubsetConstructionTests {
        
        @Test
        @DisplayName("Simple pattern produces minimal DFA")
        void testSimplePatternDFA() {
            DFAMatcher matcher = createMatcher("abc");
            // Simple literal should produce small DFA
            assertTrue(matcher.getStateCount() <= 5);
        }
        
        @Test
        @DisplayName("Alternation produces combined DFA")
        void testAlternationDFA() {
            DFAMatcher matcher = createMatcher("a|b|c");
            // Should combine alternatives efficiently
            assertTrue(matcher.getStateCount() > 0);
        }
        
        @Test
        @DisplayName("Star produces looping DFA")
        void testStarDFA() {
            DFAMatcher matcher = createMatcher("a*");
            // Star pattern should produce small looping DFA
            assertTrue(matcher.getStateCount() <= 3);
        }
        
        @Test
        @DisplayName("Complex pattern DFA construction")
        void testComplexDFA() {
            DFAMatcher matcher = createMatcher("[a-zA-Z_][a-zA-Z0-9_]*");
            // Should successfully construct DFA for identifier pattern
            assertTrue(matcher.getStateCount() > 0);
            assertTrue(matcher.matchFull("myVar123").isMatched());
        }
    }
    
    @Nested
    @DisplayName("Performance Comparison Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("DFA faster than backtracking for long strings")
        void testPerformanceAdvantage() {
            String pattern = "a*b";
            String input = "a".repeat(1000) + "b";
            
            // DFA should complete quickly
            long start = System.currentTimeMillis();
            MatchResult result = matchFull(pattern, input);
            long elapsed = System.currentTimeMillis() - start;
            
            assertTrue(result.isMatched());
            assertTrue(elapsed < 1000, "DFA should complete in under 1 second");
        }
        
        @Test
        @DisplayName("DFA handles ReDoS patterns")
        void testReDoSResistance() {
            // Classic ReDoS pattern
            String pattern = "a*a*a*a*a*b";
            String input = "a".repeat(30) + "c"; // Will never match
            
            long start = System.currentTimeMillis();
            MatchResult result = matchFull(pattern, input);
            long elapsed = System.currentTimeMillis() - start;
            
            assertFalse(result.isMatched());
            assertTrue(elapsed < 1000, "DFA should handle ReDoS patterns efficiently");
        }
    }
}
