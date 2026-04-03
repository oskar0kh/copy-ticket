export function getNextRoundOpenTime(now) {
  const next = new Date(now);
  const minute = now.getMinutes();

  if (minute < 30) {
    next.setMinutes(30, 0, 0);
    return next;
  }

  next.setHours(now.getHours() + 1);
  next.setMinutes(0, 0, 0);
  return next;
}

export function formatRemaining(milliseconds) {
  const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}시간 ${minutes}분 ${seconds}초`;
  }

  return `${minutes}분 ${seconds}초`;
}
