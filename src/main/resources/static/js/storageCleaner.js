(function () {
    const SENSITIVE_KEYS = [
        "zkv_vault_key",
        "zkv_vault_key_v1",
        "zkv_srp_session",
        "zkv_unlock_state",
        "zkv_temp_vault_key",
        "zkv_pending_share_key_payload"
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
            "zkv_temp_vault_key",
            "zkv_pending_share_key_payload"
        ];
        removeKeys(window.localStorage, keys);
        removeKeys(window.sessionStorage, keys);
    }

    function clearSrpStorage() {
        const keys = [
            "zkv_srp_session",
            "zkv_unlock_state"
        ];
        removeKeys(window.localStorage, keys);
        removeKeys(window.sessionStorage, keys);
    }

    function clearDeviceSecretForDeviceReset() {
        // Deleting this value breaks the existing MP verifier binding for this browser.
        // Recovery requires MP re-registration or development data reset.
        removeKeys(window.localStorage, ["zkv_device_secret_v1"]);
        removeKeys(window.sessionStorage, ["zkv_device_secret_v1"]);
    }

    window.StorageCleaner = {
        clearSensitiveStorage,
        clearVaultStorage,
        clearSrpStorage,
        clearDeviceSecretForDeviceReset
    };
})();
