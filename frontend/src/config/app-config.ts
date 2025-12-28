export interface AppConfig {
    backendBaseUrl: string;
    authEnabled: boolean;
    logLevel: 'debug' | 'info' | 'warn' | 'error';
    creatorName: string;
    githubUrl: string;
    gitCloneUrl: string;
}