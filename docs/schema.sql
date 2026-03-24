-- 티켓팅 연습 플랫폼 — PostgreSQL DDL
-- DBeaver 등에서 실행하여 테이블 생성
-- 실행 순서: users → performances → seats → bookings

-- ---------------------------------------------------------------------------
-- 1. users — 회원
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    deleted_at      TIMESTAMP
);

-- 'user_id'에 대해서 parital unique index 걸기 (soft delete된 계정은 재사용 허용)
CREATE UNIQUE INDEX uq_users_user_id_active ON users(user_id) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 2. performances — 공연 정보
-- ---------------------------------------------------------------------------
CREATE TABLE performances (
    id                   BIGSERIAL PRIMARY KEY,
    source_url           VARCHAR(2048) NOT NULL,
    title                VARCHAR(500) NOT NULL,
    image_url            VARCHAR(2048),
    start_date           VARCHAR(12),
    end_date             VARCHAR(12),
    link                 VARCHAR(2048),
    goods_code           VARCHAR(50) UNIQUE,
    goods_name           VARCHAR(500),
    place_code           VARCHAR(50),
    place_name           VARCHAR(500),
    play_date            VARCHAR(50),
    play_start_date      TIMESTAMP,
    play_end_date        TIMESTAMP,
    created_by           BIGINT REFERENCES users(id),
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMP
);

CREATE INDEX idx_performances_reservation_open_at ON performances(start_date);
CREATE INDEX idx_performances_source_url ON performances(source_url);
CREATE INDEX idx_performances_goods_code ON performances(goods_code);

-- ---------------------------------------------------------------------------
-- 3. seats — 좌석
-- ---------------------------------------------------------------------------
CREATE TABLE seats (
    id                  BIGSERIAL PRIMARY KEY,
    performance_id      BIGINT NOT NULL REFERENCES performances(id),
    section             VARCHAR(50),
    row_name            VARCHAR(20) NOT NULL,
    seat_number         VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    locked_at           TIMESTAMP,
    locked_by_user_id   BIGINT REFERENCES users(id),
    CONSTRAINT uq_seat_performance_row_number UNIQUE (performance_id, row_name, seat_number)
);

CREATE INDEX idx_seats_performance_id ON seats(performance_id);
CREATE INDEX idx_seats_status ON seats(performance_id, status);

-- ---------------------------------------------------------------------------
-- 4. bookings — 예매 내역
-- ---------------------------------------------------------------------------
CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    performance_id  BIGINT NOT NULL REFERENCES performances(id),
    seat_id         BIGINT NOT NULL REFERENCES seats(id),
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    completed_at    TIMESTAMP,
    CONSTRAINT uq_booking_performance_seat UNIQUE (performance_id, seat_id)
);

CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);
