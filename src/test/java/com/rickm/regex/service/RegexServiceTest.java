package com.rickm.regex.service;


import com.rickm.regex.config.RegexEngineConfig;
import com.rickm.regex.dto.CompiledPattern;
import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.exception.RegexParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegexService.
 */
@DisplayName("RegexService Tests")
class RegexServiceTest {
    
    private RegexService regexService;
    private RegexEngineConfig config;
    
    @BeforeEach
    void setUp() {
        config = new RegexEngineConfig();
        config.setCacheEnabled(true);
        config.setCacheMaxSize(100);
        regexService = new RegexService(config);
    }
    
    @Nested
    @DisplayName("Compile Tests")
    class CompileTests {
        
        @Test
        @DisplayName("Compile valid pattern")
        void testCompileValid() {
            CompiledPattern compiled = regexService.compile("[a-z]+");
            
            assertNotNull(compiled);
            assertTrue(compiled.isValid());
            assertEquals("[a-z]+", compiled.getPattern());
            assertTrue(compiled.getCompileTimeMs() >= 0);
        }
        
        @Test
        @DisplayName("Compile invalid pattern throws exception")
        void testCompileInvalid() {
            assertThrows(RegexParseException.class,
                    () -> regexService.compile("[abc"));
        }
        
        @Test
        @DisplayName("Null pattern throws exception")
        void testCompileNull() {
            assertThrows(IllegalArgumentException.class, 
                    () -> regexService.compile(null));
        }
        
        @Test
        @DisplayName("Pattern exceeding max length throws exception")
        void testCompileTooLong() {
            config.setMaxPatternLength(10);
            regexService = new RegexService(config);
            
            assertThrows(IllegalArgumentException.class, 
                    () -> regexService.compile("a".repeat(100)));
        }
    }
    
    @Nested
    @DisplayName("Match Full Tests")
    class MatchFullTests {
        
        @Test
        @DisplayName("Full match success")
        void testMatchFullSuccess() {
            MatchResult result = regexService.matchFull("[a-z]+", "hello");
            
            assertTrue(result.isMatched());
            assertEquals("hello", result.getMatchedText());
            assertEquals(0, result.getStartIndex());
            assertEquals(5, result.getEndIndex());
        }
        
        @Test
        @DisplayName("Full match failure - partial")
        void testMatchFullPartial() {
            MatchResult result = regexService.matchFull("[a-z]+", "hello123");
            
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Full match with complex pattern")
        void testMatchFullComplex() {
            MatchResult result = regexService.matchFull(
                    "[a-zA-Z_][a-zA-Z0-9_]*", 
                    "myVariable123");
            
            assertTrue(result.isMatched());
        }
    }
    
    @Nested
    @DisplayName("Find Tests")
    class FindTests {
        
        @Test
        @DisplayName("Find first match")
        void testFindFirst() {
            MatchResult result = regexService.find("[0-9]+", "abc 123 def 456");
            
            assertTrue(result.isMatched());
            assertEquals("123", result.getMatchedText());
            assertEquals(4, result.getStartIndex());
        }
        
        @Test
        @DisplayName("Find no match")
        void testFindNoMatch() {
            MatchResult result = regexService.find("[0-9]+", "no digits here");
            
            assertFalse(result.isMatched());
        }
        
        @Test
        @DisplayName("Find at start")
        void testFindAtStart() {
            MatchResult result = regexService.find("[a-z]+", "hello world");
            
            assertTrue(result.isMatched());
            assertEquals(0, result.getStartIndex());
            assertEquals("hello", result.getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {
        
        @Test
        @DisplayName("Find all matches")
        void testFindAll() {
            MatchResult result = regexService.findAll("[a-z]+", "hello world foo");
            
            assertTrue(result.isMatched());
            assertNotNull(result.getAllMatches());
            assertEquals(3, result.getAllMatches().size());
        }
        
        @Test
        @DisplayName("Find all with no matches")
        void testFindAllNoMatches() {
            MatchResult result = regexService.findAll("[0-9]+", "no digits");
            
            assertFalse(result.isMatched());
            assertTrue(result.getAllMatches() == null || result.getAllMatches().isEmpty());
        }
        
        @Test
        @DisplayName("Find all single match")
        void testFindAllSingle() {
            MatchResult result = regexService.findAll("[0-9]+", "only 123 here");
            
            assertTrue(result.isMatched());
            assertEquals(1, result.getAllMatches().size());
            assertEquals("123", result.getAllMatches().get(0).getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("Replace Tests")
    class ReplaceTests {
        
        @Test
        @DisplayName("Replace all occurrences")
        void testReplaceAll() {
            String result = regexService.replaceAll("[0-9]+", "Order 123 and 456", "XXX");
            
            assertEquals("Order XXX and XXX", result);
        }
        
        @Test
        @DisplayName("Replace with no matches")
        void testReplaceNoMatches() {
            String result = regexService.replaceAll("[0-9]+", "no digits", "XXX");
            
            assertEquals("no digits", result);
        }
        
        @Test
        @DisplayName("Replace with empty string")
        void testReplaceWithEmpty() {
            String result = regexService.replaceAll("[0-9]+", "a1b2c3", "");
            
            assertEquals("abc", result);
        }
        
        @Test
        @DisplayName("Replace all characters")
        void testReplaceAllChars() {
            String result = regexService.replaceAll(".", "abc", "*");
            
            assertEquals("***", result);
        }
    }
    
    @Nested
    @DisplayName("Split Tests")
    class SplitTests {
        
        @Test
        @DisplayName("Split by whitespace")
        void testSplitWhitespace() {
            String[] parts = regexService.split("\\s+", "hello world foo");
            
            assertEquals(3, parts.length);
            assertEquals("hello", parts[0]);
            assertEquals("world", parts[1]);
            assertEquals("foo", parts[2]);
        }
        
        @Test
        @DisplayName("Split by comma")
        void testSplitComma() {
            String[] parts = regexService.split(",", "a,b,c");
            
            assertEquals(3, parts.length);
            assertEquals("a", parts[0]);
            assertEquals("b", parts[1]);
            assertEquals("c", parts[2]);
        }
        
        @Test
        @DisplayName("Split with no matches")
        void testSplitNoMatches() {
            String[] parts = regexService.split(",", "no commas");
            
            assertEquals(1, parts.length);
            assertEquals("no commas", parts[0]);
        }
        
        @Test
        @DisplayName("Split at start and end")
        void testSplitEdges() {
            String[] parts = regexService.split(",", ",a,b,");
            
            assertEquals(4, parts.length);
            assertEquals("", parts[0]);
            assertEquals("a", parts[1]);
            assertEquals("b", parts[2]);
            assertEquals("", parts[3]);
        }
    }
    
    @Nested
    @DisplayName("Cache Tests")
    class CacheTests {
        
        @Test
        @DisplayName("Cache hit returns same result")
        void testCacheHit() {
            CompiledPattern first = regexService.compile("[a-z]+");
            CompiledPattern second = regexService.compile("[a-z]+");
            
            assertEquals(first.getPattern(), second.getPattern());
        }
        
        @Test
        @DisplayName("Cache size tracking")
        void testCacheSize() {
            assertEquals(0, regexService.getCacheSize());
            
            regexService.compile("[a-z]+");
            assertEquals(1, regexService.getCacheSize());
            
            regexService.compile("[0-9]+");
            assertEquals(2, regexService.getCacheSize());
            
            regexService.compile("[a-z]+"); // duplicate
            assertEquals(2, regexService.getCacheSize());
        }
        
        @Test
        @DisplayName("Clear cache")
        void testClearCache() {
            regexService.compile("[a-z]+");
            regexService.compile("[0-9]+");
            assertEquals(2, regexService.getCacheSize());
            
            regexService.clearCache();
            assertEquals(0, regexService.getCacheSize());
        }
        
        @Test
        @DisplayName("Disabled cache")
        void testDisabledCache() {
            config.setCacheEnabled(false);
            regexService = new RegexService(config);
            
            regexService.compile("[a-z]+");
            assertEquals(0, regexService.getCacheSize());
        }
    }
    
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Null input throws exception")
        void testNullInput() {
            assertThrows(IllegalArgumentException.class, 
                    () -> regexService.matchFull("[a-z]+", null));
        }
        
        @Test
        @DisplayName("Input exceeding max length throws exception")
        void testInputTooLong() {
            config.setMaxInputLength(10);
            regexService = new RegexService(config);
            
            assertThrows(IllegalArgumentException.class, 
                    () -> regexService.matchFull("[a-z]+", "a".repeat(100)));
        }
    }
    
    @Nested
    @DisplayName("Performance Metrics Tests")
    class MetricsTests {
        
        @Test
        @DisplayName("Compile time is tracked")
        void testCompileTime() {
            CompiledPattern compiled = regexService.compile("[a-z]+");
            
            assertTrue(compiled.getCompileTimeMs() >= 0);
        }
        
        @Test
        @DisplayName("Match time is tracked")
        void testMatchTime() {
            MatchResult result = regexService.matchFull("[a-z]+", "hello");
            
            assertTrue(result.getMatchTimeMs() >= 0);
        }
        
        @Test
        @DisplayName("Backtrack count is tracked")
        void testBacktrackCount() {
            MatchResult result = regexService.matchFull("a*a", "aaa");
            
            assertTrue(result.getBacktrackCount() >= 0);
        }
    }
}
