(function () {
    const SENSITIVE_KEYS = [
        "zkv_device_secret_v1",
        "zkv_vault_key",
        "zkv_vault_key_v1",
        "zkv_srp_session",
        "zkv_unlock_state",
        "zkv_temp_vault_key"
    ];

    function removeKeys(storage, keys) {
        if (!storage) return;

        for (const key of keys) {
            try {
                storage.removeItem(key);
            } catch (e) {
                // Storage cleanup must not block logout.
            }
        }
    }

    function clearSensitiveStorage() {
        removeKeys(window.localStorage, SENSITIVE_KEYS);
        removeKeys(window.sessionStorage, SENSITIVE_KEYS);
    }

    function clearVaultStorage() {
        const keys = [
            "zkv_vault_key",
            "zkv_vault_key_v1",
            "zkv_temp_vault_key"
        ];
        removeKeys(window.localStorage, keys);
        removeKeys(window.sessionStorage, keys);
    }

    function clearSrpStorage() {
        const keys = [
            "zkv_device_secret_v1",
            "zkv_srp_session",
            "zkv_unlock_state"
        ];
        removeKeys(window.localStorage, keys);
        removeKeys(window.sessionStorage, keys);
    }

    window.StorageCleaner = {
        clearSensitiveStorage,
        clearVaultStorage,
        clearSrpStorage
    };
})();
