# AGENTS.md

# Project Rules for AI Coding Agents

This project is Zero Knowledge Vault.

The goal is to build a secure vault service where the server does not directly know the user's Master Password or plaintext Vault data.

AI coding agents must preserve the existing security model and avoid unrelated changes.

## Core Security Flow

OAuth2 Login
-> PRE_AUTH JWT
-> Master Password based SRP verification
-> VAULT_AUTH JWT
-> VaultKey restore
-> Client-side Vault encryption/decryption
-> Server stores only ciphertext and wrapped key material

## Core Rules

* Do not change API paths unless explicitly requested.
* Do not change DTO formats unless explicitly requested.
* Do not change package conventions unless explicitly requested.
* Do not change SRP calculation logic unless explicitly requested.
* Do not change WebCrypto encryption logic unless explicitly requested.
* Do not weaken PRE_AUTH / VAULT_AUTH separation.
* Do not allow `/api/vault/**` without VAULT_AUTH.
* Do not perform large refactors unless explicitly requested.
* Do not implement unrelated modules.
* Do not create mock-only controllers unless explicitly requested.
* Do not introduce React, Vue, Vite, Next.js, or Thymeleaf unless explicitly requested.

## Security Rules

Never log or display:

* Master Password
* deviceSecret
* SRP S
* SRP K
* SRP M1
* SRP M2
* verifier
* salt
* session key
* JWT
* OAuth token
* wrappedVaultKey
* VaultKey
* itemKey
* itemCipher
* indexCipher
* itemKeyCipher
* commitHash
* raw API response containing sensitive fields
* `.env` values

If a sensitive value must be mentioned in a report, replace it with `[REDACTED]`.

## Frontend Rules

The current frontend is Spring Boot static HTML + Vanilla JS.

Main path:

`src/main/resources/static`

Rules:

* Keep the existing static HTML structure unless explicitly requested.
* Mobile-first UI is preferred.
* Use existing CSS/JS files when possible.
* Do not expose raw JSON, Base64 ciphertext, internal IDs, or version fields to normal users.
* Home is a general account area.
* Vault is a sensitive area and must require Master Password based unlock.
* Vault item/index data must not be shown on Home.
* Use message areas instead of raw alert/debug output when possible.

## Backend Rules

Preserve the current layered structure.

When implementing a feature, use vertical slice implementation if backend changes are needed:

* Entity
* Repository
* Service
* Controller
* Request DTO
* Response DTO
* Validation
* Swagger/OpenAPI documentation
* Exception handling
* DB integration
* Tests, if applicable

Do not modify backend code for pure UI tasks.

## Vault Rules

The server must not know:

* Master Password
* VaultKey
* itemKey
* Vault index plaintext
* Vault item plaintext

The server may store:

* verifier
* saltAuth
* wrappedVaultKey
* saltWrap
* KDF params
* indexCipher
* itemCipher
* itemKeyCipher
* commitHash
* version
* itemId

Vault item/index updates must preserve version-based conflict control.

Do not silently overwrite newer ciphertext with older ciphertext.

## Current Known Limitations

The following are known improvement targets:

* SRP M2 verification should be completed.
* deviceSecret storage is currently web-MVP level and should be abstracted for future stronger storage.
* AES-GCM envelope format validation should be strengthened.
* commitHash server-side verification should be considered.
* `/api/pake/auth/**` authorization policy should be made explicit.
* Mobile-first UI should be improved across the frontend.

## Testing Rules

Before or after changes, run relevant checks when possible.

Common commands:

* `rg -n "console\\.log" src\\main\\resources\\static`
* `./gradlew.bat compileJava`

If a browser test is not possible, report that clearly.

## Reporting Format

After making changes, report:

## 변경 요약

## 수정한 파일

## 추가/변경된 API

## 보안 영향

## 동시성/정합성 영향

## 실행 또는 테스트 방법

## 가정한 점

## 한계 또는 추가 확인 필요 사항

If no code was changed, say:

코드 변경 없음. 분석만 수행함.

## Final Rule

Optimize for security model preservation, correctness, and maintainability.

Do not make the code look impressive at the cost of breaking the Zero Knowledge Vault design.


## PR 리뷰 가이드

PR 리뷰를 수행할 때는 다음 문서를 우선 참고한다.

PR 리뷰는 `.github/codex/prompts/pr-review-ko.md`의 기준을 따른다.

리뷰는 반드시 한국어로 작성한다.
사소한 스타일보다 보안, 권한, 트랜잭션, 동시성, 테스트 누락, API 호환성 문제를 우선 검토한다.
