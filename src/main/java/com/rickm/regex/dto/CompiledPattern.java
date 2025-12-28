package com.rickm.regex.dto;

import com.rickm.regex.engine.parser.AstNode;
import lombok.Builder;
import lombok.Data;

/**
 * Represents a compiled regular expression pattern.
 *
 * This class encapsulates the parsed AST along with metadata
 * about the original pattern string.
 */
@Data
@Builder
public class CompiledPattern {

    /** The original regex pattern string */
    private final String pattern;

    /** The root of the parsed AST */
    private final AstNode ast;

    /** Time taken to compile the pattern in milliseconds */
    private final long compileTimeMs;

    /** Human-readable representation of the AST (for debugging) */
    private final String astDescription;

    /**
     * Checks if this compiled pattern is valid.
     *
     * @return true if the AST is non-null
     */
    public boolean isValid() {
        return ast != null;
    }
}
