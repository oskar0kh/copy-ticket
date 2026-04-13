import React, { useState, useEffect, useRef, useMemo } from 'react';
import '../private_reservation/css/PerformanceDetails.css';
import LoadingScreen from './public_seat_selection/PublicLoadingScreen';
import CaptchaModal from './public_seat_selection/PublicCaptchaModal';
import SeatSelection from './public_seat_selection/PublicSeatSelection';
import PublicSeatCheck from './public_seat_selection/PublicSeatCheck';
import BookingSuccess from './public_seat_selection/PublicBookingSuccess';
import { formatRemaining, getNextRoundOpenTime } from './roundTime';

const PublicPerformanceDetails = ({
  user,
  performanceData,
  onGoMain,
  showTimerButton = true,
  bookingOpenAt = null,
  bookingCloseAt = null,
  serverTimeOffsetMs = 0,
}) => {
  const performanceKey = useMemo(() => (
    performanceData?.goodsCode
      || performanceData?.goodsName
      || 'default-performance'
  ), [performanceData]);
  const captchaStorageKey = useMemo(() => (
    `captchaCompleted:${user?.id || 'default-user'}:${performanceKey}`
  ), [user, performanceKey]);
  const previousPerformanceKeyRef = useRef(performanceKey);
  const [selectedDate, setSelectedDate] = useState(null);
  const [currentMonth, setCurrentMonth] = useState({ year: 2026, month: 6 });
  const [reservationFlow, setReservationFlow] = useState(() => {
    const saved = window.localStorage.getItem('reservationFlow');
    return saved || null;
  }); // 'loading', 'seats', 'seat-check', 'booking-success', null
  const [captchaCompleted, setCaptchaCompleted] = useState(() => {
    return window.sessionStorage.getItem(captchaStorageKey) === 'true';
  });
  const [selectedSeatsForSuccess, setSelectedSeatsForSuccess] = useState([]);
  const [isTimerModalOpen, setIsTimerModalOpen] = useState(false);
  const [selectedTimerSeconds, setSelectedTimerSeconds] = useState('5');
  const [isReservationReady, setIsReservationReady] = useState(false);
  const [remainingTimerSeconds, setRemainingTimerSeconds] = useState(null);
  const [timerTargetAt, setTimerTargetAt] = useState(null);
  const [isCheckingRound, setIsCheckingRound] = useState(false);
  const timerTimeoutRef = useRef(null);
  const timerIntervalRef = useRef(null);
  const [now, setNow] = useState(() => new Date());
  const [errorModal, setErrorModal] = useState({ isOpen: false, title: '', message: '', onClose: null });

  useEffect(() => {
    if (captchaCompleted) {
      window.sessionStorage.setItem(captchaStorageKey, 'true');
    } else {
      window.sessionStorage.removeItem(captchaStorageKey);
    }
  }, [captchaCompleted, captchaStorageKey]);

  // reservationFlow를 localStorage에 저장
  useEffect(() => {
    if (reservationFlow) {
      window.localStorage.setItem('reservationFlow', reservationFlow);
    } else {
      window.localStorage.removeItem('reservationFlow');
    }
  }, [reservationFlow]);

  // "YYYY.MM.DD" 또는 "YYYY-MM-DD" 형식의 문자열을 Date 객체로 변환
  const parsePlayDateString = (dateStr) => {
    if (!dateStr) return null;
    const normalized = String(dateStr).replace(/\./g, '-');
    const [year, month, day] = normalized.split('-').map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
  };

  // performanceData가 있으면 초기 월/년을 공연 시작일로 설정
  useEffect(() => {
    if (performanceData?.playStartDate) {
      const startDate = parsePlayDateString(performanceData.playStartDate);
      if (!startDate || Number.isNaN(startDate.getTime())) {
        return;
      }
      setCurrentMonth({
        year: startDate.getFullYear(),
        month: startDate.getMonth() + 1
      });
      // 디폴트로 playStartDate를 선택된 날짜로 설정
      setSelectedDate(startDate.getDate());
    }

    // 동일 공연의 새로고침에서는 현재 화면(flow)을 유지하고, 다른 공연으로 바뀔 때만 초기화
    if (previousPerformanceKeyRef.current !== performanceKey) {
      setReservationFlow(null);
      setCaptchaCompleted(false);
      setIsReservationReady(false);
      setRemainingTimerSeconds(null);
      setTimerTargetAt(null);
    }

    previousPerformanceKeyRef.current = performanceKey;
  }, [performanceData, performanceKey]);

  useEffect(() => {
    return () => {
      if (timerTimeoutRef.current) {
        clearTimeout(timerTimeoutRef.current);
      }
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const timerId = window.setInterval(() => {
      setNow(new Date());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, []);

  // 예매 시작 시각을 localStorage에 저장
  useEffect(() => {
    if (bookingOpenAt != null) {
      const storedBookingStart = window.localStorage.getItem('publicBookingStartTime');
      // 저장된 값이 없거나, 현재 bookingOpenAt이 저장된 값보다 최신이면 업데이트
      if (!storedBookingStart || +bookingOpenAt > +storedBookingStart) {
        window.localStorage.setItem('publicBookingStartTime', bookingOpenAt);
      }

      if (bookingCloseAt != null) {
        window.localStorage.setItem('publicBookingCloseTime', bookingCloseAt);
      } else {
        // closeAt이 없는 라운드는 openAt + 10분 규칙을 적용하기 위해 기존 값을 제거한다.
        window.localStorage.removeItem('publicBookingCloseTime');
      }
    }
  }, [bookingOpenAt, bookingCloseAt]);

  const publicBookingWindow = useMemo(() => {
    // localStorage에서 저장된 예매 시작 시각 우선 사용
    const storedBookingStart = window.localStorage.getItem('publicBookingStartTime');
    const storedBookingClose = window.localStorage.getItem('publicBookingCloseTime');
    const effectiveBookingOpenAt = storedBookingStart || bookingOpenAt;
    const effectiveBookingCloseAt = storedBookingClose || bookingCloseAt;
    
    if (effectiveBookingOpenAt == null) return null;

    const openAt = Number(effectiveBookingOpenAt);
    if (Number.isNaN(openAt)) {
      return null;
    }
    const closeAt = effectiveBookingCloseAt != null
      ? Number(effectiveBookingCloseAt)
      : openAt + 10 * 60 * 1000;
    if (Number.isNaN(closeAt)) {
      return null;
    }
    const currentAt = now.getTime() + Number(serverTimeOffsetMs || 0);

    if (currentAt < openAt) {
      return { status: 'before', remaining: openAt - currentAt };
    }

    if (currentAt < closeAt) {
      return { status: 'open', remaining: closeAt - currentAt };
    }

    // 10분 초과 시 localStorage 정리
    window.localStorage.removeItem('publicBookingStartTime');
    window.localStorage.removeItem('publicBookingCloseTime');
    return { status: 'closed', remaining: 0 };
  }, [bookingOpenAt, bookingCloseAt, now, serverTimeOffsetMs]);

  const handleStartReservationTimer = () => {
    const seconds = Number(selectedTimerSeconds);
    const targetAt = Date.now() + seconds * 1000;

    if (timerTimeoutRef.current) {
      clearTimeout(timerTimeoutRef.current);
    }
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
    }

    setIsReservationReady(false);
    setRemainingTimerSeconds(seconds);
    setTimerTargetAt(targetAt);
    setIsTimerModalOpen(false);

    timerIntervalRef.current = setInterval(() => {
      setRemainingTimerSeconds((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(timerIntervalRef.current);
          timerIntervalRef.current = null;
          return null;
        }
        return prev - 1;
      });
    }, 1000);

    timerTimeoutRef.current = setTimeout(() => {
      setIsReservationReady(true);
      setRemainingTimerSeconds(null);
      setTimerTargetAt(null);
      timerTimeoutRef.current = null;
    }, seconds * 1000);
  };

  const getTicketOpenDateDisplay = () => {
    if (performanceData?.playStartDate) {
      return performanceData.playStartDate;
    }
    return '--.--.--';
  };

  const getTicketOpenDday = (timestamp) => {
    return `D-day`;
  };

  // 캘린더 생성 함수
  const generateCalendarDays = () => {
    const year = currentMonth.year;
    const month = currentMonth.month;

    // 해당 월의 첫 날과 마지막 날
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    const startingDayOfWeek = firstDay.getDay(); // 0: 일요일, 6: 토요일
    const daysInMonth = lastDay.getDate();

    const days = [];

    // 이전 달의 빈 칸
    for (let i = 0; i < startingDayOfWeek; i++) {
      days.push(null);
    }

    // 이번 달의 날짜
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }

    return days;
  };

  // 날짜가 공연 가능 범위 내에 있는지 확인
  const isDateSelectable = (day) => {
    if (!day || !performanceData?.playStartDate) {
      return true;
    }

    const dateToCheck = new Date(currentMonth.year, currentMonth.month - 1, day);
    const startDate = parsePlayDateString(performanceData.playStartDate);
    if (!startDate) {
      return true;
    }
    const endDate = parsePlayDateString(performanceData.playEndDate) || startDate;

    // 날짜 비교는 시간을 무시하고 날짜 부분만 비교
    const checkDateAtMidnight = new Date(dateToCheck.getFullYear(), dateToCheck.getMonth(), dateToCheck.getDate());
    const startDateAtMidnight = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
    const endDateAtMidnight = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());

    return checkDateAtMidnight >= startDateAtMidnight && checkDateAtMidnight <= endDateAtMidnight;
  };

  // 날짜가 공연 기간 범위에 속하는지 확인 (파란 동그라미 표시용)
  const isDateInPerformanceRange = (day) => {
    if (!day || !performanceData?.playStartDate) {
      return false;
    }

    const dateToCheck = new Date(currentMonth.year, currentMonth.month - 1, day);
    const startDate = parsePlayDateString(performanceData.playStartDate);
    if (!startDate) {
      return false;
    }
    const endDate = parsePlayDateString(performanceData.playEndDate) || startDate;

    const checkDateAtMidnight = new Date(dateToCheck.getFullYear(), dateToCheck.getMonth(), dateToCheck.getDate());
    const startDateAtMidnight = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
    const endDateAtMidnight = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());

    return checkDateAtMidnight >= startDateAtMidnight && checkDateAtMidnight <= endDateAtMidnight;
  };

  const calendarDays = generateCalendarDays();
  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
  const nextRoundRemainingMs = useMemo(() => {
    const adjustedNow = new Date(now.getTime() + Number(serverTimeOffsetMs || 0));
    const nextOpenAt = getNextRoundOpenTime(adjustedNow).getTime();
    return Math.max(0, nextOpenAt - adjustedNow.getTime());
  }, [now, serverTimeOffsetMs]);
  const scheduledBookingWindow = useMemo(() => {
    const adjustedNow = new Date(now.getTime() + Number(serverTimeOffsetMs || 0));
    const currentAt = adjustedNow.getTime();
    const minute = adjustedNow.getMinutes();

    if (minute < 10 || (minute >= 30 && minute < 40)) {
      const openAtDate = new Date(adjustedNow);
      if (minute < 10) {
        openAtDate.setMinutes(0, 0, 0);
      } else {
        openAtDate.setMinutes(30, 0, 0);
      }

      const openAt = openAtDate.getTime();
      const closeAt = openAt + 10 * 60 * 1000;
      return {
        status: currentAt < closeAt ? 'open' : 'closed',
        remaining: Math.max(0, closeAt - currentAt)
      };
    }

    const nextOpenAt = getNextRoundOpenTime(adjustedNow).getTime();
    return {
      status: 'before',
      remaining: Math.max(0, nextOpenAt - currentAt)
    };
  }, [now, serverTimeOffsetMs]);
  const activeBookingWindow = bookingOpenAt != null ? publicBookingWindow : scheduledBookingWindow;
  const buyNowButtonLabel = bookingOpenAt != null
    ? (publicBookingWindow?.status === 'before'
      ? `${formatRemaining(publicBookingWindow.remaining)} 후 예매 오픈`
      : publicBookingWindow?.status === 'open'
        ? '예매하기'
        : '예매 종료')
    : (activeBookingWindow?.status === 'open'
      ? '예매하기'
      : `${formatRemaining(nextRoundRemainingMs)} 후 예매 오픈`);
  const buyNowButtonDisabled = bookingOpenAt != null
    ? publicBookingWindow?.status !== 'open'
    : activeBookingWindow?.status !== 'open' || isCheckingRound;
  const showTicketOpenGuide = buyNowButtonDisabled;
  const today = new Date().getDate();
  const isCurrentMonth = currentMonth.month === new Date().getMonth() + 1 &&
                         currentMonth.year === new Date().getFullYear();

  // 공연 기간 문자열 포맷팅
  const getPerformanceDateDisplay = () => {
    // 1. playStartDate가 있으면 직접 사용
    if (performanceData?.playStartDate && !performanceData?.playEndDate) {
      return performanceData.playStartDate;
    }

    // 2. playStartDate, playEndDate가 있으면 직접 사용
    if (performanceData?.playStartDate && performanceData?.playEndDate) {
      return performanceData.playStartDate === performanceData.playEndDate
        ? performanceData.playStartDate
        : `${performanceData.playStartDate} ~ ${performanceData.playEndDate}`;
    }

    // 3. 이미 포맷팅된 playDate가 있으면 사용
    if (performanceData?.playDate) {
      return performanceData.playDate;
    }

    return '';
  };

  // 예매하기 버튼 클릭 핸들러
  const handleReservationClick = async () => {
    if (isCheckingRound) {
      return;
    }

    if (bookingOpenAt != null && publicBookingWindow?.status !== 'open') {
      return;
    }

    setIsCheckingRound(true);

    try {
      const response = await fetch('/api/public-round/current', {
        method: 'GET',
        credentials: 'include'
      });

      const contentType = response.headers.get('content-type') || '';

      // 401: 로그인 필요
      if (response.status === 401) {
        setErrorModal({
          isOpen: true,
          title: '로그인 필요',
          message: '예매를 진행하려면 로그인이 필요합니다.',
          onClose: () => setErrorModal({ ...errorModal, isOpen: false })
        });
        return;
      }

      // 404: 라운드 없음
      if (response.status === 404) {
        window.localStorage.removeItem('publicBookingStartTime');
        window.localStorage.removeItem('publicBookingCloseTime');
        setErrorModal({
          isOpen: true,
          title: '라운드 없음',
          message: '현재 열려있는 라운드가 없습니다. 잠시 후 다시 시도해주세요.',
          onClose: () => setErrorModal({ ...errorModal, isOpen: false })
        });
        return;
      }

      // 기타 에러
      if (!response.ok || !contentType.includes('application/json')) {
        window.localStorage.removeItem('publicBookingStartTime');
        window.localStorage.removeItem('publicBookingCloseTime');
        setErrorModal({
          isOpen: true,
          title: '조회 실패',
          message: '라운드 정보 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
          onClose: () => setErrorModal({ ...errorModal, isOpen: false })
        });
        return;
      }

      const currentRound = await response.json();
      if (currentRound?.status !== 'OPEN' || !currentRound?.openAt) {
        window.localStorage.removeItem('publicBookingStartTime');
        window.localStorage.removeItem('publicBookingCloseTime');
        setErrorModal({
          isOpen: true,
          title: '라운드 없음',
          message: '현재 열려있는 라운드가 없습니다. 잠시 후 다시 시도해주세요.',
          onClose: () => setErrorModal({ ...errorModal, isOpen: false })
        });
        return;
      }

      // 1단계: 로딩 화면
      setReservationFlow('loading');
      setCaptchaCompleted(false);

      // 2초 후 좌석 선택 화면으로 전환
      setTimeout(() => {
        window.history.pushState({ view: 'seat-selection' }, '');
        setReservationFlow('seats');
      }, 2000);
    } catch (error) {
      console.error('현재 라운드 조회 실패:', error);
      setErrorModal({
        isOpen: true,
        title: '네트워크 오류',
        message: '라운드 정보 조회 중 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
        onClose: () => setErrorModal({ ...errorModal, isOpen: false })
      });
    } finally {
      setIsCheckingRound(false);
    }
  };

  useEffect(() => {
    if (reservationFlow !== 'seats') return;

    const handleSeatSelectionBack = (event) => {
      // 뒤로가기로 인한 popstate만 처리 (새로고침 시 popstate는 무시)
      if (event.state?.view !== 'seat-selection') {
        setReservationFlow(null);
      }
    };

    window.addEventListener('popstate', handleSeatSelectionBack);
    return () => window.removeEventListener('popstate', handleSeatSelectionBack);
  }, [reservationFlow]);

  // CAPTCHA 입력 완료 핸들러
  const handleCaptchaComplete = (captchaInput) => {
    setCaptchaCompleted(true);
  };

  // 예매 플로우 종료 핸들러
  const handleReservationClose = () => {
    setReservationFlow(null);
    setCaptchaCompleted(false);
    setErrorModal({ ...errorModal, isOpen: false });
    window.localStorage.removeItem('publicBookingStartTime');
    window.localStorage.removeItem('publicBookingCloseTime');
  };

  // 좌석 선택 완료 핸들러
  const handleSeatSelectionSuccess = (selectedSeats) => {
    setSelectedSeatsForSuccess(selectedSeats);
    setReservationFlow('seat-check');
  };

  const handleSeatCheckBack = () => {
    setReservationFlow('seats');
  };

  const handleSeatCheckConfirm = () => {
    setReservationFlow('booking-success');
  };

  // 예매 성공 후 돌아가기 핸들러
  const handleBookingReturn = () => {
    setReservationFlow(null);
    setCaptchaCompleted(false);
    setSelectedSeatsForSuccess([]);
    window.localStorage.removeItem('reservationFlow');
    onGoMain?.();
  };

  // 예매 플로우가 진행 중이면 해당 UI 표시
  if (reservationFlow === 'loading') {
    return <LoadingScreen />;
  }

  if (reservationFlow === 'seats') {
    return (
      <>
        <SeatSelection
          performanceData={performanceData}
          user={user}
          onSuccess={handleSeatSelectionSuccess}
          onGoMain={onGoMain}
        />
        {!captchaCompleted && (
          <CaptchaModal
            onComplete={handleCaptchaComplete}
            captchaImage={null}
          />
        )}
      </>
    );
  }

  if (reservationFlow === 'booking-success') {
    return <BookingSuccess selectedSeats={selectedSeatsForSuccess} onReturn={handleBookingReturn} />;
  }

  if (reservationFlow === 'seat-check') {
    return (
      <PublicSeatCheck
        selectedSeats={selectedSeatsForSuccess}
        onBack={handleSeatCheckBack}
        onConfirm={handleSeatCheckConfirm}
      />
    );
  }

  return (
    <div className="perf-page-wrapper">
      {/* 에러 모달 */}
      {errorModal.isOpen && (
        <div className="perf-error-modal-backdrop" role="dialog" aria-modal="true" onClick={() => errorModal.onClose?.()}>
          <div className="perf-error-modal-card" onClick={(e) => e.stopPropagation()}>
            <h3 className="perf-error-modal-title">{errorModal.title}</h3>
            <p className="perf-error-modal-message">{errorModal.message}</p>
            <button 
              className="perf-error-modal-btn" 
              onClick={() => errorModal.onClose?.()}
            >
              확인
            </button>
          </div>
        </div>
      )}
      {/* 상단 네비게이션 배너 */}
      <nav className="perf-navbar">
        <div className="perf-navbar-inner">
          <div className="perf-navbar-left">
            <p
              className="perf-navbar-logo"
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
          <div className="perf-navbar-right">
            <span className="perf-navbar-user">{user?.name || '사용자'}</span>
            {showTimerButton && (
              <button
                className="perf-navbar-btn perf-navbar-timer-btn"
                onClick={() => setIsTimerModalOpen(true)}
              >
                타이머
              </button>
            )}
          </div>
        </div>
      </nav>

      {showTimerButton && isTimerModalOpen && (
        <div className="perf-timer-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="timer-modal-title">
          <div className="perf-timer-modal-card">
            <button
              type="button"
              className="perf-timer-modal-close"
              onClick={() => setIsTimerModalOpen(false)}
              aria-label="타이머 모달 닫기"
            >
              ×
            </button>
            <h3 id="timer-modal-title">예매하기 버튼 타이머</h3>
            <div className="perf-timer-copy">
              <p>예매 오픈 시각을 조절할 수 있는 타이머입니다.</p>
              <p>
                원하시는 오픈 시각을 설정하시면<br />
                해당 시각에 맞춰 예매하기 버튼이 활성화됩니다.
              </p>
              <p>타이머를 사용해서 예매를 연습해보세요!</p>
            </div>

            <label className="perf-timer-field">
              <span>시간</span>
              <select
                value={selectedTimerSeconds}
                onChange={(e) => setSelectedTimerSeconds(e.target.value)}
              >
                <option value="5">5초</option>
                <option value="10">10초</option>
                <option value="15">15초</option>
                <option value="30">30초</option>
                <option value="60">60초</option>
              </select>
            </label>

            <div className="perf-timer-modal-actions">
              <button className="perf-timer-start-btn" onClick={handleStartReservationTimer}>
                타이머 시작
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 콘텐츠 레이아웃: 왼쪽(본문) + 오른쪽(사이드바) */}
      <div className="perf-content-inner-wrapper">
        <div className="perf-content-layout">
          {/* 왼쪽: 본문 (perf-header-full + perf-main-body) */}
          <div className="perf-left-section">
            {/* 1. 상단 브레드크럼 & 타이틀 섹션 */}
            <section className="perf-header-full">
              <div className="badge-row">
                <span className="badge-item blue">단독판매 ⓘ</span>
                <span className="badge-item green">안심예매 ⓘ</span>
                <span className="badge-item red">예매대기 ⓘ</span>
              </div>
              <h1 className="perf-main-title">{performanceData?.goodsName || '공연 제목 없음'}</h1>
              {performanceData?.subGoodsName && (
                <p className="perf-sub-title">{performanceData.subGoodsName}</p>
              )}
              <div className="perf-rank-line">콘서트 주간 {performanceData?.weekRank || '00'}위</div>
            </section>

            {/* 2. 메인 콘텐츠 섹션 */}
            <main className="perf-main-body">
              <div className="perf-info-top-row">
                <div className="poster-box">
                  <img
                    src={performanceData?.goodsLargeImageUrl || "https://via.placeholder.com/320x426?text=Poster"}
                    alt={performanceData?.goodsName || "공연 포스터"}
                    className="poster-img"
                    onError={(e) => {
                      e.target.src = 'https://via.placeholder.com/320x426?text=Poster';
                    }}
                  />
                  <div className="poster-footer">
                    <span>♡ 티켓캐스트 <strong>{performanceData?.ticketCastCount || '000'}</strong></span>
                  </div>
                </div>

                <div className="info-details-table">
                <div className="info-item">
                  <span className="label">장소</span>
                  <span className="value">{performanceData?.placeName || '장소 정보 없음'}</span>
                </div>
                <div className="info-item">
                  <span className="label">공연기간</span>
                  <span className="value">{getPerformanceDateDisplay() || '공연 기간 정보 없음'}</span>
                </div>
                <div className="info-item">
                  <span className="label">공연시간</span>
                  <span className="value">{performanceData?.runningTime ? `${performanceData.runningTime}분` : '정보 없음'}</span>
                </div>
                <div className="info-item">
                  <span className="label">관람연령</span>
                  <span className="value">{performanceData?.viewRateName || '정보 없음'}</span>
                </div>
                <div className="info-item price-item">
                  <span className="label">가격</span>
                  <div className="value">
                    <button className="btn-view-price">전체가격보기 ▾</button>
                    <ul className="price-stack">
                      <li><span className="grade">ORCHESTRA PIT</span> <span className="amt">-원</span></li>
                      <li><span className="grade">OPERA STALL</span> <span className="amt">-원</span></li>
                      <li><span className="grade">스탠딩석</span> <span className="amt">-원</span></li>
                    </ul>
                  </div>
                </div>
                
                {/* 하단 혜택/배송 섹션 */}
                <div className="perf-bottom-info">
                <div className="info-item">
                    <span className="label">혜택</span>
                    <span className="value benefit-link">무이자할부 ▶</span>
                </div>
                <div className="info-item">
                    <span className="label">프로모션</span>
                    <span className="value benefit-link">카카오머니 결제 시 4천원 즉시할인(일 선착순)</span>
                </div>
                <div className="info-item">
                    <span className="label">배송</span>
                    <span className="value">2026년 00월 00일 일괄 배송되는 상품입니다.</span>
                </div>
                <div className="info-item">
                    <span className="label">유의사항</span>
                    <span className="value benefit-link">2026년 00월 00일 00시 00분~2026년 00월 00일 00시 00분까지<br></br>무통장입금 결제가 불가능합니다.</span>
                </div>
                </div>
              </div>
            </div>
            </main>
          </div>

          {/* 오른쪽: 예매 사이드바 */}
          <aside className="perf-right-sidebar">
            <div className="sticky-sidebar">
              <div className="sidebar-card">
                {showTicketOpenGuide ? (
                  <div className="ticket-open-guide">
                    <h4 className="ticket-open-guide-title">티켓오픈안내</h4>
                    <div className="ticket-open-guide-main">
                      <span className="ticket-open-guide-dday">{getTicketOpenDday(timerTargetAt)}</span>
                      <div className="ticket-open-guide-info">
                        <strong>티켓오픈</strong>
                        <span>{getTicketOpenDateDisplay()}</span>
                      </div>
                    </div>
                    <p className="ticket-open-guide-desc">티켓 오픈 시간은 예고없이 변경될 수 있습니다.</p>
                  </div>
                ) : (
                  <>
                    <div className="card-title">관람일</div>
                    <div className="mini-calendar">
                      <div className="cal-header">
                        <button
                          className="cal-nav-btn"
                          onClick={() => {
                            setCurrentMonth(prev => ({
                              ...prev,
                              month: prev.month === 1 ? 12 : prev.month - 1,
                              year: prev.month === 1 ? prev.year - 1 : prev.year
                            }));
                            setSelectedDate(null);
                          }}
                        >
                          &lt;
                        </button>
                        <span className="cal-title">{currentMonth.year}. {String(currentMonth.month).padStart(2, '0')}</span>
                        <button
                          className="cal-nav-btn"
                          onClick={() => {
                            setCurrentMonth(prev => ({
                              ...prev,
                              month: prev.month === 12 ? 1 : prev.month + 1,
                              year: prev.month === 12 ? prev.year + 1 : prev.year
                            }));
                            setSelectedDate(null);
                          }}
                        >
                          &gt;
                        </button>
                      </div>

                      <div className="cal-weekdays">
                        {weekDays.map(day => <div key={day} className="weekday">{day}</div>)}
                      </div>

                      <div className="cal-grid">
                        {calendarDays.map((day, index) => {
                          const selectable = isDateSelectable(day);
                          const inRange = isDateInPerformanceRange(day);
                          const isSunday = index % 7 === 0;
                          return (
                            <div
                              key={index}
                              className={`cal-day ${!day ? 'empty' : ''} ${day === selectedDate ? 'selected' : ''} ${!selectable ? 'disabled' : ''} ${inRange ? 'in-range' : ''} ${isSunday && day ? 'sunday' : ''}`}
                              onClick={() => day && selectable && setSelectedDate(day)}
                              style={{ cursor: selectable && day ? 'pointer' : 'default' }}
                            >
                              {day}
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    <div className="card-title">회차</div>
                    <div className="time-box selected">1회 00:00</div>
                    <div className="disclaimer-notice">잔여석 안내 서비스를 제공하지 않습니다.</div>
                  </>
                )}
              </div>

              <button className="btn-buy-now" onClick={handleReservationClick} disabled={buyNowButtonDisabled}>
                {buyNowButtonLabel}
              </button>
              <button className="btn-foreign">BOOKING / 外國語</button>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default PublicPerformanceDetails;