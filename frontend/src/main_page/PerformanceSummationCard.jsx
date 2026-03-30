import React, { useState, useEffect } from 'react';
import { performanceApi } from '../api/performanceApi';
import './css/PerformanceSummationCard.css';

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

  // 예매 상세 페이지로 이동하고 동시에 DB 저장
  const handleBooking = async () => {
    if (!performance) return;

    try {
      // 공연 정보를 DB에 저장
      await performanceApi.savePerformance(performance);

      // 저장 완료 후 콜백 실행 (PerformanceDetails로 이동)
      if (onBookingSuccess) {
        onBookingSuccess(performance);
      }
    } catch (error) {
      // 저장 실패해도 PerformanceDetails로 이동하도록 (선택사항)
      console.error('Performance save error:', error);
      // 필요하면 사용자에게 에러 메시지 표시
      alert('공연 정보 저장에 실패했습니다: ' + error.message);
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
            {performance.goodsLargeImageUrl ? (
              <img
                src={performance.goodsLargeImageUrl}
                alt={performance.goodsName}
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
            <h1>{performance.goodsName || '제목 미정'}</h1>

            {/* Subtitle */}
            {performance.subGoodsName && (
              <p className="subtitle">{performance.subGoodsName}</p>
            )}

            {/* Place Info */}
            {performance.placeName && (
              <div className="performance-info-section">
                <h3>공연 장소</h3>
                <p className="place-text">{performance.placeName}</p>
              </div>
            )}

            {/* Play Date Info */}
            {performance.playStartDate && performance.playEndDate ? (
              <div className="performance-info-section">
                <h3>공연 날짜</h3>
                <div className="date-range">
                  <div className="date-item">
                    <span className="date-label">시작</span>
                    <span className="date-value">{performance.playStartDate}</span>
                  </div>
                  <span className="date-separator">–</span>
                  <div className="date-item">
                    <span className="date-label">종료</span>
                    <span className="date-value">{performance.playEndDate}</span>
                  </div>
                </div>
              </div>
            ) : null}

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
          </div>
        </div>
      )}
    </div>
  );
}
