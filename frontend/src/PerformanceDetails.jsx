import React, { useState, useEffect } from 'react';
import { performanceApi } from './api/performanceApi';
import './styles/Performance.css';

export default function PerformanceDetails({ initialUrl }) {
  const [url, setUrl] = useState(initialUrl || '');
  const [performance, setPerformance] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // MainPage에서 initialUrl이 전달되면, parseUrl 메서드를 호출 -> 공연 정보 가져오기
  useEffect(() => {
    if (initialUrl) {
      parseUrl(initialUrl);
    }
  }, [initialUrl]);

  /**
   * URL 파싱 메서드: React의 performanceApi 사용, URL을 백엔드 API로 보내고 공연 정보를 받아옴
  */
  const parseUrl = async (urlToparse) => {
    if (!urlToparse.trim()) { // URL이 비어있으면 에러 메시지 설정
      setError('URL을 입력해주세요');
      return;
    }

    // URL이 인터파크 티켓 URL 형식이 맞는지 확인
    const interparkUrlPattern = /^https?:\/\/tickets\.interpark\.com\/goods\/\d+/;

    if (!interparkUrlPattern.test(urlToparse)) {
      setError('유효한 인터파크 티켓 URL을 입력해주세요');
      return;
    }

    // 1. 파싱 시작: 로딩 상태 설정, 에러 초기화, 공연 정보 초기화
    setLoading(true);     // 로딩 시작 (버튼 비활성화 및 로딩 메시지 표시)
    setError(null);       // 이전 에러 초기화
    setPerformance(null); // 이전 공연 정보 초기화

    // 2. React의 performanceApi.js의 parseInterParkUrl 메서드 호출 
    //    : 백엔드에 POST API 요청 -> 백엔드에서 URL 파싱, 파싱 후의 공연 정보 가져오기
    try {
      const result = await performanceApi.parseInterParkUrl(urlToparse);
      setPerformance(result);
    } catch (err) {
      setError(err.message || '페이지 파싱에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  // 폼 제출 핸들러: URL을 파싱하는 메서드 호출
  const handleParse = async (e) => {
    e.preventDefault();
    await parseUrl(url);
  };

  return (
    <div className="performance-container">
      <div className="performance-input-section">
        <h2>공연 정보 가져오기</h2>
        <form onSubmit={handleParse}> {/* 폼(URL 입력 후 버튼 누르는거) 제출 시 handleParse 함수 호출 */}
          <div className="form-group">
            <label htmlFor="url">인터파크 티켓 URL</label>
            <input
              id="url"
              type="text"
              placeholder="예시: https://tickets.interpark.com/goods/00000000"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={loading}
            />
          </div>
          <button type="submit" disabled={loading}>
            {loading ? '분석 중...' : '정보 가져오기'}
          </button>
        </form>

        {error && (
          <div className="error-message">
            ❌ {error}
          </div>
        )}
      </div>

      {/* 공연 정보가 있을 때만 공연 정보 섹션을 보여줌 */}
      {performance && (
        <div className="performance-details">
          <div className="performance-poster">
            {performance.posterImageUrl ? (
              <img
                src={performance.posterImageUrl}
                alt={performance.title}
                onError={(e) => {
                  e.target.src = 'https://via.placeholder.com/300x400?text=Poster';
                }}
              />
            ) : (
              <div className="poster-placeholder">
                포스터 이미지 없음
              </div>
            )}
          </div>

          <div className="performance-info">
            <h1>{performance.title || '제목 미정'}</h1>

            {performance.description && (
              <div className="description">
                <h3>공연 설명</h3>
                <p>{performance.description}</p>
              </div>
            )}

            {performance.venue && (
              <div className="info-item">
                <span className="label">장소:</span>
                <span>{performance.venue}</span>
              </div>
            )}

            {performance.priceRange && (
              <div className="info-item">
                <span className="label">가격:</span>
                <span>{performance.priceRange}</span>
              </div>
            )}

            {performance.schedules && performance.schedules.length > 0 && (
              <div className="info-item">
                <span className="label">공연 일정:</span>
                <ul className="schedules">
                  {performance.schedules.map((schedule, idx) => (
                    <li key={idx}>
                      {schedule.startDateTime}
                      {schedule.runtimeMinutes && ` (${schedule.runtimeMinutes})`}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {performance.reservationOpenAt && (
              <div className="info-item">
                <span className="label">예매 오픈:</span>
                <span>{new Date(performance.reservationOpenAt).toLocaleString('ko-KR')}</span>
              </div>
            )}

            {performance.reservationUrl && (
              <a href={performance.reservationUrl} target="_blank" rel="noopener noreferrer" className="button-link">
                예매하기 →
              </a>
            )}

            <div className="metadata">
              <small>파싱됨: {new Date(performance.parsedAt).toLocaleString('ko-KR')}</small>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
