async function checkMemberState() {

    const data = await checkLogin();
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

    if (data.authAuthLevel === "PRE_AUTH") {
        window.location.href = "/unlock.html";
        return;
    }

    document.getElementById("user-info").innerText =
        "환영합니다: " + data.email;
}