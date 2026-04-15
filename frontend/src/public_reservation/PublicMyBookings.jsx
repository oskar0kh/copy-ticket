import React from 'react';
import './css/PublicMyBookings.css';

export default function PublicMyBookings({ isOpen, onClose, bookings, performanceName }) {
  if (!isOpen) return null;

  const hasBookings = bookings && bookings.seatIds && bookings.seatIds.length > 0;
  const bookedAtTime = bookings?.bookedAt 
    ? new Date(bookings.bookedAt).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
    : '-';
  
  // 라운드 번호 계산 (roundId로부터 몇 번째 라운드인지 표시)
  const roundNumber = bookings?.roundId || '-';

  return (
    <div className="public-my-bookings-overlay">
      <div className="public-my-bookings-modal">
        <div className="public-my-bookings-header">
          <h2>나의 예매내역</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="public-my-bookings-content">
          {hasBookings ? (
            <>
              <div className="public-my-bookings-info">
                <p className="perf-name">{roundNumber}라운드 ({bookedAtTime})</p>
                <p className="booked-time">예매 시각: {new Date(bookings.bookedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</p>
              </div>

              <div className="public-my-bookings-seats">
                <h3>선택된 좌석</h3>
                <ul className="seats-list">
                  {bookings.seatNumbers.map((seatNumber, index) => {
                    // 'S' 제거 (S001 -> 1, S002 -> 2 등)
                    const cleanSeatNumber = seatNumber.startsWith('S') ? seatNumber.substring(1) : seatNumber;
                    return (
                      <li key={index} className="seat-item">
                        <span className="seat-label">스탠딩석</span>
                        <span className="seat-number">{cleanSeatNumber}번</span>
                      </li>
                    );
                  })}
                </ul>
                <p className="total-seats">총 {bookings.bookingCount}개 좌석</p>
              </div>
            </>
          ) : (
            <div className="no-bookings">
              <p>현재 라운드에서 예매한 내역이 없습니다.</p>
            </div>
          )}
        </div>

        <div className="public-my-bookings-footer">
          <button className="close-modal-btn" onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}
