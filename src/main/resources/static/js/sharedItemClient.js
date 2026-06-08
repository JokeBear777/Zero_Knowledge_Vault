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
        busy: false
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
        setElementVisible("shared-create-section", visible);
        setElementVisible("shared-owner-list-section", visible);
        setElementVisible("shared-participant-list-section", visible);
        setElementVisible("shared-invite-list-section", visible);
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

    function showLockedSection(message) {
        setElementVisible("shared-vault-locked-section", true);
        setElementVisible("shared-vault-open-section", false);
        setSharedContentVisible(false);
        if (message) {
            showSharedItemMessage("warning", message);
        }
    }

    function showVaultRestoreSection() {
        setElementVisible("shared-vault-locked-section", false);
        setElementVisible("shared-vault-open-section", true);
        setSharedContentVisible(false);
    }

    function showSharedContent() {
        setElementVisible("shared-vault-locked-section", false);
        setElementVisible("shared-vault-open-section", false);
        setSharedContentVisible(true);
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

        let ownerCount = 0;
        let participantCount = 0;

        for (const item of items || []) {
            let title = "";
            try {
                title = (await SharedItemCrypto.decryptSharedItem(item)).title;
            } catch (error) {
                title = "열 수 없는 비밀";
            }

            if (isOwner(item)) {
                ownerCount += 1;
                renderItemCard(ownerList, item, title);
            } else {
                participantCount += 1;
                renderItemCard(participantList, item, title);
            }
        }

        if (ownerCount === 0) renderEmpty(ownerList, "내가 만든 공유 비밀이 없어요.");
        if (participantCount === 0) renderEmpty(participantList, "참여 중인 공유 비밀이 없어요.");
    }

    async function loadSharedItems() {
        try {
            if (!await requireVaultAuthOrRedirect()) return;
            if (!isSharedCryptoReady()) {
                showLockedSection("공유 비밀을 보려면 금고 열쇠 복원이 필요해요.");
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
            const response = await APIClient.post("/api/shared-items", payload);
            showSharedItemMessage("success", "공유 비밀을 만들었어요.");
            window.location.href = "/shared-item-detail.html?id=" + encodeURIComponent(response.sharedItemId);
        } catch (error) {
            handleError(error, "공유 비밀을 만들지 못했어요.");
        } finally {
            state.busy = false;
            setBusy(byId("create-shared-item-btn"), false);
        }
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
            const item = await APIClient.get("/api/shared-items/" + encodeURIComponent(sharedItemId));
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

            if (member.role === "PARTICIPANT") {
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
                revokeButton.textContent = "권한 취소";
                revokeButton.addEventListener("click", function () {
                    revokeSharedItemMember(state.detailItem.sharedItemId, member.memberId);
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

    async function revokeSharedItemMember(sharedItemId, memberId) {
        if (!confirm("이 멤버의 접근 권한을 취소할까요?")) return;

        try {
            await APIClient.post(
                "/api/shared-items/" + encodeURIComponent(sharedItemId) + "/members/" + encodeURIComponent(memberId) + "/revoke",
                {}
            );
            showSharedItemMessage("success", "멤버 권한을 취소했어요.");
            await loadSharedItemMembers(sharedItemId);
        } catch (error) {
            handleError(error, "멤버 권한을 취소할 수 없어요.");
        }
    }

    async function createJoinRequest(inviteToken) {
        setBusy(byId("create-join-request-btn"), true);
        try {
            await APIClient.post("/api/shared-items/invite-links/" + encodeURIComponent(inviteToken) + "/join-requests", {});
            showSharedItemMessage("success", "참여 요청을 보냈어요. owner가 승인하면 공유 금고에서 확인할 수 있어요.");
        } catch (error) {
            handleError(error, "초대 링크가 만료되었거나 이미 처리된 요청이에요.");
        } finally {
            setBusy(byId("create-join-request-btn"), false);
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
        showLockedSection("공유 비밀을 보려면 금고 열쇠 복원이 필요해요.");
        byId("show-shared-vault-restore-btn")?.addEventListener("click", showVaultRestoreSection);
        byId("restore-shared-vault-key-btn")?.addEventListener("click", restoreVaultKeyFromPassword);
        byId("create-shared-item-btn")?.addEventListener("click", createSharedItem);
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
        loadSharedItemDetail(sharedItemId);
    }

    window.SharedItemClient = {
        loadSharedItems,
        loadSharedItemDetail,
        createSharedItem,
        updateSharedItem,
        deleteSharedItem,
        createInviteLink,
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
