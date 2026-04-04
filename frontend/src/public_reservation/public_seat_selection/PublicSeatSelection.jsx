import React, { useEffect, useMemo, useState } from 'react';
import './css/SeatSelection.css';

const ROWS = Array.from({ length: 20 }, (_, index) => String.fromCharCode(65 + index));
const SEATS_PER_ROW = 20;
const MAX_SELECTABLE = 4;

const buildAllAvailableSeats = () => {
  const seats = [];

  for (let rowIdx = 0; rowIdx < ROWS.length; rowIdx += 1) {
    for (let number = 1; number <= SEATS_PER_ROW; number += 1) {
      const flatIndex = rowIdx * SEATS_PER_ROW + (number - 1);
      const row = ROWS[rowIdx];
      const id = `${row}-${number}`;

      seats.push({
        id,
        row,
        number,
        displayOrder: flatIndex + 1,
        status: 'available'
      });
    }
  }

  return seats;
};

const SeatSelection = ({ performanceData, onSuccess, onGoMain }) => {
  const performanceKey = useMemo(() => (
    performanceData?.goodsCode
      || performanceData?.goodsName
      || 'default-performance'
  ), [performanceData]);

  const bannerText = useMemo(() => {
    const title = performanceData?.goodsName || '공연';
    const startDate = performanceData?.playStartDate;
    const endDate = performanceData?.playEndDate;

    let dateText = '';
    if (startDate && endDate) {
      dateText = startDate === endDate ? startDate : `${startDate} - ${endDate}`;
    } else {
      dateText = startDate || endDate || '';
    }

    return dateText ? `${title} · ${dateText}` : title;
  }, [performanceData]);

  const userSelectionStorageKey = `seatSelection:selectedSeats:${performanceKey}`;

  const [zoomLevel, setZoomLevel] = useState(1);
  const seats = useMemo(() => buildAllAvailableSeats(), []);
  const seatMap = useMemo(() => new Map(seats.map((seat) => [seat.id, seat])), [seats]);

  const [selectedSeatIds, setSelectedSeatIds] = useState([]);

  const [showModal, setShowModal] = useState(false);
  const [modalMessage, setModalMessage] = useState('');

  // SeatSelection 진입 시마다 사용자가 이전에 선택한 좌석 초기화
  useEffect(() => {
    setSelectedSeatIds([]);
    window.localStorage.removeItem(userSelectionStorageKey);
  }, [userSelectionStorageKey]);

  // SeatSelection을 떠날 때 좌석 상태 초기화
  useEffect(() => {
    return () => {
      window.localStorage.removeItem(userSelectionStorageKey);
    };
  }, [userSelectionStorageKey]);

  useEffect(() => {
    window.localStorage.setItem(userSelectionStorageKey, JSON.stringify(selectedSeatIds));
  }, [selectedSeatIds, userSelectionStorageKey]);

  const selectedSeats = useMemo(
    () => selectedSeatIds.map((id) => seatMap.get(id)).filter(Boolean),
    [selectedSeatIds, seatMap]
  );

  const toggleSeat = (seat) => {
    if (seat.status !== 'available') return;

    setSelectedSeatIds((prev) => {
      if (prev.includes(seat.id)) {
        return prev.filter((seatId) => seatId !== seat.id);
      }

      if (prev.length >= MAX_SELECTABLE) {
        return prev;
      }

      return [...prev, seat.id];
    });
  };

  const clearSelection = () => setSelectedSeatIds([]);
  const zoomIn = () => setZoomLevel((prev) => Math.min(1.5, Number((prev + 0.1).toFixed(2))));
  const zoomOut = () => setZoomLevel((prev) => Math.max(0.8, Number((prev - 0.1).toFixed(2))));

  const handleComplete = () => {
    if (selectedSeats.length === 0) return;

    if (onSuccess) {
      onSuccess(selectedSeats);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setModalMessage('');
  };

  return (
    <div className="seat-selection-outer">
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <p className="modal-text">{modalMessage}</p>
            <button type="button" className="modal-btn" onClick={closeModal}>확인</button>
          </div>
        </div>
      )}

      <nav className="seat-navbar">
        <div className="seat-navbar-inner">
          <p
            className="seat-navbar-logo"
            onClick={onGoMain}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onGoMain?.();
              }
            }}
          >
            COPY TICKET
          </p>
        </div>
      </nav>

      <section className="seat-info-banner">
        <div className="seat-info-banner-inner">
          {bannerText}
        </div>
      </section>

      <div className="seat-selection-container">
        <main className="seat-content-wrapper">
          <div className="seat-zoom-layout" style={{ transform: `scale(${zoomLevel})` }}>
            <div className="seat-notice-outer">
              <p>빠른 번호를 예매하실수록 먼저 입장할 수 있습니다.</p>
            </div>

            <div className="stage-and-seats-wrapper">
              <div className="stage-and-seats-inner">
                <div className="stage-box">
                  <span className="stage-text">STAGE</span>
                </div>

                <section className="seat-section">
                  <div className="section-header">
                    <h3>좌석도</h3>
                  </div>

                  <div className="seats-grid">
                    {seats.map((seat) => {
                      const isSelected = selectedSeatIds.includes(seat.id);
                      const displayStatus = seat.status;
                      const className = `seat ${displayStatus} ${isSelected ? 'selected' : ''}`.trim();

                      return (
                        <button
                          key={seat.id}
                          type="button"
                          className={className}
                          onClick={() => toggleSeat(seat)}
                          disabled={displayStatus !== 'available'}
                          aria-label={`${seat.row}열 ${seat.number}번 좌석`}
                          title={`${seat.row}열 ${seat.number}번`}
                        />
                      );
                    })}
                  </div>
                </section>
              </div>
            </div>
          </div>

          <div className="zoom-controls">
            <button type="button" className="zoom-btn" onClick={zoomIn} aria-label="확대">+</button>
            <button type="button" className="zoom-btn" onClick={zoomOut} aria-label="축소">-</button>
          </div>
        </main>

        <aside className="seat-sidebar">
          <section className="seat-summary">
            <header className="summary-header">
              <h2 className="summary-title">
                선택 좌석 <span className="summary-count">{selectedSeats.length}</span>
              </h2>
              <button type="button" className="summary-clear-btn" onClick={clearSelection}>전체삭제</button>
            </header>

            <div className={`summary-selection-area ${selectedSeats.length === 0 ? 'empty' : 'filled'}`}>
              {selectedSeats.length === 0 ? (
                <div className="no-selection">
                  <p>선택된 좌석이 없습니다.</p>
                </div>
              ) : (
                <div className="selected-seats-list">
                  {selectedSeats.map((seat) => (
                    <div className="selected-seat-item" key={seat.id}>
                      <div className="selected-seat-left">
                        <span className="selected-seat-title">스탠딩석</span>
                        <span className="selected-seat-sub">{seat.displayOrder}번</span>
                      </div>
                      <div className="selected-seat-right">
                        <span className="seat-price">99,000원</span>
                        <button
                          type="button"
                          className="seat-remove-btn"
                          onClick={() => toggleSeat(seat)}
                          aria-label={`${seat.row}열 ${seat.number}번 삭제`}
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <button type="button" className="btn-seat-complete" disabled={selectedSeats.length === 0} onClick={handleComplete}>
              선택 완료
            </button>
          </section>
        </aside>
      </div>
    </div>
  );
};

export default SeatSelection;