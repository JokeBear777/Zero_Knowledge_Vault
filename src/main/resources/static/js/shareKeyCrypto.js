(function () {
    const ALGORITHM = "RSA-OAEP-256";
    const VAULT_LOCKED_MESSAGE = "Vault를 먼저 열어주세요.";

    class ShareKeyVaultLockedError extends Error {
        constructor() {
            super(VAULT_LOCKED_MESSAGE);
            this.name = "ShareKeyVaultLockedError";
            this.code = "VAULT_KEY_REQUIRED";
        }
    }

    function getVaultKey() {
        return window.__zkvVaultKey || null;
    }

    function ensureVaultKey() {
        const vaultKey = getVaultKey();
        if (!vaultKey) {
            throw new ShareKeyVaultLockedError();
        }
        return vaultKey;
    }

    async function generateRsaOaepKeyPair() {
        return await crypto.subtle.generateKey(
            {
                name: "RSA-OAEP",
                modulusLength: 2048,
                publicExponent: new Uint8Array([1, 0, 1]),
                hash: "SHA-256"
            },
            true,
            ["encrypt", "decrypt"]
        );
    }

    async function exportPublicKeyBase64(publicKey) {
        const exported = await crypto.subtle.exportKey("spki", publicKey);
        return VaultCrypto.bytesToBase64(new Uint8Array(exported));
    }

    async function exportPrivateKeyBytes(privateKey) {
        const exported = await crypto.subtle.exportKey("pkcs8", privateKey);
        return new Uint8Array(exported);
    }

    async function encryptPrivateKeyForStorage(privateKeyBytes) {
        const vaultKey = ensureVaultKey();
        return await VaultCrypto.encryptAesGcm(vaultKey, privateKeyBytes);
    }

    async function createEncryptedShareKeyPayload() {
        ensureVaultKey();

        let privateKeyBytes = null;

        try {
            const keyPair = await generateRsaOaepKeyPair();
            const publicKeyBase64 = await exportPublicKeyBase64(keyPair.publicKey);
            privateKeyBytes = await exportPrivateKeyBytes(keyPair.privateKey);
            const encryptedPrivateKeyBase64 = await encryptPrivateKeyForStorage(privateKeyBytes);

            return {
                publicKeyBase64,
                encryptedPrivateKeyBase64,
                algorithm: ALGORITHM
            };
        } finally {
            if (privateKeyBytes) {
                privateKeyBytes.fill(0);
            }
        }
    }

    function isVaultLockedError(error) {
        return error instanceof ShareKeyVaultLockedError || error?.code === "VAULT_KEY_REQUIRED";
    }

    window.ShareKeyCrypto = {
        ALGORITHM,
        ShareKeyVaultLockedError,
        isVaultLockedError,
        generateRsaOaepKeyPair,
        exportPublicKeyBase64,
        exportPrivateKeyBytes,
        encryptPrivateKeyForStorage,
        createEncryptedShareKeyPayload
    };
})();
