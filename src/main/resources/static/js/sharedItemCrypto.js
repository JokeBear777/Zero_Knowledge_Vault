(function () {
    const ITEM_CIPHER_ALGORITHM = "AES-GCM-256";

    class SharedItemCryptoError extends Error {
        constructor(code, message) {
            super(message);
            this.name = "SharedItemCryptoError";
            this.code = code;
        }
    }

    function requireVaultKey() {
        if (!window.__zkvVaultKey) {
            throw new SharedItemCryptoError("VAULT_KEY_REQUIRED", "Vault 잠금 해제가 필요합니다.");
        }
        return window.__zkvVaultKey;
    }

    function requireShareKeyResponse(response) {
        if (!response?.exists || !response.publicKeyBase64 || !response.encryptedPrivateKeyBase64) {
            throw new SharedItemCryptoError("SHARE_KEY_REQUIRED", "공유키 생성이 필요합니다.");
        }
        return response;
    }

    async function getMyShareKey() {
        return requireShareKeyResponse(await APIClient.get("/api/share/keys/me"));
    }

    async function importPublicKey(publicKeyBase64) {
        return await crypto.subtle.importKey(
            "spki",
            VaultCrypto.base64ToBytes(publicKeyBase64),
            { name: "RSA-OAEP", hash: "SHA-256" },
            false,
            ["encrypt"]
        );
    }

    async function importPrivateKey(privateKeyBytes) {
        return await crypto.subtle.importKey(
            "pkcs8",
            privateKeyBytes,
            { name: "RSA-OAEP", hash: "SHA-256" },
            false,
            ["decrypt"]
        );
    }

    async function decryptMySharePrivateKey() {
        const vaultKey = requireVaultKey();
        const shareKey = await getMyShareKey();
        let privateKeyBytes = null;

        try {
            privateKeyBytes = await VaultCrypto.decryptAesGcm(vaultKey, shareKey.encryptedPrivateKeyBase64);
            const privateKey = await importPrivateKey(privateKeyBytes);
            window.__zkvSharePrivateKey = privateKey;
            return privateKey;
        } finally {
            if (privateKeyBytes) {
                privateKeyBytes.fill(0);
            }
        }
    }

    async function loadMySharePrivateKey() {
        if (window.__zkvSharePrivateKey) {
            return window.__zkvSharePrivateKey;
        }
        return await decryptMySharePrivateKey();
    }

    async function generateSharedItemKey() {
        return await VaultCrypto.generateAesKey(["encrypt", "decrypt"]);
    }

    async function encryptSharedItemText(sharedItemKey, plainText) {
        return await VaultCrypto.encryptAesGcm(
            sharedItemKey,
            VaultCrypto.utf8ToBytes(String(plainText || ""))
        );
    }

    async function decryptSharedItemText(sharedItemKey, cipherBase64) {
        const plainBytes = await VaultCrypto.decryptAesGcm(sharedItemKey, cipherBase64);
        return VaultCrypto.bytesToUtf8(plainBytes);
    }

    async function encryptSharedItemKeyForPublicKey(sharedItemKey, publicKeyBase64) {
        const publicKey = await importPublicKey(publicKeyBase64);
        let rawKey = null;

        try {
            rawKey = await VaultCrypto.exportRawKey(sharedItemKey);
            const encrypted = await crypto.subtle.encrypt(
                { name: "RSA-OAEP" },
                publicKey,
                rawKey
            );
            return VaultCrypto.bytesToBase64(new Uint8Array(encrypted));
        } finally {
            if (rawKey) {
                rawKey.fill(0);
            }
        }
    }

    async function decryptSharedItemKeyWithPrivateKey(encryptedItemKeyBase64, privateKey) {
        let rawKey = null;

        try {
            rawKey = new Uint8Array(await crypto.subtle.decrypt(
                { name: "RSA-OAEP" },
                privateKey,
                VaultCrypto.base64ToBytes(encryptedItemKeyBase64)
            ));
            return await VaultCrypto.importAesKey(rawKey, ["encrypt", "decrypt"]);
        } finally {
            if (rawKey) {
                rawKey.fill(0);
            }
        }
    }

    async function encryptItemPayload(title, content, sharedItemKey) {
        return {
            titleCipherBase64: await encryptSharedItemText(sharedItemKey, title),
            itemCipherBase64: await encryptSharedItemText(sharedItemKey, content)
        };
    }

    async function wrapSharedItemKeyForMember(sharedItemKey, publicKeyBase64) {
        return await encryptSharedItemKeyForPublicKey(sharedItemKey, publicKeyBase64);
    }

    async function createRotatedSharedItemPayload(title, content, members) {
        let newSharedItemKey = await generateSharedItemKey();

        try {
            const encryptedPayload = await encryptItemPayload(title, content, newSharedItemKey);
            const memberKeyWrappers = [];

            for (const member of members || []) {
                memberKeyWrappers.push({
                    memberId: member.memberId,
                    recipientKeyVersion: member.publicKeyVersion,
                    encryptedItemKeyBase64: await wrapSharedItemKeyForMember(
                        newSharedItemKey,
                        member.publicKeyBase64
                    )
                });
            }

            return {
                titleCipherBase64: encryptedPayload.titleCipherBase64,
                itemCipherBase64: encryptedPayload.itemCipherBase64,
                memberKeyWrappers
            };
        } finally {
            newSharedItemKey = null;
        }
    }

    async function createOwnerSharedItemPayload(title, content) {
        const shareKey = await getMyShareKey();
        const sharedItemKey = await generateSharedItemKey();
        const encryptedPayload = await encryptItemPayload(title, content, sharedItemKey);
        const ownerEncryptedItemKeyBase64 = await encryptSharedItemKeyForPublicKey(
            sharedItemKey,
            shareKey.publicKeyBase64
        );

        return {
            titleCipherBase64: encryptedPayload.titleCipherBase64,
            itemCipherBase64: encryptedPayload.itemCipherBase64,
            itemCipherAlgorithm: ITEM_CIPHER_ALGORITHM,
            ownerEncryptedItemKeyBase64,
            ownerKeyVersion: shareKey.keyVersion
        };
    }

    async function decryptSharedItem(item, options = {}) {
        const privateKey = await loadMySharePrivateKey();
        const sharedItemKey = await decryptSharedItemKeyWithPrivateKey(item.encryptedItemKeyBase64, privateKey);
        const title = item.titleCipherBase64
            ? await decryptSharedItemText(sharedItemKey, item.titleCipherBase64)
            : "";

        if (!options.includeContent) {
            return { item, title, sharedItemKey: null };
        }

        const content = item.itemCipherBase64
            ? await decryptSharedItemText(sharedItemKey, item.itemCipherBase64)
            : "";

        return { item, title, content, sharedItemKey };
    }

    function isVaultKeyRequired(error) {
        return error?.code === "VAULT_KEY_REQUIRED" || ShareKeyCrypto?.isVaultLockedError?.(error);
    }

    function isShareKeyRequired(error) {
        return error?.code === "SHARE_KEY_REQUIRED";
    }

    window.SharedItemCrypto = {
        ITEM_CIPHER_ALGORITHM,
        SharedItemCryptoError,
        isVaultKeyRequired,
        isShareKeyRequired,
        generateSharedItemKey,
        encryptSharedItemText,
        decryptSharedItemText,
        encryptSharedItemKeyForPublicKey,
        wrapSharedItemKeyForMember,
        decryptSharedItemKeyWithPrivateKey,
        loadMySharePrivateKey,
        decryptMySharePrivateKey,
        encryptItemPayload,
        createRotatedSharedItemPayload,
        createOwnerSharedItemPayload,
        decryptSharedItem
    };
})();
