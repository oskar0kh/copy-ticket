-- 티켓팅 연습 플랫폼 — PostgreSQL DDL
-- 실행 순서: users → performances → public_rounds → public_seats → public_bookings

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
-- 2. performances — 공연 정보 (API 응답에서 파싱)
-- ---------------------------------------------------------------------------
CREATE TABLE performances (
    id                      BIGSERIAL PRIMARY KEY,
    source_url              VARCHAR(2048) NOT NULL,
    goods_code              VARCHAR(50),
    goods_name              VARCHAR(500) NOT NULL,
    sub_goods_name          VARCHAR(1000),
    place_name              VARCHAR(500),
    view_rate_name          VARCHAR(100),
    running_time            VARCHAR(20),
    play_start_date         VARCHAR(10),
    play_end_date           VARCHAR(10),
    goods_large_image_url   VARCHAR(2048),
    ticket_open_date        VARCHAR(20),
    booking_end_date        VARCHAR(20),
    ticket_cast_count       INTEGER,
    week_rank               VARCHAR(10),
    created_by              BIGINT REFERENCES users(id),
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMP
);

CREATE INDEX idx_performances_source_url ON performances(source_url);
CREATE INDEX idx_performances_created_by ON performances(created_by);

-- ---------------------------------------------------------------------------
-- 3. public_rounds — 공개 라운드 (전역, 매시 :00, :30분마다 자동 생성)
-- ---------------------------------------------------------------------------
CREATE TABLE public_rounds (
    id              BIGSERIAL PRIMARY KEY,
    round_id        INTEGER NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL,
    open_at         TIMESTAMP NOT NULL,
    close_at        TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_public_rounds_status ON public_rounds(status);

-- ---------------------------------------------------------------------------
-- 4. public_seats — 좌석
-- ---------------------------------------------------------------------------
CREATE TABLE public_seats (
    id                  BIGSERIAL PRIMARY KEY,
    round_id            BIGINT NOT NULL REFERENCES public_rounds(id),
    seat_number         VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    locked_at           TIMESTAMP,
    locked_by_user_id   VARCHAR(255),
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

-- ---------------------------------------------------------------------------
-- 5. public_bookings — 예매 내역
-- ---------------------------------------------------------------------------
CREATE TABLE public_bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    booked_by_user_id VARCHAR(255) NOT NULL,
    round_id        BIGINT NOT NULL REFERENCES public_rounds(id),
    seat_id         BIGINT NOT NULL REFERENCES public_seats(id),
    seat_number     VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    booked_at       TIMESTAMP NOT NULL,
    CONSTRAINT uq_public_booking_round_seat UNIQUE (round_id, seat_id)
);

CREATE INDEX idx_public_bookings_user_created ON public_bookings(user_id, created_at DESC);
