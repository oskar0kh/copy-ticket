import React from 'react';
import './css/SeatSelection.css';

const BookingSuccess = ({ selectedSeats, onReturn }) => {
  const seatNumbers = selectedSeats.map((seat) => seat.displayOrder).join(', ');

  const handleInputUrl = () => {
    onReturn();
  };

  return (
    <div className="booking-success-outer">
      <div className="booking-success-container">
        <div className="success-card">
          <h1 className="success-title">티켓팅 성공!</h1>
          <p className="success-message">
            예매하신 좌석 번호: <span className="seat-numbers">{seatNumbers}</span>
          </p>
          <button type="button" className="success-btn" onClick={handleInputUrl}>
            메인 페이지로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
};

export default BookingSuccess;
