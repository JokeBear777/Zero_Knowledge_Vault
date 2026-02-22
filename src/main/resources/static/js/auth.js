async function checkLogin() {
    const res = await fetch("/api/member/me", {
        credentials: "include"
    });

    if (!res.ok) {
        window.location.href = "/login.html";
    }
}

window.onload = checkLogin;

function goLogin() {
    window.location.href = "/oauth2/authorization/naver";
}

async function logout() {
    await fetch("/api/logout", {
        method: "POST",
        credentials: "include"
    });

    alert("로그아웃 완료");
    window.location.href = "/login.html";
}