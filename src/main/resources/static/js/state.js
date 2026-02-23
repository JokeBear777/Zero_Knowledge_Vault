async function checkMemberState() {

    const data = await checkLogin();
    if (!data) return;

    if (data.pakeStatus === "NOT_REGISTERED") {
        window.location.href = "/setup-mp.html";
        return;
    }

    if (data.pakeStatus === "DISABLED") {
        window.location.href = "/account-disabled.html";
        return;
    }

    if (data.pakeStatus === "ROTATING") {
        window.location.href = "/rotate-mp.html";
        return;
    }

    if (data.authLevel === "PRE_AUTH") {
        window.location.href = "/unlock.html";
        return;
    }

    document.getElementById("user-info").innerText =
        "환영합니다: " + data.email;
}