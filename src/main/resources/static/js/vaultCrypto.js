(function () {
    const ENVELOPE_VERSION = 1;
    const IV_BYTES = 12;
    const DEFAULT_PBKDF2_ITERATIONS = 310000;
    const DEFAULT_ARGON2_MEMORY = 65536;
    const DEFAULT_ARGON2_ITERATIONS = 3;
    const DEFAULT_ARGON2_PARALLELISM = 1;

    function utf8ToBytes(text) {
        return new TextEncoder().encode(text);
    }

    function bytesToUtf8(bytes) {
        return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    }

    function bytesToBase64(bytes) {
        const view = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
        let binary = "";
        const chunkSize = 0x8000;

        for (let i = 0; i < view.length; i += chunkSize) {
            const chunk = view.subarray(i, i + chunkSize);
            binary += String.fromCharCode(...chunk);
        }

        return btoa(binary);
    }

    function base64ToBytes(base64) {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);

        for (let i = 0; i < binary.length; i += 1) {
            bytes[i] = binary.charCodeAt(i);
        }

        return bytes;
    }

    async function generateAesKey(usages = ["encrypt", "decrypt"]) {
        return await crypto.subtle.generateKey(
            { name: "AES-GCM", length: 256 },
            true,
            usages
        );
    }

    async function importAesKey(rawBytes, usages = ["encrypt", "decrypt"]) {
        return await crypto.subtle.importKey(
            "raw",
            rawBytes,
            { name: "AES-GCM" },
            true,
            usages
        );
    }

    async function exportRawKey(cryptoKey) {
        return new Uint8Array(await crypto.subtle.exportKey("raw", cryptoKey));
    }

    async function encryptAesGcm(cryptoKey, plaintextBytes) {
        const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
        const ciphertext = new Uint8Array(await crypto.subtle.encrypt(
            { name: "AES-GCM", iv },
            cryptoKey,
            plaintextBytes
        ));
        const envelope = new Uint8Array(1 + IV_BYTES + ciphertext.length);

        envelope[0] = ENVELOPE_VERSION;
        envelope.set(iv, 1);
        envelope.set(ciphertext, 1 + IV_BYTES);

        return bytesToBase64(envelope);
    }

    async function decryptAesGcm(cryptoKey, envelopeBase64) {
        const envelope = base64ToBytes(envelopeBase64);

        if (envelope.length <= 1 + IV_BYTES || envelope[0] !== ENVELOPE_VERSION) {
            throw new Error("Unsupported vault cipher format.");
        }

        const iv = envelope.subarray(1, 1 + IV_BYTES);
        const ciphertext = envelope.subarray(1 + IV_BYTES);

        return new Uint8Array(await crypto.subtle.decrypt(
            { name: "AES-GCM", iv },
            cryptoKey,
            ciphertext
        ));
    }

    async function sha256Base64(bytes) {
        const digest = await crypto.subtle.digest("SHA-256", bytes);
        return bytesToBase64(new Uint8Array(digest));
    }

    function normalizeKdfAlgorithm(algorithm) {
        return String(algorithm || "PBKDF2").toUpperCase();
    }

    async function derivePbkdf2Key(masterPassword, saltBytes, wrapKdfParams) {
        const iterations = Number(wrapKdfParams?.iterations || DEFAULT_PBKDF2_ITERATIONS);
        const keyMaterial = await crypto.subtle.importKey(
            "raw",
            utf8ToBytes(masterPassword),
            { name: "PBKDF2" },
            false,
            ["deriveBits"]
        );
        const bits = await crypto.subtle.deriveBits(
            {
                name: "PBKDF2",
                hash: "SHA-256",
                salt: saltBytes,
                iterations
            },
            keyMaterial,
            256
        );

        return await importAesKey(new Uint8Array(bits));
    }

    async function deriveArgon2idKey(masterPassword, saltBytes, wrapKdfParams) {
        if (!window.argon2) {
            throw new Error("Argon2 runtime is not available.");
        }

        const result = await window.argon2.hash({
            pass: masterPassword,
            salt: saltBytes,
            time: Number(wrapKdfParams?.iterations || DEFAULT_ARGON2_ITERATIONS),
            mem: Number(wrapKdfParams?.memory || DEFAULT_ARGON2_MEMORY),
            parallelism: Number(wrapKdfParams?.parallelism || DEFAULT_ARGON2_PARALLELISM),
            hashLen: 32,
            type: window.argon2.ArgonType.Argon2id,
            raw: true
        });

        return await importAesKey(new Uint8Array(result.hash));
    }

    async function deriveWrappingKey(masterPassword, saltWrapBase64, wrapKdfParams, wrapKdfAlgorithm) {
        const saltBytes = base64ToBytes(saltWrapBase64);
        const algorithm = normalizeKdfAlgorithm(wrapKdfAlgorithm);

        if (algorithm === "ARGON2ID") {
            return await deriveArgon2idKey(masterPassword, saltBytes, wrapKdfParams);
        }

        if (algorithm === "PBKDF2" || algorithm === "PBKDF2_SHA256") {
            return await derivePbkdf2Key(masterPassword, saltBytes, wrapKdfParams);
        }

        throw new Error("Unsupported wrapping KDF.");
    }

    async function wrapVaultKey(vaultKey, wrappingKey) {
        const rawVaultKey = await exportRawKey(vaultKey);
        return await encryptAesGcm(wrappingKey, rawVaultKey);
    }

    async function unwrapVaultKey(wrappedVaultKeyBase64, wrappingKey) {
        const rawVaultKey = await decryptAesGcm(wrappingKey, wrappedVaultKeyBase64);
        return await importAesKey(rawVaultKey);
    }

    async function encryptJson(cryptoKey, object) {
        return await encryptAesGcm(cryptoKey, utf8ToBytes(JSON.stringify(object)));
    }

    async function decryptJson(cryptoKey, cipherBase64) {
        const plaintextBytes = await decryptAesGcm(cryptoKey, cipherBase64);
        return JSON.parse(bytesToUtf8(plaintextBytes));
    }

    async function generateCommitHash(indexCipherBase64) {
        return await sha256Base64(base64ToBytes(indexCipherBase64));
    }

    window.VaultCrypto = {
        utf8ToBytes,
        bytesToUtf8,
        bytesToBase64,
        base64ToBytes,
        generateAesKey,
        importAesKey,
        exportRawKey,
        encryptAesGcm,
        decryptAesGcm,
        sha256Base64,
        deriveWrappingKey,
        wrapVaultKey,
        unwrapVaultKey,
        encryptJson,
        decryptJson,
        generateCommitHash
    };
})();
