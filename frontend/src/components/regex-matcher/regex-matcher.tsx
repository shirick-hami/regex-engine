import {Component, h, Prop, State} from '@stencil/core';
import {http} from "../../services/http";
import {REGEX_MATCH_API, REGEX_FIND_API, REGEX_FIND_ALL_API} from "../../utils/constants";

interface MatchResult {
    matched: boolean;
    engine: string;
    startIndex?: number;
    endIndex?: number;
    matchTimeMs: number;
    backtrackCount?: number;
    allMatches?: Array<{
        startIndex: number;
        endIndex: number;
        matchedText: string;
    }>;
}

@Component({
    tag: 'regex-matcher',
    styleUrl: 'regex-matcher.css',
    shadow: true,
})
export class RegexMatcher {
    @Prop() mode: 'match' | 'find' = 'match';

    @State() pattern: string = '';
    @State() input: string = '';
    @State() engine: string = 'BACKTRACKING';
    @State() findAll: boolean = false;
    @State() result: MatchResult | null = null;
    @State() loading: boolean = false;
    @State() error: string = '';

    private engines = ['BACKTRACKING', 'NFA', 'DFA'];

    async handleSubmit(e: Event) {
        e.preventDefault();
        if (!this.pattern || !this.input) return;

        this.loading = true;
        this.error = '';
        this.result = null;

        try {
            const endpoint = this.mode === 'match'
                ? REGEX_MATCH_API
                : this.findAll
                    ? REGEX_FIND_ALL_API
                    : REGEX_FIND_API;

            this.result = await http.post<MatchResult>(
                endpoint,
                {pattern: this.pattern, input: this.input},
                {engine: this.engine}
            );
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

    renderHighlightedText() {
        if (!this.result?.matched) return this.input;

        const matches = this.result.allMatches || [
            {startIndex: this.result.startIndex!, endIndex: this.result.endIndex!, matchedText: ''}
        ];

        if (matches.length === 0) return this.input;

        const parts: any[] = [];
        let lastEnd = 0;

        matches.forEach((match, i: number) => {
            if (match.startIndex > lastEnd) {
                parts.push(<span class="text-normal">{this.input.slice(lastEnd, match.startIndex)}</span>);
            }
            parts.push(
                <span class="text-match" key={i}>
                    {this.input.slice(match.startIndex, match.endIndex)}
                </span>
            );
            lastEnd = match.endIndex;
        });

        if (lastEnd < this.input.length) {
            parts.push(<span class="text-normal">{this.input.slice(lastEnd)}</span>);
        }

        return parts;
    }

    render() {
        const title = this.mode === 'match' ? 'Full Match' : 'Find Match';
        const description = this.mode === 'match'
            ? 'Check if the entire input matches the pattern'
            : 'Search for the pattern within the input';

        return (
            <div class="matcher-container">
                <div class="section-header">
                    <h2>{title}</h2>
                    <p>{description}</p>
                </div>

                <form class="matcher-form" onSubmit={(e) => this.handleSubmit(e)}>
                    <div class="form-group">
                        <label class="form-label">
                            <span class="label-text">Pattern</span>
                            <span class="label-hint">Regular expression</span>
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
                            <span class="label-text">Input</span>
                            <span class="label-hint">Text to match against</span>
                        </label>
                        <textarea
                            value={this.input}
                            onInput={(e) => this.input = (e.target as HTMLTextAreaElement).value}
                            placeholder="Enter your test string here..."
                            class="form-textarea mono"
                            rows={4}
                        />
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">
                                <span class="label-text">Engine</span>
                            </label>
                            <div class="engine-selector">
                                {this.engines.map(eng => (
                                    <button
                                        type="button"
                                        class={{'engine-btn': true, 'active': this.engine === eng}}
                                        onClick={() => this.engine = eng}
                                    >
                                        {eng}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {this.mode === 'find' && (
                            <div class="form-group">
                                <label class="form-label">
                                    <span class="label-text">Options</span>
                                </label>
                                <label class="checkbox-label">
                                    <input
                                        type="checkbox"
                                        checked={this.findAll}
                                        onChange={(e) => this.findAll = (e.target as HTMLInputElement).checked}
                                    />
                                    <span class="checkbox-custom"></span>
                                    <span>Find All Matches</span>
                                </label>
                            </div>
                        )}
                    </div>

                    <button type="submit" class="submit-btn" disabled={this.loading || !this.pattern || !this.input}>
                        {this.loading ? (
                            <span class="loading-spinner"></span>
                        ) : (
                            <span>Run {title}</span>
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
                    <div class={{'result-card': true, 'success': this.result.matched, 'failure': !this.result.matched}}>
                        <div class="result-header">
                            <div class="result-icon">{this.result.matched ? '✓' : '✗'}</div>
                            <h3>{this.result.matched ? 'Match Found!' : 'No Match'}</h3>
                            <span class="engine-badge">{this.result.engine}</span>
                        </div>

                        {this.result.matched && (
                            <div class="result-body">
                                <div class="highlighted-text mono">
                                    {this.renderHighlightedText()}
                                </div>

                                {this.result.allMatches && this.result.allMatches.length > 0 && (
                                    <div class="matches-list">
                                        <h4>Matches ({this.result.allMatches.length})</h4>
                                        <div class="matches-grid">
                                            {this.result.allMatches.map((m, i: number) => (
                                                <div class="match-item" key={i}>
                                                    <span class="match-index">#{i + 1}</span>
                                                    <span class="match-text mono">"{m.matchedText}"</span>
                                                    <span class="match-range">[{m.startIndex}:{m.endIndex}]</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        <div class="result-stats">
                            <div class="stat">
                                <span class="stat-label">Time</span>
                                <span class="stat-value">{this.result.matchTimeMs}ms</span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Operations</span>
                                <span class="stat-value">{this.result.backtrackCount?.toLocaleString()}</span>
                            </div>
                            {this.result.matched && (
                                <div class="stat">
                                    <span class="stat-label">Position</span>
                                    <span class="stat-value">[{this.result.startIndex}:{this.result.endIndex}]</span>
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
