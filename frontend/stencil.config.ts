import {Config} from '@stencil/core';

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
            serviceWorker: null,
            copy: []
        },
    ],
    globalStyle: 'src/global/app.css',
};
