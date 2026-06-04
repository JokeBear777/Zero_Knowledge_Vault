(function () {
    function redirectToLogin() {
        window.location.href = "/login.html";
    }

    function redirectToUnlock() {
        window.location.href = "/unlock.html";
    }

    function redirectToVaultSetup() {
        window.location.href = "/vault-setup.html";
    }

    function redirectToVault() {
        window.location.href = "/vault.html";
    }

    async function getCurrentMember(options = {}) {
        if (window.APIClient) {
            try {
                return await window.APIClient.get("/api/member/me", {
                    redirectOnAuthError: options.redirectOnAuthError !== false
                });
            } catch (error) {
                if (error.status === 401) {
                    return null;
                }
                throw error;
            }
        }

        const response = await fetch("/api/member/me", {
            credentials: "include"
        });

        if (!response.ok) {
            if (options.redirectOnAuthError !== false) {
                redirectToLogin();
            }
            return null;
        }

        return await response.json();
    }

    async function requireLogin() {
        return await getCurrentMember({ redirectOnAuthError: true });
    }

    async function requirePreAuth() {
        const member = await requireLogin();
        if (!member) return null;

        if (member.authLevel !== "PRE_AUTH") {
            redirectByMemberState(member);
            return null;
        }

        return member;
    }

    async function requireVaultAuth() {
        const member = await requireLogin();
        if (!member) return null;

        if (member.authLevel !== "VAULT_AUTH") {
            redirectToUnlock();
            return null;
        }

        return member;
    }

    function redirectByMemberState(member) {
        if (!member) {
            redirectToLogin();
            return;
        }

        if (member.pakeAuthStatus === "NOT_REGISTERED") {
            window.location.href = "/setup-mp.html";
            return;
        }

        if (member.pakeAuthStatus === "DISABLED") {
            window.location.href = "/account-disabled.html";
            return;
        }

        if (member.pakeAuthStatus === "ROTATING") {
            window.location.href = "/rotate-mp.html";
            return;
        }

        if (member.authLevel === "PRE_AUTH") {
            redirectToUnlock();
            return;
        }

        window.location.href = "/home.html";
    }

    window.AuthGuard = {
        getCurrentMember,
        requireLogin,
        requirePreAuth,
        requireVaultAuth,
        redirectByMemberState,
        redirectToLogin,
        redirectToUnlock,
        redirectToVaultSetup,
        redirectToVault
    };
})();
