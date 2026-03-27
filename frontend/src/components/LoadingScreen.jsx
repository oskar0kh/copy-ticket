import React from 'react';
import '../styles/LoadingScreen.css';

const LoadingScreen = () => {
  return (
    <div className="loading-overlay">
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p className="loading-text">예매 정원을 불러오는 중입니다.</p>
        <p className="loading-subtext">조금 기다려주세요.</p>
      </div>
    </div>
  );
};

export default LoadingScreen;
