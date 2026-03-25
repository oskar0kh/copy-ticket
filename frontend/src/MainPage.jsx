import React, { useState, useEffect } from "react";
import PerformanceSummationCard from "./PerformanceSummationCard";
import PerformanceDetails from "./PerformanceDetails";
import { performanceApi } from "./api/performanceApi";

export default function MainPage({ user, loading, lastInputUrl, onSubmitUrl, onLogout, onWithdraw }) {
  const [url, setUrl] = useState(lastInputUrl || "");
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [showPerformanceDetails, setShowPerformanceDetails] = useState(false);
  const [performanceData, setPerformanceData] = useState(null);
  const [savedPerformances, setSavedPerformances] = useState([]);
  const [loadingSavedPerformances, setLoadingSavedPerformances] = useState(false);

  // 저장된 공연 목록 조회
  useEffect(() => {
    loadSavedPerformances();
  }, []);

  const loadSavedPerformances = async () => {
    setLoadingSavedPerformances(true);
    try {
      const list = await performanceApi.getPerformanceList();
      setSavedPerformances(list || []);
    } catch (error) {
      console.error('Failed to load saved performances:', error);
      setSavedPerformances([]);
    } finally {
      setLoadingSavedPerformances(false);
    }
  };

  function handleSubmit(event) {
    event.preventDefault();
    onSubmitUrl(url);
    setShowResults(true); // handleSubmit이 호출되면, showResults를 true로 설정하여 PerformanceSummationCard 컴포넌트를 보여줌
  }

  function handleBackToInput() {
    setShowResults(false);
    setShowPerformanceDetails(false);
    setPerformanceData(null);
    setUrl("");
    // 저장된 공연 목록 다시 로드
    loadSavedPerformances();
  }

  function handleNavigateToPerformanceDetails(performance) {
    setPerformanceData(performance);
    setShowResults(false);
    setShowPerformanceDetails(true);
  }

  // 저장된 공연 제목 클릭 시 상세 정보 로드
  const handleSavedPerformanceClick = async (performanceId) => {
    try {
      const performance = await performanceApi.getPerformanceById(performanceId);
      handleNavigateToPerformanceDetails(performance);
    } catch (error) {
      console.error('Failed to load performance details:', error);
      alert('공연 정보를 불러올 수 없습니다.');
    }
  };

  async function handleConfirmWithdraw() {
    try {
      await onWithdraw();
      setIsWithdrawModalOpen(false);
    } catch (error) {
      // 에러 알림은 상위 컴포넌트 토스트에서 처리
    }
  }

  return (
    <main className={`main-shell ${showPerformanceDetails ? 'full-width' : ''}`}>
      {!showPerformanceDetails && (
      <header className="main-header">
        <div>
          <p className="tag">COPY TICKET</p>
          <h1>{showResults ? "공연 정보" : "URL 입력 메인 화면"}</h1>
          <p className="main-subtitle">
            {showResults
              ? "인터파크에서 가져온 공연 정보입니다."
              : "아래에서 인터파크 콘서트 URL을 입력해 주세요."
            }
          </p>
        </div>

        <div className="main-header-actions">
          <table className="account-table">
            <tbody>
              <tr>
                <th>계정</th>
                <td>{user ? `${user.name}` : "로그인 사용자"}</td>
              </tr>
              <tr>
                <th>로그아웃</th>
                <td>
                  <button className="ghost" onClick={onLogout} disabled={loading}>
                    로그아웃
                  </button>
                </td>
              </tr>
              <tr>
                <th>회원 탈퇴</th>
                <td>
                  <button
                    className="danger"
                    onClick={() => setIsWithdrawModalOpen(true)}
                    disabled={loading}
                  >
                    회원 탈퇴
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </header>
      )}

      {/* showResults가 true일 때, PerformanceSummationCard 컴포넌트를 표시 */}
      {showPerformanceDetails ? (
        <div>
          <PerformanceDetails user={user} onLogout={onLogout} performanceData={performanceData} />
          <div style={{ textAlign: "center", marginTop: "40px", marginBottom: "40px" }}>
            <button onClick={handleBackToInput} className="ghost" style={{ padding: "10px 20px" }}>
              ← 다른 URL 입력하기
            </button>
          </div>
        </div>
      ) : showResults ? (
        <div>
          <PerformanceSummationCard initialUrl={url} onBookingSuccess={handleNavigateToPerformanceDetails} /> {/* 입력한 URL을 PerformanceSummationCard.jsx로 전달 및 예매 성공 콜백 */}
          <div style={{ textAlign: "center", marginTop: "40px", marginBottom: "40px" }}>
            <button onClick={handleBackToInput} className="ghost" style={{ padding: "10px 20px" }}>
              ← 다른 URL 입력하기
            </button>
          </div>
        </div>
      ) : (
        <section className="url-card">
          <h2>인터파크 콘서트 URL 입력</h2>
          <p>
            인터파크 티켓의 콘서트 상세 페이지 URL을 입력하면 공연 정보를 자동으로 가져옵니다.
          </p>

          <form className="url-form" onSubmit={handleSubmit}> {/* '정보 가져오기' 폼 */}
            <label>
              URL
              <input
                type="url"
                placeholder="예시: https://tickets.interpark.com/goods/00000000"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                required
              />
            </label>

            <button type="submit" disabled={loading}> {/* 버튼 클릭하면, handleSubmit 함수가 호출됨 */}
              {loading ? "처리 중..." : "정보 가져오기"}
            </button>
          </form>

          {/* 저장된 공연 목록 섹션 */}
          {savedPerformances.length > 0 && (
            <div className="saved-performances">
              <strong>📚 저장된 공연 목록</strong>
              <ul className="saved-performances-list">
                {savedPerformances.map((perf) => (
                  <li key={perf.id} className="saved-perf-item">
                    <button
                      type="button"
                      className="saved-perf-button"
                      onClick={() => handleSavedPerformanceClick(perf.id)}
                      disabled={loadingSavedPerformances}
                    >
                      {perf.title}
                    </button>
                    <div className="saved-perf-arrow">→</div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}

      {isWithdrawModalOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="withdraw-modal-title">
          <div className="modal-card">
            <h3 id="withdraw-modal-title">정말로 탈퇴하시겠습니까?</h3>
            <p>탈퇴하면 계정 정보는 복구할 수 없습니다.</p>
            <div className="modal-actions">
              <button onClick={handleConfirmWithdraw} disabled={loading}>
                확인
              </button>
              <button
                className="ghost"
                onClick={() => setIsWithdrawModalOpen(false)}
                disabled={loading}
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}