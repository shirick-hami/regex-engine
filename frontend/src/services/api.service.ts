import {getApiUrl} from "../utils/env.service";

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface ApiRequestOptions {
    method?: HttpMethod;
    body?: unknown;
    headers?: Record<string, string>;
    query?: Record<string, string | number | boolean>;
}

/**
 * Custom API error class with structured error information
 */
export class ApiError extends Error {
    constructor(
        public readonly status: number,
        public readonly statusText: string,
        message: string,
        public readonly data?: unknown
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

/**
 * Makes an HTTP request to the API
 * @param uri - The endpoint URI (will be appended to base API URL)
 * @param options - Request options (method, body, headers, query params)
 * @returns Promise resolving to the typed response data
 * @throws ApiError for non-OK responses
 */
export async function apiRequest<T>(
    uri: string,
    options: ApiRequestOptions = {}
): Promise<T> {
    const {method = 'GET', body, headers, query} = options;

    let finalUrl = getApiUrl() + uri;

    // Query param handling
    if (query && Object.keys(query).length > 0) {
        const params = new URLSearchParams();
        Object.entries(query).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                params.append(key, String(value));
            }
        });
        const queryString = params.toString();
        if (queryString) {
            finalUrl += `?${queryString}`;
        }
    }

    const response = await fetch(finalUrl, {
        method,
        headers: {
            ...(body ? {'Content-Type': 'application/json'} : {}),
            ...(headers ?? {})
        },
        body: body ? JSON.stringify(body) : undefined,
    });

    // Handle non-OK responses
    if (!response.ok) {
        let errorMessage = response.statusText;
        let errorData: unknown = undefined;

        try {
            // Try to parse error response as JSON
            const errorJson = await response.json();
            errorMessage = errorJson.message || errorJson.error || response.statusText;
            errorData = errorJson;
        } catch {
            // If JSON parsing fails, try to get text
            try {
                errorMessage = await response.text() || response.statusText;
            } catch {
                // Use default statusText
            }
        }

        throw new ApiError(response.status, response.statusText, errorMessage, errorData);
    }

    // Handle empty response (204 No Content)
    if (response.status === 204) {
        return null as T;
    }

    // Handle responses with no content
    const contentLength = response.headers.get('content-length');
    if (contentLength === '0') {
        return null as T;
    }

    return response.json();
}
