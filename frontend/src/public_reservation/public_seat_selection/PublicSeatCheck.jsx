import React from 'react';
import './css/SeatSelection.css';

const PublicSeatCheck = ({ selectedSeats, onBack, onConfirm, holdExpiresAt, isConfirming = false }) => {
  const hasSeats = Array.isArray(selectedSeats) && selectedSeats.length > 0;

  return (
    <div className="public-seat-check-outer">
      <div className="public-seat-check-container">
        <div className="public-seat-check-card">
          <h1 className="public-seat-check-title">선택 좌석 확인</h1>
          <p className="public-seat-check-message">아래 좌석으로 예매를 진행할까요?</p>
          {holdExpiresAt && (
            <p className="public-seat-check-message">
              임시 선점 만료 시각: {new Date(holdExpiresAt).toLocaleTimeString('ko-KR', { hour12: false })}
            </p>
          )}

          <div className="public-seat-check-list" role="list" aria-label="선택 좌석 목록">
            {hasSeats ? (
              selectedSeats.map((seat) => (
                <div className="public-seat-check-item" key={seat.id} role="listitem">
                  <span className="public-seat-check-label">스탠딩석</span>
                  <span className="public-seat-check-value">{seat.displayOrder}번</span>
                </div>
              ))
            ) : (
              <p className="public-seat-check-empty">선택된 좌석이 없습니다.</p>
            )}
          </div>

          <div className="public-seat-check-actions">
            <button type="button" className="public-seat-check-btn secondary" onClick={onBack}>
              좌석 다시 선택
            </button>
            <button
              type="button"
              className="public-seat-check-btn primary"
              onClick={onConfirm}
              disabled={!hasSeats || isConfirming}
            >
              {isConfirming ? '좌석 확정 중...' : '좌석 확정하기'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PublicSeatCheck;
