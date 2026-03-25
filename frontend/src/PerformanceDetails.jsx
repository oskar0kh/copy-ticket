import React, { useState, useEffect } from 'react';
import './styles/PerformanceDetails.css';

const PerformanceDetails = ({ user, onLogout, performanceData }) => {
  const [selectedDate, setSelectedDate] = useState(null);
  const [currentMonth, setCurrentMonth] = useState({ year: 2026, month: 6 });

  // "YYYY.MM.DD" 형식의 문자열을 Date 객체로 변환
  const parsePlayDateString = (dateStr) => {
    if (!dateStr) return null;
    const [year, month, day] = dateStr.split('.');
    return new Date(year, month - 1, day);
  };

  // performanceData가 있으면 초기 월/년을 공연 시작일로 설정
  useEffect(() => {
    console.log('=== PerformanceDetails Debug ===');
    console.log('performanceData:', performanceData);
    if (performanceData) {
      console.log('Fields:');
      console.log('  title:', performanceData.title);
      console.log('  placeName:', performanceData.placeName);
      console.log('  startDate (예매 시작):', performanceData.startDate);
      console.log('  endDate (예매 종료):', performanceData.endDate);
      console.log('  playDate:', performanceData.playDate);
      console.log('  playStartDate:', performanceData.playStartDate);
      console.log('  playEndDate:', performanceData.playEndDate);
      console.log('  goodsName:', performanceData.goodsName);
      console.log('  imageUrl:', performanceData.imageUrl);
    }

    if (performanceData?.playStartDate) {
      const startDate = parsePlayDateString(performanceData.playStartDate);
      console.log('Calendar init with playStartDate:', startDate);
      setCurrentMonth({
        year: startDate.getFullYear(),
        month: startDate.getMonth() + 1
      });
      // 디폴트로 playStartDate를 선택된 날짜로 설정
      setSelectedDate(startDate.getDate());
    }
  }, [performanceData]);

  const handleLogout = async () => {
    if (onLogout) {
      await onLogout();
    }
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
    if (!day || !performanceData?.playStartDate || !performanceData?.playEndDate) {
      return true;
    }

    const dateToCheck = new Date(currentMonth.year, currentMonth.month - 1, day);
    const startDate = parsePlayDateString(performanceData.playStartDate);
    const endDate = parsePlayDateString(performanceData.playEndDate);

    // 날짜 비교는 시간을 무시하고 날짜 부분만 비교
    const checkDateAtMidnight = new Date(dateToCheck.getFullYear(), dateToCheck.getMonth(), dateToCheck.getDate());
    const startDateAtMidnight = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
    const endDateAtMidnight = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());

    return checkDateAtMidnight >= startDateAtMidnight && checkDateAtMidnight <= endDateAtMidnight;
  };

  // 날짜가 공연 기간 범위에 속하는지 확인 (파란 동그라미 표시용)
  const isDateInPerformanceRange = (day) => {
    if (!day || !performanceData?.playStartDate || !performanceData?.playEndDate) {
      return false;
    }

    const dateToCheck = new Date(currentMonth.year, currentMonth.month - 1, day);
    const startDate = parsePlayDateString(performanceData.playStartDate);
    const endDate = parsePlayDateString(performanceData.playEndDate);

    const checkDateAtMidnight = new Date(dateToCheck.getFullYear(), dateToCheck.getMonth(), dateToCheck.getDate());
    const startDateAtMidnight = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
    const endDateAtMidnight = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());

    return checkDateAtMidnight >= startDateAtMidnight && checkDateAtMidnight <= endDateAtMidnight;
  };

  const calendarDays = generateCalendarDays();
  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
  const today = new Date().getDate();
  const isCurrentMonth = currentMonth.month === new Date().getMonth() + 1 &&
                         currentMonth.year === new Date().getFullYear();

  // 공연 기간 문자열 포맷팅 (playStartDate ~ playEndDate)
  const getPerformanceDateDisplay = () => {
    // 1. playStartDate, playEndDate가 있으면 직접 사용 (이미 YYYY.MM.DD 포맷)
    if (performanceData?.playStartDate && performanceData?.playEndDate) {
      return performanceData.playStartDate === performanceData.playEndDate
        ? performanceData.playStartDate
        : `${performanceData.playStartDate} ~ ${performanceData.playEndDate}`;
    }

    // 2. 이미 포맷팅된 playDate가 있으면 사용 (예: "26-05-02 ~ 26-05-03")
    if (performanceData?.playDate) {
      return performanceData.playDate;
    }

    return '';
  };

  return (
    <div className="perf-page-wrapper">
      {/* 상단 네비게이션 배너 */}
      <nav className="perf-navbar">
        <div className="perf-navbar-inner">
          <div className="perf-navbar-left">
            <p className="perf-navbar-logo">COPY TICKET</p>
          </div>
          <div className="perf-navbar-right">
            <span className="perf-navbar-user">{user?.name || '사용자'}</span>
            <button className="perf-navbar-btn" onClick={handleLogout}>로그아웃</button>
          </div>
        </div>
      </nav>

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
              <h1 className="perf-main-title">{performanceData?.title || '레이베이 내한공연'}</h1>
              <p className="perf-sub-title">{performanceData?.goodsName || 'Laufey: A Matter of Time Tour in Seoul'}</p>
              <div className="perf-rank-line">콘서트 주간 00위</div>
            </section>

            {/* 2. 메인 콘텐츠 섹션 */}
            <main className="perf-main-body">
              <div className="perf-info-top-row">
                <div className="poster-box">
                  <img
                    src={performanceData?.imageUrl || "Default Poster"}
                    alt={performanceData?.title || "Default Title"}
                    className="poster-img"
                  />
                  <div className="poster-footer">
                    <span>♡ 티켓캐스트 000</span>
                  </div>
                </div>

                <div className="info-details-table">
                <div className="info-item">
                  <span className="label">장소</span>
                  <span className="value">{performanceData?.placeName || '킨텍스 XX홀'}</span>
                </div>
                <div className="info-item">
                  <span className="label">공연기간</span>
                  <span className="value">{getPerformanceDateDisplay() || '20xx.xx.xx'}</span>
                </div>
                <div className="info-item">
                  <span className="label">관람연령</span>
                  <span className="value">만 X세이상</span>
                </div>
                <div className="info-item price-item">
                  <span className="label">가격</span>
                  <div className="value">
                    <button className="btn-view-price">전체가격보기 ▾</button>
                    <ul className="price-stack">
                      <li><span className="grade">ORCHESTRA PIT</span> <span className="amt">999,999원</span></li>
                      <li><span className="grade">OPERA STALL</span> <span className="amt">999,9990원</span></li>
                      <li><span className="grade">스탠딩석</span> <span className="amt">99,999원</span></li>
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
                    <span className="value">2026년 XX월 XX일 일괄 배송되는 상품입니다.</span>
                </div>
                <div className="info-item">
                    <span className="label">유의사항</span>
                    <span className="value benefit-link">2026년 XX월 XX일 XX시 XX분~2026년 XX월 XX일 XX시 XX분까지<br></br>무통장입금 결제가 불가능합니다.</span>
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
                      return (
                        <div
                          key={index}
                          className={`cal-day ${!day ? 'empty' : ''} ${day === selectedDate ? 'selected' : ''} ${!selectable ? 'disabled' : ''} ${inRange ? 'in-range' : ''}`}
                          onClick={() => day && selectable && setSelectedDate(day)}
                          style={{ cursor: selectable && day ? 'pointer' : 'not-allowed' }}
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
              </div>

              <button className="btn-buy-now">예매하기</button>
              <button className="btn-foreign">BOOKING / 外國語</button>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default PerformanceDetails;