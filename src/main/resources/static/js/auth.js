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

async function logout() {
    await fetch("/api/logout", {
        method: "POST",
        credentials: "include"
    });

    window.location.href = "/login.html";
}