# Zero Knowledge Vault

Zero Knowledge Vault는 서버가 사용자의 민감한 평문 데이터를 직접 알지 못하는 구조를 목표로 만든 보안 저장소 프로젝트입니다. 일반적인 로그인 기반 서비스는 인증이 끝나면 서버 권한으로 데이터를 조회할 수 있지만, 저는 이 구조가 비밀 정보를 다루는 서비스에서는 충분하지 않다고 판단했습니다.

그래서 이 프로젝트는 다음 문제의식에서 시작했습니다.

- 로그인 성공과 Vault 접근 권한은 분리되어야 한다
- 서버는 비밀번호와 평문 Vault 데이터를 직접 알지 못해야 한다
- 멀티 디바이스 환경에서는 동시 수정 충돌도 함께 고려해야 한다


## 핵심 구조

- OAuth 로그인 이후 `PRE_AUTH` 토큰 발급
- Master Password 기반 SRP 검증 성공 시 `VAULT_AUTH`로 권한 상승
- Vault 데이터는 `vault_index + vault_item` 구조로 분리
- 서버에는 암호문, verifier, wrapped key만 저장

이 프로젝트에서 제가 집중한 것은 “보안을 위한 암호 알고리즘 사용” 자체보다,

1. 인증 수준을 어떻게 나눌 것인가
2. 저장 구조를 어떻게 설계할 것인가
3. 오래된 스냅샷 기반 충돌을 어떻게 막을 것인가

였습니다.

---

# Zero Knowledge Vault - 인증 구조와 저장 모델

## 1. SRP 기반 Step-up Authentication

이 프로젝트에서는 로그인 직후 바로 Vault를 열 수 없도록 했습니다.

- OAuth 로그인 성공
- PRE_AUTH JWT 발급
- 사용자가 Master Password 입력
- 클라이언트와 서버가 SRP 기반 검증 수행
- 검증 성공 시 VAULT_AUTH JWT 재발급

이 방식의 핵심은 서버가 사용자의 비밀번호를 직접 받지 않고도 사용자를 검증할 수 있다는 점입니다. 즉, 계정 식별과 Vault 접근 권한을 분리해 보안 단계를 한 층 더 세웠습니다.

## 2. Vault Index + Item 분리 구조

Vault 데이터는 전체 구조를 담는 `vault_index`와 개별 데이터인 `vault_item`으로 분리했습니다.

### vault_index

- item 목록
- item hash 목록
- folder/meta 정보
- commit hash

### vault_item

- item_key_cipher
- item_cipher
- version
- deleted_at

이 구조의 장점은 전체 Vault를 매번 다시 쓰지 않고도 item 단위 수정이 가능하다는 점입니다. 또한 item을 tombstone 방식으로 삭제 처리할 수 있어 멀티 디바이스 동기화와 rollback 탐지에도 유리합니다.

## 3. commit hash / digest

Index 내부에 item hash와 items_digest를 포함시켜, item tampering이나 partial rollback을 감지할 수 있도록 설계했습니다. 

---

# Zero Knowledge Vault - 공유 기능과 2단계 동시성 제어

## 1. item 단위 공유 설계

Vault 전체를 공유하는 것이 아니라, 특정 item만 안전하게 공유할 수 있는 구조도 함께 설계했습니다.

핵심 아이디어는 다음과 같습니다.

- itemCipher 자체를 새로 만들지 않음
- 원본 itemKey를 수신자 공개키로 다시 감싸 전달
- 서버는 여전히 평문도 itemKey 원문도 알 수 없음

즉 공유의 본질은 데이터를 복사하는 것이 아니라, `특정 item을 복호화할 수 있는 권한(itemKey)`을 제한적으로 전달하는 것입니다.

이 구조를 위해 다음 도메인을 분리했습니다.

- `member_share_key`
- `shared_vault_item`

개인 Vault와 공유 메타데이터를 분리함으로써 revoke, 만료, read-only 정책 같은 확장도 가능하게 했습니다.

## 2. 왜 2단계 동시성 제어가 필요했는가

Vault는 일반 DB CRUD와 달리, 클라이언트가 과거 시점의 암호문 상태를 받아 로컬에서 편집한 뒤 나중에 다시 업로드하는 구조입니다. 따라서 핵심 문제는 짧은 DB 락 충돌보다 `stale snapshot 기반 lost update`에 가깝습니다.

그래서 동시성 제어를 두 단계로 나눴습니다.

### 1단계. 존재성 경쟁 차단

동시에 같은 item을 처음 생성하는 상황에서는 애플리케이션 조회만으로는 원자성을 보장할 수 없습니다. 그래서 `(member_id, item_id)` unique constraint를 두어 DB가 최종적으로 중복 insert를 차단하도록 했습니다.

### 2단계. version 기반 OCC

이미 존재하는 item이나 index 수정은 `WHERE version = ?` 조건을 가진 조건부 update로 처리했습니다.

- 영향 row = 1 -> 정상 갱신
- 영향 row = 0 -> stale snapshot 충돌

이 방식은 긴 DB 락을 유지하지 않고도, 멀티 디바이스 환경에서 오래된 상태를 덮어쓰는 문제를 감지할 수 있다는 장점이 있습니다.
