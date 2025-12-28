package com.rickm.regex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickm.regex.configuration.TestingConfiguration;
import com.rickm.regex.dto.RegexRequest;
import com.rickm.regex.dto.RegexResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Regex Engine REST API.
 */
@SpringBootTest
@Import(TestingConfiguration.class)
@DisplayName("Regex API Integration Tests")
class RegexControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Nested
    @DisplayName("POST /api/v1/regex/compile")
    class CompileEndpointTests {
        
        @Test
        @DisplayName("Compile valid pattern")
        void testCompileValid() throws Exception {
            RegexRequest.CompileRequest request = RegexRequest.CompileRequest.builder()
                    .pattern("[a-z]+")
                    .build();
            
            MvcResult result = mockMvc.perform(post("/api/v1/regex/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.pattern").value("[a-z]+"))
                    .andReturn();
            
            RegexResponse.CompileResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    RegexResponse.CompileResponse.class);
            
            assertNotNull(response.getAstDescription());
        }
        
        @Test
        @DisplayName("Compile invalid pattern returns error")
        void testCompileInvalid() throws Exception {
            RegexRequest.CompileRequest request = RegexRequest.CompileRequest.builder()
                    .pattern("[abc")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PARSE_ERROR"));
        }
        
        @Test
        @DisplayName("Compile missing pattern returns validation error")
        void testCompileMissingPattern() throws Exception {
            mockMvc.perform(post("/api/v1/regex/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }
    
    @Nested
    @DisplayName("POST /api/v1/regex/match")
    class MatchEndpointTests {
        
        @Test
        @DisplayName("Full match success")
        void testMatchSuccess() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[a-z]+")
                    .input("hello")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.matchedText").value("hello"))
                    .andExpect(jsonPath("$.startIndex").value(0))
                    .andExpect(jsonPath("$.endIndex").value(5));
        }
        
        @Test
        @DisplayName("Full match failure")
        void testMatchFailure() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[a-z]+")
                    .input("12345")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(false));
        }
        
        @Test
        @DisplayName("Match with backtrack count")
        void testMatchWithMetrics() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("a*b")
                    .input("aaab")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.backtrackCount").exists())
                    .andExpect(jsonPath("$.matchTimeMs").exists());
        }
    }
    
    @Nested
    @DisplayName("POST /api/v1/regex/find")
    class FindEndpointTests {
        
        @Test
        @DisplayName("Find first match")
        void testFindFirst() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[0-9]+")
                    .input("abc 123 def 456")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/find")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.matchedText").value("123"))
                    .andExpect(jsonPath("$.startIndex").value(4));
        }
        
        @Test
        @DisplayName("Find no match")
        void testFindNoMatch() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[0-9]+")
                    .input("no digits here")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/find")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(false));
        }
    }
    
    @Nested
    @DisplayName("POST /api/v1/regex/find-all")
    class FindAllEndpointTests {
        
        @Test
        @DisplayName("Find all matches")
        void testFindAll() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[a-z]+")
                    .input("hello world foo")
                    .build();
            
            MvcResult result = mockMvc.perform(post("/api/v1/regex/find-all")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.allMatches").isArray())
                    .andExpect(jsonPath("$.allMatches.length()").value(3))
                    .andReturn();
            
            RegexResponse.MatchResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    RegexResponse.MatchResponse.class);
            
            assertEquals("hello", response.getAllMatches().get(0).getMatchedText());
            assertEquals("world", response.getAllMatches().get(1).getMatchedText());
            assertEquals("foo", response.getAllMatches().get(2).getMatchedText());
        }
    }
    
    @Nested
    @DisplayName("POST /api/v1/regex/replace")
    class ReplaceEndpointTests {
        
        @Test
        @DisplayName("Replace all matches")
        void testReplaceAll() throws Exception {
            RegexRequest.ReplaceRequest request = RegexRequest.ReplaceRequest.builder()
                    .pattern("[0-9]+")
                    .input("Order 123 and 456")
                    .replacement("XXX")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/replace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("Order XXX and XXX"))
                    .andExpect(jsonPath("$.replacementCount").value(2));
        }
        
        @Test
        @DisplayName("Replace with no matches")
        void testReplaceNoMatches() throws Exception {
            RegexRequest.ReplaceRequest request = RegexRequest.ReplaceRequest.builder()
                    .pattern("[0-9]+")
                    .input("no digits")
                    .replacement("XXX")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/replace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("no digits"))
                    .andExpect(jsonPath("$.replacementCount").value(0));
        }
    }
    
    @Nested
    @DisplayName("POST /api/v1/regex/split")
    class SplitEndpointTests {
        
        @Test
        @DisplayName("Split by pattern")
        void testSplit() throws Exception {
            RegexRequest.SplitRequest request = RegexRequest.SplitRequest.builder()
                    .pattern(",")
                    .input("a,b,c")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/split")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parts").isArray())
                    .andExpect(jsonPath("$.partCount").value(3))
                    .andExpect(jsonPath("$.parts[0]").value("a"))
                    .andExpect(jsonPath("$.parts[1]").value("b"))
                    .andExpect(jsonPath("$.parts[2]").value("c"));
        }
    }
    
    @Nested
    @DisplayName("GET /api/v1/regex/test")
    class TestEndpointTests {
        
        @Test
        @DisplayName("Quick test - find")
        void testQuickFind() throws Exception {
            mockMvc.perform(get("/api/v1/regex/test")
                    .param("pattern", "[a-z]+")
                    .param("input", "hello world")
                    .param("type", "find"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.matchedText").value("hello"));
        }
        
        @Test
        @DisplayName("Quick test - full")
        void testQuickFull() throws Exception {
            mockMvc.perform(get("/api/v1/regex/test")
                    .param("pattern", "[a-z]+")
                    .param("input", "hello")
                    .param("type", "full"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Quick test - findAll")
        void testQuickFindAll() throws Exception {
            mockMvc.perform(get("/api/v1/regex/test")
                    .param("pattern", "[a-z]+")
                    .param("input", "hello world")
                    .param("type", "findAll"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true))
                    .andExpect(jsonPath("$.allMatches.length()").value(2));
        }
    }
    
    @Nested
    @DisplayName("Cache Endpoints")
    class CacheEndpointTests {
        
        @Test
        @DisplayName("Get cache stats")
        void testGetCacheStats() throws Exception {
            mockMvc.perform(get("/api/v1/regex/cache/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cacheSize").exists());
        }
        
        @Test
        @DisplayName("Clear cache")
        void testClearCache() throws Exception {
            // First compile something
            RegexRequest.CompileRequest request = RegexRequest.CompileRequest.builder()
                    .pattern("[a-z]+")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            
            // Clear cache
            mockMvc.perform(delete("/api/v1/regex/cache"))
                    .andExpect(status().isNoContent());
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Invalid JSON returns error")
        void testInvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not json"))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("Parse error includes position")
        void testParseErrorPosition() throws Exception {
            RegexRequest.CompileRequest request = RegexRequest.CompileRequest.builder()
                    .pattern("(abc")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PARSE_ERROR"))
                    .andExpect(jsonPath("$.position").exists());
        }
    }
    
    @Nested
    @DisplayName("Comprehensive Pattern Tests via API")
    class PatternTests {
        
        @Test
        @DisplayName("Tab pattern")
        void testTabPattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("\\t")
                    .input("\t")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Whitespace pattern")
        void testWhitespacePattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("\\s+")
                    .input("   ")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Character class pattern")
        void testCharClassPattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[abc]+")
                    .input("abcabc")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Negated character class pattern")
        void testNegatedCharClassPattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[^0-9]+")
                    .input("hello")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Alternation pattern")
        void testAlternationPattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("cat|dog")
                    .input("dog")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
        
        @Test
        @DisplayName("Complex identifier pattern")
        void testIdentifierPattern() throws Exception {
            RegexRequest.MatchRequest request = RegexRequest.MatchRequest.builder()
                    .pattern("[a-zA-Z_][a-zA-Z0-9_]*")
                    .input("myVariable_123")
                    .build();
            
            mockMvc.perform(post("/api/v1/regex/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matched").value(true));
        }
    }
}
