import {AppConfig} from './app-config';

declare global {
    interface Window {
        __APP_CONFIG__?: Partial<AppConfig>;
    }
}

export function getRuntimeConfig(): Partial<AppConfig> {
    return window.__APP_CONFIG__ ?? {};
}