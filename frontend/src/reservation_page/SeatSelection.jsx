import React, { useEffect, useMemo, useState } from 'react';
import './css/SeatSelection.css';

const ROWS = Array.from({ length: 15 }, (_, index) => String.fromCharCode(65 + index));
const SEATS_PER_ROW = 20;
const MAX_SELECTABLE = 4;
const SYSTEM_SELECT_INTERVAL = 500;
const SYSTEM_SELECT_COUNT = 10;
const BLOCK_SIZE = 20;
const FIRST_BLOCK_WEIGHT = 2.2;
const LAST_BLOCK_WEIGHT = 0.1;

const xmur3 = (str) => {
  let h = 1779033703 ^ str.length;
  for (let i = 0; i < str.length; i += 1) {
    h = Math.imul(h ^ str.charCodeAt(i), 3432918353);
    h = (h << 13) | (h >>> 19);
  }

  return () => {
    h = Math.imul(h ^ (h >>> 16), 2246822507);
    h = Math.imul(h ^ (h >>> 13), 3266489909);
    h ^= h >>> 16;
    return h >>> 0;
  };
};

const mulberry32 = (a) => () => {
  let t = (a += 0x6d2b79f5);
  t = Math.imul(t ^ (t >>> 15), t | 1);
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

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

const generateSystemSelections = (seedKey, excludeIds = new Set()) => {
  const totalSeats = ROWS.length * SEATS_PER_ROW;
  const seed = xmur3(seedKey)();
  const random = mulberry32(seed);
  const totalBlocks = Math.ceil(totalSeats / BLOCK_SIZE);
  const rawBlockWeights = Array.from({ length: totalBlocks }, (_, blockIdx) => {
    if (totalBlocks === 1) return 1;
    const t = blockIdx / (totalBlocks - 1);
    return FIRST_BLOCK_WEIGHT + (LAST_BLOCK_WEIGHT - FIRST_BLOCK_WEIGHT) * t;
  });
  const rawAverageWeight = rawBlockWeights.reduce((sum, value) => sum + value, 0) / rawBlockWeights.length;
  const normalizedBlockWeights = rawBlockWeights.map((weight) => weight / rawAverageWeight);

  const selectedIds = [];
  let attempts = 0;
  const maxAttempts = totalSeats * 2;

  while (selectedIds.length < SYSTEM_SELECT_COUNT && attempts < maxAttempts) {
    attempts += 1;
    const flatIndex = Math.floor(random() * totalSeats);
    const blockIdx = Math.floor(flatIndex / BLOCK_SIZE);
    const weightedProbability = Math.max(0.01, Math.min(0.995, normalizedBlockWeights[blockIdx]));

    if (random() < weightedProbability) {
      const rowIdx = Math.floor(flatIndex / SEATS_PER_ROW);
      const number = (flatIndex % SEATS_PER_ROW) + 1;
      const row = ROWS[rowIdx];
      const id = `${row}-${number}`;

      if (!selectedIds.includes(id) && !excludeIds.has(id)) {
        selectedIds.push(id);
      }
    }
  }

  return selectedIds;
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
  const systemSelectionStorageKey = `seatSelection:systemSelected:${performanceKey}`;

  const [zoomLevel, setZoomLevel] = useState(1);
  const seats = useMemo(() => buildAllAvailableSeats(), []);
  const seatMap = useMemo(() => new Map(seats.map((seat) => [seat.id, seat])), [seats]);

  const [systemSelectedSeatIds, setSystemSelectedSeatIds] = useState(() => {
    const raw = window.localStorage.getItem(systemSelectionStorageKey);
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  });

  const [selectedSeatIds, setSelectedSeatIds] = useState([]);

  const [showModal, setShowModal] = useState(false);
  const [modalMessage, setModalMessage] = useState('');

  // 현재 세션에서 새로고침이 발생했는지 추적
  const [isRefreshed, setIsRefreshed] = useState(() => {
    const refreshKey = `seatSelection:refreshed:${performanceKey}`;
    const hasRefreshFlag = window.sessionStorage.getItem(refreshKey);
    if (!hasRefreshFlag) {
      window.sessionStorage.setItem(refreshKey, 'true');
      return false; // 초기 진입
    }
    return true; // 새로고침됨
  });

  // SeatSelection 진입 시마다 사용자가 이전에 선택한 좌석 초기화
  useEffect(() => {
    setSelectedSeatIds([]);
    window.localStorage.removeItem(userSelectionStorageKey);
  }, [userSelectionStorageKey]);

  // SeatSelection을 떠날 때 모든 좌석 상태 초기화 (새로고침 제외)
  // 새로고침(F5)은 컴포넌트 언마운트를 하지 않으므로 자동으로 상태 유지됨
  useEffect(() => {
    return () => {
      // 컴포넌트 언마운트 시 (화면 전환 시) 좌석 상태 초기화
      window.localStorage.removeItem(userSelectionStorageKey);
      window.localStorage.removeItem(systemSelectionStorageKey);
      window.sessionStorage.removeItem(`seatSelection:refreshed:${performanceKey}`);
    };
  }, [userSelectionStorageKey, systemSelectionStorageKey, performanceKey]);

  // 시스템이 0.5초마다 10석 추가 선택
  useEffect(() => {
    const interval = setInterval(() => {
      setSystemSelectedSeatIds((prev) => {
        if (prev.length >= seats.length) {
          clearInterval(interval);
          return prev;
        }

        const excludeSet = new Set(prev);
        const newSelections = generateSystemSelections(`${performanceKey}:${Date.now()}:${prev.length}`, excludeSet);
        if (newSelections.length === 0) {
          return prev;
        }
        const updated = [...prev, ...newSelections];
        window.localStorage.setItem(systemSelectionStorageKey, JSON.stringify(updated));
        return updated;
      });
    }, SYSTEM_SELECT_INTERVAL);

    return () => clearInterval(interval);
  }, [performanceKey, seats.length, systemSelectionStorageKey]);

  useEffect(() => {
    const nextSelected = selectedSeatIds.filter((seatId) => !systemSelectedSeatIds.includes(seatId));
    if (nextSelected.length !== selectedSeatIds.length) {
      setSelectedSeatIds(nextSelected);
      return;
    }

    window.localStorage.setItem(userSelectionStorageKey, JSON.stringify(nextSelected));
  }, [selectedSeatIds, systemSelectedSeatIds, userSelectionStorageKey]);

  const selectedSeats = useMemo(
    () => selectedSeatIds.map((id) => seatMap.get(id)).filter(Boolean),
    [selectedSeatIds, seatMap]
  );

  const toggleSeat = (seat) => {
    // 새로고침 후에만 시스템 선택 좌석 선택 차단
    if (isRefreshed && systemSelectedSeatIds.includes(seat.id)) return;
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

    const hasConflict = selectedSeatIds.some((seatId) => systemSelectedSeatIds.includes(seatId));
    if (hasConflict) {
      setShowModal(true);
      setModalMessage('이미 예매한 좌석입니다');
      return;
    }

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
                      const isSystemSelected = systemSelectedSeatIds.includes(seat.id);
                      // 새로고침 후에만 시스템 선택 좌석을 unavailable로 표시
                      const displayStatus = (isRefreshed && isSystemSelected) ? 'unavailable' : seat.status;
                      const className = `seat ${displayStatus} ${isSelected ? 'selected' : ''}`.trim();

                      return (
                        <button
                          key={seat.id}
                          type="button"
                          className={className}
                          onClick={() => toggleSeat(seat)}
                          disabled={displayStatus !== 'available' || (isRefreshed && isSystemSelected)}
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