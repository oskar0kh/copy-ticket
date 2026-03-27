import React, { useState } from 'react';
import '../styles/CaptchaModal.css';

const CaptchaModal = ({ onComplete, captchaImage }) => {
  const [inputValue, setInputValue] = useState('');

  const handleInputChange = (e) => {
    setInputValue(e.target.value.toUpperCase());
  };

  const handleCompleteClick = () => {
    if (inputValue.trim().length > 0) {
      onComplete(inputValue);
      setInputValue('');
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleCompleteClick();
    }
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
              <button className="btn-sound">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                  <path d="M15.54 8.46a6 6 0 0 1 0 8.07"></path>
                </svg>
              </button>
            </div>

            <div className="captcha-image">
              {captchaImage ? (
                <img src={captchaImage} alt="captcha" />
              ) : (
                <div className="captcha-placeholder">NBLKAE</div>
              )}
            </div>

            <button className="btn-refresh">
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
