import React, { useState } from "react";

export default function MainPage({ user, loading, lastInputUrl, onSubmitUrl, onLogout, onWithdraw }) {
  const [url, setUrl] = useState(lastInputUrl || "");
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);

  function handleSubmit(event) {
    event.preventDefault();
    onSubmitUrl(url);
  }

  async function handleConfirmWithdraw() {
    try {
      await onWithdraw();
      setIsWithdrawModalOpen(false);
    } catch (error) {
      // 에러 알림은 상위 컴포넌트 토스트에서 처리
    }
  }

  return (
    <main className="main-shell">
      <header className="main-header">
        <div>
          <p className="tag">COPY TICKET</p>
          <h1>URL 입력 메인 화면</h1>
          <p className="main-subtitle">
            아래에서 예매 URL을 입력해 주세요.
          </p>
        </div>

        <div className="main-header-actions">
          <table className="account-table">
            <tbody>
              <tr>
                <th>계정</th>
                <td>{user ? `${user.name} (${user.id})` : "로그인 사용자"}</td>
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

      <section className="url-card">
        <h2>예매 URL 입력</h2>
        <p>
          URL 저장 API는 아직 연결하지 않았습니다. 지금은 입력 UI와 화면 흐름만
          구현된 상태입니다.
        </p>

        <form className="url-form" onSubmit={handleSubmit}>
          <label>
            URL
            <input
              type="url"
              placeholder="https://example.com/ticket"
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              required
            />
          </label>

          <button type="submit" disabled={loading}>
            {loading ? "처리 중..." : "URL 입력 확인"}
          </button>
        </form>

        {lastInputUrl && (
          <div className="url-preview">
            <strong>마지막 입력 URL</strong>
            <p>{lastInputUrl}</p>
          </div>
        )}
      </section>

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