# Zero Knowledge Vault - Codex Project Guide

## 0. 문서 목적

이 문서는 Codex가 Zero Knowledge Vault 프로젝트를 이해하고 안전하게 작업하기 위한 단일 프로젝트 가이드입니다.

이 프로젝트는 서버가 사용자의 Master Password와 Vault 평문 데이터를 직접 알지 못하도록 설계한 보안 저장소 프로젝트입니다.

Codex는 기능을 빠르게 추가하는 것보다 다음을 우선해야 합니다.

- 보안 흐름 유지
- 민감정보 로그 제거
- 인증 권한 분리 유지
- Vault 데이터 정합성 보장
- 멀티 디바이스 충돌 방지
- 기존 설계 의도 훼손 방지
- 불필요한 대규모 리팩토링 금지

---

## 1. 프로젝트 개요

Zero Knowledge Vault는 서버가 사용자의 민감한 평문 데이터를 직접 알지 못하는 구조를 목표로 만든 보안 저장소 프로젝트입니다.

일반적인 로그인 기반 서비스는 인증이 끝나면 서버 권한으로 데이터를 조회할 수 있습니다. 하지만 비밀번호, 개인 메모, 인증 정보처럼 민감한 데이터를 다루는 저장소에서는 로그인 성공만으로 모든 데이터 접근을 허용하는 구조가 충분하지 않습니다.

이 프로젝트는 다음 문제의식에서 시작했습니다.

```text
1. 로그인 성공과 Vault 접근 권한은 분리되어야 한다.
2. 서버는 Master Password를 직접 알지 못해야 한다.
3. 서버는 Vault 평문 데이터를 직접 알지 못해야 한다.
4. 멀티 디바이스 환경에서는 오래된 데이터가 최신 데이터를 덮어쓰는 문제를 막아야 한다.
2. 현재 프로젝트 상태 요약

현재 프로젝트는 Spring Boot 단일 프로젝트입니다.

별도 React/Vue 프론트엔드 프로젝트는 없습니다. 프론트엔드는 Spring Boot 정적 리소스 기반 HTML/JS입니다.

현재 구조는 다음과 같습니다.

Zero_Knowledge_Vault
├── src
│   ├── main
│   │   ├── java
│   │   │   └── Zero_Knowledge_Vault
│   │   │       ├── domain
│   │   │       │   ├── member
│   │   │       │   ├── auth
│   │   │       │   └── vault
│   │   │       ├── infra
│   │   │       │   ├── security
│   │   │       │   └── QueryDsl
│   │   │       └── global
│   │   └── resources
│   │       ├── static
│   │       └── application.yml
│   └── test
├── docker-compose.yml
├── build.gradle
├── settings.gradle
└── README.md

현재 프로젝트는 운영 완성형 서비스라기보다는 보안 구조를 검증하는 프로토타입에 가깝습니다.

핵심 백엔드 구조는 구현되어 있으나, 다음 부분은 우선 개선 대상입니다.

1. 민감 로그 제거
2. JWT 환경변수 정합성 확인
3. Vault key material 조회 흐름 확인 또는 구현
4. /api/pake/auth/** 권한 정책 명시
5. /api/vault/** VAULT_AUTH 접근 제한 테스트
6. Vault item/index version 충돌 테스트
7. .env.example 작성
8. README 정리
3. 기술 스택
Backend
Java 17
Spring Boot
Spring Security
Spring Data JPA
QueryDSL
OAuth2 Client
JWT
MySQL
Redis
Gradle
Frontend
HTML
CSS
Vanilla JavaScript
WebCrypto API

현재 프론트엔드는 별도 SPA 프로젝트가 아니라 Spring Boot 정적 리소스 기반의 데모 UI입니다.

src/main/resources/static
Infrastructure
Docker Compose
MySQL
Redis

현재 docker-compose.yml에는 MySQL과 Redis 중심으로 구성되어 있습니다.

4. 핵심 인증 흐름

이 프로젝트의 핵심 흐름은 다음과 같습니다.

OAuth2 Login
    ↓
PRE_AUTH JWT 발급
    ↓
Master Password 입력
    ↓
SRP 기반 검증
    ↓
VAULT_AUTH JWT 발급
    ↓
Vault API 접근

OAuth 로그인에 성공해도 바로 Vault에 접근할 수 없습니다.

OAuth 로그인 직후에는 PRE_AUTH 권한만 부여됩니다.

Vault에 접근하려면 사용자가 Master Password 기반 SRP 검증을 통과해야 합니다.

SRP 검증에 성공하면 VAULT_AUTH 권한이 부여되고, 이때부터 Vault API에 접근할 수 있습니다.

5. 핵심 설계
5.1 OAuth 로그인과 Vault 접근 권한 분리

OAuth 로그인은 사용자의 계정 식별을 담당합니다.

Vault 접근은 별도의 Master Password 기반 SRP 검증을 통해 허용됩니다.

즉, 이 프로젝트에서는 다음 두 권한을 분리합니다.

PRE_AUTH   = OAuth 로그인은 되었지만 Vault 접근은 불가능한 상태
VAULT_AUTH = SRP 검증까지 완료되어 Vault 접근이 가능한 상태

/api/vault/** API는 반드시 VAULT_AUTH 권한이 있어야 접근 가능해야 합니다.

5.2 SRP 기반 Step-up Authentication

이 프로젝트는 Master Password를 서버로 직접 전송하지 않는 구조를 지향합니다.

Master Password 검증에는 SRP 기반 인증 흐름을 사용합니다.

서버는 사용자의 Master Password를 저장하지 않고, SRP 검증에 필요한 verifier와 salt를 저장합니다.

이를 통해 서버가 사용자의 Master Password 평문을 직접 알지 못한 상태에서도 사용자를 검증할 수 있습니다.

5.3 Vault 데이터 암호문 저장

Vault 데이터는 서버에 평문으로 저장되지 않습니다.

서버는 다음과 같은 암호화된 데이터와 메타데이터만 저장합니다.

- wrappedVaultKey
- saltWrap
- kdfParams
- indexCipher
- itemCipher
- itemKeyCipher
- version
- commitHash

Vault 데이터의 복호화는 클라이언트 측에서 수행하는 것을 전제로 합니다.

서버는 Vault 평문이나 Master Password를 알면 안 됩니다.

5.4 Vault Index와 Vault Item 분리

Vault 데이터는 크게 두 구조로 나뉩니다.

vault_index
vault_item

vault_index는 Vault의 전체 목록, 메타데이터, item hash, commit hash 등을 담는 구조입니다.

vault_item은 개별 Vault 항목의 암호문을 저장하는 구조입니다.

이렇게 분리하면 전체 Vault를 매번 다시 저장하지 않고, item 단위로 수정할 수 있습니다.

5.5 멀티 디바이스 충돌 제어

Vault는 일반적인 CRUD 서비스와 다릅니다.

클라이언트는 서버에서 암호문을 받아 로컬에서 복호화하고, 수정한 뒤 다시 암호문으로 업로드합니다.

이때 여러 디바이스가 동시에 같은 데이터를 수정하면 오래된 스냅샷이 최신 데이터를 덮어쓸 수 있습니다.

이를 막기 위해 version 기반 조건부 update를 사용합니다.

예시:

UPDATE vault_item
SET item_cipher = ?,
    version = version + 1
WHERE member_id = ?
  AND item_id = ?
  AND version = ?

요청 version과 DB version이 일치하면 수정이 성공합니다.

일치하지 않으면 stale snapshot 충돌로 판단하고 업데이트를 거부합니다.

6. 주요 도메인
6.1 Member

회원 계정과 OAuth 로그인 사용자 정보를 관리합니다.

주요 기능:

- OAuth 가입
- 사용자 정보 조회
- 회원 상태 조회
- 계정 비활성화
6.2 Auth / PAKE

Master Password 기반 SRP 인증을 담당합니다.

주요 기능:

- SRP 등록 초기화
- SRP verifier 등록
- SRP unlock 초기화
- SRP proof 검증
- VAULT_AUTH 토큰 발급
6.3 Vault

Vault 데이터 저장과 동시성 제어를 담당합니다.

주요 기능:

- Vault setup
- Vault index 저장/조회
- Vault item 생성
- Vault item 수정
- Vault item 삭제
- tombstone 기반 삭제 처리
- version 기반 충돌 감지
7. 주요 API 구조

현재 확인된 주요 API 영역은 다음과 같습니다.

/api
/api/member
/api/pake
/api/pake/auth
/api/vault

예상 역할은 다음과 같습니다.

/api/sign-up                OAuth 기반 회원가입
/api/oauth-info             OAuth 가입 정보 조회
/api/member/me              내 정보 또는 회원 상태 조회
/api/pake/register          SRP 등록
/api/pake/auth/init         SRP unlock init
/api/pake/auth/prove        SRP proof 검증
/api/vault/setup            Vault 초기화
/api/vault/index            Vault index 조회/수정
/api/vault/item             Vault item 생성/수정/삭제

주의할 점:

- /api/vault/** 는 VAULT_AUTH 권한이 필요해야 한다.
- /api/pake/auth/** 는 PRE_AUTH 상태에서 접근 가능해야 한다.
- SecurityConfig의 경로 정책과 실제 Controller 경로가 일치해야 한다.
- Vault key material 조회 API가 필요한지 확인해야 한다.
8. 주요 데이터 모델
8.1 member

사용자 계정 정보를 저장합니다.

주요 필드:

memberId
email
name
mobile
memberRole

주의할 점:

- email unique constraint 여부를 확인해야 한다.
- OAuth 중복 가입 경쟁 상황을 고려해야 한다.
8.2 member_auth_pake

SRP 인증 정보를 저장합니다.

주요 필드:

memberId
saltAuth
verifier
kdfParams
status
authVersion

주의할 점:

- Master Password 평문을 저장하면 안 된다.
- verifier, salt 등 민감한 인증 재료를 로그로 출력하면 안 된다.
- SRP 등록 중복 요청 처리를 확인해야 한다.
8.3 member_vault_key_material

VaultKey wrapping 정보를 저장합니다.

주요 필드:

memberId
wrappedVaultKey
saltWrap
kdfParams
keyVersion
status

주의할 점:

- wrappedVaultKey를 로그로 출력하면 안 된다.
- key material 조회 흐름이 실제 API로 완성되어 있는지 확인해야 한다.
- rotate, disable 같은 상태 전환 메서드가 있다면 실제 유스케이스와 연결되어 있는지 확인해야 한다.
8.4 vault_index

Vault 전체 index 암호문을 저장합니다.

주요 필드:

memberId
indexCipher
commitHash
version

주의할 점:

- index 수정은 version 조건부 update로 처리해야 한다.
- commitHash/items digest 등을 통해 부분 rollback 또는 tampering 탐지 의도를 유지해야 한다.
8.5 vault_item

개별 Vault item 암호문을 저장합니다.

주요 필드:

memberId
itemId
itemKeyCipher
itemCipher
version
deletedAt

특징:

- item 단위 수정 가능
- version 기반 OCC 가능
- deletedAt 기반 tombstone 삭제 가능
- member_id + item_id unique constraint 필요

주의할 점:

- update 시 version 조건을 반드시 사용해야 한다.
- deleted item을 update하지 않도록 조건을 명확히 해야 한다.
- 중복 생성은 DB unique constraint로 최종 방어해야 한다.
9. Codex 작업 원칙

Codex는 이 프로젝트를 수정할 때 다음 원칙을 반드시 지켜야 합니다.

1. 서버가 Master Password를 직접 저장하거나 로그로 남기면 안 된다.
2. 서버가 Vault 평문 데이터를 저장하거나 로그로 남기면 안 된다.
3. JWT, OAuth token, SRP session key, verifier, deviceSecret, wrapped key 등 민감값을 로그로 출력하면 안 된다.
4. Vault 접근 권한은 일반 로그인 권한과 분리되어야 한다.
5. /api/vault/** API는 VAULT_AUTH 권한 없이는 접근할 수 없어야 한다.
6. 멀티 디바이스 수정 충돌을 막기 위해 version 조건부 update 구조를 유지해야 한다.
7. 중복 생성 경쟁은 DB unique constraint 또는 원자적 처리로 방어해야 한다.
8. 기존 보안 흐름을 단순화하거나 우회하지 말아야 한다.
9. 명시 요청 없이 대규모 리팩토링을 하지 말아야 한다.
10. 기능 추가보다 보안과 정합성을 우선해야 한다.
10. 보안상 절대 하면 안 되는 것

다음 작업은 하지 마세요.

- Master Password를 서버 로그에 출력
- Master Password를 DB에 저장
- Vault 평문 데이터를 서버 로그에 출력
- Vault 평문 데이터를 DB에 저장
- JWT secret 출력
- JWT access token 출력
- OAuth access token 또는 refresh token 출력
- SRP S, K, M1, M2, verifier, salt를 디버그 로그로 출력
- deviceSecret 출력
- wrappedVaultKey 출력
- private key, secret, .env 값을 응답이나 로그에 출력
- VAULT_AUTH 없이 /api/vault/** 접근 허용
- version 조건 없는 Vault item update
- stale snapshot 충돌을 무시하고 덮어쓰기

민감값을 확인해야 할 경우 값 자체를 출력하지 말고 다음처럼 표시하세요.

[REDACTED]
11. 코드 수정 우선순위
11.1 1순위: 보안 위험 제거

가장 먼저 확인하고 수정해야 할 부분입니다.

- 민감정보 console.log 제거
- 민감정보 log.info/log.debug 제거
- System.out.println 제거
- JWT/OAuth/SRP 관련 민감값 로그 제거
- .env와 application.yml 변수명 정합성 확인
11.2 2순위: 인증 흐름 명확화
- PRE_AUTH와 VAULT_AUTH 권한 분리 유지
- /api/pake/auth/** 접근 권한 명시
- /api/vault/**는 VAULT_AUTH 필수로 유지
- SecurityConfig에서 의도와 실제 API 경로가 일치하는지 확인
11.3 3순위: Vault 핵심 시나리오 안정화
- Vault setup 흐름
- Vault key material 조회 흐름
- Vault index 조회/수정 흐름
- Vault item 생성/수정/삭제 흐름
- tombstone 삭제 흐름
11.4 4순위: 테스트 보강
- OAuth 후 PRE_AUTH 발급 테스트
- SRP proof 성공 후 VAULT_AUTH 발급 테스트
- VAULT_AUTH 없이 Vault API 접근 실패 테스트
- Vault item version 충돌 테스트
- 중복 item 생성 충돌 테스트
- 민감 로그 미출력 검증
12. 코딩 스타일

기존 프로젝트의 패키지 구조와 스타일을 유지하세요.

불필요한 대규모 리팩토링은 하지 마세요.

다음 원칙을 지켜주세요.

1. Controller는 요청/응답 처리에 집중한다.
2. Service는 유스케이스 흐름을 담당한다.
3. Repository는 DB 접근을 담당한다.
4. 보안 정책은 SecurityConfig와 관련 필터/핸들러에 명확히 둔다.
5. DTO와 Entity를 무분별하게 섞지 않는다.
6. 예외는 가능한 한 의미 있는 커스텀 예외와 에러 코드로 처리한다.
7. 트랜잭션 경계를 명확히 유지한다.
8. 인증/인가 관련 코드는 변경 전에 영향 범위를 먼저 분석한다.
13. Vault 동시성 규칙

Vault 데이터는 클라이언트가 과거 시점의 암호문을 받아 로컬에서 수정한 뒤 다시 업로드하는 구조입니다.

따라서 일반적인 짧은 DB 락 충돌보다 stale snapshot 문제가 중요합니다.

다음 규칙을 지켜주세요.

1. item 생성은 unique constraint로 중복 생성을 방어한다.
2. item 수정은 현재 version과 요청 version이 일치할 때만 성공해야 한다.
3. index 수정도 version 조건부 update로 처리해야 한다.
4. update 결과 row count가 0이면 stale snapshot 충돌로 처리해야 한다.
5. 충돌이 발생했을 때 조용히 덮어쓰면 안 된다.
6. delete는 가능하면 tombstone 방식으로 처리한다.
7. deleted item을 update하지 않도록 조건을 명확히 해야 한다.
14. 프론트엔드 작업 시 주의사항

현재 프론트는 정적 HTML/JS 기반 데모 UI입니다.

경로:

src/main/resources/static

작업 시 다음을 지켜주세요.

1. 프론트에서 민감값을 console.log로 출력하지 않는다.
2. deviceSecret, SRP 값, token, key material을 콘솔에 출력하지 않는다.
3. 테스트용 Base64 인코딩과 실제 암호화 흐름을 혼동하지 않는다.
4. 데모 UI와 실제 보안 플로우를 구분해서 주석이나 문서로 명확히 남긴다.
5. HttpOnly cookie 기반 인증 흐름을 유지한다.
6. 임의로 React/Vue 같은 프레임워크를 도입하지 않는다.
15. 인프라 작업 시 주의사항

현재 docker-compose.yml에는 MySQL과 Redis가 중심으로 구성되어 있습니다.

주의할 점:

1. 실제 secret을 docker-compose.yml에 직접 넣지 않는다.
2. .env 파일은 Git에 커밋하지 않는다.
3. .env.example에는 예시 값만 넣는다.
4. 운영 profile에서는 SQL log를 끄는 것을 권장한다.
5. 운영 profile에서는 cookie Secure=true를 사용해야 한다.
6. 운영 환경에서는 ddl-auto:update 사용을 피하고 migration 도구를 고려한다.
7. Redis가 compose에 있더라도 실제 앱 연동 여부를 먼저 확인한다.
16. 테스트 실행 규칙

테스트나 빌드를 실행하기 전에는 어떤 명령을 실행할지 먼저 설명하세요.

예시:

./gradlew test
./gradlew clean build

테스트 실패 시 임의로 기능을 크게 바꾸지 말고, 실패 원인을 먼저 분석하세요.

17. 금지 작업

다음 작업은 명시 요청 없이는 하지 마세요.

- 대규모 패키지 구조 변경
- 인증 흐름 단순화
- SRP 제거
- VAULT_AUTH 제거
- SecurityConfig 전체 재작성
- Entity 필드 삭제
- DB 스키마를 깨는 변경
- 프론트 프레임워크 신규 도입
- Redis 의존 기능 추가
- Docker 배포 구조 대규모 변경
- 임의의 새 기능 추가
- 민감정보 출력
- .env 내용 출력
18. Codex 응답 형식

작업 결과를 설명할 때는 다음 형식으로 정리하세요.

## 변경 요약

## 변경 파일

## 보안 영향

## 동시성/정합성 영향

## 테스트 결과

## 추가 확인 필요 사항

확실하지 않은 내용은 추측하지 말고 다음처럼 표시하세요.

확인됨
추정
미확인
위험
19. 현재 우선 개선 대상

현재 프로젝트에서 우선적으로 개선해야 할 대상은 다음입니다.

1. 민감 로그 제거
2. JWT env 변수명 정리
3. Vault key material 조회 API 흐름 확인 또는 구현
4. /api/pake/auth/** 권한 정책 명시
5. /api/vault/** VAULT_AUTH 접근 제한 테스트
6. Vault item/index version 충돌 테스트
7. .env.example 작성
8. README 정리
20. 포트폴리오 관점 핵심 설명

이 프로젝트를 포트폴리오에서 설명할 때 핵심은 다음입니다.

1. OAuth 로그인과 Vault 접근 권한을 PRE_AUTH / VAULT_AUTH로 분리했다.
2. Master Password를 서버로 직접 보내지 않기 위해 SRP 기반 인증 흐름을 적용했다.
3. 서버는 Vault 평문이 아니라 암호문과 key wrapping metadata만 저장한다.
4. Vault 데이터를 index와 item으로 분리해 item 단위 수정을 가능하게 했다.
5. 멀티 디바이스 환경에서 stale snapshot이 최신 데이터를 덮어쓰지 않도록 version 조건부 update를 적용했다.
6. item 삭제는 hard delete가 아니라 tombstone 방식으로 처리할 수 있도록 설계했다.
21. 현재 한계

현재 프로젝트는 운영 가능한 완성형 보안 서비스라기보다는 보안 구조를 검증하기 위한 프로토타입에 가깝습니다.

현재 한계는 다음과 같습니다.

- 정적 HTML 기반 데모 UI
- 일부 프론트 암호화 흐름이 테스트용 Base64 처리에 가까움
- 공유 기능 미구현
- Vault key material 조회 흐름 확인 필요
- 테스트 코드 부족
- 운영 profile 미정리
- Dockerfile/CI/CD 미구성
- ddl-auto:update 사용 가능성
- 민감 로그 제거 필요

이 한계를 숨기지 말고, 개선 과제로 명확히 관리하세요.

22. 최종 방향

이 프로젝트의 최종 방향은 다음입니다.

보안 알고리즘을 단순히 사용한 프로젝트가 아니라,
로그인 권한과 비밀 저장소 접근 권한을 분리하고,
서버가 평문을 알 수 없는 저장 구조를 설계하며,
멀티 디바이스 충돌까지 고려한 Vault 백엔드 프로젝트로 발전시키는 것.

Codex는 이 방향을 해치지 않는 범위에서만 코드를 수정해야 합니다.