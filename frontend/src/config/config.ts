import {AppConfig} from './app-config';
import {DEFAULT_CONFIG} from './default-config';
import {getRuntimeConfig} from './runtime-config';

let cachedConfig: AppConfig | null = null;

export function getAppConfig(): AppConfig {
    if (cachedConfig) return cachedConfig;

    cachedConfig = {
        ...DEFAULT_CONFIG,
        ...getRuntimeConfig(),
    };

    return cachedConfig;
}