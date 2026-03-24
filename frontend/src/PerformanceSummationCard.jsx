import React, { useState, useEffect } from 'react';
import { performanceApi } from './api/performanceApi';
import './styles/Performance.css';

export default function PerformanceDetails({ initialUrl }) {
  const [url, setUrl] = useState(initialUrl || '');
  const [performance, setPerformance] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (initialUrl) {
      parseUrl(initialUrl);
    }
  }, [initialUrl]);

  const parseUrl = async (urlToparse) => {
    if (!urlToparse.trim()) {
      setError('URL을 입력해주세요');
      return;
    }

    const interparkUrlPattern = /^https?:\/\/tickets\.interpark\.com\/goods\/\d+/;

    if (!interparkUrlPattern.test(urlToparse)) {
      setError('유효한 인터파크 티켓 URL을 입력해주세요');
      return;
    }

    setLoading(true);
    setError(null);
    setPerformance(null);

    try {
      const result = await performanceApi.parseInterParkUrl(urlToparse);
      setPerformance(result);
    } catch (err) {
      setError(err.message || '페이지 파싱에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const handleParse = async (e) => {
    e.preventDefault();
    await parseUrl(url);
  };

  // 예매 오픈 시간 포맷팅
  const formatReservationTime = (dateStr) => {
    if (!dateStr) return '정보 없음';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }).replace(/\./g, '-').replace(' ', ' ');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="performance-container">
      <div className="performance-input-section">
        <h2>공연 정보 가져오기</h2>
        <form onSubmit={handleParse}>
          <div className="form-group">
            <label htmlFor="url">URL</label>
            <input
              id="url"
              type="text"
              placeholder="https://tickets.interpark.com/goods/00000000"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={loading}
            />
            <button type="submit" disabled={loading}>
              {loading ? '분석 중...' : '가져오기'}
            </button>
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
        </form>
      </div>

      {/* Performance Details - Interpark Style */}
      {performance && (
        <div className="performance-details">
          {/* Left: Poster */}
          <div className="performance-poster">
            {performance.posterImageUrl ? (
              <img
                src={performance.posterImageUrl}
                alt={performance.title}
                onError={(e) => {
                  e.target.src = 'https://via.placeholder.com/320x426?text=Poster';
                }}
              />
            ) : (
              <div className="poster-placeholder">
                포스터 이미지 없음
              </div>
            )}
          </div>

          {/* Right: Info */}
          <div className="performance-info">
            {/* Title */}
            <h1>{performance.title || '제목 미정'}</h1>

            {/* Description */}
            {performance.description && (
              <div className="description">
                <h3>공연 설명</h3>
                <p>{performance.description}</p>
              </div>
            )}

            {/* Venue, Price */}
            <div className="performance-info-section">
              <h3>기본 정보</h3>
              {performance.venue && (
                <div className="info-item">
                  <span className="label">공연장</span>
                  <span>{performance.venue}</span>
                </div>
              )}
              {performance.priceRange && (
                <div className="info-item">
                  <span className="label">가격</span>
                  <span>{performance.priceRange}</span>
                </div>
              )}
            </div>

            {/* Schedules */}
            {performance.schedules && performance.schedules.length > 0 && (
              <div className="performance-info-section">
                <h3>공연 일정</h3>
                <ul className="schedules">
                  {performance.schedules.map((schedule, idx) => (
                    <li key={idx}>
                      {schedule.startDateTime}
                      {schedule.runtimeMinutes && ` (${schedule.runtimeMinutes}분)`}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Reservation Open Time */}
            {performance.reservationOpenAt && (
              <div className="reservation-section">
                <span className="label">예매 오픈</span>
                <span>{formatReservationTime(performance.reservationOpenAt)}</span>
              </div>
            )}

            {/* Button */}
            {performance.reservationUrl ? (
              <a
                href={performance.reservationUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="button-link"
              >
                예매하기 →
              </a>
            ) : (
              <button className="button-link" disabled style={{ cursor: 'not-allowed', opacity: 0.6 }}>
                예매 불가
              </button>
            )}

            {/* Metadata */}
            <div className="metadata">
              파싱됨: {new Date(performance.parsedAt).toLocaleString('ko-KR')}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
