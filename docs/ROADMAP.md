# 티켓팅 연습 플랫폼 — 구현 로드맵

본 프로젝트는 아래 2가지 모드를 하나의 서비스에서 제공한다.
- (개인) URL 입력 기반 개인 맞춤형 티켓팅 연습 페이지
- (공용) 실제 사용자끼리 경쟁하는 공개 티켓팅 연습 페이지(30분 단위 오픈)

핵심 전략은 **진입 방식은 2개, 예매 엔진은 1개**다.  
즉, 화면 진입 경로는 분리하되 대기열·좌석 락·트랜잭션·결제 로직은 공통으로 사용한다.  
**프론트엔드:** React / **백엔드:** Spring Boot / **인프라:** PostgreSQL, Docker, Redis

---

## 1. 기술 스택

| 구분 | 현재 |
|------|------|
| **Backend** | Spring Boot 4.x, Web, JPA, Validation, Spring Security, Redis |
| **Frontend** | React |
| **DB** | PostgreSQL |
| **인프라** | Redis, Docker |

---

## 2. Phase 흐름도

```
Phase 0  기반 작업 (패키지·DB) — Redis 설정은 좌석 선점/대기열 단계에서 진행
    ↓
Phase 1  로그인 / 회원가입 + 모드 선택 메인
    ↓
Phase 2  (개인) URL 파싱 + 개인 연습 페이지
    ↓
Phase 3  (공용) 공개 예매 UI 흐름 (Loading → Captcha → Seats → Success)
    ↓
Phase 4  공개 라운드 기초 + '예매하기' 버튼 활성화
    ↓
Phase 5  좌석 선택 + 예매 완료 + Redis 대기열/트래픽 제어
    ↓
Phase 6  나의 예매내역 (마이페이지)
    ↓
Phase 7  라운드 관리 및 데이터 정리
```

---

## 3. Phase 0 — 기반 작업

**목표:** 도메인·DB·보안·인프라 기반을 먼저 갖추고, 이후 기능을 단계적으로 붙이기 쉽게 한다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 0-1 | 패키지 구조 | `controller`, `service`, `repository`, `domain/entity`, `dto`, `config` 등 계층 분리 |
| 0-2 | DB 스키마 설계 및 Entity 구현 | 'users/seats/performances/bookings' DB Schema 설계, Schema에 맞게 Entity 코드 구현 |

**산출물:** DB Schema, ER Diagram, 'User, Seat, Performance, Booking' Entity 코드

---

## 4. Phase 1 — 로그인/회원가입 API 및 UI, 모드 선택 메인 구현 (프론트엔드 + 백엔드)

**목표:** 인증 기능을 완성하고, 로그인 이후 개인/공용 모드 선택 가능한 메인 화면 제공

| 순서 | 작업 | 설명 |
|------|------|------|
| 1-1 | Spring Security 설정 | 의존성 추가, 로그인/로그아웃 URL, 세션 기반 인증, BCrypt 비밀번호 인코더 설정, `SecurityContext` / `@AuthenticationPrincipal`로 현재 사용자 조회 |
| 1-2 | 'users' 테이블(User 엔티티) 접근용 Repository 코드 구현(UserRepository) |
| 1-3 | 로그인/회원가입 API 구현 (Controller, Service) (API 경로: /api/signup, /api/login, /api/logout, /api/auth/me) |
| 1-4 | 로그인/회원가입용 인증 Request/Response DTO 구현 |
| 1-5 | (프론트) 로그인/회원가입 UI 구현 | 아이디, 비밀번호, 이름 — 유효성 검사, 중복 체크, 실패 시 메시지, 성공 시 메인(URL 입력) UI로 리다이렉트 |
| 1-6 | (프론트) 메인 화면 UI 구현 (모드 선택) | 로그아웃 버튼, 회원탈퇴 버튼, `개인 연습 시작` / `공개 라운드 참가` 진입 버튼 제공 |

**산출물:** `/signup`, `/login`, `/logout`, 로그인 후 모드 선택 메인 진입

---

## 5. Phase 2 — (개인) 개인 연습 페이지 + 공연 정보 파싱 로직 구현

**목표:** 입력 URL 기반으로 공연 정보를 추출하여, 사용자 전용 개인 연습 페이지를 구성.
정적 html : Jsoup / SPA(JS 랜더링 사이트) : Playwright 사용 -> 이제 사용 안함

| 순서 | 작업 | 설명 |
|------|------|------|
| 2-1 | 목표 사이트 선택 (인터파크 티켓) | 목표 티켓팅 사이트 지정 및 분석 |
| 2-2 | URL 파싱 기반 외부 API 조회 | 사용자가 URL 입력 시, 서버가 URL에서 goodsCode 추출 후 Interpark summary API 호출 |
| 2-3 | 핵심 공연 정보 파싱 | 제목, 이미지 URL, 공연 설명, 예매 열림 시간 등 추출 → DTO 반환 |
| 2-4 | 개인 연습 UI 구현 | 파싱한 공연 정보를 개인 연습 화면 변수에 매핑 |
| 2-5 | 예매 화면 진입 흐름 | `예매하기` 클릭 시 예매 로딩창/좌석 선택/성공 화면 기본 흐름 구현 |
| 2-6 | 개인 모드 접근 정책 | 개인 연습 페이지는 생성 사용자만 조회 가능(createdBy 소유권 검증) |

**변경사항:** 기존에는 Playwright를 사용한 SPA 사이트의 html 랜더링 후, 랜더링된 html에서 공연 정보들을 찾아서 추출하는 방식을 사용했으나, 랜더링 시간이 너무 오래걸리고 랜더링 결과에 따라 공연 정보가 포함되지 않은 경우가 많았음. 그래서 인터파크 사이트의 API들을 분석해, 핵심 공연 정보들을 요청하는 API(summary API)를 찾았고, 해당 API를 대신 호출해 공연 정보를 받아오는 방식으로 선회함

**summary API 구조:** 'https://api-ticketfront.interpark.com/v1/goods/%s/summary?goodsCode=%s&passCode=&priceGrade=&seatGrade=&ts=%d' 추출한 goodsCode를 API에 포함해서 전달

**산출물:** 파싱 결과 DTO (title, imageUrl, 등), 개인 공연 요약 페이지, 개인 티켓팅 연습 페이지

---

## 6. Phase 3 — (공용) 공개 티켓팅 연습 페이지 UI 구현 (Loading → Captcha → SeatSelection → BookingSuccess)

**목표:** 사용자가 “예매하기” 버튼을 누르면 Loading → Captcha → SeatSelection → BookingSuccess 순서로 진행되는 예매 흐름을 구현한다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 3-1 | 단계별 UI 렌더링 | 상태(loading/captcha/seats/booking-success)별 조건부 렌더링 |
| 3-2 | Session 상태 저장 | reservationFlow, captchaCompleted를 localStorage/sessionStorage에 저장 |
| 3-3 | 새로고침 복원 | 새로고침 시 상태 자동 복원하여 트랜잭션 유지 |
| 3-4 | 뒤로가기 취소 | SeatSelection에서 뒤로가기 시 모달로 확인, “취소” 선택하면 이전 페이지로 이동 |

**Frontend 구현 파일:**
- `PublicPerformanceDetails.jsx` - 상태 관리, 단계별 전환
- `PublicLoadingScreen.jsx` - 로딩 화면
- `PublicCaptchaModal.jsx` - CAPTCHA 모달
- `PublicSeatSelection.jsx` - 좌석 선택
- `PublicBookingSuccess.jsx` - 예매 완료

**산출물:** 공개 예매 UI 흐름 완성, 단계별 상태 관리

---

## 7. Phase 4 — 30분 단위 라운드 생성 + '예매하기' 버튼 활성화 로직 구현

**목표:** 매시 00분/30분마다 라운드가 OPEN 상태로 전환되고, 그 10분 전후의 보정/복구 흐름까지 포함해 프론트의 '예매하기' 버튼 상태를 안정적으로 제어한다. 서버는 `WAITING → OPEN → CLOSED` 상태 머신을 관리하고, 프론트는 SSE와 동기화 API를 조합해 화면을 갱신한다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 4-1 | PublicRound 엔티티 설계 | `id`, `roundNumber`, `status(/OPEN/CLOSED)`, `openAt`, `closeAt`, `createdAt`, `updatedAt` 필드 (`closeAt = openAt + 10분`) |
| 4-2 | OPEN 라운드 생성/만료 스케줄러 | 매시 00/30분에 새로운 OPEN 라운드 생성 & SSE를 발행. 5초 주기로 만료된 `OPEN` 라운드를 `CLOSED`로 정리 |
| 4-3 | Scheduler 신호 프론트 전달 (SSE) | OPEN 라운드 생성 시 `roundCreated` 이벤트를 발행 -> 프론트에 RoundEventDto 전달 -> 프론트가 Dto 속 '현재 서버 시각(serverNow) 기준으로 버튼 오픈/종료 시간 계산 |
| 4-4 | 라운드 조회/생성/삭제 API | (컨트롤러) `/sync`, `/current`, 등의 API로 클라이언트 구독,클라이언트-서버시각 동기화 등의 서비스 메서드 경로 지정 |
| 4-5 | OPEN 라운드 찾는 레포지토리 메서드 작성 | public_rounds 테이블에서 아래 2가지 조건 만족하는 라운드 찾는 native SQL 쿼리 메서드 작성 (openAt, closeAt 시간 비교는 UTC+9 기준) |

**전체 흐름:**
- 사용자가 '예매하기' 버튼 열렸을때 버튼 클릭하면, 레포지토리 쿼리 메서드 실행
- public_rounds 테이블에서 아래 2가지 조건 만족하는 레코드 조회
    1) `status = OPEN`
    2) `openAt <= now < closeAt` (단, UTC+9 기준으로 openAt, closeAt 비교)
- 요청 시점의 서버 시간을 기준으로 `public_rounds`에서 `status = OPEN`이고 `openAt <= now < closeAt`인 경우만 다음 창으로 넘어갈 수 있도록 막음.
- `closeAt`이 지난 `OPEN` 라운드는 스케줄러가 주기적으로 확인해서 `CLOSED` 처리함.

**산출물:** PublicRound 엔티티, 라운드 생성/만료 스케줄러, SSE 이벤트 발행/구독 흐름, 라운드 API, 10분 윈도우 기반 버튼 활성화 로직

---

## 8. Phase 5 — 좌석 선택 + 예매 완료 + Redis 선점/대기열/트래픽 제어

**목표:** OPEN 라운드 생성 시 `public_seats` 400개를 미리 생성하고, 사용자는 좌석 목록을 조회/선택한 뒤 3단계(임시 선점 → 확인 → 최종 확정)를 거쳐 예매를 완료한다. 동시에 Redis 기반 대기열/진입 제한으로 트래픽을 제어하고, 만료/이탈 시 자동 복구한다.

**상태 전이:** AVAILABLE → LOCKED(임시 선점, TTL) → BOOKED(최종 확정) / 또는 TTL 만료 시 AVAILABLE로 복구

| 순서 | 작업 | 설명 |
|------|------|------|
| 5-1 | 공개 라운드 좌석 사전 생성 | OPEN 라운드 생성 시 `public_seats` 400개 레코드 생성 (status=AVAILABLE) |
| 5-2 | 선택 좌석 확인 화면 구현 | PublicSeatSelection과 PublicBookingSuccess 사이에 "선택한 좌석 확인하기" 화면 추가 (선택 좌석 목록, hold 남은 시간 표시) |
| 5-3 | 좌석 목록 조회 API | GET `/api/public-seat/{roundId}` — 현재 라운드 좌석 조회, 각 좌석의 상태(AVAILABLE/LOCKED/BOOKED) 및 hold 만료 시간 반영 |
| 5-4 | 좌석 선택 UI 상태 관리 | PublicSeatSelection에서 사용자가 고른 좌석을 프론트 상태로 관리, 새로고침 시 서버 좌석 상태(hold 여부/TTL) 재조회 |

| 5-5 | 선택 완료(임시 선점) API | POST `/api/public-seat/hold` — 라운드 검증 + 조건부 UPDATE(AVAILABLE→LOCKED + hold_token/hold_expires_at 저장) + Redis hold TTL 시작 |
| 5-6 | 화면 이탈 시 선점 해제 | 확인 화면에서 나갈 때 DELETE `/api/public-seat/hold` 호출 — LOCKED→AVAILABLE 즉시 해제, 테이블에서 토큰/만료 기간 삭제 |
| 5-7 | 좌석 확정 API | POST `/api/public-booking/confirm` — 라운드 검증 + 소유권 검증(userId + holdToken) + 조건부 UPDATE(LOCKED→BOOKED + lock 컬럼 NULL 처리) + public_bookings 저장 |
| 5-8 | 소유권/토큰 검증 | LOCKED 좌석은 `locked_by_user_id + hold_token + hold_expires_at > now()` 모두 일치해야만 BOOKED 전환 허용 |
| 5-9 | 다건 원자성 보장 | 좌석 여러 개 선점/확정 시 전부 성공만 허용, 일부 실패 시 전체 롤백. 실패한 seat_id를 응답으로 반환 |
| 5-10 | 확정 트랜잭션 구조 | 1) 라운드 유효성 검증 2) LOCKED→BOOKED 조건부 UPDATE 3) updatedCount 검증 4) public_bookings INSERT 5) 커밋 후 Redis hold 삭제 |
| 5-11 | TTL 만료 자동 복구 배치 | 1~5초 주기로 LOCKED 좌석 중 hold_expires_at <= now() 인 것을 AVAILABLE로 자동 복구 (스케줄러 실행) |

| 5-12 | Redis 대기열/진입 제한 | "예매하기" 버튼 클릭 → Redis Queue에 요청 저장, 토큰 기반 진입량 제한(라운드당 예: 1000명 동시 진입) |
| 5-13 | 트래픽 테스트 | 다중 사용자 고동시성 요청 시뮬레이션 — 중복 예매 0건, TTL 복구 동작, hold_expires_at 추적, 확정 정합성 검증 |

**핵심 설계:**
- **hold_token** : 같은 사용자 중복 요청 방지, UUID 기반
- **hold_expires_at** : DB에 저장해 Redis 장애 시 복구 기준 제공, TTL 만료 후 AVAILABLE 자동 복구
- **다건 처리** : 요청한 모든 좌석에 대해 성공/실패를 분리해 반환, FAILED 건수 > 0이면 전체 롤백
- **만료 정책** : Redis TTL 만료 + 스케줄러 실행으로 DB 상태도 함께 복구

**산출물:** 선점(LOCKED) 및 확정(BOOKED) 2단계 API, hold_token/hold_expires_at 관리 로직, 선택 좌석 확인 화면, TTL 만료 자동 복구 배치, Redis 대기열/진입 제한, 고동시성 트래픽 테스트 코드

**참고 링크:** https://velog.io/@zvyg1023/Spring-RedisRedisson-%EB%B6%84%EC%82%B0%EB%9D%BD%EC%9D%84-%ED%99%9C%EC%9A%A9%ED%95%9C-%EC%A2%8C%EC%84%9D-%EC%84%A0%EC%A0%90-%EA%B0%9C%EB%B0%9C

---

## 9. Phase 6 — 나의 예매내역 (마이페이지)

**목표:** PublicPerformanceDetails 헤더의 사용자 이름 오른쪽에 “나의 예매내역” 버튼을 배치하고, 클릭 시 사용자가 현재 라운드에서 예매한 좌석 목록을 표시한다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 6-1 | 예매내역 조회 API | GET `/api/public-booking/my-bookings` — 로그인 사용자의 현재 라운드 bookings 레코드 조회 |
| 6-2 | 예매내역 UI | PublicMyBookings 컴포넌트: 공연명, 선택한 좌석 목록(최대 4개), 예매 시각, 라운드 정보 등 표시 |
| 6-3 | 버튼 배치 | PublicPerformanceDetails 헤더 우측 사용자 이름 옆에 “나의 예매내역” 버튼 추가 |
| 6-4 | 모달 표시 | 클릭 시 모달로 예매내역 표시 (기존 접근 경로 유지) |

**산출물:** 예매내역 조회 API, PublicMyBookings 컴포넌트, 헤더 버튼 추가

---

## 10. Phase 7 — 라운드 관리 및 데이터 정리

**목표:** 새로운 라운드 시작 시 이전 라운드의 public_bookings/public_seats 데이터를 soft delete하여 시스템을 정리한다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 8-1 | 라운드 종료 정책 | 라운드 closeAt 시각 도달 시 라운드 상태를 CLOSED로 변경, 신규 진입 차단 |
| 8-2 | 데이터 정리 Scheduler | 새 라운드 생성 시, 이전 라운드의 모든 public_bookings/public_seats 레코드 soft delete |

**산출물:** 라운드 종료 및 데이터 정리 로직

---

## 11. 구현 체크리스트

| Phase | 항목 | 완료 |
|-------|------|:----:|
| **0** | 패키지 구조, DB 스키마 | ☐ |
| **1** | Spring Security, 회원가입, 로그인, 모드 선택 메인 | ☐ |
| **2** | (개인) URL 파싱, 공연 정보 DTO, 개인 연습 페이지 | ☐ |
| **3** | 공개 예매 UI 흐름(Loading/Captcha/Seats/Success) | ✅ |
| **4** | PublicRound 엔티티, OPEN/CLOSED 전환, SSE, 10분 버튼 활성화 | ✅ (핵심 흐름) / ⏳ (실서비스 연동 보강) |
| **5** | 좌석 조회/선택, 임시 선점(LOCKED), 좌석 확정(BOOKED), Redis 대기열/트래픽 제어, public_bookings 저장 | ⏳ |
| **6** | 나의 예매내역 (마이페이지) | ⏳ |
| **7** | 라운드 관리, 데이터 정리 (soft delete) | ⏳ |

---

## 12. 아키텍처 요약

| 구간 | 방식 |
|------|------|
| **라운드 기초** | `OPEN → CLOSED` 상태 전환, 30분 슬롯 단위 OPEN 생성/만료, openAt~closeAt 기반 버튼 활성화 |
| **예매 흐름** | Loading → Captcha → SeatSelection → BookingSuccess (단계별 UI) |
| **좌석 선택/임시 선점** | "선택 완료" → 라운드 유효성 검증 → 조건부 UPDATE로 `public_seats`의 AVAILABLE 좌석을 LOCKED 전환 + Redis hold TTL 시작 + 확인 화면 이동 |
| **좌석 확정** | "좌석 확정하기" → LOCKED(본인+holdToken 검증) 좌석만 BOOKED 전환 → 업데이트 건수 검증 후 public_bookings 저장 → Redis hold 해제 |
| **마이페이지** | PublicPerformanceDetails 헤더의 "나의 예매내역" 버튼 → 예매 조회 |
| **대기열** | 트래픽 과부하 시 Redis 기반 대기열/토큰 처리 |
| **데이터 정리** | 새 라운드 시작 시 이전 라운드 public_bookings/public_seats soft delete |

**DB 테이블:**
- `public_rounds`: id, roundId, status, openAt, closeAt, createdAt, updatedAt
- `public_seats`: id, roundId, seatNumber, status, lockedAt, lockedByUserId
- `public_bookings`: id, userId, roundId, seatId, status, createdAt, completedAt

상세 테이블·ER 다이어그램은 `docs/SCHEMA.md` 참고.
