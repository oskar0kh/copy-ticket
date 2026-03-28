import React, { useState, useEffect } from 'react';
import '../styles/CaptchaModal.css';

const CaptchaModal = ({ onComplete, captchaImage }) => {
  const [captchaText, setCaptchaText] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // 랜덤 6자리 알파벳 대문자 생성
  const generateRandomCaptcha = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let result = '';
    for (let i = 0; i < 6; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  };

  // 컴포넌트 마운트 시 초기 CAPTCHA 생성
  useEffect(() => {
    setCaptchaText(generateRandomCaptcha());
  }, []);

  const handleInputChange = (e) => {
    setInputValue(e.target.value.toUpperCase());
    setErrorMessage('');
  };

  const handleCompleteClick = () => {
    if (inputValue.trim().length === 0) {
      return;
    }

    // 입력값과 CAPTCHA 텍스트 비교
    if (inputValue.trim() !== captchaText.trim()) {
      setErrorMessage('문자가 일치하지 않습니다. 다시 입력해주세요');
      setInputValue('');
      return;
    }

    // 일치하면 완료
    onComplete(inputValue);
    setInputValue('');
    setErrorMessage('');
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleCompleteClick();
    }
  };

  const handleRefresh = () => {
    setCaptchaText(generateRandomCaptcha());
    setInputValue('');
    setErrorMessage('');
  };

  return (
    <div className="captcha-modal-overlay">
      <div className="captcha-modal">
        <div className="modal-header">
          <h2>화면의 문자를 입력해주세요</h2>
          <p>문자 입력 인증 후 좌석을 선택할 수 있습니다</p>
        </div>

        <div className="modal-content">
          <div className="captcha-image-box">
            <div className="captcha-controls">
              <button className="btn-sound" title="음성 듣기">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                  <path d="M15.54 8.46a6.5 6.5 0 0 1 0 9.07"></path>
                  <path d="M19.07 4.93a10 10 0 0 1 0 14.14"></path>
                </svg>
              </button>
            </div>

            <div className="captcha-image">
              {captchaImage ? (
                <img src={captchaImage} alt="captcha" />
              ) : (
                <div className="captcha-placeholder">{captchaText}</div>
              )}
            </div>

            <button className="btn-refresh" onClick={handleRefresh}>
              <span>⟳</span>
            </button>
          </div>
        </div>

        <div className="modal-input">
          <input
            type="text"
            placeholder="화면의 문자를 입력해주세요 (대소문자 구분 없음)"
            value={inputValue}
            onChange={handleInputChange}
            onKeyPress={handleKeyPress}
            maxLength="10"
          />
          {errorMessage && (
            <div className="error-message">{errorMessage}</div>
          )}
        </div>

        <button
          className="btn-complete"
          onClick={handleCompleteClick}
          disabled={inputValue.trim().length === 0}
        >
          입력완료
        </button>
      </div>
    </div>
  );
};

export default CaptchaModal;
