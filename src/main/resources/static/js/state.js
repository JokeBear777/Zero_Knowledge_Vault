async function checkMemberState() {
    const data = window.AuthGuard
        ? await window.AuthGuard.requireLogin()
        : await checkLogin();
    if (!data) return;

    if (data.pakeAuthStatus === "NOT_REGISTERED") {
        window.location.href = "/setup-mp.html";
        return;
    }

    if (data.pakeAuthStatus === "DISABLED") {
        window.location.href = "/account-disabled.html";
        return;
    }

    if (data.pakeAuthStatus === "ROTATING") {
        window.location.href = "/rotate-mp.html";
        return;
    }

    if (data.authLevel === "PRE_AUTH") {
        window.location.href = "/unlock.html";
        return;
    }

    const userInfo = document.getElementById("user-info");
    if (userInfo) {
        userInfo.className = "message message-success";
        userInfo.textContent = data.email + " 계정으로 로그인되어 있습니다.";
    }
}
