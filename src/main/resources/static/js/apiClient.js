(function () {
    const MESSAGE = {
        unauthorized: "로그인이 필요합니다.",
        forbidden: "Vault 접근을 위해 잠금 해제가 필요합니다.",
        conflict: "다른 기기에서 먼저 수정했습니다. 최신 상태를 불러온 뒤 다시 시도해주세요.",
        server: "일시적인 서버 오류가 발생했습니다.",
        network: "네트워크 오류가 발생했습니다. 연결을 확인해주세요.",
        badRequest: "요청을 처리할 수 없습니다."
    };

    class ApiError extends Error {
        constructor(message, status, body) {
            super(message);
            this.name = "ApiError";
            this.status = status;
            this.body = body;
            this.userMessage = message;
            this.isConflict = status === 409;
        }
    }

    async function parseBody(response) {
        const contentType = response.headers.get("content-type") || "";

        if (contentType.includes("application/json")) {
            try {
                return await response.json();
            } catch (e) {
                return null;
            }
        }

        try {
            return await response.text();
        } catch (e) {
            return null;
        }
    }

    function redirectTo(path) {
        if (window.location.pathname !== path) {
            window.location.href = path;
        }
    }

    function handleApiError(response, responseBody, path, options) {
        const status = response.status;
        const shouldRedirect = options.redirectOnAuthError !== false;

        if (status === 401) {
            if (shouldRedirect) {
                redirectTo("/login.html");
            }
            throw new ApiError(MESSAGE.unauthorized, status, responseBody);
        }

        if (status === 403) {
            if (shouldRedirect && String(path).startsWith("/api/vault/")) {
                redirectTo("/unlock.html");
            }
            throw new ApiError(MESSAGE.forbidden, status, responseBody);
        }

        if (status === 409) {
            throw new ApiError(MESSAGE.conflict, status, responseBody);
        }

        if (status >= 500) {
            throw new ApiError(MESSAGE.server, status, responseBody);
        }

        throw new ApiError(MESSAGE.badRequest, status, responseBody);
    }

    async function request(path, options = {}) {
        const fetchOptions = { ...options };
        const body = fetchOptions.body;

        delete fetchOptions.redirectOnAuthError;

        fetchOptions.credentials = fetchOptions.credentials || "include";
        fetchOptions.headers = {
            ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
            ...(fetchOptions.headers || {})
        };

        if (body !== undefined && typeof body !== "string" && !(body instanceof FormData)) {
            fetchOptions.body = JSON.stringify(body);
        }

        try {
            const response = await fetch(path, fetchOptions);
            const responseBody = await parseBody(response);

            if (!response.ok) {
                handleApiError(response, responseBody, path, options);
            }

            return responseBody;
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }

            throw new ApiError(MESSAGE.network, 0, null);
        }
    }

    function get(path, options = {}) {
        return request(path, { ...options, method: "GET" });
    }

    function post(path, body, options = {}) {
        return request(path, { ...options, method: "POST", body });
    }

    function put(path, body, options = {}) {
        return request(path, { ...options, method: "PUT", body });
    }

    function patch(path, body, options = {}) {
        return request(path, { ...options, method: "PATCH", body });
    }

    function del(path, body, options = {}) {
        return request(path, { ...options, method: "DELETE", body });
    }

    window.APIClient = {
        ApiError,
        request,
        get,
        post,
        put,
        patch,
        del,
        handleApiError
    };
})();
