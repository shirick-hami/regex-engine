import {Component, h, State} from '@stencil/core';
import {http} from "../../services/http";
import {REGEX_REPLACE_API} from "../../utils/constants";

interface ReplaceResult {
    result: string;
    replacementCount: number;
}

@Component({
    tag: 'regex-replace',
    styleUrl: 'regex-replace.css',
    shadow: true,
})
export class RegexReplace {
    @State() pattern: string = '';
    @State() input: string = '';
    @State() replacement: string = '';
    @State() engine: string = 'BACKTRACKING';
    @State() result: ReplaceResult | null = null;
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
            this.result = await http.post<ReplaceResult>(
                REGEX_REPLACE_API,
                {
                    pattern: this.pattern,
                    input: this.input,
                    replacement: this.replacement
                },
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

    render() {
        return (
            <div class="replace-container">
                <div class="section-header">
                    <h2>Replace All</h2>
                    <p>Find and replace all occurrences of the pattern</p>
                </div>

                <form class="replace-form" onSubmit={(e) => this.handleSubmit(e)}>
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
                                placeholder="[0-9]+"
                                class="form-input mono"
                            />
                            <span class="input-suffix">/g</span>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">
                            <span class="label-text">Replacement</span>
                        </label>
                        <input
                            type="text"
                            value={this.replacement}
                            onInput={(e) => this.replacement = (e.target as HTMLInputElement).value}
                            placeholder="XXX"
                            class="form-input-full mono"
                        />
                    </div>

                    <div class="form-group">
                        <label class="form-label">
                            <span class="label-text">Input</span>
                        </label>
                        <textarea
                            value={this.input}
                            onInput={(e) => this.input = (e.target as HTMLTextAreaElement).value}
                            placeholder="Order #123 placed on 2024-01-15"
                            class="form-textarea mono"
                            rows={4}
                        />
                    </div>

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

                    <button type="submit" class="submit-btn" disabled={this.loading || !this.pattern || !this.input}>
                        {this.loading ? (
                            <span class="loading-spinner"></span>
                        ) : (
                            <span>Replace All</span>
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
                    <div class="result-card">
                        <div class="result-header">
                            <h3>
                                {this.result.replacementCount > 0
                                    ? `${this.result.replacementCount} Replacement${this.result.replacementCount > 1 ? 's' : ''} Made`
                                    : 'No Matches Found'}
                            </h3>
                        </div>

                        <div class="result-comparison">
                            <div class="comparison-panel">
                                <h4>Before</h4>
                                <pre class="mono">{this.input}</pre>
                            </div>
                            <div class="comparison-arrow">→</div>
                            <div class="comparison-panel output">
                                <h4>After</h4>
                                <pre class="mono">{this.result.result}</pre>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
