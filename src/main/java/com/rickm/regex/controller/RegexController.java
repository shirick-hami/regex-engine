package com.rickm.regex.controller;

import com.rickm.regex.dto.CompiledPattern;
import com.rickm.regex.dto.RegexRequest;
import com.rickm.regex.dto.RegexResponse;
import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.service.RegexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for regex operations.
 *
 * Supports three matching engines:
 * - BACKTRACKING: Traditional recursive backtracking
 * - NFA: Thompson's NFA simulation
 * - DFA: Deterministic finite automaton
 */
@RestController
@RequestMapping("/regex")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Regex Operations", description = "Regular expression matching and manipulation")
public class RegexController {

    private final RegexService regexService;

    /**
     * Compiles and validates a regex pattern.
     */
    @PostMapping("/compile")
    @Operation(summary = "Compile a regex pattern",
            description = "Parses and compiles a regex pattern, returning the AST structure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pattern compiled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pattern")
    })
    public ResponseEntity<RegexResponse.CompileResponse> compile(
            @Valid @RequestBody RegexRequest.CompileRequest request) {

        log.info("Compiling pattern: {}", request.getPattern());

        CompiledPattern compiled = regexService.compile(request.getPattern());

        return ResponseEntity.ok(RegexResponse.CompileResponse.builder()
                .valid(true)
                .pattern(compiled.getPattern())
                .compileTimeMs(compiled.getCompileTimeMs())
                .astDescription(compiled.getAstDescription())
                .build());
    }

    /**
     * Matches the entire input string against the pattern.
     */
    @PostMapping("/match")
    @Operation(summary = "Full string match",
            description = "Checks if the entire input string matches the pattern")
    public ResponseEntity<RegexResponse.MatchResponse> matchFull(
            @Valid @RequestBody RegexRequest.MatchRequest request,
            @Parameter(description = "Matching engine: BACKTRACKING, NFA, or DFA")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("Full match - pattern: {}, engine: {}", request.getPattern(), engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);
        MatchResult result = regexService.matchFull(request.getPattern(), request.getInput(), matchEngine);

        return ResponseEntity.ok(toMatchResponse(result, engine));
    }

    /**
     * Finds the first match of the pattern in the input string.
     */
    @PostMapping("/find")
    @Operation(summary = "Find first match",
            description = "Searches for the first occurrence of the pattern")
    public ResponseEntity<RegexResponse.MatchResponse> find(
            @Valid @RequestBody RegexRequest.MatchRequest request,
            @Parameter(description = "Matching engine: BACKTRACKING, NFA, or DFA")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("Find - pattern: {}, engine: {}", request.getPattern(), engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);
        MatchResult result = regexService.find(request.getPattern(), request.getInput(), matchEngine);

        return ResponseEntity.ok(toMatchResponse(result, engine));
    }

    /**
     * Finds all non-overlapping matches.
     */
    @PostMapping("/find-all")
    @Operation(summary = "Find all matches",
            description = "Finds all non-overlapping occurrences of the pattern")
    public ResponseEntity<RegexResponse.MatchResponse> findAll(
            @Valid @RequestBody RegexRequest.MatchRequest request,
            @Parameter(description = "Matching engine: BACKTRACKING, NFA, or DFA")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("FindAll - pattern: {}, engine: {}", request.getPattern(), engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);
        MatchResult result = regexService.findAll(request.getPattern(), request.getInput(), matchEngine);

        return ResponseEntity.ok(toMatchResponse(result, engine));
    }

    /**
     * Replaces all matches with the replacement string.
     */
    @PostMapping("/replace")
    @Operation(summary = "Replace all matches",
            description = "Replaces all occurrences of the pattern")
    public ResponseEntity<RegexResponse.ReplaceResponse> replaceAll(
            @Valid @RequestBody RegexRequest.ReplaceRequest request,
            @Parameter(description = "Matching engine")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("Replace - pattern: {}, engine: {}", request.getPattern(), engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);

        MatchResult matches = regexService.findAll(request.getPattern(), request.getInput(), matchEngine);
        int matchCount = matches.getAllMatches() != null ? matches.getAllMatches().size() : 0;

        String result = regexService.replaceAll(
                request.getPattern(),
                request.getInput(),
                request.getReplacement(),
                matchEngine);

        return ResponseEntity.ok(RegexResponse.ReplaceResponse.builder()
                .input(request.getInput())
                .pattern(request.getPattern())
                .replacement(request.getReplacement())
                .result(result)
                .replacementCount(matchCount)
                .build());
    }

    /**
     * Splits the input string by pattern matches.
     */
    @PostMapping("/split")
    @Operation(summary = "Split by pattern",
            description = "Splits the input string using the pattern as delimiter")
    public ResponseEntity<RegexResponse.SplitResponse> split(
            @Valid @RequestBody RegexRequest.SplitRequest request,
            @Parameter(description = "Matching engine")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("Split - pattern: {}, engine: {}", request.getPattern(), engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);
        String[] parts = regexService.split(request.getPattern(), request.getInput(), matchEngine);

        return ResponseEntity.ok(RegexResponse.SplitResponse.builder()
                .input(request.getInput())
                .pattern(request.getPattern())
                .parts(parts)
                .partCount(parts.length)
                .build());
    }

    /**
     * Quick match test using GET request.
     */
    @GetMapping("/test")
    @Operation(summary = "Quick match test",
            description = "Simple GET endpoint for quick pattern testing")
    public ResponseEntity<RegexResponse.MatchResponse> test(
            @Parameter(description = "The regex pattern") @RequestParam String pattern,
            @Parameter(description = "The input string") @RequestParam String input,
            @Parameter(description = "Match type: full, find, or findAll")
            @RequestParam(defaultValue = "find") String type,
            @Parameter(description = "Matching engine")
            @RequestParam(defaultValue = "BACKTRACKING") String engine) {

        log.info("Test - pattern: {}, type: {}, engine: {}", pattern, type, engine);

        RegexService.MatchEngine matchEngine = parseEngine(engine);
        MatchResult result;

        switch (type.toLowerCase()) {
            case "full":
                result = regexService.matchFull(pattern, input, matchEngine);
                break;
            case "findall":
                result = regexService.findAll(pattern, input, matchEngine);
                break;
            default:
                result = regexService.find(pattern, input, matchEngine);
        }

        return ResponseEntity.ok(toMatchResponse(result, engine));
    }

    /**
     * Benchmarks all engines on a pattern.
     */
    @PostMapping("/benchmark")
    @Operation(summary = "Benchmark engines",
            description = "Compares performance of all three engines")
    public ResponseEntity<Map<String, Object>> benchmark(
            @Valid @RequestBody RegexRequest.MatchRequest request) {

        log.info("Benchmark - pattern: {}", request.getPattern());

        Map<String, Object> results = regexService.benchmark(
                request.getPattern(),
                request.getInput());

        return ResponseEntity.ok(results);
    }

    /**
     * Lists available engines.
     */
    @GetMapping("/engines")
    @Operation(summary = "List engines", description = "Returns available matching engines")
    public ResponseEntity<Map<String, Object>> listEngines() {
        return ResponseEntity.ok(Map.of(
                "engines", List.of(
                        Map.of("name", "BACKTRACKING",
                                "description", "Traditional recursive backtracking matcher"),
                        Map.of("name", "NFA",
                                "description", "Thompson's NFA simulation - linear time"),
                        Map.of("name", "DFA",
                                "description", "Deterministic finite automaton - fastest matching")
                ),
                "default", "BACKTRACKING"
        ));
    }

    // ===== Helper Methods =====

    private RegexService.MatchEngine parseEngine(String engine) {
        try {
            return RegexService.MatchEngine.valueOf(engine.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RegexService.MatchEngine.BACKTRACKING;
        }
    }

    private RegexResponse.MatchResponse toMatchResponse(MatchResult result, String engine) {
        List<RegexResponse.MatchInfo> allMatches = null;

        if (result.getAllMatches() != null) {
            allMatches = result.getAllMatches().stream()
                    .map(m -> RegexResponse.MatchInfo.builder()
                            .startIndex(m.getStartIndex())
                            .endIndex(m.getEndIndex())
                            .matchedText(m.getMatchedText())
                            .build())
                    .collect(Collectors.toList());
        }

        return RegexResponse.MatchResponse.builder()
                .matched(result.isMatched())
                .pattern(result.getPattern())
                .input(result.getInput())
                .startIndex(result.getStartIndex())
                .endIndex(result.getEndIndex())
                .matchedText(result.getMatchedText())
                .backtrackCount(result.getBacktrackCount())
                .matchTimeMs(result.getMatchTimeMs())
                .allMatches(allMatches)
                .engine(engine)
                .build();
    }
}
