import {Config} from '@stencil/core';
import replace from "@rollup/plugin-replace";

const isProd = process.env.NODE_ENV === 'production';

export const config: Config = {
    namespace: 'regex-engine',
    outputTargets: [
        {
            type: 'dist',
            esmLoaderPath: '../loader',
        },
        {
            type: 'dist-custom-elements',
        },
        {
            type: 'www',
            dir: 'www',                                    // Output directly to www/
            baseUrl: isProd ? '/regex-engine/' : '/',
            serviceWorker: null,
            empty: true,                                   // Clean the folder before build
        },
    ],
    rollupPlugins: {
        before: [
            replace({
                preventAssignment: true,
                values: {
                    // Replace the import path based on environment
                    '../environments/environment': isProd
                        ? '../environments/environment.prod'
                        : '../environments/environment',
                    './environments/environment': isProd
                        ? './environments/environment.prod'
                        : './environments/environment',
                },
            }),
        ],
    },
    globalStyle: 'src/global/app.css',
};
