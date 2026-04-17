import React, { useEffect, useMemo, useState } from 'react';
import './css/SeatSelection.css';

const MAX_SELECTABLE = 4;

const normalizeSeat = (seat, index) => ({
  id: seat.id,
  roundId: seat.roundId,
  seatNumber: seat.seatNumber || `S${String(index + 1).padStart(3, '0')}`,
  displayOrder: seat.displayOrder || index + 1,
  status: String(seat.status || 'AVAILABLE').toLowerCase(),
  lockedAt: seat.lockedAt || null,
  holdExpiresAt: seat.holdExpiresAt || null,
});

const parseStoredSeatIds = (value) => {
  if (!value) return [];

  try {
    const parsed = JSON.parse(value);
    if (!Array.isArray(parsed)) return [];

    return parsed
      .map((seatId) => Number(seatId))
      .filter((seatId) => Number.isFinite(seatId));
  } catch {
    return [];
  }
};

const PublicSeatSelection = ({ performanceData, roundId, queueSessionToken, onSuccess, onGoMain }) => {
  const performanceKey = useMemo(() => (
    performanceData?.goodsCode
      || performanceData?.goodsName
      || 'default-performance'
  ), [performanceData]);

  const storageKey = useMemo(() => (
    `publicSeatSelection:selectedSeats:${roundId || 'no-round'}:${performanceKey}`
  ), [roundId, performanceKey]);

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

  const [zoomLevel, setZoomLevel] = useState(1);
  const [seats, setSeats] = useState([]);
  const seatMap = useMemo(() => new Map(seats.map((seat) => [seat.id, seat])), [seats]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [isLoadingSeats, setIsLoadingSeats] = useState(false);
  const [isHoldingSeats, setIsHoldingSeats] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [modalMessage, setModalMessage] = useState('');

  useEffect(() => {
    if (!roundId) {
      setSeats([]);
      setSelectedSeatIds([]);
      return;
    }

    const savedSeatIds = parseStoredSeatIds(window.localStorage.getItem(storageKey));
    setSelectedSeatIds(savedSeatIds);
  }, [roundId, storageKey]);

  useEffect(() => {
    if (!roundId) {
      return;
    }

    let cancelled = false;

    const loadSeats = async () => {
      setIsLoadingSeats(true);

      try {
        const response = await fetch(`/api/public-seat/${roundId}`, {
          method: 'GET',
          credentials: 'include',
          headers: queueSessionToken
            ? {
              'X-Public-Queue-Token': queueSessionToken
            }
            : undefined
        });

        if (response.status === 404) {
          throw new Error('현재 라운드의 좌석 정보를 찾을 수 없습니다.');
        }

        if (!response.ok) {
          throw new Error('좌석 정보를 불러오지 못했습니다.');
        }

        const payload = await response.json();
        const serverSeats = Array.isArray(payload) ? payload : (payload?.seats || []);
        const normalizedSeats = serverSeats.map(normalizeSeat);

        if (cancelled) return;

        setSeats(normalizedSeats);
        setSelectedSeatIds((prev) => {
          const availableSeatIds = new Set(
            normalizedSeats
              .filter((seat) => seat.status === 'available')
              .map((seat) => seat.id)
          );
          const next = prev.filter((seatId) => availableSeatIds.has(seatId));

          if (prev.length !== next.length) {
            setModalMessage('일부 좌석의 상태가 변경되어 선택이 조정되었습니다.');
            setShowModal(true);
          }

          return next;
        });
      } catch (error) {
        if (cancelled) return;

        setSeats([]);
        setSelectedSeatIds([]);
        setModalMessage(error.message || '좌석 정보를 불러오지 못했습니다.');
        setShowModal(true);
      } finally {
        if (!cancelled) {
          setIsLoadingSeats(false);
        }
      }
    };

    loadSeats();

    return () => {
      cancelled = true;
    };
  }, [roundId]);

  useEffect(() => {
    if (!roundId) {
      return;
    }

    window.localStorage.setItem(storageKey, JSON.stringify(selectedSeatIds));
  }, [selectedSeatIds, storageKey, roundId]);

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

  const handleComplete = async () => {
    if (selectedSeats.length === 0 || !roundId || isHoldingSeats) return;

    const seatIds = selectedSeats.map((seat) => seat.id).filter(Boolean);
    if (seatIds.length === 0) return;

    setIsHoldingSeats(true);

    try {
      const response = await fetch('/api/public-seat/hold', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(queueSessionToken ? { 'X-Public-Queue-Token': queueSessionToken } : {})
        },
        body: JSON.stringify({
          roundId,
          seatIds
        })
      });

      const contentType = response.headers.get('content-type') || '';
      const payload = contentType.includes('application/json') ? await response.json() : null;

      if (response.status === 401) {
        throw new Error('좌석 선점을 진행하려면 로그인이 필요합니다.');
      }

      if (response.status === 409) {
        throw new Error(payload?.message || '이미 선점되었거나 예매 완료된 좌석이 포함되어 있습니다.');
      }

      if (!response.ok) {
        throw new Error(payload?.message || '좌석 임시 선점 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }

      onSuccess?.(selectedSeats, {
        holdToken: payload?.holdToken || null,
        holdExpiresAt: payload?.holdExpiresAt || null
      });
    } catch (error) {
      setModalMessage(error.message || '좌석 임시 선점 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      setShowModal(true);
    } finally {
      setIsHoldingSeats(false);
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
              <p>
                {isLoadingSeats
                  ? '좌석 정보를 불러오는 중입니다.'
                  : '원하는 좌석을 선택한 뒤 선택 완료를 눌러주세요.'}
              </p>
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
                      const className = `seat ${seat.status === 'available' ? 'available' : 'unavailable'} ${isSelected ? 'selected' : ''}`.trim();

                      return (
                        <button
                          key={seat.id}
                          type="button"
                          className={className}
                          onClick={() => toggleSeat(seat)}
                          disabled={seat.status !== 'available'}
                          aria-label={`${seat.seatNumber} 좌석`}
                          title={seat.seatNumber}
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
                        <span className="selected-seat-title">좌석</span>
                        <span className="selected-seat-sub">{seat.displayOrder}번</span>
                      </div>
                      <div className="selected-seat-right">
                        <span className="seat-price">99,000원</span>
                        <button
                          type="button"
                          className="seat-remove-btn"
                          onClick={() => toggleSeat(seat)}
                          aria-label={`${seat.displayOrder}번 삭제`}
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <button type="button" className="btn-seat-complete" disabled={selectedSeats.length === 0 || isLoadingSeats || isHoldingSeats} onClick={handleComplete}>
              {isHoldingSeats ? '좌석 임시 선점 중...' : '선택 완료'}
            </button>
          </section>
        </aside>
      </div>
    </div>
  );
};

export default PublicSeatSelection;
