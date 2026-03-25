import React, { useState, useEffect } from 'react';
import { performanceApi } from './api/performanceApi';
import './styles/PerformanceSummationCard.css';

export default function PerformanceDetails({ initialUrl, onBookingSuccess }) {
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

    const interparkUrlPattern = /^https?:\/\/tickets\.interpark\.com\/goods\/\d{8}$/;

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

  // 예매 상세 페이지로 이동 (DB 저장 없음)
  const handleBooking = async () => {
    if (!performance) return;

    // 공연 정보와 함께 콜백 실행 (PerformanceDetails로 이동)
    if (onBookingSuccess) {
      onBookingSuccess(performance);
    }
  };

  // 예매 시간 포맷팅
  const formatDateTime = (dateStr) => {
    if (!dateStr) return '정보 없음';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
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
            {performance.imageUrl ? (
              <img
                src={performance.imageUrl}
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

            {/* Venue, Price */}
            {performance.placeName && (
              <div className="performance-info-section">
                <h3>공연장</h3>
                <p>{performance.placeName}</p>
              </div>
            )}

            {/* Play Date Info */}
            {performance.playDate && (
              <div className="performance-info-section">
                <h3>공연 날짜</h3>
                <p>{performance.playDate}</p>
              </div>
            )}

            {/* Reservation Time */}
            {performance.startDate && (
              <div className="reservation-section">
                <span className="label">예매 시작</span>
                <span>{performance.startDate}</span>
              </div>
            )}

            {/* Button */}
            <button
              className="button-link"
              onClick={handleBooking}
              style={{
                cursor: 'pointer'
              }}
            >
              예매 연습하기
            </button>

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
