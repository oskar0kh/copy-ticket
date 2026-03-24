# 티켓팅 연습 플랫폼 — DB 스키마 설계

> PostgreSQL 기준, JPA 엔티티 설계 시 참고용  
> 대기열(Queue)은 Kafka + Redis로 처리하므로 별도 테이블 없음.  
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
| **users** | 회원 (로그인/회원가입, 예매 주체) |
| **performances** | 공연 정보 (URL 파싱 결과, 사용자당 최대 5개) |
| **seats** | 공연별 좌석, 상태(AVAILABLE/LOCKED/BOOKED) |
| **bookings** | 예매 내역 (마이페이지, 결제 확정) |

---

## 5. 시퀀스/플로우와 테이블 매핑

1. **로그인** → `users`
2. **URL 입력** → HTML 파싱 (응답만 반환, DB 저장 안함)
3. **PerformanceSummationCard 표시** → 파싱 결과를 프론트엔드에서 카드로 표시
4. **"예매하기" 버튼 클릭** → `/api/performance/save` POST 요청
5. **DB 저장** → `performances` 생성 (사용자당 5개 초과 시 가장 오래된 것 soft delete)
6. **좌석 선택 페이지** → `seats` 조회 및 선택
7. **대기열** → Kafka + Redis
8. **좌석 선점** → Redis 락, `seats.status` = LOCKED, `seats.locked_by_user_id`
9. **결제 완료** → `seats.status` = BOOKED, `bookings` COMPLETED, Redis 락 해제
10. **비정상 종료** → Redis TTL 5분; 필요 시 스케줄로 `seats` LOCKED → AVAILABLE
11. **마이페이지** → `bookings` + `performances` + `seats` 조인

---

## 6. 보완 (선택)

| 항목 | 제안 | 이유 |
|------|------|------|
| **performances.goods_code** | UNIQUE (이미 적용) | 같은 공연을 중복으로 저장하지 않기 위해 인터파크 상품 코드로 유일성 보장 |
| **performances.deleted_at** | soft delete (이미 적용) | 사용자당 5개 초과 시 가장 오래된 공연을 삭제하되, 히스토리 유지 가능 |
| **bookings** | `cancelled_at` (TIMESTAMP NULL) | CANCELLED 상태일 때 취소 시각을 남기면 이후 통계/이력 분석에 유리. 필수는 아님. |
