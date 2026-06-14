(function () {
    const DEVICE_SECRET_KEY = "zkv_device_secret_v1";
    const DEVICE_SECRET_MISSING_MESSAGE = "이 브라우저에 필요한 기기 정보가 없어 금고를 열 수 없어요. 이 기기에서 다시 설정해주세요.";
    const RESTORE_FAILURE_MESSAGE = "마스터 비밀번호가 올바르지 않거나 이 브라우저에서 금고 열쇠를 복원할 수 없어요.";
    const CONFLICT_MESSAGE = "다른 사용자 또는 기기에서 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 수정해주세요.";
    const state = {
        detailItem: null,
        decryptedTitle: "",
        decryptedContent: "",
        sharedItemKey: null,
        sharedItems: [],
        busy: false,
        pendingRevokeMember: null,
        revokeConfirmResolve: null
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function showSharedItemMessage(type, message) {
        const area = byId("shared-item-message") || byId("shared-invite-message");
        if (!area) return;
        area.className = "message message-" + type;
        area.textContent = message;
    }

    function setBusy(button, busy) {
        if (button) button.disabled = busy;
    }

    function formatDate(value) {
        if (!value) return "-";
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString();
    }

    function formatShortDate(value) {
        if (!value) return "저장됨";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "저장됨";

        const diffMs = Date.now() - date.getTime();
        const minute = 60 * 1000;
        const hour = 60 * minute;
        const day = 24 * hour;

        if (diffMs >= 0 && diffMs < minute) return "방금";
        if (diffMs >= 0 && diffMs < hour) return Math.floor(diffMs / minute) + "분 전";
        if (diffMs >= 0 && diffMs < day) return Math.floor(diffMs / hour) + "시간 전";

        return date.toLocaleDateString();
    }

    function clearNode(node) {
        if (node) node.textContent = "";
    }

    function createText(tagName, className, text) {
        const element = document.createElement(tagName);
        if (className) element.className = className;
        element.textContent = text || "";
        return element;
    }

    function createBadge(text) {
        const value = text || "-";
        let badgeClass = "status-pill shared-badge";
        if (value === "OWNER") badgeClass += " badge-owner";
        if (value === "READ_WRITE") badgeClass += " badge-write";
        if (value === "READ_ONLY") badgeClass += " badge-read";
        return createText("span", badgeClass, value);
    }

    function createRoleBadge(role) {
        const value = role === "OWNER" ? "OWNER" : "PARTICIPANT";
        const className = value === "OWNER" ? "role-badge owner" : "role-badge participant";
        return createText("span", className, value);
    }

    function isOwner(item) {
        return item?.role === "OWNER";
    }

    function canEdit(item) {
        return isOwner(item) || (item?.role === "PARTICIPANT" && item?.permission === "READ_WRITE");
    }

    function sharedItemsUnlockUrl() {
        return "/unlock.html?next=/shared-items.html";
    }

    function hasDeviceSecret() {
        return Boolean(localStorage.getItem(DEVICE_SECRET_KEY));
    }

    function setElementVisible(id, visible) {
        byId(id)?.classList.toggle("hidden", !visible);
    }

    function setSharedContentVisible(visible) {
        const hasCombinedList = Boolean(byId("shared-vault-items"));

        setElementVisible("shared-content-section", visible);
        if (hasCombinedList) {
            setElementVisible("shared-owner-list-section", false);
            setElementVisible("shared-participant-list-section", false);
            setElementVisible("shared-invite-list-section", visible);
            if (!visible) byId("shared-create-sheet")?.classList.add("hidden");
        } else {
            setElementVisible("shared-create-section", visible);
            setElementVisible("shared-owner-list-section", visible);
            setElementVisible("shared-participant-list-section", visible);
            setElementVisible("shared-invite-list-section", visible);
        }
        setElementVisible("shared-detail-section", visible);

        if (!visible) {
            setElementVisible("edit-section", false);
            setElementVisible("owner-management-section", false);
            setElementVisible("delete-shared-item-btn", false);
        }
    }

    function isSharedCryptoReady() {
        return Boolean(window.__zkvVaultKey && window.__zkvSharePrivateKey);
    }

    function setSharedOpenStatus(opened) {
        const status = byId("shared-open-status");
        if (!status) return;

        status.className = opened
            ? "vault-lock-status vault-open-status status-success"
            : "vault-lock-status vault-open-status status-warning";
        const icon = document.createElement("span");
        icon.setAttribute("aria-hidden", "true");
        icon.textContent = opened ? "🔓" : "🔒";
        status.replaceChildren(icon, document.createTextNode(opened ? " 열려 있어요" : " 잠겨 있어요"));
    }

    function showLockedSection(message) {
        setElementVisible("shared-vault-locked-section", true);
        setElementVisible("shared-vault-open-section", false);
        setSharedContentVisible(false);
        setSharedOpenStatus(false);
        if (message) {
            showSharedItemMessage("warning", message);
        }
    }

    function showVaultRestoreSection() {
        setElementVisible("shared-vault-locked-section", false);
        setElementVisible("shared-vault-open-section", true);
        setSharedContentVisible(false);
        setSharedOpenStatus(false);
    }

    function showSharedContent() {
        setElementVisible("shared-vault-locked-section", false);
        setElementVisible("shared-vault-open-section", false);
        setSharedContentVisible(true);
        setSharedOpenStatus(true);
    }

    async function requireVaultAuthOrRedirect() {
        const member = await AuthGuard.requireLogin();
        if (!member) return false;
        if (member.authLevel !== "VAULT_AUTH") {
            setSharedContentVisible(false);
            showSharedItemMessage("warning", "금고 열기가 필요해요.");
            window.setTimeout(function () {
                window.location.href = sharedItemsUnlockUrl();
            }, 700);
            return false;
        }
        return true;
    }

    function handleError(error, fallback) {
        if (error?.isConflict || error?.status === 409) {
            showSharedItemMessage("warning", CONFLICT_MESSAGE);
            return;
        }

        if (SharedItemCrypto.isVaultKeyRequired(error)) {
            showLockedSection("공유 비밀을 보려면 금고 열쇠 복원이 필요해요.");
            return;
        }

        if (error?.status === 403) {
            setSharedContentVisible(false);
            showSharedItemMessage("warning", "금고 열기가 필요해요.");
            window.setTimeout(function () {
                window.location.href = sharedItemsUnlockUrl();
            }, 700);
            return;
        }

        if (SharedItemCrypto.isShareKeyRequired(error)) {
            showSharedItemMessage("warning", "공유키가 필요해요. 공유키를 먼저 만든 뒤 다시 시도해주세요.");
            return;
        }

        showSharedItemMessage("error", error?.userMessage || fallback);
    }

    async function restoreVaultKeyFromPassword() {
        const input = byId("shared-vault-master-password");
        const button = byId("restore-shared-vault-key-btn");
        const masterPassword = input?.value || "";

        if (!masterPassword) {
            showSharedItemMessage("warning", "마스터 비밀번호를 입력해주세요.");
            return;
        }

        if (!hasDeviceSecret()) {
            if (input) input.value = "";
            showVaultRestoreSection();
            showSharedItemMessage("error", DEVICE_SECRET_MISSING_MESSAGE);
            return;
        }

        setBusy(button, true);
        try {
            showSharedItemMessage("info", "금고 열쇠를 복원하고 있어요.");
            const keyMaterial = await APIClient.get("/api/vault/key-material", {
                redirectOnAuthError: false
            });
            const wrappingKey = await VaultCrypto.deriveWrappingKey(
                masterPassword,
                keyMaterial.saltWrapBase64,
                keyMaterial.wrapKdfParams || {},
                keyMaterial.wrapKdfAlgorithm
            );
            window.__zkvVaultKey = await VaultCrypto.unwrapVaultKey(
                keyMaterial.wrappedVaultKeyBase64,
                wrappingKey
            );
            await SharedItemCrypto.loadMySharePrivateKey();
            input.value = "";
            showSharedContent();
            showSharedItemMessage("success", "금고 열쇠를 복원했어요.");

            const detailId = new URLSearchParams(window.location.search).get("id");
            if (window.location.pathname === "/shared-item-detail.html" && detailId) {
                await loadSharedItemDetail(detailId);
            } else if (window.location.pathname === "/shared-items.html") {
                await loadSharedItems();
            }
        } catch (error) {
            if (input) input.value = "";
            window.__zkvVaultKey = null;
            window.__zkvSharePrivateKey = null;
            showVaultRestoreSection();

            if (SharedItemCrypto.isShareKeyRequired(error)) {
                showSharedItemMessage("warning", "공유키가 없어 공유 비밀을 열 수 없어요. 공유키를 먼저 만들어주세요.");
                return;
            }

            if (error?.status === 403) {
                handleError(error, RESTORE_FAILURE_MESSAGE);
                return;
            }

            showSharedItemMessage("error", RESTORE_FAILURE_MESSAGE);
        } finally {
            setBusy(button, false);
        }
    }

    function getSharedItemIconLabel(title) {
        const normalizedTitle = String(title || "").trim().toLowerCase();
        if (normalizedTitle.includes("github")) return "GH";
        if (normalizedTitle.includes("google")) return "G";
        if (normalizedTitle.includes("학교")) return "학";
        if (normalizedTitle.includes("은행")) return "₩";
        if (normalizedTitle.includes("email") || normalizedTitle.includes("이메일")) return "@";
        return "🔐";
    }

    function getVisibleSharedItems() {
        const searchInput = byId("shared-search-input");
        const sortSelect = byId("shared-sort-select");
        const keyword = (searchInput?.value || "").trim().toLowerCase();
        const filtered = keyword
            ? state.sharedItems.filter(item => item.title.toLowerCase().includes(keyword))
            : [...state.sharedItems];

        if (sortSelect?.value === "title") {
            filtered.sort((a, b) => a.title.localeCompare(b.title, "ko"));
            return filtered;
        }

        filtered.sort((a, b) => {
            const left = new Date(a.updatedAt || a.createdAt || 0).getTime();
            const right = new Date(b.updatedAt || b.createdAt || 0).getTime();
            return right - left;
        });
        return filtered;
    }

    function renderCombinedSharedItems() {
        const list = byId("shared-vault-items");
        const emptyState = byId("shared-empty-state");
        const count = byId("shared-secret-count");
        if (!list) return false;

        list.textContent = "";
        const visibleItems = getVisibleSharedItems();
        const hasItems = state.sharedItems.length > 0;
        const hasVisibleItems = visibleItems.length > 0;

        if (count) count.textContent = "(" + state.sharedItems.length + ")";
        if (emptyState) {
            emptyState.classList.toggle("hidden", hasVisibleItems);
            emptyState.textContent = hasItems
                ? "검색 결과가 없어요."
                : "아직 공유 비밀이 없어요. 첫 번째 공유 비밀을 만들어보세요.";
        }

        for (const item of visibleItems) {
            const card = document.createElement("article");
            card.className = "shared-vault-card";
            card.tabIndex = 0;

            const icon = createText("div", "shared-vault-icon", getSharedItemIconLabel(item.title));
            const main = document.createElement("div");
            main.className = "shared-vault-main";
            main.append(
                createText("div", "shared-vault-title", item.title || "열 수 없는 비밀"),
                createText("div", "shared-vault-preview", "공유 비밀 내용은 안전하게 암호화돼요.")
            );

            const meta = document.createElement("div");
            meta.className = "shared-vault-meta";
            meta.append(
                createRoleBadge(item.role),
                createText("span", "updated-at", formatShortDate(item.updatedAt || item.createdAt))
            );

            const arrow = createText("span", "chevron", ">");
            arrow.setAttribute("aria-hidden", "true");

            const openDetail = function () {
                window.location.href = "/shared-item-detail.html?id=" + encodeURIComponent(item.sharedItemId);
            };
            card.addEventListener("click", openDetail);
            card.addEventListener("keydown", function (event) {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    openDetail();
                }
            });

            card.append(icon, main, meta, arrow);
            list.appendChild(card);
        }

        return true;
    }

    function renderItemCard(container, item, title) {
        const card = document.createElement("article");
        card.className = "shared-item-card";

        const heading = createText("h3", null, title || "열 수 없는 비밀");
        const meta = document.createElement("div");
        meta.className = "shared-item-meta";
        meta.append(
            createBadge(item.role),
            createBadge(item.permission),
            createText("span", "muted", item.ownerEmailMasked || item.emailMasked || "공유 멤버"),
            createText("span", "muted", "수정 " + formatDate(item.updatedAt || item.createdAt))
        );

        const arrow = createText("span", "shared-item-arrow", "›");

        card.classList.add("is-link");
        card.addEventListener("click", function () {
            window.location.href = "/shared-item-detail.html?id=" + encodeURIComponent(item.sharedItemId);
        });

        card.append(heading, meta, arrow);
        container.appendChild(card);
    }

    function renderEmpty(container, text) {
        container.appendChild(createText("div", "empty-state", text));
    }

    async function renderSharedItemList(items) {
        const ownerList = byId("owner-shared-items");
        const participantList = byId("participant-shared-items");
        clearNode(ownerList);
        clearNode(participantList);

        const normalizedItems = [];

        let ownerCount = 0;
        let participantCount = 0;

        for (const item of items || []) {
            let title = "";
            try {
                title = (await SharedItemCrypto.decryptSharedItem(item)).title;
            } catch (error) {
                title = "열 수 없는 비밀";
            }

            normalizedItems.push({ ...item, title });

            if (isOwner(item)) {
                ownerCount += 1;
                renderItemCard(ownerList, item, title);
            } else {
                participantCount += 1;
                renderItemCard(participantList, item, title);
            }
        }

        state.sharedItems = normalizedItems;
        if (renderCombinedSharedItems()) return;

        if (ownerCount === 0) renderEmpty(ownerList, "내가 만든 공유 비밀이 없어요.");
        if (participantCount === 0) renderEmpty(participantList, "참여 중인 공유 비밀이 없어요.");
    }

    async function loadSharedItems() {
        try {
            if (!await requireVaultAuthOrRedirect()) return;
            if (!isSharedCryptoReady()) {
                showLockedSection();
                return;
            }

            showSharedContent();
            showSharedItemMessage("info", "공유 금고 목록을 불러오고 있어요.");
            const items = await APIClient.get("/api/shared-items");
            await renderSharedItemList(items);
            showSharedItemMessage("success", "공유 금고 목록을 불러왔어요.");
        } catch (error) {
            handleError(error, "공유 비밀을 불러오지 못했어요.");
        }
    }

    async function createSharedItem() {
        if (state.busy) return;

        const title = byId("create-title")?.value?.trim() || "";
        const content = byId("create-content")?.value || "";

        if (!title || !content) {
            showSharedItemMessage("warning", "제목과 내용을 입력해주세요.");
            return;
        }

        state.busy = true;
        setBusy(byId("create-shared-item-btn"), true);
        try {
            if (!await requireVaultAuthOrRedirect()) return;
            if (!isSharedCryptoReady()) {
                showLockedSection("공유 비밀을 만들려면 금고 열쇠 복원이 필요해요.");
                return;
            }

            showSharedItemMessage("info", "공유 비밀을 암호화해 만들고 있어요.");
            const payload = await SharedItemCrypto.createOwnerSharedItemPayload(title, content);
            await APIClient.post("/api/shared-items", payload);
            closeSharedCreateSheet();
            await loadSharedItems();
            showSharedItemMessage("success", "공유 비밀을 만들었어요.");
        } catch (error) {
            handleError(error, "공유 비밀을 만들지 못했어요.");
        } finally {
            state.busy = false;
            setBusy(byId("create-shared-item-btn"), false);
        }
    }

    function resetSharedCreateForm() {
        const title = byId("create-title");
        const content = byId("create-content");
        if (title) title.value = "";
        if (content) content.value = "";
    }

    function openSharedCreateSheet() {
        if (!isSharedCryptoReady()) {
            showLockedSection("공유 비밀을 만들려면 금고 열쇠 복원이 필요해요.");
            return;
        }

        resetSharedCreateForm();
        byId("shared-create-sheet")?.classList.remove("hidden");
        byId("shared-create-section")?.classList.remove("hidden");
        byId("create-title")?.focus();
    }

    function closeSharedCreateSheet() {
        resetSharedCreateForm();
        byId("shared-create-sheet")?.classList.add("hidden");
    }

    function renderSharedItemDetail(item) {
        byId("detail-title").textContent = state.decryptedTitle || "열 수 없는 비밀";
        byId("detail-content").textContent = state.decryptedContent || "";
        byId("detail-meta").textContent = [
            item.role,
            item.permission,
            "수정 " + formatDate(item.updatedAt || item.createdAt)
        ].filter(Boolean).join(" / ");

        if (canEdit(item)) {
            byId("edit-section")?.classList.remove("hidden");
            byId("edit-title").value = state.decryptedTitle || "";
            byId("edit-content").value = state.decryptedContent || "";
        } else {
            byId("edit-section")?.classList.add("hidden");
        }

        byId("read-only-note")?.classList.toggle("hidden", canEdit(item));

        if (isOwner(item)) {
            byId("owner-management-section")?.classList.remove("hidden");
            byId("delete-shared-item-btn")?.classList.remove("hidden");
            loadJoinRequests(item.sharedItemId);
            loadSharedItemMembers(item.sharedItemId);
        } else {
            byId("owner-management-section")?.classList.add("hidden");
            byId("delete-shared-item-btn")?.classList.add("hidden");
        }
    }

    async function loadSharedItemDetail(sharedItemId) {
        try {
            if (!await requireVaultAuthOrRedirect()) return;
            if (!isSharedCryptoReady()) {
                showLockedSection("공유 비밀 상세를 보려면 금고 열쇠 복원이 필요해요.");
                return;
            }

            showSharedContent();
            showSharedItemMessage("info", "공유 비밀을 불러오고 있어요.");
            const item = await getSharedItemDetail(sharedItemId);
            const decrypted = await SharedItemCrypto.decryptSharedItem(item, { includeContent: true });

            state.detailItem = item;
            state.decryptedTitle = decrypted.title;
            state.decryptedContent = decrypted.content;
            state.sharedItemKey = decrypted.sharedItemKey;
            renderSharedItemDetail(item);
            showSharedItemMessage("success", "공유 비밀을 열었어요.");
        } catch (error) {
            handleError(error, "공유 비밀 상세를 불러오지 못했어요.");
        }
    }

    async function updateSharedItem(sharedItemId) {
        if (state.busy || !state.detailItem) return;
        if (!canEdit(state.detailItem)) {
            showSharedItemMessage("warning", "읽기 전용 권한이라 수정할 수 없습니다.");
            return;
        }

        state.busy = true;
        setBusy(byId("update-shared-item-btn"), true);
        try {
            const encryptedPayload = await SharedItemCrypto.encryptItemPayload(
                byId("edit-title").value.trim(),
                byId("edit-content").value,
                state.sharedItemKey
            );
            await APIClient.patch("/api/shared-items/" + encodeURIComponent(sharedItemId), {
                expectedVersion: state.detailItem.version,
                titleCipherBase64: encryptedPayload.titleCipherBase64,
                itemCipherBase64: encryptedPayload.itemCipherBase64
            });
            showSharedItemMessage("success", "공유 비밀을 수정했어요.");
            await loadSharedItemDetail(sharedItemId);
        } catch (error) {
            handleError(error, "공유 비밀을 수정하지 못했어요.");
        } finally {
            state.busy = false;
            setBusy(byId("update-shared-item-btn"), false);
        }
    }

    async function deleteSharedItem(sharedItemId) {
        if (!state.detailItem || !isOwner(state.detailItem)) {
            showSharedItemMessage("warning", "이 작업을 할 수 있는 권한이 없어요.");
            return;
        }

        if (!confirm("정말 이 공유 비밀을 삭제할까요?\n참여자도 더 이상 접근할 수 없어요.")) return;

        try {
            await APIClient.del("/api/shared-items/" + encodeURIComponent(sharedItemId));
            window.location.href = "/shared-items.html";
        } catch (error) {
            handleError(error, "공유 비밀을 삭제하지 못했어요.");
        }
    }

    async function getSharedItemDetail(sharedItemId) {
        return await APIClient.get("/api/shared-items/" + encodeURIComponent(sharedItemId));
    }

    async function getRotationContext(sharedItemId) {
        return await APIClient.get("/api/shared-items/" + encodeURIComponent(sharedItemId) + "/rotation-context");
    }

    async function rotateKey(sharedItemId, payload) {
        return await APIClient.post(
            "/api/shared-items/" + encodeURIComponent(sharedItemId) + "/rotate-key",
            payload
        );
    }

    async function createInviteLink(sharedItemId) {
        if (!state.detailItem || !isOwner(state.detailItem)) {
            showSharedItemMessage("warning", "이 작업을 할 수 있는 권한이 없어요.");
            return;
        }

        setBusy(byId("create-invite-btn"), true);
        try {
            const response = await APIClient.post("/api/shared-items/" + encodeURIComponent(sharedItemId) + "/invite-links", {});
            byId("invite-link-url").value = response.inviteUrl || "";
            byId("invite-link-expires").textContent = "만료: " + formatDate(response.expiresAt);
            byId("invite-link-result")?.classList.remove("hidden");
            showSharedItemMessage("success", "초대 링크를 만들었어요.");
        } catch (error) {
            handleError(error, "초대 링크를 만들 수 없어요.");
        } finally {
            setBusy(byId("create-invite-btn"), false);
        }
    }

    async function copyInviteLink() {
        const input = byId("invite-link-url");
        if (!input?.value) return;
        try {
            await navigator.clipboard.writeText(input.value);
            showSharedItemMessage("success", "초대 링크를 복사했어요.");
        } catch (error) {
            showSharedItemMessage("warning", "브라우저가 자동 복사를 허용하지 않아요. 초대 링크를 직접 선택해 복사해주세요.");
        }
    }

    function renderJoinRequests(requests) {
        const container = byId("join-request-list");
        clearNode(container);

        if (!requests?.length) {
            renderEmpty(container, "대기 중인 참여 요청이 없어요.");
            return;
        }

        for (const request of requests) {
            const card = document.createElement("article");
            card.className = "shared-item-card";
            card.append(
                createText("h3", null, request.requesterEmailMasked || "요청자"),
                createText("p", "muted", "상태: " + request.status + " / 요청: " + formatDate(request.requestedAt))
            );

            const permissionSelect = document.createElement("select");
            permissionSelect.className = "input";
            const readOnly = document.createElement("option");
            readOnly.value = "READ_ONLY";
            readOnly.textContent = "READ_ONLY";
            const readWrite = document.createElement("option");
            readWrite.value = "READ_WRITE";
            readWrite.textContent = "READ_WRITE";
            permissionSelect.append(readOnly, readWrite);

            const approveButton = document.createElement("button");
            approveButton.type = "button";
            approveButton.className = "button button-primary";
            approveButton.textContent = "승인";
            approveButton.addEventListener("click", function () {
                approveJoinRequest(state.detailItem.sharedItemId, request, permissionSelect.value);
            });

            const rejectButton = document.createElement("button");
            rejectButton.type = "button";
            rejectButton.className = "button button-secondary";
            rejectButton.textContent = "거절";
            rejectButton.addEventListener("click", function () {
                rejectJoinRequest(state.detailItem.sharedItemId, request.joinRequestId);
            });

            const actions = document.createElement("div");
            actions.className = "shared-inline-actions";
            actions.append(permissionSelect, approveButton, rejectButton);
            card.appendChild(actions);
            container.appendChild(card);
        }
    }

    async function loadJoinRequests(sharedItemId) {
        try {
            renderJoinRequests(await APIClient.get("/api/shared-items/" + encodeURIComponent(sharedItemId) + "/join-requests"));
        } catch (error) {
            clearNode(byId("join-request-list"));
            renderEmpty(byId("join-request-list"), "참여 요청 목록을 불러오지 못했습니다.");
        }
    }

    async function approveJoinRequest(sharedItemId, request, permission) {
        if (!state.sharedItemKey) {
            showSharedItemMessage("warning", "공유 비밀 열쇠를 확인하지 못해 요청을 승인할 수 없어요.");
            return;
        }

        try {
            const encryptedItemKeyBase64 = await SharedItemCrypto.encryptSharedItemKeyForPublicKey(
                state.sharedItemKey,
                request.requesterPublicKeyBase64
            );
            await APIClient.post(
                "/api/shared-items/" + encodeURIComponent(sharedItemId) + "/join-requests/" + encodeURIComponent(request.joinRequestId) + "/approve",
                {
                    recipientKeyVersion: request.requesterKeyVersion,
                    encryptedItemKeyBase64,
                    permission
                }
            );
            showSharedItemMessage("success", "참여 요청을 승인했어요.");
            await loadJoinRequests(sharedItemId);
            await loadSharedItemMembers(sharedItemId);
        } catch (error) {
            handleError(error, "요청을 승인할 수 없어요.");
        }
    }

    async function rejectJoinRequest(sharedItemId, joinRequestId) {
        try {
            await APIClient.post(
                "/api/shared-items/" + encodeURIComponent(sharedItemId) + "/join-requests/" + encodeURIComponent(joinRequestId) + "/reject",
                {}
            );
            showSharedItemMessage("success", "참여 요청을 거절했어요.");
            await loadJoinRequests(sharedItemId);
        } catch (error) {
            handleError(error, "요청을 거절할 수 없어요.");
        }
    }

    function getApiErrorCode(error) {
        return error?.body?.code || error?.body?.errorCode || "";
    }

    function getRotationConflictMessage(error) {
        const code = getApiErrorCode(error);
        if (code === "RECIPIENT_KEY_VERSION_CONFLICT") {
            return "멤버의 공개키 정보가 변경되었어요. 최신 상태를 다시 불러온 뒤 다시 시도해주세요.";
        }
        return "공유 금고 상태가 변경되었어요. 최신 상태를 다시 불러온 뒤 다시 시도해주세요.";
    }

    function getJoinRequestErrorMessage(error) {
        const code = getApiErrorCode(error);
        if (code === "INVITE_LINK_NOT_FOUND" || code === "INVITE_TOKEN_REQUIRED") {
            return "초대 링크가 만료되었거나 올바르지 않아요.";
        }
        if (code === "SHARED_ITEM_MEMBER_ALREADY_EXISTS") {
            return "이미 참여 중인 공유 비밀이에요.";
        }
        if (code === "JOIN_REQUEST_ALREADY_EXISTS") {
            return "이미 참여 요청을 보낸 공유 비밀이에요.";
        }
        return "참여 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요.";
    }

    function extractInviteTokenFromInput(value) {
        const raw = String(value || "").trim();
        if (!raw) return "";

        const looksLikeUrl = raw.includes("://") || raw.startsWith("/") || raw.startsWith("?") || raw.includes("token=");
        if (!looksLikeUrl) {
            return raw;
        }

        try {
            const url = new URL(raw, window.location.origin);
            return (url.searchParams.get("token") || "").trim();
        } catch (error) {
            return "";
        }
    }

    function setJoinRequestSheetVisible(visible) {
        byId("shared-join-sheet")?.classList.toggle("hidden", !visible);
        if (visible) {
            window.setTimeout(function () {
                byId("shared-join-input")?.focus();
            }, 0);
        }
    }

    function openJoinRequestSheet() {
        const input = byId("shared-join-input");
        if (input) input.value = "";
        setJoinRequestSheetVisible(true);
    }

    function closeJoinRequestSheet() {
        const input = byId("shared-join-input");
        if (input) input.value = "";
        setJoinRequestSheetVisible(false);
    }

    async function createJoinRequestFromInviteToken(inviteToken) {
        const token = String(inviteToken || "").trim();
        if (!token) {
            throw new Error("Invite token is required.");
        }

        return await APIClient.post(
            "/api/shared-items/invite-links/" + encodeURIComponent(token) + "/join-requests",
            {}
        );
    }

    async function submitJoinRequestFromInviteInput(rawValue) {
        const token = extractInviteTokenFromInput(rawValue);
        if (!token) {
            showSharedItemMessage("error", "초대 링크 또는 초대 코드를 확인할 수 없어요.");
            return false;
        }

        await createJoinRequestFromInviteToken(token);
        return true;
    }

    async function submitJoinRequestFromSheet() {
        const input = byId("shared-join-input");
        const button = byId("submit-shared-join-btn");
        const rawValue = input?.value || "";
        const token = extractInviteTokenFromInput(rawValue);

        if (!token) {
            showSharedItemMessage("error", "초대 링크 또는 초대 코드를 입력해주세요.");
            input?.focus();
            return;
        }

        setBusy(button, true);
        try {
            await createJoinRequestFromInviteTokenWithMessage(token, {
                successMessage: "참여 요청을 보냈어요. 공유 비밀의 소유자가 승인하면 접근할 수 있어요.",
                errorMessage: "참여 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요."
            });
            closeJoinRequestSheet();
            await loadSharedItems();
        } catch (error) {
            if (error?.status === 401) {
                return;
            }
            input?.focus();
        } finally {
            setBusy(button, false);
        }
    }

    function setRevokeButtonsBusy(busy) {
        document.querySelectorAll("[data-revoke-member-id]").forEach(function (button) {
            button.disabled = busy;
        });
    }

    function closeRevokeConfirmModal(confirmed) {
        byId("revoke-rotation-modal")?.classList.add("hidden");
        const resolve = state.revokeConfirmResolve;
        state.pendingRevokeMember = null;
        state.revokeConfirmResolve = null;
        if (resolve) resolve(Boolean(confirmed));
    }

    function confirmRevokeWithRotation(member) {
        const modal = byId("revoke-rotation-modal");
        const target = byId("revoke-target-label");
        if (!modal) {
            showSharedItemMessage("error", "접근 해제 확인 창을 열 수 없어요.");
            return Promise.resolve(false);
        }

        state.pendingRevokeMember = member;
        if (target) target.textContent = member?.emailMasked || "선택한 멤버";
        modal.classList.remove("hidden");

        return new Promise(function (resolve) {
            state.revokeConfirmResolve = resolve;
        });
    }

    function bindRevokeConfirmModal() {
        byId("cancel-revoke-rotation-btn")?.addEventListener("click", function () {
            closeRevokeConfirmModal(false);
        });
        byId("confirm-revoke-rotation-btn")?.addEventListener("click", function () {
            closeRevokeConfirmModal(true);
        });
        byId("revoke-rotation-modal")?.addEventListener("click", function (event) {
            if (event.target === byId("revoke-rotation-modal")) {
                closeRevokeConfirmModal(false);
            }
        });
    }

    function renderMembers(members) {
        const container = byId("member-list");
        clearNode(container);

        if (!members?.length) {
            renderEmpty(container, "멤버 목록이 비어 있어요.");
            return;
        }

        for (const member of members) {
            const card = document.createElement("article");
            card.className = "shared-item-card";
            card.append(
                createText("h3", null, member.emailMasked || "멤버"),
                createText("p", "muted", [member.role, member.permission, member.status].filter(Boolean).join(" / "))
            );

            if (member.role === "PARTICIPANT" && member.status === "ACTIVE") {
                const nextPermission = member.permission === "READ_WRITE" ? "READ_ONLY" : "READ_WRITE";
                const permissionButton = document.createElement("button");
                permissionButton.type = "button";
                permissionButton.className = "button button-secondary";
                permissionButton.textContent = nextPermission + "로 변경";
                permissionButton.addEventListener("click", function () {
                    updateMemberPermission(state.detailItem.sharedItemId, member.memberId, nextPermission);
                });

                const revokeButton = document.createElement("button");
                revokeButton.type = "button";
                revokeButton.className = "button button-danger";
                revokeButton.dataset.revokeMemberId = String(member.memberId);
                revokeButton.textContent = "권한 취소";
                revokeButton.addEventListener("click", function () {
                    revokeSharedItemMember(state.detailItem.sharedItemId, member);
                });

                const actions = document.createElement("div");
                actions.className = "shared-inline-actions";
                actions.append(permissionButton, revokeButton);
                card.appendChild(actions);
            }

            container.appendChild(card);
        }
    }

    async function loadSharedItemMembers(sharedItemId) {
        try {
            renderMembers(await APIClient.get("/api/shared-items/" + encodeURIComponent(sharedItemId) + "/members"));
        } catch (error) {
            clearNode(byId("member-list"));
            renderEmpty(byId("member-list"), "멤버 목록을 불러오지 못했습니다.");
        }
    }

    async function updateMemberPermission(sharedItemId, memberId, permission) {
        try {
            await APIClient.patch(
                "/api/shared-items/" + encodeURIComponent(sharedItemId) + "/members/" + encodeURIComponent(memberId) + "/permission",
                { permission }
            );
            showSharedItemMessage("success", "멤버 권한을 변경했어요.");
            await loadSharedItemMembers(sharedItemId);
        } catch (error) {
            handleError(error, "멤버 권한을 변경할 수 없어요.");
        }
    }

    async function revokeSharedItemMember(sharedItemId, member) {
        if (state.busy || !state.detailItem || !isOwner(state.detailItem)) {
            showSharedItemMessage("warning", "이 작업을 수행할 권한이 없어요.");
            return;
        }

        const revokedMemberId = typeof member === "object" ? member.memberId : member;
        if (!revokedMemberId) {
            showSharedItemMessage("warning", "접근 해제할 멤버를 확인할 수 없어요.");
            return;
        }

        const confirmed = await confirmRevokeWithRotation(
            typeof member === "object" ? member : { memberId: revokedMemberId }
        );
        if (!confirmed) return;

        state.busy = true;
        setRevokeButtonsBusy(true);

        try {
            if (!await requireVaultAuthOrRedirect()) return;
            if (!isSharedCryptoReady()) {
                showLockedSection("멤버 접근 해제에는 금고 열쇠 복원이 필요해요.");
                return;
            }

            showSharedItemMessage("info", "공유 비밀을 확인하고 있어요.");
            const detail = await getSharedItemDetail(sharedItemId);
            const decrypted = await SharedItemCrypto.decryptSharedItem(detail, { includeContent: true });

            showSharedItemMessage("info", "남아 있는 멤버 정보를 확인하고 있어요.");
            const context = await getRotationContext(sharedItemId);
            if (Number(detail.version) !== Number(context.version)) {
                showSharedItemMessage("warning", getRotationConflictMessage({ status: 409 }));
                await loadSharedItemDetail(sharedItemId);
                return;
            }

            const revokedMemberIds = [Number(revokedMemberId)];
            const remainingMembers = (context.members || []).filter(function (contextMember) {
                return contextMember.status === "ACTIVE"
                    && !revokedMemberIds.includes(Number(contextMember.memberId));
            });

            showSharedItemMessage("info", "새 공유 열쇠를 만들고 남아 있는 멤버에게만 전달하고 있어요.");
            const rotatedPayload = await SharedItemCrypto.createRotatedSharedItemPayload(
                decrypted.title,
                decrypted.content,
                remainingMembers
            );

            showSharedItemMessage("info", "공유 금고를 업데이트하고 있어요.");
            await rotateKey(sharedItemId, {
                expectedVersion: context.version,
                expectedKeyVersion: context.keyVersion,
                expectedMembershipVersion: context.membershipVersion,
                titleCipherBase64: rotatedPayload.titleCipherBase64,
                itemCipherBase64: rotatedPayload.itemCipherBase64,
                memberKeyWrappers: rotatedPayload.memberKeyWrappers,
                revokedMemberIds
            });

            showSharedItemMessage("success", "멤버 접근이 해제되었고 남아 있는 멤버만 새 공유 비밀을 열 수 있어요.");
            await loadSharedItemDetail(sharedItemId);
        } catch (error) {
            if (error?.isConflict || error?.status === 409) {
                showSharedItemMessage("warning", getRotationConflictMessage(error));
                await loadSharedItemDetail(sharedItemId);
                return;
            }
            handleError(error, "멤버 접근 해제와 키 재생성에 실패했어요.");
        } finally {
            state.busy = false;
            setRevokeButtonsBusy(false);
        }
    }

    async function createJoinRequestFromInviteTokenWithMessage(inviteToken, options = {}) {
        const successMessage = options.successMessage || "참여 요청을 보냈어요. 공유 비밀의 소유자가 승인하면 접근할 수 있어요.";
        const errorMessage = options.errorMessage || "참여 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요.";
        setBusy(byId("create-join-request-btn"), true);
        try {
            await createJoinRequestFromInviteToken(inviteToken);
            showSharedItemMessage("success", successMessage);
        } catch (error) {
            const code = getApiErrorCode(error);
            if (code === "INVITE_LINK_NOT_FOUND" || code === "INVITE_TOKEN_REQUIRED") {
                showSharedItemMessage("error", "초대 링크가 만료되었거나 올바르지 않아요.");
                throw error;
            }
            if (code === "SHARED_ITEM_MEMBER_ALREADY_EXISTS") {
                showSharedItemMessage("warning", "이미 참여 중인 공유 비밀이에요.");
                throw error;
            }
            if (code === "JOIN_REQUEST_ALREADY_EXISTS") {
                showSharedItemMessage("warning", "이미 참여 요청을 보낸 공유 비밀이에요.");
                throw error;
            }
            showSharedItemMessage("error", errorMessage);
            throw error;
        } finally {
            setBusy(byId("create-join-request-btn"), false);
        }
    }

    async function createJoinRequest(inviteToken) {
        try {
            await createJoinRequestFromInviteTokenWithMessage(inviteToken);
            return true;
        } catch (error) {
            return false;
        }
    }

    async function loadInvitePage() {
        const token = new URLSearchParams(window.location.search).get("token");
        if (!token) {
            showSharedItemMessage("error", "초대 링크를 확인할 수 없어요.");
            return;
        }

        try {
            const member = await AuthGuard.requireLogin();
            if (!member) return;
            if (member.authLevel !== "VAULT_AUTH") {
                showSharedItemMessage("warning", "참여 요청을 보내려면 금고 열기가 필요해요. 금고를 연 뒤 초대 링크를 다시 열어주세요.");
                byId("unlock-for-invite")?.classList.remove("hidden");
            } else {
                showSharedItemMessage("info", "초대 링크를 확인했어요. 참여 요청을 보낼 수 있어요.");
            }
            byId("create-join-request-btn")?.addEventListener("click", function () {
                createJoinRequest(token);
            });
        } catch (error) {
            handleError(error, "초대 링크를 처리할 수 없어요.");
        }
    }

    async function copyDetailContent() {
        const content = byId("detail-content")?.textContent || "";
        if (!content) {
            showSharedItemMessage("warning", "복사할 내용이 없어요.");
            return;
        }

        try {
            await navigator.clipboard.writeText(content);
            showSharedItemMessage("success", "내용을 복사했어요.");
        } catch (error) {
            showSharedItemMessage("warning", "브라우저가 자동 복사를 허용하지 않아요. 내용을 직접 선택해 복사해주세요.");
        }
    }

    function bindListPage() {
        showLockedSection();
        byId("show-shared-vault-restore-btn")?.addEventListener("click", showVaultRestoreSection);
        byId("restore-shared-vault-key-btn")?.addEventListener("click", restoreVaultKeyFromPassword);
        byId("shared-vault-master-password")?.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                restoreVaultKeyFromPassword();
            }
        });
        byId("open-shared-create-btn")?.addEventListener("click", openSharedCreateSheet);
        byId("open-shared-create-top-btn")?.addEventListener("click", openSharedCreateSheet);
        byId("close-shared-create-btn")?.addEventListener("click", closeSharedCreateSheet);
        byId("cancel-shared-create-btn")?.addEventListener("click", closeSharedCreateSheet);
        byId("shared-create-sheet")?.addEventListener("click", function (event) {
            if (event.target === byId("shared-create-sheet")) {
                closeSharedCreateSheet();
            }
        });
        byId("shared-create-form")?.addEventListener("submit", function (event) {
            event.preventDefault();
            createSharedItem();
        });
        byId("open-join-request-sheet-btn")?.addEventListener("click", openJoinRequestSheet);
        byId("close-shared-join-btn")?.addEventListener("click", closeJoinRequestSheet);
        byId("cancel-shared-join-btn")?.addEventListener("click", closeJoinRequestSheet);
        byId("shared-join-sheet")?.addEventListener("click", function (event) {
            if (event.target === byId("shared-join-sheet")) {
                closeJoinRequestSheet();
            }
        });
        byId("shared-join-form")?.addEventListener("submit", function (event) {
            event.preventDefault();
            submitJoinRequestFromSheet();
        });
        byId("shared-join-input")?.addEventListener("keydown", function (event) {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                submitJoinRequestFromSheet();
            }
        });
        byId("shared-search-input")?.addEventListener("input", renderCombinedSharedItems);
        byId("shared-sort-select")?.addEventListener("change", renderCombinedSharedItems);
        if (!byId("shared-create-form")) {
            byId("create-shared-item-btn")?.addEventListener("click", createSharedItem);
        }
        loadSharedItems();
    }

    function bindDetailPage() {
        const sharedItemId = new URLSearchParams(window.location.search).get("id");
        if (!sharedItemId) {
            showSharedItemMessage("error", "공유 비밀을 확인할 수 없어요.");
            return;
        }

        showLockedSection("공유 비밀 상세를 보려면 금고 열쇠 복원이 필요해요.");
        byId("show-shared-vault-restore-btn")?.addEventListener("click", showVaultRestoreSection);
        byId("restore-shared-vault-key-btn")?.addEventListener("click", restoreVaultKeyFromPassword);
        byId("update-shared-item-btn")?.addEventListener("click", function () {
            updateSharedItem(sharedItemId);
        });
        byId("delete-shared-item-btn")?.addEventListener("click", function () {
            deleteSharedItem(sharedItemId);
        });
        byId("create-invite-btn")?.addEventListener("click", function () {
            createInviteLink(sharedItemId);
        });
        byId("copy-invite-btn")?.addEventListener("click", copyInviteLink);
        byId("copy-detail-content-btn")?.addEventListener("click", copyDetailContent);
        bindRevokeConfirmModal();
        loadSharedItemDetail(sharedItemId);
    }

    window.SharedItemClient = {
        loadSharedItems,
        loadSharedItemDetail,
        createSharedItem,
        updateSharedItem,
        deleteSharedItem,
        getRotationContext,
        rotateKey,
        createInviteLink,
        createJoinRequestFromInviteToken,
        createJoinRequest,
        loadJoinRequests,
        approveJoinRequest,
        rejectJoinRequest,
        loadSharedItemMembers,
        updateMemberPermission,
        revokeSharedItemMember,
        renderSharedItemList,
        renderSharedItemDetail,
        renderJoinRequests,
        renderMembers,
        showSharedItemMessage,
        restoreVaultKeyFromPassword,
        bindListPage,
        bindDetailPage,
        loadInvitePage
    };
})();
