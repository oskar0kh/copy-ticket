import React, { useEffect, useMemo, useState } from "react";
import { formatRemaining, getNextRoundOpenTime } from "./roundTime";
import "./css/PublicPerformanceEntrance.css";

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatDateTimeToMinute(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function PublicPerformanceEntrance({ loading, onBackToModeSelection, onEnterPublicPractice }) {
  const [now, setNow] = useState(() => new Date());
  const [serverTimeOffsetMs, setServerTimeOffsetMs] = useState(0);

  useEffect(() => {
    const timerId = window.setInterval(() => {
      setNow(new Date());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    const syncServerTime = async () => {
      try {
        const response = await fetch('/api/public-round/sync');
        if (!response.ok) {
          return;
        }

        const payload = await response.json();
        if (!isMounted) return;

        if (payload?.serverNow) {
          const offset = new Date(payload.serverNow).getTime() - Date.now();
          setServerTimeOffsetMs(offset);
        }
      } catch (error) {
        console.error('공개 라운드 입장 시간 동기화 실패:', error);
      }
    };

    syncServerTime();

    return () => {
      isMounted = false;
    };
  }, []);

  const adjustedNow = useMemo(
    () => new Date(now.getTime() + Number(serverTimeOffsetMs || 0)),
    [now, serverTimeOffsetMs]
  );
  const nextRoundOpenTime = useMemo(() => getNextRoundOpenTime(adjustedNow), [adjustedNow]);
  const remainingToNextRound = useMemo(() => (
    formatRemaining(nextRoundOpenTime.getTime() - adjustedNow.getTime())
  ), [nextRoundOpenTime, adjustedNow]);

  return (
    <section className="mode-card competition-card public-entrance-card">
      <div className="public-entrance-header">
        <div>
          <p className="mode-card-label">공개 경쟁</p>
          <h2>공개 라운드 참가 준비</h2>
          <p className="public-entrance-subtitle">
            매시 00분과 30분에 열리는 공개 라운드에서 자신의 티켓팅 실력을 확인하세요!
          </p>
        </div>
      </div>

      <div className="public-round-time-grid" aria-live="polite">
        <div className="public-round-time-card">
          <span className="public-round-time-label">현재 시각</span>
          <strong className="public-round-time-value">{formatDateTimeToMinute(adjustedNow)}</strong>
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
            <li>라운드가 열리면 다른 사용자들과 같은 공연과 좌석 풀을 공유합니다.</li>
            <li>라운드는 10분 동안 진행되며, 10분이 지나면 라운드가 종료되고 '예매하기' 버튼도 비활성화 됩니다. 이후 20분 뒤에 새로운 라운드가 시작됩니다.</li>
          </ul>
        </article>
        <article className="public-entrance-guide-card">
          <h3>진행 방식</h3>
          <ol>
            <li>왼쪽 하단의 '연습 화면 입장' 버튼을 누르면 준비 화면으로 이동합니다.</li>
            <li>공개 라운드가 열리기 전에는 '예매하기' 버튼이 비활성화 됩니다.</li>
            <li>공개 라운드가 열리면 '예매하기' 버튼으로 입장합니다.</li>
            <li>동일 라운드 참가자와 같은 좌석 풀을 보게 됩니다.</li>
            <li>좌석은 최대 4개까지 선택할 수 있으며, 선택 후 '선택 완료' 버튼을 누르시면 예매가 완료됩니다.</li>
            <li>단, 다른 사람이 이미 선택한 좌석을 고르셨을 경우 예매가 불가능합니다.</li>
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
