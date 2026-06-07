(function () {
    const PENDING_PAYLOAD_KEY = "zkv_pending_share_key_payload";
    const PENDING_PAYLOAD_TTL_MS = 60 * 1000;
    const ALLOWED_INTENTS = new Set([
        "create-share-key",
        "regenerate-share-key"
    ]);

    function normalizeIntent(value) {
        return ALLOWED_INTENTS.has(value) ? value : null;
    }

    function buildUnlockUrl(intent) {
        const normalizedIntent = normalizeIntent(intent);
        const intentQuery = normalizedIntent ? "&intent=" + encodeURIComponent(normalizedIntent) : "";
        return "/unlock.html?next=/share.html" + intentQuery;
    }

    function isValidPayload(payload) {
        return Boolean(
            payload
            && typeof payload.publicKeyBase64 === "string"
            && payload.publicKeyBase64.length > 0
            && typeof payload.encryptedPrivateKeyBase64 === "string"
            && payload.encryptedPrivateKeyBase64.length > 0
            && payload.algorithm === ShareKeyCrypto.ALGORITHM
        );
    }

    function sanitizePayload(payload) {
        return {
            publicKeyBase64: payload.publicKeyBase64,
            encryptedPrivateKeyBase64: payload.encryptedPrivateKeyBase64,
            algorithm: payload.algorithm
        };
    }

    function storePendingPayload(intent, payload) {
        const normalizedIntent = normalizeIntent(intent);
        if (!normalizedIntent || !isValidPayload(payload)) {
            throw new Error("공유키 작업 정보를 준비할 수 없습니다.");
        }

        sessionStorage.setItem(PENDING_PAYLOAD_KEY, JSON.stringify({
            intent: normalizedIntent,
            expiresAt: Date.now() + PENDING_PAYLOAD_TTL_MS,
            payload: sanitizePayload(payload)
        }));
    }

    function consumePendingPayload(intent) {
        const normalizedIntent = normalizeIntent(intent);
        let raw;

        try {
            raw = sessionStorage.getItem(PENDING_PAYLOAD_KEY);
            sessionStorage.removeItem(PENDING_PAYLOAD_KEY);
        } catch (error) {
            return null;
        }

        if (!normalizedIntent || !raw) {
            return null;
        }

        try {
            const pending = JSON.parse(raw);
            if (
                pending.intent !== normalizedIntent
                || Number(pending.expiresAt) < Date.now()
                || !isValidPayload(pending.payload)
            ) {
                return null;
            }
            return sanitizePayload(pending.payload);
        } catch (error) {
            return null;
        }
    }

    function clearPendingPayload() {
        try {
            sessionStorage.removeItem(PENDING_PAYLOAD_KEY);
        } catch (error) {
            // Pending encrypted payload cleanup must not block navigation.
        }
    }

    window.ShareKeyIntent = {
        PENDING_PAYLOAD_KEY,
        normalizeIntent,
        buildUnlockUrl,
        storePendingPayload,
        consumePendingPayload,
        clearPendingPayload
    };
})();
