import {apiRequest, ApiRequestOptions} from "./api.service";

// Type for query parameters
type QueryParams = ApiRequestOptions['query'];

/**
 * HTTP helper object providing convenient methods for common HTTP operations.
 * All methods automatically handle JSON serialization and base URL configuration.
 *
 * @example
 * // Simple GET
 * const data = await http.get<User[]>('/users');
 *
 * // GET with query params
 * const users = await http.get<User[]>('/users', { page: 1, limit: 10 });
 *
 * // POST with body
 * const created = await http.post<User>('/users', { name: 'John' });
 *
 * // POST with query params and body
 * const result = await http.post<Result>('/match', { pattern: '.*' }, { engine: 'NFA' });
 */
export const http = {
    /**
     * Performs a GET request
     * @param url - Endpoint URL
     * @param query - Optional query parameters
     * @param headers - Optional custom headers
     */
    get: <T>(url: string, query?: QueryParams, headers?: Record<string, string>): Promise<T> =>
        apiRequest<T>(url, {method: 'GET', query, headers}),

    /**
     * Performs a POST request
     * @param url - Endpoint URL
     * @param body - Optional request body (will be JSON stringified)
     * @param queryOrHeaders - Optional query parameters OR headers (if no query needed)
     * @param headers - Optional custom headers (when query is provided)
     */
    post: <T>(
        url: string,
        body?: unknown,
        queryOrHeaders?: QueryParams | Record<string, string>,
        headers?: Record<string, string>
    ): Promise<T> => {
        // Detect if third param is query or headers
        // Query params have string|number|boolean values, headers are always string
        const isQuery = queryOrHeaders && Object.values(queryOrHeaders).some(
            v => typeof v === 'number' || typeof v === 'boolean'
        );

        if (isQuery || headers) {
            // Called with query params: post(url, body, query, headers?)
            return apiRequest<T>(url, {
                method: 'POST',
                body,
                query: queryOrHeaders as QueryParams,
                headers
            });
        } else {
            // Called without query params: post(url, body, headers?)
            return apiRequest<T>(url, {
                method: 'POST',
                body,
                headers: queryOrHeaders as Record<string, string>
            });
        }
    },

    /**
     * Performs a PUT request
     * @param url - Endpoint URL
     * @param body - Optional request body (will be JSON stringified)
     * @param queryOrHeaders - Optional query parameters OR headers
     * @param headers - Optional custom headers (when query is provided)
     */
    put: <T>(
        url: string,
        body?: unknown,
        queryOrHeaders?: QueryParams | Record<string, string>,
        headers?: Record<string, string>
    ): Promise<T> => {
        const isQuery = queryOrHeaders && Object.values(queryOrHeaders).some(
            v => typeof v === 'number' || typeof v === 'boolean'
        );

        if (isQuery || headers) {
            return apiRequest<T>(url, {
                method: 'PUT',
                body,
                query: queryOrHeaders as QueryParams,
                headers
            });
        } else {
            return apiRequest<T>(url, {
                method: 'PUT',
                body,
                headers: queryOrHeaders as Record<string, string>
            });
        }
    },

    /**
     * Performs a PATCH request
     * @param url - Endpoint URL
     * @param body - Optional request body (will be JSON stringified)
     * @param queryOrHeaders - Optional query parameters OR headers
     * @param headers - Optional custom headers (when query is provided)
     */
    patch: <T>(
        url: string,
        body?: unknown,
        queryOrHeaders?: QueryParams | Record<string, string>,
        headers?: Record<string, string>
    ): Promise<T> => {
        const isQuery = queryOrHeaders && Object.values(queryOrHeaders).some(
            v => typeof v === 'number' || typeof v === 'boolean'
        );

        if (isQuery || headers) {
            return apiRequest<T>(url, {
                method: 'PATCH',
                body,
                query: queryOrHeaders as QueryParams,
                headers
            });
        } else {
            return apiRequest<T>(url, {
                method: 'PATCH',
                body,
                headers: queryOrHeaders as Record<string, string>
            });
        }
    },

    /**
     * Performs a DELETE request
     * @param url - Endpoint URL
     * @param queryOrHeaders - Optional query parameters OR headers
     * @param headers - Optional custom headers (when query is provided)
     */
    delete: <T>(
        url: string,
        queryOrHeaders?: QueryParams | Record<string, string>,
        headers?: Record<string, string>
    ): Promise<T> => {
        const isQuery = queryOrHeaders && Object.values(queryOrHeaders).some(
            v => typeof v === 'number' || typeof v === 'boolean'
        );

        if (isQuery || headers) {
            return apiRequest<T>(url, {
                method: 'DELETE',
                query: queryOrHeaders as QueryParams,
                headers
            });
        } else {
            return apiRequest<T>(url, {
                method: 'DELETE',
                headers: queryOrHeaders as Record<string, string>
            });
        }
    },
};
