import React, { useState, useEffect } from "react";
import PerformanceSummationCard from "../private_reservation/PerformanceSummationCard";
import PerformanceDetails from "../private_reservation/PerformanceDetails";
import PublicPerformanceData from "../public_reservation/PublicPerformanceData";
import PublicPerformanceEntrance from "../public_reservation/PublicPerformanceEntrance";
import { performanceApi } from "../api/performanceApi";
import "../private_reservation/css/PerformanceSummationCard.css";

export default function MainPage({ user, loading, lastInputUrl, onSubmitUrl, onLogout, onWithdraw }) {
  const sharedDetailsStorageKey = 'mainPage:performanceDetails';
  const sharedViewStateStorageKey = 'mainPage:viewState';
  const detailsStorageKey = user?.id ? `mainPage:performanceDetails:${user.id}` : sharedDetailsStorageKey;
  const viewStateStorageKey = user?.id ? `mainPage:viewState:${user.id}` : sharedViewStateStorageKey;
  const readSavedViewState = () => {
    const raw = window.localStorage.getItem(viewStateStorageKey) || window.localStorage.getItem(sharedViewStateStorageKey);
    if (!raw) return null;

    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  };

  const [url, setUrl] = useState(() => {
    const savedViewState = readSavedViewState();
    return savedViewState?.url ?? lastInputUrl ?? "";
  });
  const [selectedMode, setSelectedMode] = useState(() => {
    const savedViewState = readSavedViewState();
    return savedViewState?.selectedMode ?? null;
  });
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [performanceToDelete, setPerformanceToDelete] = useState(null);
  const [showResults, setShowResults] = useState(() => {
    const savedViewState = readSavedViewState();
    return Boolean(savedViewState?.showResults);
  });
  const [performanceData, setPerformanceData] = useState(() => {
    const raw = window.localStorage.getItem(detailsStorageKey) || window.localStorage.getItem(sharedDetailsStorageKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  });
  const [showPerformanceDetails, setShowPerformanceDetails] = useState(() => {
    const savedViewState = readSavedViewState();
    return Boolean(
      savedViewState?.showPerformanceDetails
      || window.localStorage.getItem(detailsStorageKey)
      || window.localStorage.getItem(sharedDetailsStorageKey)
    );
  });
  const [showPublicPerformanceDetails, setShowPublicPerformanceDetails] = useState(() => {
    const savedViewState = readSavedViewState();
    return Boolean(savedViewState?.showPublicPerformanceDetails);
  });
  const [savedPerformances, setSavedPerformances] = useState([]);
  const [loadingSavedPerformances, setLoadingSavedPerformances] = useState(false);

  useEffect(() => {
    const currentView = window.history.state?.view;
    if (!currentView) {
      window.history.replaceState({ view: 'mode-selection' }, '');
    }
  }, []);

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
    window.history.pushState({ view: 'practice-results' }, '');
    onSubmitUrl(url);
    setShowResults(true); // handleSubmit이 호출되면, showResults를 true로 설정하여 PerformanceSummationCard 컴포넌트를 보여줌
  }

  function handleSelectMode(nextMode) {
    if (nextMode === 'competition') {
      window.history.pushState({ view: 'competition-entrance' }, '');
    } else if (nextMode === 'practice') {
      window.history.pushState({ view: 'practice-input' }, '');
    }

    setSelectedMode(nextMode);
    setShowResults(false);
    setShowPerformanceDetails(false);
    setShowPublicPerformanceDetails(false);
    setPerformanceData(null);
    window.localStorage.removeItem(detailsStorageKey);
    window.localStorage.removeItem(sharedDetailsStorageKey);
    window.localStorage.removeItem('reservationFlow');

    if (nextMode === 'practice') {
      setUrl(lastInputUrl || "");
    } else {
      setUrl("");
    }
  }

  function handleBackToModeSelection() {
    setSelectedMode(null);
    setShowResults(false);
    setShowPerformanceDetails(false);
    setShowPublicPerformanceDetails(false);
    setPerformanceData(null);
    window.localStorage.removeItem(detailsStorageKey);
    window.localStorage.removeItem(sharedDetailsStorageKey);
    window.localStorage.removeItem('reservationFlow');
    setUrl("");
  }

  function handleBackToInput() {
    setShowResults(false);
    setShowPerformanceDetails(false);
    setShowPublicPerformanceDetails(false);
    setPerformanceData(null);
    window.localStorage.removeItem(detailsStorageKey);
    window.localStorage.removeItem(sharedDetailsStorageKey);
    window.localStorage.removeItem('reservationFlow');
    setUrl("");
    // 저장된 공연 목록 다시 로드
    loadSavedPerformances();
  }

  function handleNavigateToPerformanceDetails(performance) {
    window.history.pushState({ view: 'performance-details' }, '');
    window.localStorage.setItem(detailsStorageKey, JSON.stringify(performance));
    window.localStorage.setItem(sharedDetailsStorageKey, JSON.stringify(performance));
    window.localStorage.removeItem('reservationFlow');
    setPerformanceData(performance);
    setShowResults(false);
    setShowPerformanceDetails(true);
    setShowPublicPerformanceDetails(false);
  }

  function handleNavigateToPublicPerformanceDetails() {
    window.history.pushState({ view: 'public-performance-details' }, '');
    setShowResults(false);
    setShowPerformanceDetails(false);
    setShowPublicPerformanceDetails(true);
  }

  function handleBackFromPublicPerformanceDetails() {
    setShowPublicPerformanceDetails(false);
    // 공개 티켓팅 종료 시 예매 시작 시간 정리
    window.localStorage.removeItem('publicBookingStartTime');
    // reservationFlow는 유지하여 새로고침 시 화면 유지 가능하도록 함
  }

  function handleNavigateBackFromPublicPerformanceDetails() {
    // 뒤로가기 시에만 reservationFlow 정리
    window.localStorage.removeItem('reservationFlow');
    window.localStorage.removeItem('publicBookingStartTime');
    window.history.back();
  }

  useEffect(() => {
    if (showPerformanceDetails && performanceData) {
      window.localStorage.setItem(detailsStorageKey, JSON.stringify(performanceData));
      window.localStorage.setItem(sharedDetailsStorageKey, JSON.stringify(performanceData));
      return;
    }
    window.localStorage.removeItem(detailsStorageKey);
    window.localStorage.removeItem(sharedDetailsStorageKey);
  }, [showPerformanceDetails, performanceData, detailsStorageKey]);

  useEffect(() => {
    const nextViewState = {
      selectedMode,
      showResults,
      showPublicPerformanceDetails,
      url,
    };

    const shouldClearViewState =
      !selectedMode
      && !showResults
      && !showPublicPerformanceDetails
      && !url;

    if (shouldClearViewState) {
      window.localStorage.removeItem(viewStateStorageKey);
      window.localStorage.removeItem(sharedViewStateStorageKey);
      return;
    }

    window.localStorage.setItem(viewStateStorageKey, JSON.stringify(nextViewState));
    window.localStorage.setItem(sharedViewStateStorageKey, JSON.stringify(nextViewState));
  }, [selectedMode, showResults, showPerformanceDetails, showPublicPerformanceDetails, url, viewStateStorageKey]);

  useEffect(() => {
    const handleBrowserBack = (event) => {
      const backView = event.state?.view;

      if (showPublicPerformanceDetails) {
        handleBackFromPublicPerformanceDetails();
        return;
      }

      if (showPerformanceDetails) {
        // SeatSelection에서 뒤로가기로 PerformanceDetails 상태로 복귀하는 경우는 메인으로 보내지 않음
        if (event.state?.view === 'performance-details') {
          return;
        }

        handleBackToInput();
        return;
      }

      if (showResults || selectedMode === 'competition' || selectedMode === 'practice') {
        if (
          backView === 'mode-selection'
          || backView === 'practice-input'
          || backView === 'competition-entrance'
          || backView == null
        ) {
          handleBackToModeSelection();
          return;
        }

        handleBackToModeSelection();
      }
    };

    window.addEventListener('popstate', handleBrowserBack);
    return () => window.removeEventListener('popstate', handleBrowserBack);
  }, [showPerformanceDetails, showPublicPerformanceDetails, showResults, selectedMode]);

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

  // 삭제 버튼 클릭 시 모달 열기
  const handleDeleteClick = (e, performance) => {
    e.stopPropagation(); // 상위 버튼 클릭 방지
    setPerformanceToDelete(performance);
    setIsDeleteModalOpen(true);
  };

  // 삭제 확인
  const handleConfirmDelete = async () => {
    if (!performanceToDelete) return;

    try {
      await performanceApi.deletePerformance(performanceToDelete.id);
      setIsDeleteModalOpen(false);
      setPerformanceToDelete(null);
      // 저장된 공연 목록 다시 로드
      loadSavedPerformances();
    } catch (error) {
      console.error('Failed to delete performance:', error);
      alert('공연 삭제에 실패했습니다.');
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
    <main className={`main-shell ${showPerformanceDetails || showPublicPerformanceDetails ? 'full-width' : ''}`}>
      {!showPerformanceDetails && !showPublicPerformanceDetails && (
      <header className="main-header">
        <div>
          <p className="tag">COPY TICKET</p>
          <h1>
            {showResults
              ? "공연 정보"
              : selectedMode === "practice"
                ? "개인 연습"
                : selectedMode === "competition"
                  ? "공개 경쟁"
                  : "모드 선택"
            }
          </h1>
          <p className="main-subtitle">
            {showResults
              ? "인터파크에서 가져온 공연 정보입니다."
              : selectedMode === "practice"
                ? "인터파크 URL을 입력해 개인 티켓팅 연습을 진행할 수 있습니다."
                : selectedMode === "competition"
                  ? "가상의 공연으로 다른 사용자들과 티켓팅 실력을 겨뤄보세요."
                  : "개인 연습과 공개 경쟁 중 원하는 모드를 선택해 주세요."
            }
          </p>
        </div>

        <div className="main-header-actions">
          {selectedMode && !showResults && !showPerformanceDetails && (
            <button className="ghost mode-back-button" onClick={handleBackToModeSelection} disabled={loading}>
              모드 다시 선택
            </button>
          )}
          <table className="account-table">
            <tbody>
              <tr>
                <th>계정</th>
                <td>{user ? `${user.name}` : "로그인 사용자"}</td>
              </tr>
              <tr>
                <th>로그아웃</th>
                <td>
                  <button className="logout" onClick={onLogout} disabled={loading}>
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
          <PerformanceDetails
            user={user}
            onLogout={onLogout}
            performanceData={performanceData}
            onGoMain={handleBackToInput}
          />
          <div style={{ textAlign: "center", marginTop: "40px", marginBottom: "40px" }}>
            <button onClick={handleBackToInput} className="ghost" style={{ padding: "10px 20px" }}>
              ← URL 입력 화면으로 돌아가기
            </button>
          </div>
        </div>
      ) : showPublicPerformanceDetails ? (
        <div>
          <PublicPerformanceData
            user={user}
            onGoMain={handleNavigateBackFromPublicPerformanceDetails}
          />
          <div style={{ textAlign: "center", marginTop: "40px", marginBottom: "40px" }}>
            <button onClick={handleNavigateBackFromPublicPerformanceDetails} className="ghost" style={{ padding: "10px 20px" }}>
              ← 참가 준비 화면으로 돌아가기
            </button>
          </div>
        </div>
      ) : selectedMode === "competition" ? (
        <PublicPerformanceEntrance
          loading={loading}
          onBackToModeSelection={handleBackToModeSelection}
          onEnterPublicPractice={handleNavigateToPublicPerformanceDetails}
        />
      ) : showResults ? (
        <div>
          <PerformanceSummationCard initialUrl={url} onBookingSuccess={handleNavigateToPerformanceDetails} /> {/* 입력한 URL을 PerformanceSummationCard.jsx로 전달 및 예매 성공 콜백 */}
          <div style={{ textAlign: "center", marginTop: "40px", marginBottom: "40px" }}>
            <button onClick={handleBackToInput} className="ghost" style={{ padding: "10px 20px" }}>
              ← 다른 URL 입력하기
            </button>
          </div>
        </div>
      ) : selectedMode === "practice" ? (
        <section className="url-card">
          <h2>인터파크 콘서트 URL 입력</h2>
          <p>
            인터파크 티켓의 콘서트 상세 페이지 URL을 입력하면 공연 정보(공연 제목, 오픈 시각 등)를 자동으로 가져옵니다.
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
              <strong>📚 저장된 URL 목록</strong>
              <p className="saved-performances-notice">공연 URL은 최대 5개까지 저장할 수 있습니다.</p>
              <ul className="saved-performances-list">
                {savedPerformances.map((perf) => (
                  <li key={perf.id} className="saved-perf-item">
                    <button
                      type="button"
                      className="saved-perf-button"
                      onClick={() => handleSavedPerformanceClick(perf.id)}
                      disabled={loadingSavedPerformances}
                    >
                      {perf.goodsName}
                    </button>
                    <button
                      type="button"
                      className="saved-perf-delete-btn"
                      onClick={(e) => handleDeleteClick(e, perf)}
                      disabled={loadingSavedPerformances}
                      title="삭제"
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      ) : (
        <section className="mode-selection-card">
          <div className="mode-selection-grid">
            <button type="button" className="mode-option" data-mode="practice" onClick={() => handleSelectMode("practice")} disabled={loading}>
              <span className="mode-option-label">개인 연습</span>
              <strong>인터파크 URL 입력하기</strong>
              <span className="mode-option-description">연습하고 싶으신 인터파크 공연의 URL을 입력하시면, 실제 티켓팅 화면 및 흐름과 유사하게 연습하실 수 있습니다.</span>
            </button>
            <button type="button" className="mode-option" data-mode="competition" onClick={() => handleSelectMode("competition")} disabled={loading}>
              <span className="mode-option-label">공개 경쟁</span>
              <strong>여러 사용자들과 경쟁하기</strong>
              <span className="mode-option-description">매시 00분/30분 기준, 공개 티켓팅 페이지가 열립니다.<br></br>실제 사용자들과 경쟁해서 자신의 티켓팅 실력을 확인해보세요!</span>
            </button>
          </div>
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

      {isDeleteModalOpen && performanceToDelete && (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="delete-modal-title">
          <div className="modal-card">
            <h3 id="delete-modal-title">정말로 삭제하시겠습니까?</h3>
            <p>"{performanceToDelete.goodsName}" 공연이 삭제됩니다.</p>
            <div className="modal-actions">
              <button onClick={handleConfirmDelete} disabled={loadingSavedPerformances}>
                확인
              </button>
              <button
                className="ghost"
                onClick={() => {
                  setIsDeleteModalOpen(false);
                  setPerformanceToDelete(null);
                }}
                disabled={loadingSavedPerformances}
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