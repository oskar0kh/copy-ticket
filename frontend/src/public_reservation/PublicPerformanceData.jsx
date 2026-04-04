import React, { useMemo } from "react";
import PublicPerformanceDetails from "./PublicPerformanceDetails";
import "./css/PublicPerformanceData.css";
import { getNextRoundOpenTime } from "./roundTime";

function formatDateYmd(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function createPublicPosterDataUrl() {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="852" viewBox="0 0 640 852">
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#0f172a" />
          <stop offset="50%" stop-color="#1d4ed8" />
          <stop offset="100%" stop-color="#7c3aed" />
        </linearGradient>
        <radialGradient id="glow" cx="50%" cy="35%" r="60%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.24" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0" />
        </radialGradient>
      </defs>
      <rect width="640" height="852" fill="url(#bg)" />
      <circle cx="510" cy="155" r="165" fill="url(#glow)" />
      <circle cx="138" cy="675" r="210" fill="#22c55e" fill-opacity="0.14" />
      <circle cx="478" cy="592" r="172" fill="#f59e0b" fill-opacity="0.12" />
      <rect x="58" y="58" width="524" height="736" rx="28" fill="none" stroke="#ffffff" stroke-opacity="0.18" stroke-width="2" />
      <text x="72" y="166" fill="#e0f2fe" font-family="Arial, sans-serif" font-size="28" font-weight="700" letter-spacing="4">PUBLIC PRACTICE</text>
      <text x="72" y="266" fill="#ffffff" font-family="Arial, sans-serif" font-size="66" font-weight="800">LUNAR</text>
      <text x="72" y="336" fill="#ffffff" font-family="Arial, sans-serif" font-size="66" font-weight="800">FESTIVAL</text>
      <text x="72" y="404" fill="#dbeafe" font-family="Arial, sans-serif" font-size="24" font-weight="600">OPEN TICKETING TRAINING SESSION</text>
      <text x="72" y="630" fill="#ffffff" font-family="Arial, sans-serif" font-size="30" font-weight="700">MAIN STAGE</text>
      <text x="72" y="688" fill="#e0f2fe" font-family="Arial, sans-serif" font-size="24" font-weight="600">Seoul Dome, Mock Arena</text>
      <text x="72" y="744" fill="#ffffff" font-family="Arial, sans-serif" font-size="22" font-weight="600">Practice with a fictional performance lineup</text>
    </svg>
  `;

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

const publicPerformanceData = {
  goodsCode: "public-practice-2026",
  goodsName: "루나 페스티벌 2026",
  subGoodsName: "공개 티켓팅 연습용 가상 공연",
  placeName: "서울 돔 메인홀",
  playStartDate: formatDateYmd(new Date()),
  playDate: formatDateYmd(new Date()),
  runningTime: 150,
  viewRateName: "만 15세 이상",
  ticketCastCount: 4821,
  weekRank: 1,
  goodsLargeImageUrl: createPublicPosterDataUrl(),
};

export default function PublicPerformanceData({ user, onGoMain }) {
  const bookingOpenAt = useMemo(() => getNextRoundOpenTime(new Date()).getTime(), []);

  return (
    <div className="public-performance-page">
      <PublicPerformanceDetails
        user={user}
        performanceData={publicPerformanceData}
        onGoMain={onGoMain}
        bookingOpenAt={bookingOpenAt}
        showTimerButton={false}
      />
    </div>
  );
}
