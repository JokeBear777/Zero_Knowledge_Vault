function getTokenFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("token");
}

(function init() {
    const token = getTokenFromUrl();

    if (token) {
        localStorage.setItem("accessToken", token);
        window.history.replaceState({}, document.title, "/");
        window.location.href = "/home.html";
        return;
    }

})();

function goLogin() {
    window.location.href = "/login.html";
}

function logout() {
    localStorage.removeItem("accessToken");
    alert("로그아웃 완료");
    location.reload();
}


