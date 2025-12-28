# Regex Engine

A Regular Expression Engine built with Spring Boot, featuring three matching algorithms and a beautiful web UI.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Matching Engines](#matching-engines)
- [Supported Regex Syntax](#supported-regex-syntax)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Web UI](#web-ui)
- [Health Monitoring](#health-monitoring)
- [Configuration](#configuration)
- [Testing](#testing)

## Overview

This project implements a custom regular expression engine from scratch, including:

- **Lexer/Tokenizer**: Converts regex patterns into tokens
- **Parser**: Builds an Abstract Syntax Tree (AST) from tokens
- **Three Matching Engines**:
  - **Backtracking Matcher**: Traditional recursive backtracking
  - **NFA Matcher**: Thompson's NFA simulation (linear time)
  - **DFA Matcher**: Deterministic finite automaton (fastest)
- **REST API**: Full-featured API with Swagger documentation
- **Web UI**: Beautiful Stencil.js interface

## Features

- ✅ Three matching engine implementations
- ✅ Full regex pattern compilation and validation
- ✅ Multiple matching modes (full match, find, find all)
- ✅ String replacement and splitting
- ✅ Engine benchmarking and comparison
- ✅ Pattern caching for performance
- ✅ Backtrack limit protection (prevents catastrophic backtracking)
- ✅ Timeout protection
- ✅ Health monitoring with Spring Actuator
- ✅ Comprehensive error handling
- ✅ OpenAPI/Swagger documentation
- ✅ Beautiful responsive web UI
- ✅ Extensive test coverage

## Matching Engines

### Backtracking Engine
Traditional recursive backtracking algorithm. Best for:
- Small to medium inputs
- Patterns without pathological backtracking

```
Time Complexity: O(2^n) worst case, O(n) typical
Space Complexity: O(n) call stack
```

### NFA Engine (Thompson's Construction)
Simulates an NFA by tracking all possible states simultaneously.

```
Time Complexity: O(n * m) where n=input, m=pattern states
Space Complexity: O(m) state set
```

Best for:
- Patterns that would cause excessive backtracking
- When consistent performance is needed

### DFA Engine
Converts the pattern to a deterministic finite automaton.

```
Time Complexity: O(n) matching + O(2^m) construction
Space Complexity: O(2^m) states (practical patterns much smaller)
```

Best for:
- Patterns used many times on different inputs
- Maximum matching speed
- Large inputs

### Choosing an Engine

| Scenario | Recommended Engine |
|----------|-------------------|
| Simple patterns, short input | Backtracking |
| User-provided patterns | NFA (safe from ReDoS) |
| Repeated matching, same pattern | DFA |
| Unknown pattern complexity | NFA |
| Maximum single-match speed | DFA |



| Syntax | Description | Example |
|--------|-------------|---------|
| `\t` | Tab character | `\t` matches a tab |
| `\s` | Whitespace (space, tab, newline, etc.) | `\s+` matches one or more spaces |
| `[abc]` | Character class - matches any character inside | `[aeiou]` matches vowels |
| `[^abc]` | Negated class - matches any character NOT inside | `[^0-9]` matches non-digits |
| `[a-z]` | Character range | `[a-z]` matches lowercase letters |
| `[a-zA-Z]` | Multiple ranges | `[a-zA-Z0-9]` matches alphanumeric |
| `*` | Zero or more (greedy) | `a*` matches "", "a", "aa", etc. |
| `+` | One or more (greedy) | `a+` matches "a", "aa", etc. |
| `?` | Zero or one (greedy) | `colou?r` matches "color" or "colour" |
| `\|` | Alternation (OR) | `cat\|dog` matches "cat" or "dog" |
| `\x` | Escape character x | `\\.` matches a literal dot |
| `.` | Any printable character (except newline) | `.+` matches any non-empty string |
| `(expr)` | Grouping | `(ab)+` matches "ab", "abab", etc. |

### Operator Precedence (Descending Order)

1. **Grouping** `(EXPR)` - Highest precedence
2. **Literals** `'x'` - Character literals and classes
3. **Quantifiers** `*`, `+`, `?` - Bind tightly to preceding atom
4. **Concatenation** - Implicit sequencing
5. **Alternation** `|` - Lowest precedence

This means:
- `ab*` is parsed as `a(b*)`, not `(ab)*`
- `ab|cd` is parsed as `(ab)|(cd)`, not `a(b|c)d`

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         REST API Layer                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  RegexController  │  GlobalExceptionHandler              │  │
│  └──────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Service Layer                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    RegexService                          │  │
│  │  - Pattern compilation and caching                       │  │
│  │  - Match orchestration                                   │  │
│  │  - Replace/Split operations                              │  │
│  └──────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                         Core Engine                             │
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │
│  │   RegexLexer   │→│  RegexParser   │→│BacktrackingMatcher│  │
│  │   (Tokenizer)  │  │ (AST Builder)  │  │   (Matching)    │  │
│  └────────────────┘  └────────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                        Model Layer                              │
│  ┌──────────┐  ┌─────────────────┐  ┌──────────────────────┐  │
│  │ AstNode  │  │ CompiledPattern │  │    MatchResult       │  │
│  └──────────┘  └─────────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Overview

| Component         | Responsibility |
|-------------------|----------------|
| `RegexLexer`      | Tokenizes regex patterns into discrete tokens |
| `RegexParser`     | Recursive descent parser that builds an AST |
| `AstNode`         | Represents nodes in the abstract syntax tree |
| `RegexMatcher`    | Implements the matching algorithm |
| `RegexService`    | Business logic, caching, and orchestration |
| `RegexController` | REST API endpoints |

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Building the Project

```bash
# Clone the repository
git clone <repository-url>
cd regex-engine

# Build with Maven
mvn clean install

# Run the application
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### Quick Test

```bash
# Test a simple pattern
curl -X GET "http://localhost:8080/api/v1/regex/test?pattern=[a-z]+&input=hello&type=full"

# Expected response:
{
  "matched": true,
  "pattern": "[a-z]+",
  "input": "hello",
  "startIndex": 0,
  "endIndex": 5,
  "matchedText": "hello",
  "backtrackCount": 0,
  "matchTimeMs": 1
}
```

## API Reference

Base URL: `http://localhost:8080/api/v1/regex`

### Swagger UI

Access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

### Endpoints

#### POST /compile

Compiles and validates a regex pattern.

**Request:**
```json
{
  "pattern": "[a-z]+"
}
```

**Response:**
```json
{
  "valid": true,
  "pattern": "[a-z]+",
  "compileTimeMs": 2,
  "astDescription": "PLUS\n  CHAR_CLASS[a, b, c, ...]\n"
}
```

#### POST /match

Checks if the entire input matches the pattern.

**Request:**
```json
{
  "pattern": "[a-z]+",
  "input": "hello"
}
```

**Response:**
```json
{
  "matched": true,
  "pattern": "[a-z]+",
  "input": "hello",
  "startIndex": 0,
  "endIndex": 5,
  "matchedText": "hello",
  "backtrackCount": 0,
  "matchTimeMs": 1
}
```

#### POST /find

Finds the first match in the input string.

**Request:**
```json
{
  "pattern": "[0-9]+",
  "input": "Order 12345 confirmed"
}
```

**Response:**
```json
{
  "matched": true,
  "startIndex": 6,
  "endIndex": 11,
  "matchedText": "12345"
}
```

#### POST /find-all

Finds all non-overlapping matches.

**Request:**
```json
{
  "pattern": "[a-z]+",
  "input": "hello world foo bar"
}
```

**Response:**
```json
{
  "matched": true,
  "allMatches": [
    {"startIndex": 0, "endIndex": 5, "matchedText": "hello"},
    {"startIndex": 6, "endIndex": 11, "matchedText": "world"},
    {"startIndex": 12, "endIndex": 15, "matchedText": "foo"},
    {"startIndex": 16, "endIndex": 19, "matchedText": "bar"}
  ]
}
```

#### POST /replace

Replaces all matches with a replacement string.

**Request:**
```json
{
  "pattern": "[0-9]+",
  "input": "Order 123 and Order 456",
  "replacement": "XXX"
}
```

**Response:**
```json
{
  "input": "Order 123 and Order 456",
  "pattern": "[0-9]+",
  "replacement": "XXX",
  "result": "Order XXX and Order XXX",
  "replacementCount": 2
}
```

#### POST /split

Splits the input string by pattern matches.

**Request:**
```json
{
  "pattern": ",\\s*",
  "input": "apple, banana, cherry"
}
```

**Response:**
```json
{
  "input": "apple, banana, cherry",
  "pattern": ",\\s*",
  "parts": ["apple", "banana", "cherry"],
  "partCount": 3
}
```

#### GET /test

Quick pattern testing via query parameters.

```bash
GET /test?pattern=[a-z]+&input=hello&type=find
```

Parameters:
- `pattern`: The regex pattern
- `input`: The input string
- `type`: One of `full`, `find`, `findAll` (default: `find`)

#### GET /cache/stats

Returns cache statistics.

```json
{
  "cacheSize": 42
}
```

#### DELETE /cache

Clears the pattern cache.

## Usage Examples

### Validating Email-like Patterns

```bash
curl -X POST http://localhost:8080/api/v1/regex/match \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "[a-z]+@[a-z]+\\.[a-z]+",
    "input": "user@example.com"
  }'
```

### Extracting Numbers

```bash
curl -X POST http://localhost:8080/api/v1/regex/find-all \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "[0-9]+",
    "input": "The price is $123 or $456"
  }'
```

### Identifier Validation

```bash
curl -X POST http://localhost:8080/api/v1/regex/match \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "[a-zA-Z_][a-zA-Z0-9_]*",
    "input": "myVariable123"
  }'
```

### Splitting CSV

```bash
curl -X POST http://localhost:8080/api/v1/regex/split \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": ",",
    "input": "a,b,c,d,e"
  }'
```

## Backtracking Algorithm

The engine uses a **recursive backtracking algorithm** for pattern matching. Here's how it works:

### Algorithm Overview

```
FUNCTION match(node, position):
    SWITCH node.type:
        CASE LITERAL:
            IF input[position] == node.char THEN
                RETURN position + 1
            ELSE
                RETURN FAIL
        
        CASE STAR:
            // Greedy: try to match as many as possible
            positions = [position]
            current = position
            WHILE match(node.child, current) succeeds:
                positions.add(result)
                current = result
            
            // Try from most matches to zero (backtracking)
            FOR i = positions.length-1 DOWN TO 0:
                RETURN positions[i]  // Continue with rest of pattern
            
        CASE ALTERNATION:
            // Try first branch
            result = match(node.left, position)
            IF result != FAIL THEN
                RETURN result
            
            // Backtrack and try second branch
            RETURN match(node.right, position)
        
        // ... other cases
```

### Backtracking in Action

Consider pattern `a*ab` matching against input `"aaab"`:

1. `a*` greedily matches `"aaa"` (positions 0, 1, 2, 3)
2. `a` tries to match at position 3, but finds `'b'` - FAIL
3. **Backtrack**: `a*` reduces to `"aa"` (positions 0, 1, 2)
4. `a` matches at position 2
5. `b` matches at position 3
6. SUCCESS!

### Greedy Quantifiers

All quantifiers (`*`, `+`, `?`) are greedy:
- They match as much as possible first
- Then backtrack if the rest of the pattern fails

### Safety Mechanisms

1. **Backtrack Limit**: Prevents catastrophic backtracking
2. **Timeout**: Prevents infinite matching
3. **Input Length Limit**: Prevents memory exhaustion

## Configuration

Configure the engine via `application.properties`:

```properties
# Maximum pattern length (characters)
regex.engine.max-pattern-length=10000

# Maximum input length (characters)
regex.engine.max-input-length=1000000

# Maximum backtracking steps
regex.engine.max-backtrack-limit=100000

# Operation timeout (milliseconds)
regex.engine.timeout-ms=30000

# Pattern caching
regex.engine.cache-enabled=true
regex.engine.cache-max-size=1000
```

## Testing

Run the comprehensive test suite:

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Categories

| Test Class                | Description                                          |
|---------------------------|------------------------------------------------------|
| `BasicLexerServerTest`    | Lexer unit tests for all syntax constructs           |
| `BasicParserServerTest`   | Parser unit tests for all syntax constructs          |
| `BacktrackingMatcherTest` | Matcher unit tests for all patterns via backtracking |
| `NFAMatcherTest`          | Matcher unit tests for all patterns via nfa          |
| `DFAMatcherTest`          | Matcher unit tests for all patterns via dfa          |
| `RegexServiceTest`        | Service layer tests                                  |
| `RegexControllerTest`     | Full API integration tests                           |

## Performance Considerations

### Pattern Complexity

Some patterns can cause excessive backtracking:

```
# Problematic (exponential backtracking)
Pattern: a*a*a*a*b
Input: "aaaaaaaaaaaaaaaaaaac"

# Better approach: simplify the pattern
Pattern: a+b
```

### Recommendations

1. **Avoid nested quantifiers**: `(a+)+` can cause exponential backtracking
2. **Be specific**: Use character classes instead of `.` when possible
3. **Use anchoring**: Know when you need full match vs. find
4. **Cache patterns**: Reuse compiled patterns for better performance

### Metrics

The API returns performance metrics with each response:

- `backtrackCount`: Number of backtracking steps
- `matchTimeMs`: Time taken for matching
- `compileTimeMs`: Time taken for pattern compilation

## Error Handling

The API returns structured error responses:

```json
{
  "error": "PARSE_ERROR",
  "message": "Unmatched '('",
  "position": 5,
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Types

| Error Code | Description |
|------------|-------------|
| `PARSE_ERROR` | Invalid regex pattern syntax |
| `VALIDATION_ERROR` | Missing required fields |
| `BACKTRACK_LIMIT_EXCEEDED` | Pattern caused too much backtracking |
| `TIMEOUT` | Operation took too long |
| `INVALID_ARGUMENT` | Input validation failed |
| `INTERNAL_ERROR` | Unexpected server error |

## License

MIT License

Copyright (c) 2025 Saptarick Mishra

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Author

**Saptarick Mishra**

- GitHub: [github.com/shirick-hami/regex-engine](https://github.com/shirick-hami/regex-engine)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Submit a pull request

---

Designed and developed with ❤️ by **Saptarick Mishra**
