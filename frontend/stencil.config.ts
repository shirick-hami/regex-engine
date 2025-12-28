import {Config} from '@stencil/core';
import replace from "@rollup/plugin-replace";

const isProd = process.env.NODE_ENV === 'production';

export const config: Config = {
    namespace: 'regex-engine-ui',
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
            baseUrl: isProd ? '/regex-engine/' : '/',
            serviceWorker: null,
            copy: []
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
