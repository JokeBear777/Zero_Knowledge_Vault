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

    function setRegenerateModalVisible(visible) {
        regenerateModalOpen = visible;
        byId("share-key-regenerate-modal")?.classList.toggle("hidden", !visible);
        if (visible) {
            window.setTimeout(function () {
                byId("confirm-share-key-regenerate-btn")?.focus();
            }, 0);
        }
    }

    function openRegenerateConfirmModal() {
        setRegenerateModalVisible(true);
    }

    function closeRegenerateConfirmModal() {
        setRegenerateModalVisible(false);
    }

    async function getShareKeyRewrapCandidates() {
        return await APIClient.get("/api/share/keys/rewrap-candidates");
    }

    async function rewrapShareKeys(payload) {
        return await APIClient.post("/api/share/keys/rewrap", payload);
    }

    async function refreshSharePrivateKeyCache() {
        window.__zkvSharePrivateKey = null;
        return await SharedItemCrypto.loadMySharePrivateKey();
    }

    function isShareKeyVersionMismatchError(error) {
        const code = error?.body?.code;
        return code === "SHARE_KEY_REWRAP_TARGET_VERSION_CONFLICT"
            || code === "SHARED_ITEM_REWRAP_NOT_REQUIRED"
            || code === "SHARED_ITEM_MEMBERSHIP_VERSION_CONFLICT";
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
            heading.textContent = "공유 금고가 준비됐어요.";

            const metaList = document.createElement("div");
            metaList.className = "share-meta-list";
            addMetaRow(metaList, "상태", currentState.status === "ACTIVE" ? "사용 가능" : "확인 필요");
            addMetaRow(metaList, "준비한 날", formatDate(currentState.createdAt));

            const note = document.createElement("p");
            note.className = "muted";
            note.textContent = "이제 공유 금고에서 새 공유 비밀을 만들고 초대할 수 있어요.";

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
        title.textContent = "공유 금고 열쇠가 아직 없어요.";

        const description = document.createElement("p");
        description.className = "muted";
        description.textContent = "공유 비밀을 사용하려면 이 기기에서 공유키를 먼저 만들어야 해요.";

        const securityNote = document.createElement("p");
        securityNote.className = "muted";
        securityNote.textContent = "개인키는 브라우저에서 암호화된 값으로만 다뤄져요.";

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
            ? "공유키를 다시 만들려면 먼저 금고를 열어야 해요."
            : intent === "create-share-key"
                ? "공유키를 만들려면 먼저 금고를 열어야 해요."
                : "금고 열기가 필요해요.";

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

        if (error?.isConflict) {
            showShareMessage("warning", "공유 금고 상태가 바뀌었어요. 최신 상태를 다시 불러온 뒤 다시 시도해주세요.");
            return;
        }

        showShareMessage("error", error?.userMessage || fallback);
    }

    async function loadMyShareKey() {
        bindEvents();
        const intent = intentHandled ? null : readShareIntent();
        intentHandled = true;

        try {
            const allowed = await requireVaultAuthOrRedirect(intent);
            if (!allowed) return;

            showShareMessage("info", "공유키 상태를 확인하고 있어요.");
            const response = await APIClient.get("/api/share/keys/me");
            const state = sanitizeShareKeyState(response);
            renderShareKeyState(state);

            if (intent) {
                await continueShareIntent(intent, state);
                return;
            }

            showShareMessage("success", state.exists ? "공유 금고가 준비되어 있어요." : "공유키가 아직 없어요.");
        } catch (error) {
            renderShareKeyState({ exists: false });
            handleError(error, "공유키 상태를 확인하지 못했어요.");
        }
    }

    async function continueShareIntent(intent, state) {
        if (intent === "create-share-key" && state.exists) {
            ShareKeyIntent.clearPendingPayload();
            showShareMessage("success", "공유키가 이미 준비되어 있어요.");
            return;
        }

        const payload = ShareKeyIntent.consumePendingPayload(intent);
        showShareMessage(
            "info",
            intent === "regenerate-share-key"
                ? "금고가 열렸어요. 공유키를 다시 만들게요."
                : "금고가 열렸어요. 공유키를 만들게요."
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
            showShareMessage("success", "공유키가 이미 준비되어 있어요.");
            return;
        }

        actionInProgress = true;
        try {
            setActionButtonsBusy(true);
            showShareMessage("info", "공유키를 만들고 있어요.");
            const payload = options.payload || await ShareKeyCrypto.createEncryptedShareKeyPayload();
            const response = await APIClient.post("/api/share/keys", payload);
            renderShareKeyState(response);
            showShareMessage("success", "공유키를 만들었어요.");
        } catch (error) {
            handleError(
                error,
                options.automatic
                    ? "공유키 만들기 정보를 확인할 수 없어요. 버튼으로 다시 시도해주세요."
                    : "공유키를 만들지 못했어요.",
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

    async function executeShareKeyRegeneration(options = {}) {
        let previousPrivateKey = null;
        let regenerated = false;

        try {
            setActionButtonsBusy(true);
            showShareMessage("info", "기존 공유 비밀을 확인하는 중이에요...");
            previousPrivateKey = await SharedItemCrypto.loadMySharePrivateKey();

            showShareMessage("info", "새 공유키를 만드는 중이에요...");
            const payload = options.payload || await ShareKeyCrypto.createEncryptedShareKeyPayload();

            showShareMessage("info", "서버에 업데이트하는 중이에요...");
            const response = await APIClient.post("/api/share/keys/regenerate", payload);
            regenerated = true;
            renderShareKeyState(response);

            showShareMessage("info", "기존 공유 비밀을 확인하는 중이에요...");
            const candidates = await getShareKeyRewrapCandidates();
            const candidateList = Array.isArray(candidates) ? candidates : [];
            const targetKeyVersion = Number(response.keyVersion);

            if (candidateList.length === 0) {
                showShareMessage("success", "새 공유키를 만들었어요. 업데이트할 기존 공유 비밀은 없어요.");
                return;
            }

            if (candidateList.some(function (candidate) {
                return Number(candidate.currentActiveKeyVersion) !== targetKeyVersion;
            })) {
                showShareMessage("warning", "공유키 정보가 바뀌었어요. 새로고침 후 다시 시도해주세요.");
                return;
            }

            showShareMessage("info", "공유 비밀 열쇠를 새 키로 다시 감싸는 중이에요...");
            const items = [];
            for (const candidate of candidateList) {
                try {
                    const sharedItemKey = await SharedItemCrypto.decryptSharedItemKeyWithPrivateKey(
                        candidate.encryptedItemKeyBase64,
                        previousPrivateKey
                    );
                    const encryptedItemKeyBase64 = await SharedItemCrypto.encryptSharedItemKeyForPublicKey(
                        sharedItemKey,
                        payload.publicKeyBase64
                    );
                    items.push({
                        sharedItemId: candidate.sharedItemId,
                        expectedMembershipVersion: candidate.membershipVersion,
                        encryptedItemKeyBase64
                    });
                } catch (error) {
                    showShareMessage("warning", "기존 공유 비밀 열쇠를 복원하지 못했어요. 기존 공유키 정보가 필요할 수 있어요.");
                    return;
                }
            }

            if (items.length === 0) {
                showShareMessage("success", "새 공유키를 만들었어요. 업데이트할 기존 공유 비밀은 없어요.");
                return;
            }

            showShareMessage("info", "서버에 업데이트하는 중이에요...");
            await rewrapShareKeys({
                targetKeyVersion,
                items
            });
            showShareMessage("success", "공유키를 재생성하고 기존 공유 비밀을 업데이트했어요.");
        } catch (error) {
            if (isShareKeyVersionMismatchError(error)) {
                showShareMessage("warning", "공유키 정보가 바뀌었어요. 새로고침 후 다시 시도해주세요.");
                return;
            }

            if (ShareKeyCrypto.isVaultLockedError(error)) {
                handleVaultKeyRequired("regenerate-share-key");
                return;
            }

            if (error?.status === 409) {
                showShareMessage("warning", "공유 금고 상태가 바뀌었어요. 최신 상태를 다시 불러온 뒤 다시 시도해주세요.");
                return;
            }

            handleError(error, "공유키 재생성에 실패했어요. 잠시 후 다시 시도해주세요.", {
                intent: "regenerate-share-key",
                redirectOnVaultRequired: !options.automatic
            });
        } finally {
            if (regenerated) {
                try {
                    await refreshSharePrivateKeyCache();
                } catch (error) {
                    window.__zkvSharePrivateKey = null;
                }
            }
            previousPrivateKey = null;
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

        if (!options.automatic) {
            openRegenerateConfirmModal();
            return;
        }

        return await executeShareKeyRegeneration(options);

        actionInProgress = true;
        try {
            setActionButtonsBusy(true);
            showShareMessage("info", "공유키를 다시 만들고 있어요.");
            const payload = options.payload || await ShareKeyCrypto.createEncryptedShareKeyPayload();
            const response = await APIClient.post("/api/share/keys/regenerate", payload);
            renderShareKeyState(response);
            showShareMessage("success", "공유키를 다시 만들었어요.");
        } catch (error) {
            handleError(
                error,
                options.automatic
                    ? "공유키 다시 만들기 정보를 확인할 수 없어요. 버튼으로 다시 시도해주세요."
                    : "공유키를 다시 만들지 못했어요.",
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
            showShareMessage("warning", "삭제할 공유키가 없어요.");
            return;
        }

        if (!confirm("공유키를 삭제할까요?")) {
            return;
        }

        const button = byId("deleteShareKeyBtn");

        try {
            setButtonBusy(button, true);
            showShareMessage("info", "공유키를 삭제하고 있어요.");
            await APIClient.del("/api/share/keys/me");
            renderShareKeyState({ exists: false });
            showShareMessage("success", "공유키를 삭제했어요.");
        } catch (error) {
            handleError(error, "공유키를 삭제하지 못했어요.");
        } finally {
            setButtonBusy(button, false);
        }
    }

    function showSharedItemsReady() {
        window.location.href = "/shared-items.html";
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
        byId("close-share-key-regenerate-btn")?.addEventListener("click", closeRegenerateConfirmModal);
        byId("cancel-share-key-regenerate-btn")?.addEventListener("click", closeRegenerateConfirmModal);
        byId("share-key-regenerate-modal")?.addEventListener("click", function (event) {
            if (event.target === byId("share-key-regenerate-modal")) {
                closeRegenerateConfirmModal();
            }
        });
        byId("confirm-share-key-regenerate-btn")?.addEventListener("click", function () {
            closeRegenerateConfirmModal();
            regenerateShareKey({ automatic: true });
        });
        byId("deleteShareKeyBtn")?.addEventListener("click", deleteShareKey);
        byId("sharedItemsBtn")?.addEventListener("click", showSharedItemsReady);
    }

    window.ShareKeyClient = {
        loadMyShareKey,
        createShareKey,
        regenerateShareKey,
        deleteShareKey,
        getShareKeyRewrapCandidates,
        rewrapShareKeys,
        renderShareKeyState,
        showShareMessage
    };
})();
