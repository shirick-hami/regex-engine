import {Component, h, State} from '@stencil/core';
import {getAppConfig} from "../../config/config";

@Component({
    tag: 'regex-benchmark',
    styleUrl: 'regex-benchmark.css',
    shadow: true,
})
export class RegexBenchmark {
    private appConfig = getAppConfig();

    @State() pattern: string = '';
    @State() input: string = '';
    @State() result: any = null;
    @State() loading: boolean = false;
    @State() error: string = '';

    private presets = [
        {name: 'Simple literal', pattern: 'hello', input: 'hello world hello there'},
        {name: 'Character class', pattern: '[a-z]+', input: 'The quick brown fox jumps over 123 lazy dogs'},
        {
            name: 'Complex pattern',
            pattern: '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+',
            input: 'Contact us at info@example.com or support@test.org'
        },
        {name: 'Long input', pattern: 'a+b', input: 'a'.repeat(100) + 'b'},
        {name: 'Backtrack stress', pattern: 'a*a*a*b', input: 'a'.repeat(20) + 'c'},
    ];

    async handleSubmit(e: Event) {
        e.preventDefault();
        if (!this.pattern || !this.input) return;

        this.loading = true;
        this.error = '';
        this.result = null;

        try {
            const res = await fetch(this.appConfig.backendBaseUrl + '/api/v1/regex/benchmark', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({pattern: this.pattern, input: this.input}),
            });

            const data = await res.json();

            if (!res.ok) {
                this.error = data.message || 'An error occurred';
            } else {
                this.result = data;
            }
        } catch (e) {
            if (e instanceof Error) {
                this.error = e.message;
            } else {
                this.error = String(e);
            }
        } finally {
            this.loading = false;
        }
    }

    applyPreset(preset: any) {
        this.pattern = preset.pattern;
        this.input = preset.input;
    }

    getBarWidth(timeMs: number) {
        if (!this.result) return 0;
        const times = [
            this.result.backtracking?.timeMs || 0,
            this.result.nfa?.timeMs || 0,
            this.result.dfa?.timeMs || 0,
        ].filter(t => t > 0);
        const max = Math.max(...times, 1);
        return (timeMs / max) * 100;
    }

    render() {
        return (
            <div class="benchmark-container">
                <div class="section-header">
                    <h2>Engine Benchmark</h2>
                    <p>Compare performance across Backtracking, NFA, and DFA engines</p>
                </div>

                <div class="presets">
                    <span class="presets-label">Quick presets:</span>
                    <div class="presets-list">
                        {this.presets.map(preset => (
                            <button
                                class="preset-btn"
                                onClick={() => this.applyPreset(preset)}
                            >
                                {preset.name}
                            </button>
                        ))}
                    </div>
                </div>

                <form class="benchmark-form" onSubmit={(e) => this.handleSubmit(e)}>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">
                                <span class="label-text">Pattern</span>
                            </label>
                            <div class="input-wrapper pattern-input">
                                <span class="input-prefix">/</span>
                                <input
                                    type="text"
                                    value={this.pattern}
                                    onInput={(e) => this.pattern = (e.target as HTMLInputElement).value}
                                    placeholder="[a-z]+"
                                    class="form-input mono"
                                />
                                <span class="input-suffix">/</span>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label">
                                <span class="label-text">Input ({this.input.length} chars)</span>
                            </label>
                            <textarea
                                value={this.input}
                                onInput={(e) => this.input = (e.target as HTMLTextAreaElement).value}
                                placeholder="Text to match against..."
                                class="form-textarea mono"
                                rows={3}
                            />
                        </div>
                    </div>

                    <button type="submit" class="submit-btn" disabled={this.loading || !this.pattern || !this.input}>
                        {this.loading ? (
                            <span class="loading-spinner"></span>
                        ) : (
                            <span>🚀 Run Benchmark</span>
                        )}
                    </button>
                </form>

                {this.error && (
                    <div class="result-card error">
                        <div class="result-icon">❌</div>
                        <div class="result-content">
                            <h3>Error</h3>
                            <p class="mono">{this.error}</p>
                        </div>
                    </div>
                )}

                {this.result && (
                    <div class="benchmark-results">
                        <div class="results-header">
                            <h3>Results</h3>
                            <span class="compile-time">Compile: {this.result.compileTimeMs}ms</span>
                        </div>

                        <div class="engine-results">
                            {/* Backtracking */}
                            <div class="engine-card">
                                <div class="engine-header">
                                    <span class="engine-icon">🔄</span>
                                    <h4>Backtracking</h4>
                                </div>
                                {this.result.backtracking?.error ? (
                                    <div class="engine-error">{this.result.backtracking.error}</div>
                                ) : (
                                    <div class="engine-stats">
                                        <div class="stat-row">
                                            <span class="stat-label">Result</span>
                                            <span class={{
                                                'stat-value': true,
                                                'matched': this.result.backtracking?.matched
                                            }}>
                        {this.result.backtracking?.matched ? '✓ Matched' : '✗ No Match'}
                      </span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">Time</span>
                                            <span class="stat-value time">{this.result.backtracking?.timeMs}ms</span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">Operations</span>
                                            <span
                                                class="stat-value">{this.result.backtracking?.operations?.toLocaleString()}</span>
                                        </div>
                                        <div class="time-bar">
                                            <div class="time-bar-fill backtracking"
                                                 style={{width: `${this.getBarWidth(this.result.backtracking?.timeMs)}%`}}></div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* NFA */}
                            <div class="engine-card">
                                <div class="engine-header">
                                    <span class="engine-icon">⚡</span>
                                    <h4>NFA</h4>
                                </div>
                                {this.result.nfa?.error ? (
                                    <div class="engine-error">{this.result.nfa.error}</div>
                                ) : (
                                    <div class="engine-stats">
                                        <div class="stat-row">
                                            <span class="stat-label">Result</span>
                                            <span class={{'stat-value': true, 'matched': this.result.nfa?.matched}}>
                        {this.result.nfa?.matched ? '✓ Matched' : '✗ No Match'}
                      </span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">Time</span>
                                            <span class="stat-value time">{this.result.nfa?.timeMs}ms</span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">State Transitions</span>
                                            <span
                                                class="stat-value">{this.result.nfa?.operations?.toLocaleString()}</span>
                                        </div>
                                        <div class="time-bar">
                                            <div class="time-bar-fill nfa"
                                                 style={{width: `${this.getBarWidth(this.result.nfa?.timeMs)}%`}}></div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* DFA */}
                            <div class="engine-card">
                                <div class="engine-header">
                                    <span class="engine-icon">🎯</span>
                                    <h4>DFA</h4>
                                </div>
                                {this.result.dfa?.error ? (
                                    <div class="engine-error">{this.result.dfa.error}</div>
                                ) : (
                                    <div class="engine-stats">
                                        <div class="stat-row">
                                            <span class="stat-label">Result</span>
                                            <span class={{'stat-value': true, 'matched': this.result.dfa?.matched}}>
                        {this.result.dfa?.matched ? '✓ Matched' : '✗ No Match'}
                      </span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">Time</span>
                                            <span class="stat-value time">{this.result.dfa?.timeMs}ms</span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">DFA States</span>
                                            <span class="stat-value">{this.result.dfa?.stateCount}</span>
                                        </div>
                                        <div class="stat-row">
                                            <span class="stat-label">Construction</span>
                                            <span class="stat-value">{this.result.dfa?.constructionTimeMs}ms</span>
                                        </div>
                                        <div class="time-bar">
                                            <div class="time-bar-fill dfa"
                                                 style={{width: `${this.getBarWidth(this.result.dfa?.timeMs)}%`}}></div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
