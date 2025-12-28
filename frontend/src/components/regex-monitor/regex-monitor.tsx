import {Component, h, State} from '@stencil/core';
import {http} from "../../services/http";
import {
    MONITOR_STATUS_API,
    MONITOR_METRICS_API,
    MONITOR_METRICS_RESET_API,
    MONITOR_CACHE_API
} from "../../utils/constants";

interface StatusResponse {
    status: string;
    healthCheckTimeMs: number;
    totalRequests: number;
}

interface EngineMetrics {
    totalMatches: number;
    successfulMatches: number;
    failedMatches: number;
    successRate: number;
    averageMatchTimeMs: number;
    compilations: number;
    cacheSize: number;
    cacheMaxSize: number;
}

interface JvmMetrics {
    uptimeMs: number;
    heapUsed: number;
    heapMax: number;
    availableProcessors: number;
}

interface SystemMetrics {
    javaVersion: string;
    javaVendor: string;
    osName: string;
    osArch: string;
}

interface MetricsResponse {
    engine: EngineMetrics;
    jvm: JvmMetrics;
    system: SystemMetrics;
}

@Component({
    tag: 'regex-monitor',
    styleUrl: 'regex-monitor.css',
    shadow: true,
})
export class RegexMonitor {
    @State() status: StatusResponse | null = null;
    @State() metrics: MetricsResponse | null = null;
    @State() loading: boolean = true;
    @State() autoRefresh: boolean = true;

    private refreshInterval: any;

    async componentWillLoad() {
        await this.refresh();
        this.startAutoRefresh();
    }

    disconnectedCallback() {
        this.stopAutoRefresh();
    }

    startAutoRefresh() {
        if (this.refreshInterval) return;
        this.refreshInterval = setInterval(() => {
            if (this.autoRefresh) {
                this.refresh();
            }
        }, 5000);
    }

    stopAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    }

    async refresh() {
        try {
            const statusData = await http.get<StatusResponse>(MONITOR_STATUS_API);
            const metricsData = await http.get<MetricsResponse>(MONITOR_METRICS_API);

            this.status = statusData;
            this.metrics = metricsData;
        } catch (e) {
            console.error('Failed to fetch metrics:', e);
        } finally {
            this.loading = false;
        }
    }

    async resetMetrics() {
        try {
            await http.post<void>(MONITOR_METRICS_RESET_API);
            await this.refresh();
        } catch (e) {
            console.error('Failed to reset metrics:', e);
        }
    }

    async clearCache() {
        try {
            await http.delete<void>(MONITOR_CACHE_API);
            await this.refresh();
        } catch (e) {
            console.error('Failed to clear cache:', e);
        }
    }

    formatBytes(bytes: number): string {
        const units = ['B', 'KB', 'MB', 'GB'];
        let value = bytes;
        let unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return `${value.toFixed(1)} ${units[unitIndex]}`;
    }

    formatUptime(ms: number): string {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) return `${days}d ${hours % 24}h`;
        if (hours > 0) return `${hours}h ${minutes % 60}m`;
        if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
        return `${seconds}s`;
    }

    render() {
        if (this.loading) {
            return (
                <div class="monitor-container">
                    <div class="loading-state">
                        <div class="loading-spinner"></div>
                        <p>Loading metrics...</p>
                    </div>
                </div>
            );
        }

        const engine = this.metrics?.engine || {} as EngineMetrics;
        const jvm = this.metrics?.jvm || {} as JvmMetrics;
        const system = this.metrics?.system || {} as SystemMetrics;

        return (
            <div class="monitor-container">
                <div class="section-header">
                    <div class="header-content">
                        <h2>Health Monitor</h2>
                        <p>Real-time engine health and performance metrics</p>
                    </div>
                    <div class="header-actions">
                        <label class="auto-refresh-toggle">
                            <input
                                type="checkbox"
                                checked={this.autoRefresh}
                                onChange={(e) => this.autoRefresh = (e.target as HTMLInputElement).checked}
                            />
                            <span class="toggle-slider"></span>
                            <span class="toggle-label">Auto-refresh</span>
                        </label>
                        <button class="refresh-btn" onClick={() => this.refresh()}>
                            ↻ Refresh
                        </button>
                    </div>
                </div>

                {/* Status Card */}
                <div class="status-card" data-status={this.status?.status}>
                    <div class="status-indicator">
                        <div class="status-pulse"></div>
                        <span class="status-text">{this.status?.status || 'UNKNOWN'}</span>
                    </div>
                    <div class="status-details">
                        <div class="status-detail">
                            <span class="detail-label">Health Check</span>
                            <span class="detail-value">{this.status?.healthCheckTimeMs}ms</span>
                        </div>
                        <div class="status-detail">
                            <span class="detail-label">Total Requests</span>
                            <span class="detail-value">{this.status?.totalRequests?.toLocaleString()}</span>
                        </div>
                    </div>
                </div>

                {/* Metrics Grid */}
                <div class="metrics-grid">
                    {/* Engine Metrics */}
                    <div class="metrics-card">
                        <div class="card-header">
                            <span class="card-icon">⚙️</span>
                            <h3>Engine Metrics</h3>
                        </div>
                        <div class="metrics-list">
                            <div class="metric-item">
                                <span class="metric-label">Total Matches</span>
                                <span class="metric-value">{engine.totalMatches?.toLocaleString() || 0}</span>
                            </div>
                            <div class="metric-item success">
                                <span class="metric-label">Successful</span>
                                <span class="metric-value">{engine.successfulMatches?.toLocaleString() || 0}</span>
                            </div>
                            <div class="metric-item error">
                                <span class="metric-label">Failed</span>
                                <span class="metric-value">{engine.failedMatches?.toLocaleString() || 0}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Success Rate</span>
                                <span class="metric-value">
                                    {engine.successRate ? `${(engine.successRate * 100).toFixed(1)}%` : 'N/A'}
                                </span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Avg Match Time</span>
                                <span class="metric-value">
                                    {engine.averageMatchTimeMs ? `${engine.averageMatchTimeMs.toFixed(2)}ms` : 'N/A'}
                                </span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Compilations</span>
                                <span class="metric-value">{engine.compilations?.toLocaleString() || 0}</span>
                            </div>
                        </div>
                    </div>

                    {/* Cache Metrics */}
                    <div class="metrics-card">
                        <div class="card-header">
                            <span class="card-icon">💾</span>
                            <h3>Pattern Cache</h3>
                        </div>
                        <div class="cache-visual">
                            <div class="cache-bar">
                                <div
                                    class="cache-fill"
                                    style={{width: `${(engine.cacheSize / engine.cacheMaxSize) * 100}%`}}
                                ></div>
                            </div>
                            <div class="cache-stats">
                                <span>{engine.cacheSize || 0}</span>
                                <span>/</span>
                                <span>{engine.cacheMaxSize || 0}</span>
                            </div>
                        </div>
                        <button class="action-btn danger" onClick={() => this.clearCache()}>
                            Clear Cache
                        </button>
                    </div>

                    {/* JVM Metrics */}
                    <div class="metrics-card">
                        <div class="card-header">
                            <span class="card-icon">☕</span>
                            <h3>JVM</h3>
                        </div>
                        <div class="metrics-list">
                            <div class="metric-item">
                                <span class="metric-label">Uptime</span>
                                <span class="metric-value">{this.formatUptime(jvm.uptimeMs || 0)}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Heap Used</span>
                                <span class="metric-value">{this.formatBytes(jvm.heapUsed || 0)}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Heap Max</span>
                                <span class="metric-value">{this.formatBytes(jvm.heapMax || 0)}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Processors</span>
                                <span class="metric-value">{jvm.availableProcessors}</span>
                            </div>
                        </div>
                        <div class="memory-bar">
                            <div
                                class="memory-fill"
                                style={{width: `${(jvm.heapUsed / jvm.heapMax) * 100}%`}}
                            ></div>
                        </div>
                    </div>

                    {/* System Info */}
                    <div class="metrics-card">
                        <div class="card-header">
                            <span class="card-icon">🖥️</span>
                            <h3>System</h3>
                        </div>
                        <div class="metrics-list">
                            <div class="metric-item">
                                <span class="metric-label">Java Version</span>
                                <span class="metric-value mono">{system.javaVersion}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Vendor</span>
                                <span class="metric-value">{system.javaVendor}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">OS</span>
                                <span class="metric-value">{system.osName}</span>
                            </div>
                            <div class="metric-item">
                                <span class="metric-label">Architecture</span>
                                <span class="metric-value mono">{system.osArch}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Actions */}
                <div class="actions-bar">
                    <button class="action-btn" onClick={() => this.resetMetrics()}>
                        🔄 Reset Metrics
                    </button>
                </div>
            </div>
        );
    }
}
