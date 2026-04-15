# 티켓팅 연습 플랫폼 — DB 스키마 설계

> PostgreSQL 기준, JPA 엔티티 설계 시 참고용  
> 대기열(Queue)은 Redis로 처리하므로 별도 테이블 없음.  
> **티켓팅 사이트별 UI**는 프론트엔드 템플릿으로만 관리 (DB 사용 X).

---

## 1. ER 개요

```
┌─────────────┐                    ┌─────────────────┐
│   users     │────────────────────│  performances   │
│             │                    │  (created_by)   │
└──────┬──────┘                    └────────┬────────┘
       │                                   │
       │   ┌─────────────────┐             │
       └───│    bookings     │◄────────────┤
           └────────┬────────┘             │
                    │            ┌─────────┴────────┐
                    └────────────│      seats      │
                                 └─────────────────┘
```

---

## 2. 테이블 정의

### 2.1. `users` — 회원

**회원 정보 저장 테이블**

- **용도:** 로그인/회원가입, 예매 주체, 마이페이지 예매 내역 조회, 좌석 락 소유자 식별

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| idx | BIGSERIAL | PK | 회원 식별자 |
| id | VARCHAR(255) | NOT NULL, UNIQUE | 로그인 아이디 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 비밀번호 |
| name | VARCHAR(100) | NOT NULL | 이름 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 가입 시각 |
| updated_at | TIMESTAMP | NULL, DEFAULT now() | 수정 시각 |
| deleted_at | TIMESTAMP | NULL | 회원 삭제 시, 삭제 시간 기록 (Soft Delete) |

---

### 2.2. `performances` — 공연 정보

**공연 정보 저장 테이블**

- URL HTML 파싱 후, 인터파크 공연 상세 정보 저장
- 사용자당 최대 5개의 공연 정보만 저장 (soft delete로 초과분 관리)
- "예매하기" 버튼 클릭 시 DB에 저장

**저장 정책**

- 파싱 후 UI 표시: DB 저장 안함 (메모리/응답만)
- "예매하기" 클릭 → `/api/performance/save` POST 요청 시 DB 저장
- 사용자당 5개 초과 시: 가장 오래된 공연 soft delete 후 새로 저장

**인덱스**

- `(start_date)` — 예매 시작 시간 조회용
- `(source_url)` — URL로 공연 조회할 때 사용
- `(goods_code)` — 인터파크 상품 코드로 공연 조회

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 공연 식별자 |
| source_url | VARCHAR(2048) | NOT NULL | 사용자가 입력한 원본 URL |
| title | VARCHAR(500) | NOT NULL | 공연 제목 |
| image_url | VARCHAR(2048) | NULL | 포스터/이미지 URL |
| start_date | VARCHAR(12) | NULL | 예매 시작 시간 (YYYYMMDDHHMM) |
| end_date | VARCHAR(12) | NULL | 예매 종료 시간 (YYYYMMDDHHMM) |
| link | VARCHAR(2048) | NULL | 공연 URL (인터파크) |
| goods_code | VARCHAR(50) | UNIQUE | 인터파크 상품 코드 |
| goods_name | VARCHAR(500) | NULL | 인터파크 상품명 |
| place_code | VARCHAR(50) | NULL | 공연장 코드 |
| place_name | VARCHAR(500) | NULL | 공연장명 |
| play_date | VARCHAR(50) | NULL | 공연 날짜 범위 (예: "26-06-07 ~ 26-06-07") |
| play_start_date | TIMESTAMP | NULL | 공연 시작일 |
| play_end_date | TIMESTAMP | NULL | 공연 종료일 |
| created_by | BIGINT | NULL, FK → users(id) | URL을 입력한 사용자 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | 수정 시각 |
| deleted_at | TIMESTAMP | NULL | 소프트 삭제 시각 (최대 5개 초과 시 가장 오래된 것 삭제) |

---

### 2.3. `public_seats` — 공개 라운드 좌석

**공개 라운드 좌석 테이블**

- 좌석 선택 화면 목록/상태 표시
- "선택 완료" 시 조건부 UPDATE로 `AVAILABLE -> LOCKED` 전환 + Redis hold TTL 시작
- "좌석 확정하기" 시 조건부 UPDATE로 `LOCKED -> BOOKED` 전환
- LOCKED 좌석은 `locked_by_user_id + hold_token + hold_expires_at`로 소유권/만료 시점 검증

**비정상 종료/만료 복구**

- Redis hold TTL 만료 시 선점 해제
- DB의 LOCKED는 스케줄러 또는 조회 시 보정으로 `AVAILABLE` 복구

**제약:** `(round_id, seat_number)` UNIQUE — 동일 라운드 내 좌석 중복 방지.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 좌석 식별자 |
| round_id | BIGINT | NOT NULL, FK → public_rounds(id) | 공개 라운드 |
| seat_number | VARCHAR(20) | NOT NULL | 좌석 번호 |
| status | VARCHAR(20) | NOT NULL | AVAILABLE / LOCKED / BOOKED |
| locked_at | TIMESTAMP | NULL | 선점 시각 (status=LOCKED일 때) |
| locked_by_user_id | BIGINT | NULL, FK → users(id) | 선점한 사용자 (status=LOCKED일 때) |
| hold_token | VARCHAR(120) | NULL | 선점 토큰 (status=LOCKED일 때) |
| hold_expires_at | TIMESTAMP | NULL | 선점 만료 시각 (status=LOCKED일 때) |

**status**

| 값 | 의미 |
|-----------|------------------|
| AVAILABLE | 선택 가능한 좌석 |
| LOCKED | 확인 화면 대기 중 임시 선점 상태. TTL 만료 시 AVAILABLE 전환 |
| BOOKED | 예매 완료 |

---

### 2.4. `public_bookings` — 공개 예매 내역

**예매 내역 (마이페이지용 + 좌석 확정 확인용)**

- "좌석 확정하기" 클릭 시 트랜잭션: 좌석 BOOKED, `public_bookings`에 BOOKED 기록, Redis hold 해제
- 마이페이지에서 [예매 내역 = `user_id`]로 `public_bookings` 조회 (좌석, 예매 시각, 라운드 정보 등)

**정합성**

- 좌석 선택 완료로 LOCKED가 된 좌석만 확정 가능
- 확정 시 `LOCKED + owner + hold_token + hold_expires_at > now` 조건 검증 후 BOOKED 전환

**인덱스**

- `(user_id, created_at DESC)` — 마이페이지 목록
- `(round_id, seat_id)` UNIQUE — 한 좌석당 한 건의 완료 예매만

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 예매 식별자 |
| user_id | BIGINT | NOT NULL, FK → users(id) | 예매한 회원 |
| round_id | BIGINT | NOT NULL, FK → public_rounds(id) | 공개 라운드 |
| seat_id | BIGINT | NOT NULL, FK → public_seats(id) | 좌석 |
| seat_number | VARCHAR(20) | NOT NULL | 좌석 번호 |
| status | VARCHAR(20) | NOT NULL | PENDING / BOOKED / CANCELLED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 예매(선점) 시각 |
| booked_at | TIMESTAMP | NOT NULL | 예매 확정 시각 (status=BOOKED일 때) |

---

## 3. Redis (테이블 아님)

토큰 부여, 순번, 대기 상태는 DB가 아닌 Redis로 관리.

| 구분 | 저장소 | 용도 |
|------|--------|------|
| 대기열 | Redis | 예매하기 클릭 시 진입량 제한, 토큰/순번 관리. DB 테이블 없음. |
| 좌석 선점 락 | Redis | `seat:hold:{roundId}:{seatId}` 형태, TTL 3~5분. Redisson 분산 락 권장. |
| DB 비관적 락 | PostgreSQL | 결제 확정 시 `SELECT ... FOR UPDATE`로 해당 좌석 행 잠근 뒤 `BOOKED` 업데이트. |

---

## 4. 스키마 요약 (테이블 목록)

| 테이블 | 역할 |
|--------|------|
| **users** | 회원 (로그인/회원가입, 예매 주체) |
| **performances** | 공연 정보 (URL 파싱 결과, 사용자당 최대 5개) |
| **public_seats** | 공개 라운드 좌석, 상태(AVAILABLE/LOCKED/BOOKED) |
| **public_bookings** | 공개 예매 내역 (마이페이지, 좌석 확정) |

---

## 5. 시퀀스/플로우와 테이블 매핑

1. **로그인** → `users`
2. **URL 입력** → HTML 파싱 (응답만 반환, DB 저장 안함)
3. **PerformanceSummationCard 표시** → 파싱 결과를 프론트엔드에서 카드로 표시
4. **"예매하기" 버튼 클릭** → `/api/performance/save` POST 요청
5. **DB 저장** → `performances` 생성 (사용자당 5개 초과 시 가장 오래된 것 soft delete)
6. **좌석 선택 페이지** → `public_seats` 조회 및 선택
7. **대기열** → Redis
8. **선택 완료(임시 선점)** → Redis hold + `public_seats.status` = LOCKED + `locked_by_user_id/hold_token/hold_expires_at` 저장
9. **좌석 확정** → `public_seats.status` = BOOKED, `public_bookings` BOOKED, Redis hold 해제
10. **비정상 종료/만료** → Redis TTL 만료; 필요 시 스케줄로 `public_seats` LOCKED → AVAILABLE
11. **마이페이지** → `public_bookings` + `public_seats` 조인

---

## 6. 보완 (선택)

| 항목 | 제안 | 이유 |
|------|------|------|
| **performances.goods_code** | UNIQUE (이미 적용) | 같은 공연을 중복으로 저장하지 않기 위해 인터파크 상품 코드로 유일성 보장 |
| **performances.deleted_at** | soft delete (이미 적용) | 사용자당 5개 초과 시 가장 오래된 공연을 삭제하되, 히스토리 유지 가능 |
| **bookings** | `cancelled_at` (TIMESTAMP NULL) | CANCELLED 상태일 때 취소 시각을 남기면 이후 통계/이력 분석에 유리. 필수는 아님. |

---

## 7. 좌석 선점/확정 상세 설계 및 SQL 템플릿

### 1. `public_seats` Schema, Constraint, Index 설정

- `ck_public_seats_status` : 좌석들의 status를 'AVAILABLE, LOCKED, BOOKED'로 제한
- `ck_public_seats_lock_consistency` : status에 따라 다른 컬럼들의 NULL/NOT NULL 여부 결정
    - **LOCKED** : `locked_at, locked_by_user_id, hold_token, hold_expires_at` 모두 NOT NULL
    - **AVAILABLE/BOOKED** : 위 lock 컬럼들 모두 NULL

```sql
-- ---------------------------------------------------------------------------
-- 4. public_seats — 좌석
-- ---------------------------------------------------------------------------
CREATE TABLE public_seats (
    id                  BIGSERIAL PRIMARY KEY,
    round_id            BIGINT NOT NULL REFERENCES public_rounds(id),
    seat_number         VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    locked_at           TIMESTAMP,
    locked_by_user_id   BIGINT REFERENCES users(id),
    hold_token          VARCHAR(120),
    hold_expires_at     TIMESTAMP,
    CONSTRAINT ck_public_seats_status
        CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED')),
    CONSTRAINT ck_public_seats_lock_consistency
        CHECK (
            (
                status = 'LOCKED'
                AND locked_at IS NOT NULL
                AND locked_by_user_id IS NOT NULL
                AND hold_token IS NOT NULL
                AND hold_expires_at IS NOT NULL
            )
            OR
            (
                status IN ('AVAILABLE', 'BOOKED')
                AND locked_at IS NULL
                AND locked_by_user_id IS NULL
                AND hold_token IS NULL
                AND hold_expires_at IS NULL
            )
        ),
    CONSTRAINT uq_public_seat_round_seat_number UNIQUE (round_id, seat_number)
);

CREATE INDEX idx_public_seats_round_id ON public_seats(round_id);
CREATE INDEX idx_public_seats_status ON public_seats(round_id, status);
CREATE INDEX idx_public_seats_round_status_expires ON public_seats(round_id, status, hold_expires_at);
CREATE INDEX idx_public_seats_lock_owner_token ON public_seats(locked_by_user_id, hold_token);
```

---

### 2. 좌석 예매 흐름 (AVAILABLE → LOCKED → BOOKED)

#### 2-1. 임시 선점 SQL (AVAILABLE → LOCKED)

- **'선택 완료' 버튼 눌렀을 때, 현재 좌석 status 확인**
  - `status=AVAILABLE`이면, LOCKED로 전환 + Redis 토큰 저장 + TTL 만료 시점 저장
  - **현재 단계가 실패할 경우, 다른 사용자가 해당 좌석을 먼저 잡은 상태** → status=AVAILABLE로 롤백

```sql
UPDATE public_seats
SET
    status = 'LOCKED',
    locked_at = now(),
    locked_by_user_id = :userId,
    hold_token = :holdToken,
    hold_expires_at = :holdExpiresAt
WHERE round_id = :roundId
    AND id = ANY(:seatIds)
    AND status = 'AVAILABLE';
```

**성공 조건:** `updated_count == 요청 seatIds 개수`

---

#### 2-2. 확정 트랜잭션 (1개 트랜잭션으로 진행: 좌석 확정 + 예매내역 저장)

**트랜잭션 흐름:**
1. 라운드 유효성(`OPEN`, `open_at <= now < close_at`) 검증
2. 좌석 확정 SQL 실행 (LOCKED -> BOOKED)
3. `updated_count` 검증 (요청 좌석 수와 일치해야 성공)
4. 예매 내역 저장 SQL 실행 (`public_bookings` 저장)
5. 커밋 후 Redis hold 키 삭제

---

##### (1) 좌석 확정 SQL (LOCKED → BOOKED)

- **'선택 좌석 확인' 화면에서 '좌석 확정하기' 버튼 눌렀을 때**
  1) '기존에 LOCKED로 상태 변경됐을 때의 user_id == 현재 user_id' 같은지 확인
  2) 사용자가 같다면, 좌석 status를 LOCKED → BOOKED로 **조건부 UPDATE** 수행

```sql
UPDATE public_seats
SET
    status = 'BOOKED',
    locked_at = NULL,
    locked_by_user_id = NULL,
    hold_token = NULL,
    hold_expires_at = NULL
WHERE round_id = :roundId
    AND id = ANY(:seatIds)
    AND status = 'LOCKED'
    AND locked_by_user_id = :userId
    AND hold_token = :holdToken
    AND hold_expires_at > now();
```

**성공 조건:** `updated_count == 요청 seatIds 개수`

---

##### (2) 확정 직후, 트랜잭션 성공 여부 확인 + 예매 내역 저장 SQL

- 좌석 확정 SQL에서 조건부 UPDATE 수행한 후, 성공한 좌석과 실패한 좌석을 분리해서 반환

```sql
WITH requested_seats AS (
    SELECT unnest(:seatIds) as seat_id
),
inserted_bookings AS (
    INSERT INTO public_bookings (user_id, round_id, seat_id, seat_number, status, created_at, booked_at)
    SELECT
        :userId,
        :roundId,
        s.id,
        s.seat_number,
        'BOOKED',
        now(),
        now()
    FROM public_seats s
    WHERE s.round_id = :roundId
      AND s.id = ANY(:seatIds)
      AND s.status = 'BOOKED'
    ON CONFLICT (round_id, seat_id) DO NOTHING
    RETURNING seat_id
)
SELECT 
    seat_id,
    CASE 
        WHEN seat_id IN (SELECT seat_id FROM inserted_bookings) 
        THEN 'SUCCESS'
        ELSE 'FAILED'
    END as result
FROM requested_seats
ORDER BY seat_id;
```

**결과 해석:**

1) **만약 'UPDATE된 개수 == 요청 컬럼 개수' : 트랜잭션 성공, `status=BOOKED` 확정**
   - `result = SUCCESS`: 예매 내역 저장 완료
   - `public_seats` : 선택한 좌석들의 status 전부 LOCKED → BOOKED로 UPDATE
    - `public_bookings` : BOOKED로 바뀐 좌석들만 테이블에 기록 (seat_number, booked_at 포함)

2) **만약 'UPDATE된 개수 ≠ 요청 컬럼 개수' : 트랜잭션 실패, `status=LOCKED` 유지 & 실패한 seat_id 반환**
   - `result = FAILED`: 실패한 seat_id (상태 불일치, 중복 예매, 등)
   - **'FAILED 건수 > 0'이면 트랜잭션 롤백 처리**
   - 예: 요청 [1, 2, 3] → 결과 [(1, SUCCESS), (2, FAILED), (3, SUCCESS)] → 2번 좌석 실패

**트랜잭션 실패 원인:**
- 사실상 현재 사용자만 예매할 수 있는 좌석인데(LOCKED 상태), 중간에 오류가 나서 실패한 거!
- 오류 예시
  1. **TTL이 먼저 만료됨** : 확인 화면에서 너무 오래 머물렀거나, Redis hold/DB `hold_expires_at`이 지나서 해당 좌석이 이미 해제된 경우
  2. **같은 사용자의 상태가 바뀜** : 이탈 처리나 만료 복구 배치가 먼저 동작해서, LOCKED → AVAILABLE로 돌아간 경우
  3. **토큰/소유자 불일치** : 다른 요청에서 이미 같은 좌석을 다른 holdToken으로 처리했거나, 중복 제출로 이전 holdToken이 무효가 된 경우
  4. **이미 BOOKED된 상태** : 같은 요청이 중복으로 들어오거나, 재시도 중 먼저 성공한 요청이 있었던 경우

---

### 3. 화면 이탈 처리, 만료 복구 배치

#### 3-1. TTL 만료/비정상 종료 시, status 복구 (LOCKED → AVAILABLE)

- Redis TTL 만료/비정상 종료 시, 선점한 좌석들의 status를 LOCKED → AVAILABLE로 복구
- 실행 주기: 1~5초 권장

```sql
UPDATE public_seats
SET
    status = 'AVAILABLE',
    locked_at = NULL,
    locked_by_user_id = NULL,
    hold_token = NULL,
    hold_expires_at = NULL
WHERE round_id = :roundId
    AND status = 'LOCKED'
    AND hold_expires_at <= now();
```

#### 3-2. 사용자가 '선택 좌석 확인' 화면에서 나갔을 때, status 롤백 (LOCKED → AVAILABLE)

- 확인 화면에서 이탈했을 때, 해당 사용자 선점 좌석 즉시 반환

```sql
UPDATE public_seats
SET
    status = 'AVAILABLE',
    locked_at = NULL,
    locked_by_user_id = NULL,
    hold_token = NULL,
    hold_expires_at = NULL
WHERE round_id = :roundId
    AND status = 'LOCKED'
    AND locked_by_user_id = :userId
    AND hold_token = :holdToken;
```

