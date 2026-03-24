// src/api/performanceApi.js

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const performanceApi = {
  /**
   * Interpark 콘서트 URL을 파싱하고 공연 정보를 반환
   * URL을 파라미터로 받고, 백엔드 API로 POST 요청을 보내서 공연 정보를 받아옴
   * 
   * @param {string} url - 인터파크 티켓 URL
   * @returns {Promise<Object>} 공연 정보 객체
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  parseInterParkUrl: async (url) => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/parse`, { // 백엔드 API 엔드포인트에 맞게 수정
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 세션 쿠키 포함해서 POST API 요청 전송
        body: JSON.stringify({ url }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '페이지 파싱에 실패했습니다');
      }

      return await response.json(); // 성공 시 공연 정보 객체 반환
    } catch (error) {
      console.error('Performance parsing error:', error);
      throw error;
    }
  },
};
