package com.rickm.regex.engine.matcher;

import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.exception.BacktrackLimitExceededException;
import com.rickm.regex.engine.exception.RegexTimeoutException;
import com.rickm.regex.engine.parser.AstNode;
import com.rickm.regex.engine.parser.NodeType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Backtracking regex matcher implementation using continuation-passing style.
 *
 * This matcher uses a recursive backtracking algorithm with continuations
 * to match regular expressions against input strings. The continuation
 * represents "what to match next" and enables proper backtracking.
 *
 * <h2>Algorithm Overview</h2>
 * The key insight is that each match operation takes a continuation (Cont)
 * that represents the rest of the pattern to match. When a match succeeds,
 * we call the continuation. When it fails, we backtrack by trying alternatives.
 *
 * <h2>Key Points</h2>
 * <ul>
 *   <li>Position is saved before matching and restored after (for backtracking)</li>
 *   <li>Continuations chain together to match the full pattern</li>
 *   <li>Quantifiers use iterative greedy matching with backtracking</li>
 * </ul>
 */
@Slf4j
public class BacktrackingMatcher implements RegexMatcher {

    /** The AST to match against */
    private final AstNode ast;

    /** Maximum number of backtracking steps allowed */
    private final long maxBacktracks;

    /** Maximum execution time in milliseconds */
    private final long timeoutMs;

    /** Counter for backtracking steps */
    private long backtrackCount;

    /** Start time of matching operation */
    private long startTime;

    /** Whitespace characters for \s matching */
    private static final Set<Character> WHITESPACE_CHARS = Set.of(
            ' ', '\t', '\n', '\r', '\f', '\u000B'
    );

    /**
     * Functional interface for continuations.
     * A continuation represents "what to do next" after a successful match.
     */
    @FunctionalInterface
    public interface Cont {
        boolean run();
    }

    /**
     * Input wrapper that tracks position and allows save/restore for backtracking.
     */
    public static class Input {
        private final String text;
        private int pos;

        private Input(String text) {
            this.text = text;
            this.pos = 0;
        }

        public static Input of(String text) {
            return new Input(text);
        }

        public int currentPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public boolean atEnd() {
            return pos >= text.length();
        }

        public boolean atBeginning() {
            return pos == 0;
        }

        public char current() {
            return text.charAt(pos);
        }

        public void advance(int n) {
            pos += n;
        }

        public int markPosition() {
            return pos;
        }

        public void restorePosition(int mark) {
            pos = mark;
        }

        public int length() {
            return text.length();
        }
    }

    /**
     * Creates a new backtracking matcher.
     *
     * @param ast the AST to match against
     * @param maxBacktracks maximum backtracking steps
     * @param timeoutMs maximum execution time
     */
    public BacktrackingMatcher(AstNode ast, long maxBacktracks, long timeoutMs) {
        this.ast = ast;
        this.maxBacktracks = maxBacktracks;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Attempts to match the pattern against the entire input string.
     *
     * @param inputStr the input string
     * @return match result
     */
    @Override
    public MatchResult matchFull(String inputStr) {
        this.backtrackCount = 0;
        this.startTime = System.currentTimeMillis();

        Input input = Input.of(inputStr);

        log.debug("Starting full match against input of length {}", inputStr.length());

        // For full match, we need to match the entire string
        final int[] endIndex = {0};

        boolean hasMatch = match(input, ast, () -> {
            endIndex[0] = input.currentPos();
            // Full match requires consuming entire input
            return input.atEnd();
        });

        long elapsedMs = System.currentTimeMillis() - startTime;

        if (hasMatch) {
            MatchResult result = MatchResult.match(inputStr, "", 0, endIndex[0]);
            result.setBacktrackCount(backtrackCount);
            result.setMatchTimeMs(elapsedMs);
            log.debug("Full match succeeded in {}ms with {} backtracks", elapsedMs, backtrackCount);
            return result;
        }

        MatchResult result = MatchResult.noMatch(inputStr, "");
        result.setBacktrackCount(backtrackCount);
        result.setMatchTimeMs(elapsedMs);
        log.debug("Full match failed in {}ms with {} backtracks", elapsedMs, backtrackCount);
        return result;
    }

    /**
     * Searches for the first match of the pattern in the input string.
     *
     * @param inputStr the input string
     * @return match result
     */
    @Override
    public MatchResult find(String inputStr) {
        this.backtrackCount = 0;
        this.startTime = System.currentTimeMillis();

        Input input = Input.of(inputStr);

        log.debug("Starting find in input of length {}", inputStr.length());

        while (true) {
            int startIndex = input.currentPos();
            final int[] endIndex = {0};

            boolean hasMatch = match(input, ast, () -> {
                endIndex[0] = input.currentPos();
                return true;
            });

            if (hasMatch) {
                long elapsedMs = System.currentTimeMillis() - startTime;
                MatchResult result = MatchResult.match(inputStr, "", startIndex, endIndex[0]);
                result.setBacktrackCount(backtrackCount);
                result.setMatchTimeMs(elapsedMs);
                log.debug("Found match at [{}, {}) in {}ms", startIndex, endIndex[0], elapsedMs);
                return result;
            }

            // We are at the end of the input - no match
            if (input.atEnd()) {
                long elapsedMs = System.currentTimeMillis() - startTime;
                MatchResult result = MatchResult.noMatch(inputStr, "");
                result.setBacktrackCount(backtrackCount);
                result.setMatchTimeMs(elapsedMs);
                return result;
            }

            // Try to match from next index
            input.advance(1);
        }
    }

    /**
     * Finds all non-overlapping matches in the input string.
     *
     * @param inputStr the input string
     * @return match result with all matches
     */
    @Override
    public MatchResult findAll(String inputStr) {
        this.backtrackCount = 0;
        this.startTime = System.currentTimeMillis();

        Input input = Input.of(inputStr);
        List<MatchResult.MatchInfo> allMatches = new ArrayList<>();

        while (!input.atEnd()) {
            int startIndex = input.currentPos();
            final int[] endIndex = {0};

            boolean hasMatch = match(input, ast, () -> {
                endIndex[0] = input.currentPos();
                return true;
            });

            if (hasMatch) {
                allMatches.add(MatchResult.MatchInfo.builder()
                        .startIndex(startIndex)
                        .endIndex(endIndex[0])
                        .matchedText(inputStr.substring(startIndex, endIndex[0]))
                        .build());

                // Move past this match (ensure progress for zero-width matches)
                if (endIndex[0] > startIndex) {
                    input.setPos(endIndex[0]);
                } else {
                    input.advance(1);
                }
            } else {
                input.advance(1);
            }
        }

        long elapsedMs = System.currentTimeMillis() - startTime;

        MatchResult result = MatchResult.builder()
                .matched(!allMatches.isEmpty())
                .input(inputStr)
                .pattern("")
                .allMatches(allMatches)
                .backtrackCount(backtrackCount)
                .matchTimeMs(elapsedMs)
                .build();

        if (!allMatches.isEmpty()) {
            result.setStartIndex(allMatches.get(0).getStartIndex());
            result.setEndIndex(allMatches.get(0).getEndIndex());
            result.setMatchedText(allMatches.get(0).getMatchedText());
        }

        return result;
    }

    /**
     * Core matching function using continuation-passing style.
     *
     * @param input the input being matched
     * @param ast the AST node to match
     * @param cont the continuation (what to do after this node matches)
     * @return true if match succeeded
     */
    private boolean match(Input input, AstNode ast, Cont cont) {
        checkLimits();

        if (ast == null) {
            return cont.run();
        }

        NodeType type = ast.getType();

        switch (type) {
            case LITERAL:
                return matchLiteral(input, ast, cont);

            case ESCAPED:
                return matchEscaped(input, ast, cont);

            case TAB:
                return matchTab(input, cont);

            case WHITESPACE:
                return matchWhitespace(input, cont);

            case ANY_CHAR:
                return matchAnyChar(input, cont);

            case CHAR_CLASS:
                return matchCharClass(input, ast, cont);

            case NEGATED_CHAR_CLASS:
                return matchNegatedCharClass(input, ast, cont);

            case CONCAT:
                return matchConcat(input, ast.getChildren(), 0, cont);

            case ALTERNATION:
                return matchAlternation(input, ast.getChildren(), 0, cont);

            case STAR:
                return matchRepeatIterative(input, ast.getFirstChild(), 0, Long.MAX_VALUE, cont);

            case PLUS:
                return matchRepeatIterative(input, ast.getFirstChild(), 1, Long.MAX_VALUE, cont);

            case QUESTION:
                return matchRepeatIterative(input, ast.getFirstChild(), 0, 1, cont);

            case GROUP:
                return match(input, ast.getFirstChild(), cont);

            default:
                log.warn("Unknown node type: {}", type);
                return false;
        }
    }

    /**
     * Matches a literal character with proper position save/restore.
     */
    private boolean matchLiteral(Input input, AstNode ast, Cont cont) {
        // Special case: null character matches empty string (for empty patterns)
        if (ast.getCharacter() == '\0') {
            return cont.run();
        }

        if (input.atEnd()) return false;

        if (input.current() == ast.getCharacter()) {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches an escaped character.
     */
    private boolean matchEscaped(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;

        if (input.current() == ast.getCharacter()) {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches a tab character.
     */
    private boolean matchTab(Input input, Cont cont) {
        if (input.atEnd()) return false;

        if (input.current() == '\t') {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches any whitespace character.
     */
    private boolean matchWhitespace(Input input, Cont cont) {
        if (input.atEnd()) return false;

        if (WHITESPACE_CHARS.contains(input.current())) {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches any printable character (except newline).
     */
    private boolean matchAnyChar(Input input, Cont cont) {
        if (input.atEnd()) return false;

        char c = input.current();
        // Match any character except line terminators
        if (c != '\n' && c != '\r') {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches a character class [abc].
     */
    private boolean matchCharClass(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;

        if (ast.getCharSet().contains(input.current())) {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches a negated character class [^abc].
     */
    private boolean matchNegatedCharClass(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;

        char c = input.current();
        // Must not be in the set and must not be a line terminator
        if (!ast.getCharSet().contains(c) && c != '\n' && c != '\r') {
            int savedPos = input.markPosition();
            input.advance(1);
            try {
                return cont.run();
            } finally {
                input.restorePosition(savedPos);
            }
        }
        return false;
    }

    /**
     * Matches a concatenation of expressions recursively.
     */
    private boolean matchConcat(Input input, List<AstNode> exprs, int currExpr, Cont cont) {
        if (currExpr == exprs.size()) {
            return cont.run();
        }

        // Match current expression, with continuation to match rest
        return match(input, exprs.get(currExpr), () ->
                matchConcat(input, exprs, currExpr + 1, cont)
        );
    }

    /**
     * Matches an alternation by trying each branch.
     */
    private boolean matchAlternation(Input input, List<AstNode> exprs, int currExpr, Cont cont) {
        if (currExpr == exprs.size()) {
            // Tried all alternatives, no match
            return false;
        }

        boolean matched = match(input, exprs.get(currExpr), cont);
        if (matched) return true;

        // Backtrack and try next alternative
        backtrackCount++;
        return matchAlternation(input, exprs, currExpr + 1, cont);
    }

    /**
     * Matches a repetition (*, +, ?) using ITERATIVE greedy matching with backtracking.
     *
     * This avoids stack overflow by:
     * 1. First, greedily match as many as possible ITERATIVELY (storing positions)
     * 2. Then, try the continuation
     * 3. If continuation fails, backtrack by reducing matches one at a time
     *
     * @param input the input
     * @param repeatExpr the expression to repeat
     * @param repeatMin minimum repetitions required
     * @param repeatMax maximum repetitions allowed
     * @param cont the continuation
     * @return true if match succeeded
     */
    private boolean matchRepeatIterative(Input input, AstNode repeatExpr,
                                         long repeatMin, long repeatMax, Cont cont) {

        int startPos = input.markPosition();

        // Collect all possible match positions greedily (iteratively, not recursively)
        List<Integer> matchPositions = new ArrayList<>();
        matchPositions.add(startPos); // Position with 0 matches

        long matchCount = 0;
        while (matchCount < repeatMax) {
            int posBeforeMatch = input.currentPos();

            // Try to match one more occurrence
            // We use a simple continuation that just returns true to test if match succeeds
            final boolean[] matched = {false};
            final int[] endPos = {0};

            boolean success = matchSingle(input, repeatExpr, () -> {
                matched[0] = true;
                endPos[0] = input.currentPos();
                return true;
            });

            if (!success || !matched[0]) {
                // Cannot match more
                break;
            }

            // Check for zero-width match to prevent infinite loop
            if (endPos[0] == posBeforeMatch) {
                // Zero-width match - don't add more positions, just keep current
                break;
            }

            // Move input to end position and record it
            input.setPos(endPos[0]);
            matchCount++;
            matchPositions.add(endPos[0]);

            checkLimits();
        }

        // Now try continuation from each position, starting from most greedy (most matches)
        // and backtracking to fewer matches
        for (int i = matchPositions.size() - 1; i >= 0; i--) {
            long currentMatchCount = i; // Number of repetitions at this position

            if (currentMatchCount < repeatMin) {
                // Not enough matches, can't use this position
                continue;
            }

            if (i < matchPositions.size() - 1) {
                backtrackCount++;
            }

            int pos = matchPositions.get(i);
            input.setPos(pos);

            if (cont.run()) {
                // Continuation succeeded - restore position for proper backtracking semantics
                input.setPos(startPos);
                return true;
            }
        }

        // No position worked
        input.setPos(startPos);
        return false;
    }

    /**
     * Matches a single occurrence of the expression.
     * This is a helper for matchRepeatIterative to test if one match is possible.
     */
    private boolean matchSingle(Input input, AstNode ast, Cont cont) {
        checkLimits();

        if (ast == null) {
            return cont.run();
        }

        NodeType type = ast.getType();

        switch (type) {
            case LITERAL:
                return matchLiteralSingle(input, ast, cont);

            case ESCAPED:
                return matchEscapedSingle(input, ast, cont);

            case TAB:
                return matchTabSingle(input, cont);

            case WHITESPACE:
                return matchWhitespaceSingle(input, cont);

            case ANY_CHAR:
                return matchAnyCharSingle(input, cont);

            case CHAR_CLASS:
                return matchCharClassSingle(input, ast, cont);

            case NEGATED_CHAR_CLASS:
                return matchNegatedCharClassSingle(input, ast, cont);

            case CONCAT:
                return matchConcatSingle(input, ast.getChildren(), 0, cont);

            case ALTERNATION:
                return matchAlternationSingle(input, ast.getChildren(), 0, cont);

            case STAR:
                return matchRepeatIterative(input, ast.getFirstChild(), 0, Long.MAX_VALUE, cont);

            case PLUS:
                return matchRepeatIterative(input, ast.getFirstChild(), 1, Long.MAX_VALUE, cont);

            case QUESTION:
                return matchRepeatIterative(input, ast.getFirstChild(), 0, 1, cont);

            case GROUP:
                return matchSingle(input, ast.getFirstChild(), cont);

            default:
                return false;
        }
    }

    // Single-match versions that DON'T restore position (for use in iterative repeat)

    private boolean matchLiteralSingle(Input input, AstNode ast, Cont cont) {
        if (ast.getCharacter() == '\0') {
            return cont.run();
        }
        if (input.atEnd()) return false;
        if (input.current() == ast.getCharacter()) {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchEscapedSingle(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;
        if (input.current() == ast.getCharacter()) {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchTabSingle(Input input, Cont cont) {
        if (input.atEnd()) return false;
        if (input.current() == '\t') {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchWhitespaceSingle(Input input, Cont cont) {
        if (input.atEnd()) return false;
        if (WHITESPACE_CHARS.contains(input.current())) {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchAnyCharSingle(Input input, Cont cont) {
        if (input.atEnd()) return false;
        char c = input.current();
        if (c != '\n' && c != '\r') {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchCharClassSingle(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;
        if (ast.getCharSet().contains(input.current())) {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchNegatedCharClassSingle(Input input, AstNode ast, Cont cont) {
        if (input.atEnd()) return false;
        char c = input.current();
        if (!ast.getCharSet().contains(c) && c != '\n' && c != '\r') {
            input.advance(1);
            return cont.run();
        }
        return false;
    }

    private boolean matchConcatSingle(Input input, List<AstNode> exprs, int currExpr, Cont cont) {
        if (currExpr == exprs.size()) {
            return cont.run();
        }
        return matchSingle(input, exprs.get(currExpr), () ->
                matchConcatSingle(input, exprs, currExpr + 1, cont)
        );
    }

    private boolean matchAlternationSingle(Input input, List<AstNode> exprs, int currExpr, Cont cont) {
        if (currExpr == exprs.size()) {
            return false;
        }
        int savedPos = input.markPosition();
        boolean matched = matchSingle(input, exprs.get(currExpr), cont);
        if (matched) return true;
        input.restorePosition(savedPos);
        backtrackCount++;
        return matchAlternationSingle(input, exprs, currExpr + 1, cont);
    }

    /**
     * Checks resource limits and throws exceptions if exceeded.
     */
    private void checkLimits() {
        if (backtrackCount > maxBacktracks) {
            throw new BacktrackLimitExceededException("", "", maxBacktracks, backtrackCount);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > timeoutMs) {
            throw new RegexTimeoutException("", "", timeoutMs, elapsed);
        }
    }
}
