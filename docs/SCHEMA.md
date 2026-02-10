# 티켓팅 연습 플랫폼 — DB 스키마 설계

> PostgreSQL 기준, JPA 엔티티 설계 시 참고용  
> 대기열(Queue)은 Kafka + Redis로 처리하므로 별도 테이블 없음.  
> **티켓팅 사이트별 UI**는 프론트엔드 템플릿으로만 관리 (DB 사용 X).

---

## 1. ER 개요

```
┌─────────────┐                    ┌─────────────────┐
│   users     │────────────────────│  performances   │
│ (last_entered_url)               │  (created_by)   │
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
| id | BIGSERIAL | PK | 회원 식별자 |
| username | VARCHAR(255) | NOT NULL, UNIQUE | 로그인 아이디 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 비밀번호 |
| name | VARCHAR(100) | NOT NULL | 이름 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 가입 시각 |
| updated_at | TIMESTAMP | NULL, DEFAULT now() | 수정 시각 |
| last_entered_url | TEXT | NULL | 가장 최근에 입력한 URL (덮어쓰기) |
| last_entered_url_at | TIMESTAMP | NULL | 일정 시간 지나면 URL 비우기용 |

---

### 2.2. `performances` — 공연 정보

**공연 정보 저장 테이블**

- URL HTML 파싱 후, "제목·이미지·설명·예매 오픈 시각" 저장
- 기존에 만들어 둔 UI 템플릿에 이 정보만 넣어서 표시
- `reservation_open_at`으로 해당 시간에 예매 버튼 활성화

**인덱스**

- `(reservation_open_at)` — 스케줄/버튼 활성화 조회용
- `(source_url)` — 재진입 시 `users.last_entered_url`로 공연 조회할 때 `WHERE source_url = ?` 사용 가능

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 공연 식별자 |
| source_url | VARCHAR(2048) | NOT NULL | 사용자가 입력한 원본 URL |
| title | VARCHAR(500) | NOT NULL | 공연 제목 (파싱) |
| image_url | VARCHAR(2048) | NULL | 포스터/이미지 URL (파싱) |
| description | TEXT | NULL | 공연 설명 (파싱) |
| reservation_open_at | TIMESTAMP | NULL | 예매 열리는 시각 (파싱) — 자동 예매 버튼용 |
| template_code | VARCHAR(50) | NOT NULL | 사용할 UI 템플릿 (site_templates.code 또는 enum) |
| created_by | BIGINT | NULL, FK → users(id) | URL을 입력한 사용자 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | 수정 시각 |

---

### 2.3. `seats` — 좌석

**좌석 테이블**

- 좌석 선택 화면 목록/상태 표시
- 결제 페이지 진입 시 **Redis 분산 락**과 연동해 선점 (LOCKED), 결제 완료 시 BOOKED
- "이미 결제 진행중인 좌석"은 Redis 락 존재 여부 + DB의 LOCKED로 판단 가능

**비정상 종료 시**

- Redis 락에 TTL 5분 설정 → 만료 시 Redis 키 삭제
- DB의 LOCKED는 스케줄러/만료 이벤트로 AVAILABLE 복구하거나, "좌석 목록 API"에서 Redis만 보고 락 여부 반환하는 방식으로 일관성 유지 가능 (정책에 따라 선택)

**제약:** `(performance_id, row_name, seat_number)` UNIQUE — 동일 공연 내 좌석 중복 방지.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 좌석 식별자 |
| performance_id | BIGINT | NOT NULL, FK → performances(id) | 공연 |
| section | VARCHAR(50) | NULL | 구역 (예: R석, S석) |
| row_name | VARCHAR(20) | NOT NULL | 행 (예: A, B, 1, 2) |
| seat_number | VARCHAR(20) | NOT NULL | 좌석 번호 |
| status | VARCHAR(20) | NOT NULL | AVAILABLE / LOCKED / BOOKED |
| locked_at | TIMESTAMP | NULL | 선점 시각 (status=LOCKED일 때) |
| locked_by_user_id | BIGINT | NULL, FK → users(id) | 선점한 사용자 (status=LOCKED일 때) |

**status**

| 값 | 의미 |
|-----------|------------------|
| AVAILABLE | 선택 가능한 좌석 |
| LOCKED | 결제 페이지 진입 중 (Redis 락 + DB 선점). 5분 TTL 후 미결제면 AVAILABLE 전환 |
| BOOKED | 예매 완료 |

---

### 2.4. `bookings` — 예매 내역

**예매 내역 (마이페이지용 + 예매 확정 확인용)**

- "결제하기" 클릭 시 트랜잭션: 좌석 BOOKED, `bookings`에 COMPLETED 기록, Redis 락 해제
- 마이페이지에서 [예매 내역 = `user_id`]로 `bookings` 조회 (공연명, 좌석, 예매 시각 등)

**정합성**

- 좌석 선택 → 결제 페이지 진입 → 결제 완료 시에만 COMPLETED로 생성하고, 그때 seat를 BOOKED로 변경 (이전 단계는 Redis 락만 사용)

**인덱스**

- `(user_id, created_at DESC)` — 마이페이지 목록
- `(performance_id, seat_id)` UNIQUE — 한 좌석당 한 건의 완료 예매만

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 예매 식별자 |
| user_id | BIGINT | NOT NULL, FK → users(id) | 예매한 회원 |
| performance_id | BIGINT | NOT NULL, FK → performances(id) | 공연 |
| seat_id | BIGINT | NOT NULL, FK → seats(id) | 좌석 |
| status | VARCHAR(20) | NOT NULL | PENDING_PAYMENT / COMPLETED / CANCELLED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 예매(선점) 시각 |
| completed_at | TIMESTAMP | NULL | 결제 완료 시각 (status=COMPLETED일 때) |

---

## 3. Redis / Kafka (테이블 아님)

토큰 부여, 순번, 대기 상태는 DB가 아닌 Kafka + Redis로 관리.

| 구분 | 저장소 | 용도 |
|------|--------|------|
| 대기열 | Kafka + Redis | 예매하기 클릭 시 진입량 제한, 토큰/순번 관리. DB 테이블 없음. |
| 좌석 선점 락 | Redis | `seat:{performanceId}:{seatId}` 형태, TTL 5분. Redisson 분산 락 권장. |
| DB 비관적 락 | PostgreSQL | 결제 확정 시 `SELECT ... FOR UPDATE`로 해당 좌석 행 잠근 뒤 `BOOKED` 업데이트. |

---

## 4. 스키마 요약 (테이블 목록)

| 테이블 | 역할 |
|--------|------|
| **users** | 회원 (로그인/회원가입, 예매 주체, 최근 입력 URL 저장) |
| **performances** | 공연 정보 (URL 파싱 결과 + 예매 오픈 시각) |
| **seats** | 공연별 좌석, 상태(AVAILABLE/LOCKED/BOOKED) |
| **bookings** | 예매 내역 (마이페이지, 결제 확정) |

---

## 5. 시퀀스/플로우와 테이블 매핑

1. **로그인** → `users`
2. **URL 입력·저장·복원** → `users.last_entered_url` 덮어쓰기, 파싱 후 `performances` 생성/조회
3. **UI 표시** → `performances` + 프론트엔드 템플릿(`template_code`로 매핑)
4. **예매 오픈 시각** → `performances.reservation_open_at`
5. **예매 클릭 → 대기열** → Kafka + Redis
6. **좌석 선택 → 결제 페이지** → Redis 락, `seats.status` = LOCKED, `seats.locked_by_user_id`
7. **결제 완료** → `seats.status` = BOOKED, `bookings` COMPLETED, Redis 락 해제
8. **비정상 종료** → Redis TTL 5분; 필요 시 스케줄로 `seats` LOCKED → AVAILABLE
9. **마이페이지** → `bookings` + `performances` + `seats` 조인

---

## 6. 보완 (선택)

| 항목 | 제안 | 이유 |
|------|------|------|
| **performances** | `source_url` UNIQUE | 같은 URL로 공연을 하나만 두고 재사용하려면 UNIQUE로 중복 생성 방지. URL이 사이트마다 달라질 수 있으면 생략. |
| **bookings** | `cancelled_at` (TIMESTAMP NULL) | CANCELLED 상태일 때 취소 시각을 남기면 이후 통계/이력 분석에 유리. 필수는 아님. |
