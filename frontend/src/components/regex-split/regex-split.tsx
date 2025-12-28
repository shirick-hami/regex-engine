import {Component, h, State} from '@stencil/core';
import {http} from "../../services/http";
import {REGEX_SPLIT_API} from "../../utils/constants";

interface SplitResult {
    parts: string[];
    partCount: number;
}

@Component({
    tag: 'regex-split',
    styleUrl: 'regex-split.css',
    shadow: true,
})
export class RegexSplit {
    @State() pattern: string = '';
    @State() input: string = '';
    @State() engine: string = 'BACKTRACKING';
    @State() result: SplitResult | null = null;
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
            this.result = await http.post<SplitResult>(
                REGEX_SPLIT_API,
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

    render() {
        return (
            <div class="split-container">
                <div class="section-header">
                    <h2>Split</h2>
                    <p>Split the input string using the pattern as delimiter</p>
                </div>

                <form class="split-form" onSubmit={(e) => this.handleSubmit(e)}>
                    <div class="form-group">
                        <label class="form-label">
                            <span class="label-text">Delimiter Pattern</span>
                        </label>
                        <div class="input-wrapper pattern-input">
                            <span class="input-prefix">/</span>
                            <input
                                type="text"
                                value={this.pattern}
                                onInput={(e) => this.pattern = (e.target as HTMLInputElement).value}
                                placeholder=",\\s*"
                                class="form-input mono"
                            />
                            <span class="input-suffix">/</span>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">
                            <span class="label-text">Input</span>
                        </label>
                        <textarea
                            value={this.input}
                            onInput={(e) => this.input = (e.target as HTMLTextAreaElement).value}
                            placeholder="apple, banana, cherry, date"
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
                            <span>Split</span>
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
                            <h3>{this.result.partCount} Part{this.result.partCount !== 1 ? 's' : ''}</h3>
                        </div>

                        <div class="parts-list">
                            <div class="parts-grid">
                                {this.result.parts.map((part: string, i: number) => (
                                    <div class="part-item" key={i}>
                                        <span class="part-index">#{i}</span>
                                        <span class="part-text">"{part}"</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }
}
