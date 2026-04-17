import React, { useEffect, useMemo, useRef, useState } from 'react';
import './css/LoadingScreen.css';

const POLL_INTERVAL_MS = 1000;

const PublicLoadingScreen = ({
  roundId,
  performanceName = '공연',
  queueStorageKey,
  onReady,
  onCancel,
}) => {
  const [queueInfo, setQueueInfo] = useState({
    state: 'JOINING',
    position: null,
    peopleAhead: null,
    sessionToken: null,
    tokenExpiresAt: null,
  });
  const [statusMessage, setStatusMessage] = useState('대기열에 진입하는 중입니다.');
  const [errorMessage, setErrorMessage] = useState('');
  const onReadyRef = useRef(onReady);
  const onCancelRef = useRef(onCancel);

  useEffect(() => {
    onReadyRef.current = onReady;
  }, [onReady]);

  useEffect(() => {
    onCancelRef.current = onCancel;
  }, [onCancel]);

  const statusLabel = useMemo(() => {
    switch (queueInfo.state) {
      case 'WAITING':
        return '대기 중';
      case 'READY':
        return '입장 가능';
      case 'CLOSED':
        return '라운드 종료';
      case 'NOT_IN_QUEUE':
        return '대기열 없음';
      default:
        return '연결 중';
    }
  }, [queueInfo.state]);

  useEffect(() => {
    if (!roundId) {
      setErrorMessage('대기열 정보를 찾을 수 없습니다. 잠시 후 다시 시도해주세요.');
      return undefined;
    }

    let isMounted = true;
    let pollTimerId = null;
    let readyHandled = false;

    const readStoredQueueContext = () => {
      if (!queueStorageKey) {
        return null;
      }

      const saved = window.localStorage.getItem(queueStorageKey);
      if (!saved) {
        return null;
      }

      try {
        const parsed = JSON.parse(saved);
        if (Number(parsed?.roundId) !== Number(roundId)) {
          return null;
        }

        const expiresAt = typeof parsed?.tokenExpiresAt === 'number'
          ? parsed.tokenExpiresAt
          : Date.parse(parsed?.tokenExpiresAt || '');
        if (parsed?.state !== 'READY' || !parsed?.sessionToken || !expiresAt || Number.isNaN(expiresAt)) {
          return null;
        }

        if (Date.now() >= expiresAt) {
          window.localStorage.removeItem(queueStorageKey);
          return null;
        }

        return parsed;
      } catch {
        return null;
      }
    };

    const persistQueueContext = (payload) => {
      if (!queueStorageKey || !payload) {
        return;
      }

      const parsedExpiresAt = typeof payload?.tokenExpiresAt === 'number'
        ? payload.tokenExpiresAt
        : Date.parse(payload?.tokenExpiresAt || '');
      const queueContext = {
        roundId: payload?.roundId ?? roundId,
        state: payload?.state || 'JOINING',
        position: payload?.position ?? null,
        peopleAhead: payload?.peopleAhead ?? null,
        sessionToken: payload?.sessionToken || null,
        tokenExpiresAt: parsedExpiresAt,
        updatedAt: Date.now(),
      };

      window.localStorage.setItem(queueStorageKey, JSON.stringify(queueContext));
    };

    const clearQueueContext = () => {
      if (queueStorageKey) {
        window.localStorage.removeItem(queueStorageKey);
      }
    };

    const parseResponse = async (response) => {
      const contentType = response.headers.get('content-type') || '';
      if (!contentType.includes('application/json')) {
        return null;
      }

      return response.json();
    };

    const applyQueuePayload = (payload) => {
      if (!payload) {
        return false;
      }

      setQueueInfo({
        state: payload.state || 'JOINING',
        position: payload.position ?? null,
        peopleAhead: payload.peopleAhead ?? null,
        sessionToken: payload.sessionToken || null,
          tokenExpiresAt: typeof payload.tokenExpiresAt === 'number'
            ? payload.tokenExpiresAt
            : Date.parse(payload.tokenExpiresAt || ''),
      });
      persistQueueContext(payload);

      if (payload.state === 'WAITING') {
        const positionText = payload.position != null ? `${payload.position}번` : '대기 중';
        setStatusMessage(`현재 ${positionText} 대기 중입니다.`);
        return false;
      }

      if (payload.state === 'READY') {
        setStatusMessage('입장 가능 상태입니다. 다음 화면으로 이동합니다.');
        return true;
      }

      if (payload.state === 'CLOSED') {
        setErrorMessage('현재 라운드가 종료되었습니다. 잠시 후 다시 시도해주세요.');
        clearQueueContext();
        return false;
      }

      if (payload.state === 'NOT_IN_QUEUE') {
        setErrorMessage('대기열 정보를 찾을 수 없습니다. 다시 예매하기를 눌러주세요.');
        clearQueueContext();
        return false;
      }

      return false;
    };

    const pollQueueStatus = async () => {
      if (!isMounted || readyHandled) {
        return;
      }

      try {
        const response = await fetch(`/api/public-queue/status?roundId=${roundId}`, {
          method: 'GET',
          credentials: 'include',
        });

        const payload = await parseResponse(response);

        if (!response.ok) {
          throw new Error(payload?.message || '대기열 상태를 확인하지 못했습니다.');
        }

        const isReady = applyQueuePayload(payload);
        if (isReady && !readyHandled) {
          readyHandled = true;
          window.clearInterval(pollTimerId);
          onReadyRef.current?.(payload);
        }
      } catch (error) {
        if (!isMounted) {
          return;
        }

        window.clearInterval(pollTimerId);
        setErrorMessage(error.message || '대기열 상태를 확인하지 못했습니다.');
      }
    };

    const joinQueue = async () => {
      const storedQueueContext = readStoredQueueContext();
      if (storedQueueContext) {
        setQueueInfo({
          state: storedQueueContext.state || 'READY',
          position: storedQueueContext.position ?? null,
          peopleAhead: storedQueueContext.peopleAhead ?? null,
          sessionToken: storedQueueContext.sessionToken || null,
          tokenExpiresAt: storedQueueContext.tokenExpiresAt || null,
        });
        setStatusMessage('입장 가능 상태를 복원했습니다. 다음 화면으로 이동합니다.');
        readyHandled = true;
        onReadyRef.current?.(storedQueueContext);
        return;
      }

      try {
        setErrorMessage('');
        setStatusMessage('대기열에 진입하는 중입니다.');

        const response = await fetch('/api/public-queue/join', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ roundId }),
        });

        const payload = await parseResponse(response);

        if (!response.ok) {
          throw new Error(payload?.message || '대기열 진입에 실패했습니다.');
        }

        if (!isMounted) {
          return;
        }

        const isReady = applyQueuePayload(payload);
        if (isReady && !readyHandled) {
          readyHandled = true;
          onReadyRef.current?.(payload);
          return;
        }

        pollQueueStatus();
        pollTimerId = window.setInterval(pollQueueStatus, POLL_INTERVAL_MS);
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setErrorMessage(error.message || '대기열 진입에 실패했습니다.');
      }
    };

    joinQueue();

    return () => {
      isMounted = false;
      if (pollTimerId) {
        window.clearInterval(pollTimerId);
      }
    };
  }, [roundId, queueStorageKey]);

  return (
    <div className="loading-overlay">
      <div className="loading-container">
        <div className="loading-spinner" />
        <div className="loading-copy">
          <p className="loading-title">{performanceName}</p>
          <p className="loading-text">{errorMessage || statusMessage}</p>
          {!errorMessage && (
            <p className="loading-subtext">현재 상태: {statusLabel}</p>
          )}
        </div>

        {!errorMessage && (
          <div className="loading-queue-card">
            <div className="loading-queue-row">
              <span className="loading-queue-label">대기번호</span>
              <strong className="loading-queue-value">
                {queueInfo.position != null ? `${queueInfo.position}번` : '-'}
              </strong>
            </div>
            <div className="loading-queue-row">
              <span className="loading-queue-label">앞에 대기 중</span>
              <strong className="loading-queue-value">
                {queueInfo.peopleAhead != null ? `${queueInfo.peopleAhead}명` : '-'}
              </strong>
            </div>
          </div>
        )}

        <div className="loading-actions">
          <button
            type="button"
            className="loading-cancel-btn"
            onClick={() => {
              if (queueStorageKey) {
                window.localStorage.removeItem(queueStorageKey);
              }
              onCancelRef.current?.();
            }}
          >
            예매 취소
          </button>
        </div>
      </div>
    </div>
  );
};

export default PublicLoadingScreen;
