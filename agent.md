# Project Rules for AI Coding Agents

This project is Zero Knowledge Vault.

The goal is to build a secure vault service where the server does not directly know the user's Master Password or plaintext Vault data.

Before making changes, understand the requested scope and modify only the relevant files.

## Core Project Flow

OAuth2 Login
-> PRE_AUTH JWT
-> Master Password based SRP verification
-> VAULT_AUTH JWT
-> Vault API access

Rules:

- PRE_AUTH means the user is logged in but cannot access Vault data.
- VAULT_AUTH means the user passed SRP verification and can access Vault APIs.
- /api/pake/auth/** should be accessible after PRE_AUTH.
- /api/vault/** must require VAULT_AUTH.

## Implementation Rules

Follow vertical slice implementation.

When implementing a feature, include the necessary layers:

- Entity
- Repository
- Service
- Controller
- Request/Response DTO
- Validation
- Swagger/OpenAPI documentation
- Exception handling
- DB integration
- Tests, if applicable

Do not implement unrelated modules.

Do not create mock-only controllers unless explicitly requested.

Use the existing package structure and response format.

Do not change API paths, response formats, package conventions, security authority names, entity relationships, or transaction boundaries unless explicitly requested.

Do not perform large refactors or broad formatting-only changes unless explicitly requested.

## Security Rules

Never do the following:

- Store Master Password in the database
- Send Master Password to the server as plaintext
- Log Master Password
- Log Vault plaintext data
- Log JWT secret
- Log JWT access token
- Log OAuth access token or refresh token
- Log SRP S, K, M1, M2, verifier, salt, or session key
- Log deviceSecret
- Log wrappedVaultKey
- Print .env values
- Allow /api/vault/** without VAULT_AUTH
- Update Vault item without version checking
- Ignore stale snapshot conflicts

If a sensitive value must be mentioned, replace it with [REDACTED].

## Vault Concurrency Rules

Vault data is edited on the client side and uploaded back as ciphertext.

Preserve these rules:

- Vault item creation must be protected by a unique constraint or equivalent atomic check.
- Vault item update must use version-based conditional update.
- Vault index update must use version-based conditional update.
- If affected row count is 0, treat it as a stale snapshot conflict.
- Never silently overwrite newer ciphertext with older ciphertext.
- Deleted items should use tombstone style if the existing design uses deletedAt.
- Do not update deleted items unless explicitly required.

## Frontend Rules

The current frontend is static HTML/JavaScript under:

src/main/resources/static

Rules:

- Do not introduce React, Vue, Next.js, Vite, or another frontend framework unless explicitly requested.
- Do not console.log sensitive values.
- Do not print deviceSecret, SRP values, JWT, OAuth tokens, or wrappedVaultKey.
- Keep HttpOnly cookie based authentication flow.
- Do not confuse Base64 demo encoding with real encryption.

## Infrastructure Rules

- Do not commit real secrets.
- Do not print .env values.
- Use .env.example for example values only.
- Do not hardcode secrets in docker-compose.yml or application.yml.
- Check whether Redis is actually used before adding Redis-dependent logic.
- Do not add Docker, Redis, CI/CD, or deployment changes unless explicitly requested.

## Testing Rules

Before running tests or build commands, state which command will be run.

Common commands:

- ./gradlew test
- ./gradlew clean build

For security-sensitive changes, prefer tests for:

- PRE_AUTH issuance
- VAULT_AUTH issuance
- /api/vault/** blocked without VAULT_AUTH
- Vault item/index version conflict
- Duplicate item creation conflict
- Sensitive values not logged

## Prohibited Changes Without Explicit Request

Do not perform these changes unless explicitly requested:

- Large package restructuring
- Authentication flow simplification
- Removing SRP
- Removing PRE_AUTH / VAULT_AUTH separation
- Rewriting SecurityConfig entirely
- Changing existing API paths
- Changing response formats
- Removing entity fields
- Breaking DB schema compatibility
- Adding a new frontend framework
- Adding Redis-dependent behavior without confirming usage
- Adding deployment structure unrelated to the task
- Implementing unrelated modules
- Creating mock-only controllers
- Printing sensitive values
- Printing .env contents

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

## Uncertainty Labels

Use these labels when reporting findings:

- 확인됨: 코드나 설정 파일에서 직접 확인함
- 추정: 구조상 가능성이 높지만 추가 확인 필요
- 미확인: 관련 파일을 찾지 못함
- 위험: 보안, 데이터 정합성, 배포 장애 가능성이 있음

Do not present guesses as confirmed facts.

## Final Rule

Do not optimize for making the code look impressive.

Optimize for preserving the security model, correctness, and maintainability.

This project should remain a Zero Knowledge Vault backend where:

- Login authority and Vault authority are separated.
- The server does not know the user's Master Password.
- The server does not store Vault plaintext.
- Multi-device stale updates are detected and rejected.