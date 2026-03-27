import React, { useState } from 'react';
import '../styles/SeatSelection.css';

const SeatSelection = ({ performanceData }) => {
  const [selectedSeats, setSelectedSeats] = useState([]);

  // 좌석 섹션 데이터 생성
  const generateSeats = (count) => {
    const seats = [];
    for (let i = 0; i < count; i++) {
      const seatNum = i + 1;
      const isAvailable = Math.random() > 0.3; // 70% 예약 가능
      seats.push({
        id: `seat-${seatNum}`,
        number: seatNum,
        available: isAvailable,
      });
    }
    return seats;
  };

  const sections = [
    { name: '스탠딩 A (입장방향)', seats: generateSeats(140) },
    { name: '스탠딩 B (입장방향)', seats: generateSeats(140) },
    { name: '스탠딩 C (입장방향)', seats: generateSeats(100) },
    { name: '스탠딩 D (입장방향)', seats: generateSeats(100) },
  ];

  const handleSeatClick = (sectionIndex, seatId) => {
    const seatKey = `${sectionIndex}-${seatId}`;
    if (selectedSeats.includes(seatKey)) {
      setSelectedSeats(selectedSeats.filter(s => s !== seatKey));
    } else {
      setSelectedSeats([...selectedSeats, seatKey]);
    }
  };

  return (
    <div className="seat-selection-container">
      <div className="seat-main">
        <div className="seat-header">
          <h2>{performanceData?.goodsName || '공연 제목'}</h2>
          <p>{performanceData?.placeName || '공연 장소'} · 2026-04-04(토) 6:00 PM · 일정변경가</p>
        </div>

        <div className="seat-notice">
          <p>소중한 예술 시연법을 심혜할 안내 문구가 표시됩니다.</p>
        </div>

        <div className="stage-section">
          <div className="stage-label">STAGE</div>
          <div className="stage-area"></div>
        </div>

        <div className="seat-sections">
          {sections.map((section, sectionIndex) => (
            <div key={sectionIndex} className="seat-section">
              <div className="section-header">
                <h3>{section.name}</h3>
                <span className="section-price">가격정보</span>
              </div>

              <div className="seats-grid">
                {section.seats.map((seat) => (
                  <button
                    key={seat.id}
                    className={`seat ${seat.available ? 'available' : 'unavailable'} ${
                      selectedSeats.includes(`${sectionIndex}-${seat.id}`) ? 'selected' : ''
                    }`}
                    onClick={() => handleSeatClick(sectionIndex, seat.id)}
                    disabled={!seat.available}
                  >
                    <span>{seat.number}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="seat-legend">
          <div className="legend-item">
            <div className="legend-color available"></div>
            <span>예약 가능</span>
          </div>
          <div className="legend-item">
            <div className="legend-color unavailable"></div>
            <span>예약 불가</span>
          </div>
          <div className="legend-item">
            <div className="legend-color selected"></div>
            <span>선택된 좌석</span>
          </div>
        </div>
      </div>

      <aside className="seat-sidebar">
        <div className="seat-summary">
          <div className="summary-title">선택 좌석</div>

          {selectedSeats.length > 0 ? (
            <div className="selected-seats-list">
              {selectedSeats.map((seatKey) => {
                const [sectionIndex, seatId] = seatKey.split('-');
                const seat = sections[sectionIndex].seats.find(s => s.id === seatId);
                return (
                  <div key={seatKey} className="selected-seat-item">
                    <span>{sections[sectionIndex].name}</span>
                    <span className="seat-num">{seat.number}번</span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="no-selection">
              <p>선택한 좌석이 없습니다.</p>
            </div>
          )}

          <div className="summary-info">
            <div className="info-row">
              <span>선택 좌석 수</span>
              <span>{selectedSeats.length}개</span>
            </div>
          </div>

          <button className="btn-seat-complete" disabled={selectedSeats.length === 0}>
            좌석 선택 완료
          </button>
        </div>
      </aside>
    </div>
  );
};

export default SeatSelection;
