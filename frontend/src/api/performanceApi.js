// src/api/performanceApi.js

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const performanceApi = {
  /**
   * Interpark 콘서트 URL을 파싱하고 공연 정보를 반환
   * URL을 파라미터로 받고, 백엔드 API로 POST 요청을 보내서 공연 정보를 받아옴
   *
   * @param {string} url - 인터파크 티켓 URL
   * @returns {Promise<Object>} PerformanceResponseDto 객체
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  parseInterParkUrl: async (url) => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/parse`, {
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

  /**
   * 파싱된 공연 정보를 DB에 저장
   * 사용자당 최대 5개까지만 저장 (5개 초과 시 가장 오래된 것을 soft delete)
   *
   * @param {Object} performanceData - 파싱된 공연 정보 객체
   * @returns {Promise<Object>} 저장된 공연 정보 (id, title, message)
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  savePerformance: async (performanceData) => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/save`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 세션 쿠키 포함해서 POST API 요청 전송 (인증 확인)
        body: JSON.stringify(performanceData),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '공연 정보 저장에 실패했습니다');
      }

      return await response.json(); // 성공 시 저장된 공연 정보 반환
    } catch (error) {
      console.error('Performance save error:', error);
      throw error;
    }
  },

  /**
   * 사용자의 저장된 공연 목록 조회
   *
   * @returns {Promise<Array>} 저장된 공연의 목록 (id, title, goodsCode 포함)
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  getPerformanceList: async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/list`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 세션 쿠키 포함해서 요청 전송 (인증 확인)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '공연 목록 조회에 실패했습니다');
      }

      return await response.json(); // 공연 목록 배열 반환
    } catch (error) {
      console.error('Performance list error:', error);
      throw error;
    }
  },

  /**
   * 특정 공연 정보 조회 (DB에서 저장된 정보)
   *
   * @param {number} performanceId - 공연 ID
   * @returns {Promise<Object>} 공연 상세 정보 (PerformanceResponseDto)
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  getPerformanceById: async (performanceId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/${performanceId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 세션 쿠키 포함해서 요청 전송 (인증 확인)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '공연 정보 조회에 실패했습니다');
      }

      return await response.json(); // 공연 정보 객체 반환
    } catch (error) {
      console.error('Performance detail error:', error);
      throw error;
    }
  },

  /**
   * 공연 삭제 (soft delete)
   *
   * @param {number} performanceId - 삭제할 공연 ID
   * @returns {Promise<Object>} 삭제 결과 ({ message })
   * @throws {Error} API 요청 실패 시 에러 발생
   */
  deletePerformance: async (performanceId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/performance/${performanceId}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 세션 쿠키 포함해서 요청 전송 (인증 확인)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '공연 삭제에 실패했습니다');
      }

      return await response.json(); // 삭제 결과 반환
    } catch (error) {
      console.error('Performance delete error:', error);
      throw error;
    }
  },
};


