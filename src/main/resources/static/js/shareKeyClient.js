(function () {
    let bound = false;
    let currentState = null;
    let actionInProgress = false;
    let intentHandled = false;

    function byId(id) {
        return document.getElementById(id);
    }

    function showShareMessage(type, text) {
        const message = byId("share-message");
        if (!message) return;

        message.className = "message message-" + type;
        message.textContent = text;
    }

    function setButtonBusy(button, busy) {
        if (!button) return;
        button.disabled = busy;
    }

    function setActionButtonsBusy(busy) {
        setButtonBusy(byId("createShareKeyBtn"), busy);
        setButtonBusy(byId("regenerateShareKeyBtn"), busy);
    }

    function readShareIntent() {
        const params = new URLSearchParams(window.location.search);
        const intent = ShareKeyIntent.normalizeIntent(params.get("intent"));

        if (params.has("intent")) {
            history.replaceState(null, "", "/share.html");
        }

        return intent;
    }

    function formatDate(value) {
        if (!value) return "-";

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "-";
        }

        return date.toLocaleString();
    }

    function addMetaRow(list, label, value) {
        const row = document.createElement("div");
        row.className = "share-meta-row";

        const labelEl = document.createElement("span");
        labelEl.className = "muted";
        labelEl.textContent = label;

        const valueEl = document.createElement("strong");
        valueEl.textContent = value || "-";

        row.append(labelEl, valueEl);
        list.appendChild(row);
    }

    async function requireVaultAuthOrRedirect(intent) {
        const member = await AuthGuard.requireLogin();
        if (!member) return false;

        if (member.authLevel !== "VAULT_AUTH") {
            handleVaultKeyRequired(intent);
            return false;
        }

        return true;
    }

    function sanitizeShareKeyState(state) {
        if (!state?.exists) {
            return { exists: false };
        }

        return {
            exists: true,
            keyVersion: state.keyVersion,
            algorithm: state.algorithm,
            status: state.status,
            createdAt: state.createdAt
        };
    }

    function renderShareKeyState(state) {
        currentState = sanitizeShareKeyState(state);

        const statusCard = byId("share-status-card");
        const createButton = byId("createShareKeyBtn");
        const regenerateButton = byId("regenerateShareKeyBtn");
        const deleteButton = byId("deleteShareKeyBtn");

        if (!statusCard) return;

        statusCard.textContent = "";

        if (currentState.exists) {
            const heading = document.createElement("p");
            heading.className = "share-status-title";
            heading.textContent = "공유 저장소 준비 완료";

            const metaList = document.createElement("div");
            metaList.className = "share-meta-list";
            addMetaRow(metaList, "버전", currentState.keyVersion ? "v" + currentState.keyVersion : "-");
            addMetaRow(metaList, "상태", currentState.status || "-");
            addMetaRow(metaList, "알고리즘", currentState.algorithm || "-");
            addMetaRow(metaList, "생성일", formatDate(currentState.createdAt));

            const note = document.createElement("p");
            note.className = "muted";
            note.textContent = "공유 item 목록 기능은 준비 중입니다.";

            statusCard.append(heading, metaList, note);
            createButton?.classList.add("hidden");
            regenerateButton?.classList.remove("hidden");
            deleteButton?.classList.remove("hidden");
            return;
        }

        const empty = document.createElement("div");
        empty.className = "share-empty-state";

        const title = document.createElement("p");
        title.className = "share-status-title";
        title.textContent = "공유키가 없습니다.";

        const description = document.createElement("p");
        description.className = "muted";
        description.textContent = "공유 기능을 사용하려면 이 기기에서 공유키를 먼저 생성해야 합니다.";

        const securityNote = document.createElement("p");
        securityNote.className = "muted";
        securityNote.textContent = "공개키만 서버에 저장되고, 개인키는 클라이언트에서 암호화된 값으로만 전송됩니다.";

        empty.append(title, description, securityNote);
        statusCard.appendChild(empty);

        createButton?.classList.remove("hidden");
        regenerateButton?.classList.add("hidden");
        deleteButton?.classList.add("hidden");
    }

    function redirectToUnlockSoon(intent) {
        window.setTimeout(function () {
            window.location.href = ShareKeyIntent.buildUnlockUrl(intent);
        }, 700);
    }

    function handleVaultKeyRequired(intent) {
        const message = intent === "regenerate-share-key"
            ? "공유키 재생성을 위해 Vault 잠금 해제가 필요합니다."
            : intent === "create-share-key"
                ? "공유키 생성을 위해 Vault 잠금 해제가 필요합니다."
                : "Vault 잠금 해제가 필요합니다.";

        showShareMessage("warning", message);
        redirectToUnlockSoon(intent);
    }

    function handleError(error, fallback, options = {}) {
        if (ShareKeyCrypto.isVaultLockedError(error)) {
            if (options.redirectOnVaultRequired === false) {
                showShareMessage("error", fallback);
                return;
            }
            handleVaultKeyRequired(options.intent);
            return;
        }

        if (error?.status === 403) {
            if (options.redirectOnVaultRequired === false) {
                showShareMessage("error", fallback);
                return;
            }
            handleVaultKeyRequired(options.intent);
            return;
        }

        showShareMessage(error?.isConflict ? "warning" : "error", error?.userMessage || fallback);
    }

    async function loadMyShareKey() {
        bindEvents();
        const intent = intentHandled ? null : readShareIntent();
        intentHandled = true;

        try {
            const allowed = await requireVaultAuthOrRedirect(intent);
            if (!allowed) return;

            showShareMessage("info", "공유키 상태를 확인하고 있습니다.");
            const response = await APIClient.get("/api/share/keys/me");
            const state = sanitizeShareKeyState(response);
            renderShareKeyState(state);

            if (intent) {
                await continueShareIntent(intent, state);
                return;
            }

            showShareMessage("success", state.exists ? "공유 저장소가 준비되어 있습니다." : "공유키가 아직 없습니다.");
        } catch (error) {
            renderShareKeyState({ exists: false });
            handleError(error, "공유키 상태를 확인하지 못했습니다.");
        }
    }

    async function continueShareIntent(intent, state) {
        if (intent === "create-share-key" && state.exists) {
            ShareKeyIntent.clearPendingPayload();
            showShareMessage("success", "공유키가 이미 존재합니다.");
            return;
        }

        const payload = ShareKeyIntent.consumePendingPayload(intent);
        showShareMessage(
            "info",
            intent === "regenerate-share-key"
                ? "Vault 잠금 해제가 완료되었습니다. 공유키 재생성을 계속합니다."
                : "Vault 잠금 해제가 완료되었습니다. 공유키 생성을 계속합니다."
        );

        if (intent === "regenerate-share-key" && state.exists) {
            await regenerateShareKey({ automatic: true, payload });
            return;
        }

        await createShareKey({ automatic: true, payload });
    }

    async function createShareKey(options = {}) {
        if (actionInProgress) return;

        if (currentState?.exists) {
            ShareKeyIntent.clearPendingPayload();
            showShareMessage("success", "공유키가 이미 존재합니다.");
            return;
        }

        actionInProgress = true;
        try {
            setActionButtonsBusy(true);
            showShareMessage("info", "공유키를 생성하고 있습니다.");
            const payload = options.payload || await ShareKeyCrypto.createEncryptedShareKeyPayload();
            const response = await APIClient.post("/api/share/keys", payload);
            renderShareKeyState(response);
            showShareMessage("success", "공유키가 생성되었습니다.");
        } catch (error) {
            handleError(
                error,
                options.automatic
                    ? "공유키 생성 정보를 확인할 수 없습니다. 공유키 생성 버튼으로 다시 시도해주세요."
                    : "공유키 생성에 실패했습니다.",
                {
                    intent: "create-share-key",
                    redirectOnVaultRequired: !options.automatic
                }
            );
        } finally {
            actionInProgress = false;
            setActionButtonsBusy(false);
        }
    }

    async function regenerateShareKey(options = {}) {
        if (actionInProgress) return;

        if (!currentState?.exists) {
            await createShareKey(options);
            return;
        }

        actionInProgress = true;
        try {
            setActionButtonsBusy(true);
            showShareMessage("info", "공유키를 재생성하고 있습니다.");
            const payload = options.payload || await ShareKeyCrypto.createEncryptedShareKeyPayload();
            const response = await APIClient.post("/api/share/keys/regenerate", payload);
            renderShareKeyState(response);
            showShareMessage("success", "공유키가 재생성되었습니다.");
        } catch (error) {
            handleError(
                error,
                options.automatic
                    ? "공유키 재생성 정보를 확인할 수 없습니다. 공유키 재생성 버튼으로 다시 시도해주세요."
                    : "공유키 재생성에 실패했습니다.",
                {
                    intent: "regenerate-share-key",
                    redirectOnVaultRequired: !options.automatic
                }
            );
        } finally {
            actionInProgress = false;
            setActionButtonsBusy(false);
        }
    }

    async function deleteShareKey() {
        if (!currentState?.exists) {
            showShareMessage("warning", "삭제할 공유키가 없습니다.");
            return;
        }

        if (!confirm("공유키를 삭제할까요?")) {
            return;
        }

        const button = byId("deleteShareKeyBtn");

        try {
            setButtonBusy(button, true);
            showShareMessage("info", "공유키를 삭제하고 있습니다.");
            await APIClient.del("/api/share/keys/me");
            renderShareKeyState({ exists: false });
            showShareMessage("success", "공유키가 삭제되었습니다.");
        } catch (error) {
            handleError(error, "공유키 삭제에 실패했습니다.");
        } finally {
            setButtonBusy(button, false);
        }
    }

    function showSharedItemsReady() {
        showShareMessage("info", "공유 item 기능은 준비 중입니다.");
    }

    function bindEvents() {
        if (bound) return;
        bound = true;

        byId("createShareKeyBtn")?.addEventListener("click", function () {
            createShareKey();
        });
        byId("regenerateShareKeyBtn")?.addEventListener("click", function () {
            regenerateShareKey();
        });
        byId("deleteShareKeyBtn")?.addEventListener("click", deleteShareKey);
        byId("sharedItemsBtn")?.addEventListener("click", showSharedItemsReady);
    }

    window.ShareKeyClient = {
        loadMyShareKey,
        createShareKey,
        regenerateShareKey,
        deleteShareKey,
        renderShareKeyState,
        showShareMessage
    };
})();
