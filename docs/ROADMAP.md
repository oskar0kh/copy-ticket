# 티켓팅 연습 플랫폼 — 구현 로드맵

사용자가 URL을 입력하면 공연 정보를 파싱해 보여주고, 예매 오픈 시각에 맞춰 대기열·좌석 선점·가상 결제까지 연습할 수 있는 플랫폼의 단계별 구현 계획이다.  
**백엔드:** Spring Boot + Thymeleaf 중심. **인프라:** PostgreSQL, Redis, Kafka(로컬/Docker).

---

## 1. 기술 스택

| 구분 | 현재 |
|------|------|
| **Backend** | Spring Boot 4.x, Web, JPA, Validation, Spring Security, Redis, Kafka Client |
| **Frontend** | React |
| **DB** | PostgreSQL |
| **인프라** | Redis, Kafka, Docker |
| **기타 프레임워크** | Playwright |

---

## 2. Phase 흐름도

```
Phase 0  기반 작업 (패키지·DB) — Redis/Kafka 설정은 Phase 5에서 진행
    ↓
Phase 1  로그인 / 회원가입
    ↓
Phase 2  HTML 파싱 + 공연 정보 UI 템플릿
    ↓
Phase 3  예매 열림 시간 파싱 + 자동 예매 버튼
    ↓
Phase 4  대기열 (Kafka + Redis) — 트래픽 제어
    ↓
Phase 5  좌석 선택 + Redis 분산 락 + DB 락
    ↓
Phase 6  가상 결제 + 예매 완료
    ↓
Phase 7  마이페이지(예매 내역) + 락 TTL 비정상 종료 처리
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

## 4. Phase 1 — 로그인/회원가입 API 및 UI, URL 입력 UI 구현 (프론트엔드 + 백엔드)

**목표:** 로그인/회원가입 API/UI 구현, 

| 순서 | 작업 | 설명 |
|------|------|------|
| 1-1 | Spring Security 설정 | 의존성 추가, 로그인/로그아웃 URL, 세션 기반 인증, BCrypt 비밀번호 인코더 설정, `SecurityContext` / `@AuthenticationPrincipal`로 현재 사용자 조회 |
| 1-2 | 'users' 테이블(User 엔티티) 접근용 Repository 코드 구현(UserRepository) |
| 1-3 | 로그인/회원가입 API 구현 (Controller, Service) (API 경로: /api/signup, /api/login, /api/logout, /api/auth/me) |
| 1-4 | 로그인/회원가입용 인증 Request/Response DTO 구현 |
| 1-5 | (프론트) 로그인/회원가입 UI 구현 | 아이디, 비밀번호, 이름 — 유효성 검사, 중복 체크, 실패 시 메시지, 성공 시 메인(URL 입력) UI로 리다이렉트 |
| 1-6 | (프론트) 메인 화면 UI 구현 (URL 입력창) | 로그아웃 버튼, 회원탈퇴 버튼, URL 입력창, URL 입력 버튼 구현 |

**산출물:** `/signup`, `/login`, `/logout`, 로그인 후 메인(URL 입력) 화면 진입

---

## 5. Phase 2 — HTML 파싱 + UI 템플릿

**목표:** 입력 URL의 HTML에서 공연 정보를 추출해 미리 정의한 UI 템플릿에 반영.
정적 html : Jsoup / SPA(JS 랜더링 사이트) : Playwright 사용

| 순서 | 작업 | 설명 |
|------|------|------|
| 3-1 | 목표 사이트 선택 (인터파크 티켓) | 목표 티켓팅 사이트 지정 및 분석 |
| 3-2 | SPA 사이트의 URL 파싱 기반 외부 API 조회 방식 | 사용자가 URL 입력 시, 서버가 URL에서 goodsCode 추출 후 'Interpark summary API'를 직접 호출해 공연 정보 반환 |
| 3-3 | 핵심 공연 정보 파싱 | 제목, 이미지 URL, 공연 설명, 예매 열림 시간(Phase 4 연동) 추출 → DTO 반환 |
| 3-4 | 인터파크 UI 구현 | 인터파크 티켓의 “예매 화면” UI 구현, 파싱한 공연 정보를 UI의 변수들에 매핑 |
| 3-5 | 예매 버튼 후 화면 | “예매하기” 클릭 시 다음 화면(예매 로딩창, 좌석 선택 화면, 예매 성공 화면) UI 분석 후 구현 (대기열 진입 전 단계와 연동) |
| 3-6 | 좌석 랜덤 선택 로직 구현 | 좌석 선택 화면에서 0.5초마다 10자리씩 랜덤하게 '선택 완료' 상태로 돌입, 앞번호가 뒷번호보다 선택될 확률 높도록 가중치 설정 |

**변경사항:** 기존에는 Playwright를 사용한 SPA 사이트의 html 랜더링 후, 랜더링된 html에서 공연 정보들을 찾아서 추출하는 방식을 사용했으나, 랜더링 시간이 너무 오래걸리고 랜더링 결과에 따라 공연 정보가 포함되지 않은 경우가 많았음. 그래서 인터파크 사이트의 API들을 분석해, 핵심 공연 정보들을 요청하는 API(summary API)를 찾았고, 해당 API를 대신 호출해 공연 정보를 받아오는 방식으로 선회함

**summary API 구조:** 'https://api-ticketfront.interpark.com/v1/goods/%s/summary?goodsCode=%s&passCode=&priceGrade=&seatGrade=&ts=%d' 추출한 goodsCode를 API에 포함해서 전달

**산출물:** 파싱 결과 DTO (title, imageUrl, 등), 공연 요약 페이지, 티켓팅 연습 페이지, 예매 로딩창, 좌석 선택 화면, 예매 성공 화면

---

## 6. Phase 3 — 예매 버튼 활성화 타이머 구현

**목표:** HTML에서 “예매 열리는 시간”을 파싱하고, 해당 시각에 예매 버튼 활성화

| 순서 | 작업 | 설명 |
|------|------|------|
| 4-1 | 예매 버튼 활성화 타이머 구현 | 사용자가 5,10,15,30,60초 단위로 예매 버튼 활성화 시간 설정 가능, 시간 되면 버튼 활성화 |

**산출물:** 예매 버튼 활성화 타이머 구현, “사용자가 설정한 시간에 예매 버튼 동작” 플로우

---

## 7. Phase 4 — 대기열 (Kafka + Redis)

**목표:** 짧은 시간 대량 트래픽을 “진입 허용 수”로 제한하고, 나머지는 대기열로 관리.

| 순서 | 작업 | 설명 |
|------|------|------|
| 5-0 | Redis / Kafka 설정 | `build.gradle` 의존성, `application.yml` 프로파일별 설정 (로컬/테스트) — 이 단계에서 인프라 연동 |
| 5-1 | 진입 제한 정책 | 동시 예매 페이지 진입 허용 수 N 정의, 초과 시 대기 |
| 5-2 | Redis 대기열 | 토큰(또는 순번) 발급, 진입 시 토큰 소비 — Redis List/Sorted Set 또는 카운터+TTL 조합 |
| 5-3 | Kafka 연동 | 대기 요청을 Kafka 토픽에 적재, Consumer에서 순차 처리 후 Redis 토큰 부여 또는 리다이렉트 URL 등 콜백 |
| 5-4 | API 설계 | “예매하기 클릭” → 대기열 등록 → 폴링 또는 SSE로 “진입 가능” 알림 → 예매 페이지 이동 |

**산출물:** 대기열 등록/조회 API, 진입 허용 시 예매 페이지 진입 플로우

---

## 8. Phase 5 — 좌석 선택 + 분산 락 + DB 락

**목표:** 좌석 선택 후 결제 페이지 진입 시 해당 좌석을 실시간으로 잠그고 중복 선택을 막는다.

| 순서 | 작업 | 설명 |
|------|------|------|
| 6-1 | 좌석 도메인 | 공연별 좌석, 상태: `AVAILABLE` / `LOCKED` / `BOOKED` (→ SCHEMA.md) |
| 6-2 | Redis 분산 락 | Redisson 또는 `SET key NX EX` — “좌석 선택 → 결제 페이지 진입” 시 `seat:{performanceId}:{seatId}` 락, TTL 5분 |
| 6-3 | DB 비관적 락(선택) | 결제 확정 시 `SELECT ... FOR UPDATE`로 해당 좌석 행 잠근 뒤 `BOOKED`로 변경 — Redis 락과 이중 정합성 |
| 6-4 | 선점 실패 처리 | 이미 락이 있으면 “이미 결제 진행 중인 좌석” 메시지, 좌석 선택 화면에서 해당 좌석 비활성/회색 처리 |
| 6-5 | 결제 페이지 | 락 획득한 사용자만 결제 페이지 진입, 결제 API는 Phase 7에서 “예매 성공” 처리와 연동 |

**산출물:** 좌석 목록/상태 API(락 걸린 좌석 포함), 좌석 선택 → 락 시도 → 결제 페이지 진입/거절 플로우

---

## 9. Phase 6 — 가상 결제 + 예매 완료

**목표:** 결제 버튼 클릭 시 DB에 예매 확정, 좌석 상태 `BOOKED`, Redis 락 해제.

| 순서 | 작업 | 설명 |
|------|------|------|
| 7-1 | 결제 요청 API | “결제하기” 클릭 시 트랜잭션: 좌석 상태 `BOOKED`, Booking 레코드 생성, Redis 락 해제 |
| 7-2 | 성공 응답 | “예매에 성공하셨습니다!” 메시지, 프론트 토스트 등 표시 |
| 7-3 | 실패 처리 | 락 만료/이미 예매됨 등에 대한 에러 메시지 |

**산출물:** 가상 결제 API, 예매 완료 후 마이페이지에서 조회 가능한 Booking 저장

---

## 10. Phase 7 — 마이페이지 + 비정상 종료(TTL)

**목표:** 예매 내역 조회, 좌석만 잡고 나간 경우 5분 후 자동 해제.

| 순서 | 작업 | 설명 |
|------|------|------|
| 8-1 | 마이페이지 API/화면 | 로그인 사용자별 Booking 목록 (공연명, 좌석, 예매 시각 등) |
| 8-2 | Redis TTL | Phase 6 좌석 락 5분 TTL — 만료 시 키 삭제, 해당 좌석 다시 `AVAILABLE`로 복구 (키 만료 이벤트 또는 주기 정리) |
| 8-3 | 일관성 | TTL 만료 시 DB의 선점(LOCKED) 상태도 원복하는 로직 (배치 또는 이벤트 기반 선택) |

**산출물:** 마이페이지 예매 내역, 5분 미결제 시 좌석 자동 해제

---

## 11. 구현 체크리스트

| Phase | 항목 | 완료 |
|-------|------|:----:|
| **0** | 패키지 구조, DB 스키마 | ☐ |
| **1** | Spring Security, 회원가입, 로그인, 메인 진입 | ☐ |
| **2** | HTML 파싱, 공연 정보 DTO, UI 템플릿 | ☐ |
| **3** | 예매 오픈 시각 파싱, 자동 예매 버튼 | ☐ |
| **4** | Redis/Kafka 설정, 대기열, 진입 제한 API | ☐ |
| **5** | 좌석 도메인, Redis 분산 락, (선택) FOR UPDATE, 결제 페이지 진입 | ☐ |
| **6** | 가상 결제 API, 예매 완료 처리 | ☐ |
| **7** | 마이페이지, Redis TTL 비정상 종료 처리 | ☐ |

---

## 12. 아키텍처 요약

| 구간 | 방식 |
|------|------|
| **진입 제어** | Kafka + Redis 대기열(토큰) → 예매 페이지 진입량 제한 |
| **좌석 선점** | Redis 분산 락(Redisson 권장) → 결제 페이지 진입 시 선점 |
| **정합성** | PostgreSQL에서 최종 `BOOKED` 확정, 필요 시 `FOR UPDATE` |
| **비정상 종료** | Redis 락 TTL 5분 → 자동 해제 후 좌석 복구 |

상세 테이블·ER·Redis/Kafka 용도는 `docs/SCHEMA.md` 참고.
