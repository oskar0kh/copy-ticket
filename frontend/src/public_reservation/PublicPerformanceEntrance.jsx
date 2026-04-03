import React, { useEffect, useMemo, useState } from "react";
import { formatRemaining, getNextRoundOpenTime } from "./roundTime";
import "./css/PublicPerformanceEntrance.css";

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatDateTimeToMinute(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function PublicPerformanceEntrance({ loading, onBackToModeSelection, onEnterPublicPractice }) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timerId = window.setInterval(() => {
      setNow(new Date());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, []);

  const nextRoundOpenTime = useMemo(() => getNextRoundOpenTime(now), [now]);
  const remainingToNextRound = useMemo(() => (
    formatRemaining(nextRoundOpenTime.getTime() - now.getTime())
  ), [nextRoundOpenTime, now]);

  return (
    <section className="mode-card competition-card public-entrance-card">
      <div className="public-entrance-header">
        <div>
          <p className="mode-card-label">공개 경쟁</p>
          <h2>공개 라운드 참가 준비</h2>
          <p className="public-entrance-subtitle">
            매시 00분과 30분에 열리는 공개 라운드에 맞춰 접속 상태와 진입 시간을 확인하세요.
          </p>
        </div>
      </div>

      <div className="public-round-time-grid" aria-live="polite">
        <div className="public-round-time-card">
          <span className="public-round-time-label">현재 시각</span>
          <strong className="public-round-time-value">{formatDateTimeToMinute(now)}</strong>
        </div>
        <div className="public-round-time-card">
          <span className="public-round-time-label">다음 공개 라운드</span>
          <strong className="public-round-time-value">{formatDateTimeToMinute(nextRoundOpenTime)}</strong>
        </div>
        <div className="public-round-time-card highlight">
          <span className="public-round-time-label">남은 시간</span>
          <strong className="public-round-time-value countdown">{remainingToNextRound}</strong>
        </div>
      </div>

      <div className="public-entrance-guide-grid">
        <article className="public-entrance-guide-card">
          <h3>참가 전 확인</h3>
          <ul>
            <li>로그인 상태를 유지해 주세요.</li>
            <li>브라우저 탭은 한 개만 열어 두는 것을 권장합니다.</li>
            <li>새로고침보다는 준비 상태를 먼저 맞추는 편이 안정적입니다.</li>
            <li>라운드가 열리면 같은 공연과 좌석 풀을 공유하게 됩니다.</li>
          </ul>
        </article>
        <article className="public-entrance-guide-card">
          <h3>진행 방식</h3>
          <ol>
            <li>공개 라운드가 열리면 참여 버튼으로 입장합니다.</li>
            <li>동일 라운드 참가자와 같은 좌석 풀을 보게 됩니다.</li>
            <li>좌석 선택과 결제는 이후 단계에서 공통 흐름으로 연결됩니다.</li>
          </ol>
        </article>
      </div>

      <div className="mode-card-actions">
        <button type="button" className="ghost" onClick={onBackToModeSelection} disabled={loading}>
          모드 다시 선택
        </button>
        <button type="button" onClick={onEnterPublicPractice} disabled={loading}>
          연습 화면 입장
        </button>
      </div>
    </section>
  );
}
