import React, { useState } from 'react';
import './styles/PerformanceDetails.css';

const PerformanceDetails = ({ user, onLogout }) => {
  const [selectedDate, setSelectedDate] = useState(7);
  const [currentMonth, setCurrentMonth] = useState({ year: 2026, month: 6 });

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

  const calendarDays = generateCalendarDays();
  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
  const today = new Date().getDate();
  const isCurrentMonth = currentMonth.month === new Date().getMonth() + 1 &&
                         currentMonth.year === new Date().getFullYear();

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
              <h1 className="perf-main-title">레이베이 내한공연</h1>
              <p className="perf-sub-title">Laufey: A Matter of Time Tour in Seoul</p>
              <div className="perf-rank-line">콘서트 주간 56위</div>
            </section>

            {/* 2. 메인 콘텐츠 섹션 */}
            <main className="perf-main-body">
              <div className="perf-info-top-row">
                <div className="poster-box">
                  <img
                    src="https://ticketimage.interpark.com/Play/image/large/24/24005142_p.gif"
                    alt="Laufey"
                    className="poster-img"
                  />
                  <div className="poster-footer">
                    <span>♡ 티켓캐스트 567</span>
                  </div>
                </div>

                <div className="info-details-table">
                <div className="info-item">
                  <span className="label">장소</span>
                  <span className="value">킨텍스 제2전시장 9홀</span>
                </div>
                <div className="info-item">
                  <span className="label">공연기간</span>
                  <span className="value">2026.06.07</span>
                </div>
                <div className="info-item">
                  <span className="label">관람연령</span>
                  <span className="value">만 7세이상</span>
                </div>
                <div className="info-item price-item">
                  <span className="label">가격</span>
                  <div className="value">
                    <button className="btn-view-price">전체가격보기 ▾</button>
                    <ul className="price-stack">
                      <li><span className="grade">ORCHESTRA PIT</span> <span className="amt">314,000원</span></li>
                      <li><span className="grade">OPERA STALL</span> <span className="amt">338,000원</span></li>
                      <li><span className="grade">스탠딩석</span> <span className="amt">154,000원</span></li>
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
                    <span className="value">2026년 05월 18일 일괄 배송되는 상품입니다.</span>
                </div>
                <div className="info-item">
                    <span className="label">유의사항</span>
                    <span className="value benefit-link">2026년 00월 00일 00시 00분~2026년 00월 00일 23시 59분까지<br></br>무통장입금 결제가 불가능합니다.</span>
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
                      onClick={() => setCurrentMonth(prev => ({
                        ...prev,
                        month: prev.month === 1 ? 12 : prev.month - 1,
                        year: prev.month === 1 ? prev.year - 1 : prev.year
                      }))}
                    >
                      &lt;
                    </button>
                    <span className="cal-title">{currentMonth.year}. {String(currentMonth.month).padStart(2, '0')}</span>
                    <button
                      className="cal-nav-btn"
                      onClick={() => setCurrentMonth(prev => ({
                        ...prev,
                        month: prev.month === 12 ? 1 : prev.month + 1,
                        year: prev.month === 12 ? prev.year + 1 : prev.year
                      }))}
                    >
                      &gt;
                    </button>
                  </div>

                  <div className="cal-weekdays">
                    {weekDays.map(day => <div key={day} className="weekday">{day}</div>)}
                  </div>

                  <div className="cal-grid">
                    {calendarDays.map((day, index) => (
                      <div
                        key={index}
                        className={`cal-day ${!day ? 'empty' : ''} ${day === selectedDate ? 'selected' : ''} ${day && isCurrentMonth && day < today ? 'past' : ''}`}
                        onClick={() => day && setSelectedDate(day)}
                      >
                        {day}
                      </div>
                    ))}
                  </div>
                </div>

                <div className="card-title">회차</div>
                <div className="time-box selected">1회 18:00</div>
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