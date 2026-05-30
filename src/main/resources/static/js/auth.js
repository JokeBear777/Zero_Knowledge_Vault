async function checkLogin() {
    const res = await fetch("/api/member/me", {
        credentials: "include"
    });

    if (!res.ok) {
        window.location.href = "/login.html";
        return null;
    }

    return await res.json();
}

function clearSensitiveBrowserState() {
    const sensitiveKeys = [
        "zkv_device_secret_v1",
        "zkv_vault_key",
        "zkv_vault_key_v1",
        "zkv_srp_session",
        "zkv_unlock_state",
        "zkv_temp_vault_key"
    ];

    for (const key of sensitiveKeys) {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    }
}

async function logout() {
    try {
        await fetch("/api/logout", {
            method: "POST",
            credentials: "include"
        });
    } finally {
        clearSensitiveBrowserState();
    }

    window.location.href = "/login.html";
}
