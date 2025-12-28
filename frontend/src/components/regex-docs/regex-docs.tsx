import {Component, h} from '@stencil/core';
import {getAppConfig} from "../../config/config";

@Component({
    tag: 'regex-docs',
    styleUrl: 'regex-docs.css',
    shadow: true,
})
export class RegexDocs {
    private appConfig = getAppConfig();

    private features = [
        {
            category: 'Matching Engines',
            icon: '⚙️',
            items: [
                {
                    name: 'Backtracking Engine',
                    desc: 'Traditional recursive backtracking with continuation-passing style'
                },
                {name: 'NFA Engine', desc: "Thompson's NFA simulation - linear time, immune to ReDoS attacks"},
                {name: 'DFA Engine', desc: 'Deterministic finite automaton - fastest matching after construction'},
            ]
        },
        {
            category: 'Regex Syntax',
            icon: '📝',
            items: [
                {name: 'Literals', desc: 'Match exact characters (a, b, 1, 2, etc.)'},
                {name: 'Dot (.)', desc: 'Match any character except newline'},
                {name: 'Character Classes', desc: '[abc], [a-z], [^abc] - match character sets'},
                {name: 'Quantifiers', desc: '* (zero+), + (one+), ? (optional)'},
                {name: 'Alternation', desc: 'a|b - match either pattern'},
                {name: 'Groups', desc: '(abc) - group patterns together'},
                {name: 'Escapes', desc: '\\., \\*, \\+, \\?, \\t, \\s - special characters'},
            ]
        },
        {
            category: 'API Operations',
            icon: '🔌',
            items: [
                {name: 'Match', desc: 'Check if entire input matches the pattern'},
                {name: 'Find', desc: 'Find first occurrence of pattern in input'},
                {name: 'Find All', desc: 'Find all non-overlapping matches'},
                {name: 'Replace', desc: 'Replace all matches with replacement string'},
                {name: 'Split', desc: 'Split input string by pattern matches'},
                {name: 'Compile', desc: 'Validate and compile pattern to AST'},
                {name: 'Benchmark', desc: 'Compare performance across all engines'},
            ]
        },
        {
            category: 'Safety Features',
            icon: '🛡️',
            items: [
                {name: 'Backtrack Limit', desc: 'Prevents catastrophic backtracking (configurable)'},
                {name: 'Timeout Protection', desc: 'Maximum execution time per operation'},
                {name: 'Input Validation', desc: 'Pattern and input length limits'},
                {name: 'Pattern Caching', desc: 'LRU cache for compiled patterns'},
            ]
        },
        {
            category: 'Monitoring',
            icon: '📊',
            items: [
                {name: 'Health Endpoints', desc: 'Spring Actuator health checks'},
                {name: 'Metrics', desc: 'Match counts, success rates, timing'},
                {name: 'JVM Stats', desc: 'Memory usage, uptime, processors'},
                {name: 'Cache Stats', desc: 'Pattern cache size and management'},
            ]
        },
    ];

    private techStack = [
        {name: 'Spring Boot 4.0', icon: '🍃'},
        {name: 'Java 21', icon: '☕'},
        {name: 'Stencil.js', icon: '⚡'},
        {name: 'TypeScript', icon: '📘'},
        {name: 'OpenAPI/Swagger', icon: '📄'},
    ];

    render() {
        return (
            <div class="docs-container">
                {/* Hero Section */}
                <section class="hero">
                    <div class="hero-content">
                        <div class="hero-badge">Open Source</div>
                        <h1>Simplified Regex Engine</h1>
                        <p class="hero-subtitle">
                            A regular expression engine with three matching algorithms,
                            REST API, and beautiful web interface.
                        </p>
                        <div class="hero-actions">
                            <a href={`${this.appConfig.githubUrl}`} target="_blank"
                               rel="noopener noreferrer" class="btn-primary">
                <span class="btn-icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path
                        d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
                  </svg>
                </span>
                                <span>View on GitHub</span>
                            </a>
                            <a href={`${this.appConfig.backendBaseUrl}/swagger-ui.html`} target="_blank" class="btn-secondary">
                                <span>API Docs</span>
                            </a>
                        </div>
                    </div>
                    <div class="hero-visual">
                        <div class="code-preview">
                            <div class="code-header">
                                <span class="dot red"></span>
                                <span class="dot yellow"></span>
                                <span class="dot green"></span>
                            </div>
                            <pre class="code-body"><code><span
                                class="keyword">POST</span> /api/v1/regex/match?engine=<span class="string">NFA</span>
                                {'{'}
                                <span class="key">"pattern"</span>: <span class="string">"[a-z]+@[a-z]+\\.com"</span>,
  <span class="key">"input"</span>: <span class="string">"hello@world.com"</span>
                                {'}'}

                                <span class="comment">// Response</span>
                                {'{'}
                                <span class="key">"matched"</span>: <span class="boolean">true</span>,
  <span class="key">"matchTimeMs"</span>: <span class="number">2</span>,
  <span class="key">"engine"</span>: <span class="string">"NFA"</span>
                                {'}'}</code></pre>
                        </div>
                    </div>
                </section>

                {/* Engine Comparison */}
                <section class="engines-section">
                    <h2>Three Matching Engines</h2>
                    <p class="section-subtitle">Choose the right engine for your use case</p>

                    <div class="engines-grid">
                        <div class="engine-card">
                            <div class="engine-icon">🔄</div>
                            <h3>Backtracking</h3>
                            <div class="engine-complexity">O(2<sup>n</sup>) worst case</div>
                            <p>Traditional recursive backtracking with continuation-passing style. Best for simple
                                patterns and short inputs.</p>
                            <ul class="engine-pros">
                                <li>Simple implementation</li>
                                <li>Good for most patterns</li>
                                <li>Familiar behavior</li>
                            </ul>
                        </div>

                        <div class="engine-card featured">
                            <div class="engine-badge">Recommended</div>
                            <div class="engine-icon">⚡</div>
                            <h3>NFA</h3>
                            <div class="engine-complexity">O(n × m) guaranteed</div>
                            <p>Thompson's NFA simulation tracks all possible states simultaneously. Immune to ReDoS
                                attacks.</p>
                            <ul class="engine-pros">
                                <li>Linear time guarantee</li>
                                <li>Safe for user input</li>
                                <li>Consistent performance</li>
                            </ul>
                        </div>

                        <div class="engine-card">
                            <div class="engine-icon">🎯</div>
                            <h3>DFA</h3>
                            <div class="engine-complexity">O(n) matching</div>
                            <p>Converts pattern to deterministic automaton. Fastest matching after one-time construction
                                cost.</p>
                            <ul class="engine-pros">
                                <li>Fastest matching</li>
                                <li>Best for repeated use</li>
                                <li>Predictable performance</li>
                            </ul>
                        </div>
                    </div>
                </section>

                {/* Features */}
                <section class="features-section">
                    <h2>Features</h2>
                    <div class="features-grid">
                        {this.features.map(category => (
                            <div class="feature-category">
                                <div class="category-header">
                                    <span class="category-icon">{category.icon}</span>
                                    <h3>{category.category}</h3>
                                </div>
                                <ul class="feature-list">
                                    {category.items.map(item => (
                                        <li>
                                            <span class="feature-name">{item.name}</span>
                                            <span class="feature-desc">{item.desc}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                </section>

                {/* Tech Stack */}
                <section class="tech-section">
                    <h2>Built With</h2>
                    <div class="tech-grid">
                        {this.techStack.map(tech => (
                            <div class="tech-item">
                                <span class="tech-icon">{tech.icon}</span>
                                <span class="tech-name">{tech.name}</span>
                            </div>
                        ))}
                    </div>
                </section>

                {/* Quick Start */}
                <section class="quickstart-section">
                    <h2>Quick Start</h2>
                    <div class="quickstart-grid">
                        <div class="quickstart-step">
                            <div class="step-number">1</div>
                            <h4>Clone Repository</h4>
                            <code>{this.appConfig.gitCloneUrl}</code>
                        </div>
                        <div class="quickstart-step">
                            <div class="step-number">2</div>
                            <h4>Build & Run Backend</h4>
                            <code>cd regex-engine && mvn spring-boot:run</code>
                        </div>
                        <div class="quickstart-step">
                            <div class="step-number">3</div>
                            <h4>Build & Run Frontend</h4>
                            <code>cd frontend && npm install && npm start</code>
                        </div>
                    </div>
                </section>

                {/* Footer */}
                <footer class="docs-footer">
                    <div class="footer-content">
                        <div class="license-info">
                            <h4>MIT License</h4>
                            <p>
                                Copyright © 2025 {this.appConfig.creatorName}. Permission is hereby granted, free of charge,
                                to any person obtaining a copy of this software and associated documentation files,
                                to deal in the Software without restriction, including without limitation the rights
                                to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
                                the Software.
                            </p>
                        </div>
                        <div class="credits">
                            <p>
                                Designed and developed with ❤️ by <strong>{this.appConfig.creatorName}</strong>
                            </p>
                            <div class="footer-links">
                                <a href={`${this.appConfig.githubUrl}`} target="_blank"
                                   rel="noopener noreferrer">
                                    GitHub
                                </a>
                                <span class="separator">•</span>
                                <a href={`${this.appConfig.backendBaseUrl}/swagger-ui.html`} target="_blank">
                                    API Documentation
                                </a>
                                <span class="separator">•</span>
                                <a href={`${this.appConfig.backendBaseUrl}//api-docs`} target="_blank">
                                    OpenAPI Spec
                                </a>
                            </div>
                        </div>
                    </div>
                </footer>
            </div>
        );
    }
}
