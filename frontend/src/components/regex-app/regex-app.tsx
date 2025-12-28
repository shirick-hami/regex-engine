import {Component, h, State} from '@stencil/core';
import {getDeveloper, getGithubUrl} from "../../utils/env.service";
import {http} from "../../services/http";
import {MONITOR_STATUS_API} from "../../utils/constants";

interface StatusResponse {
    status: string;
    error?: string;
}

@Component({
    tag: 'regex-app',
    styleUrl: 'regex-app.css',
    shadow: true,
})
export class RegexApp {
    @State() activeTab: string = 'docs';
    @State() status: StatusResponse | null = null;

    private tabs = [
        {id: 'docs', label: 'Documentation', icon: '📚'},
        {id: 'match', label: 'Match', icon: '⚡'},
        {id: 'find', label: 'Find', icon: '🔍'},
        {id: 'replace', label: 'Replace', icon: '✏️'},
        {id: 'split', label: 'Split', icon: '✂️'},
        {id: 'benchmark', label: 'Benchmark', icon: '📊'},
        {id: 'monitor', label: 'Monitor', icon: '💓'},
    ];

    async componentWillLoad() {
        await this.checkStatus();
    }

    async checkStatus() {
        try {
            this.status = await http.get<StatusResponse>(MONITOR_STATUS_API);
        } catch (e) {
            if (e instanceof Error) {
                this.status = {status: 'DOWN', error: e.message};
            } else {
                this.status = {status: 'DOWN', error: String(e)};
            }
        }
    }

    render() {
        return (
            <div class="app-container">
                <header class="app-header">
                    <div class="logo">
                        <span class="logo-icon">⟨⟩</span>
                        <h1>Regex Engine</h1>
                        <span class="version">v1.0</span>
                    </div>
                    <div class="status-badge" data-status={this.status?.status}>
                        <span class="status-dot"></span>
                        <span class="status-text">{this.status?.status || 'Checking...'}</span>
                    </div>
                </header>

                <nav class="tab-nav">
                    {this.tabs.map(tab => (
                        <button
                            class={{'tab-btn': true, 'active': this.activeTab === tab.id}}
                            onClick={() => this.activeTab = tab.id}
                        >
                            <span class="tab-icon">{tab.icon}</span>
                            <span class="tab-label">{tab.label}</span>
                        </button>
                    ))}
                </nav>

                <main class="app-main">
                    {this.activeTab === 'docs' && <regex-docs/>}
                    {this.activeTab === 'match' && <regex-matcher mode="match"/>}
                    {this.activeTab === 'find' && <regex-matcher mode="find"/>}
                    {this.activeTab === 'replace' && <regex-replace/>}
                    {this.activeTab === 'split' && <regex-split/>}
                    {this.activeTab === 'benchmark' && <regex-benchmark/>}
                    {this.activeTab === 'monitor' && <regex-monitor/>}
                </main>

                <footer class="app-footer">
                    <span>MIT License © 2025 {getDeveloper()}</span>
                    <span class="separator">•</span>
                    <span>Backtracking | NFA | DFA</span>
                    <span class="separator">•</span>
                    <a href={getGithubUrl()} target="_blank" rel="noopener noreferrer">GitHub</a>
                </footer>
            </div>
        );
    }
}
