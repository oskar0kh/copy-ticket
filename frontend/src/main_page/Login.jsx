import React, { useEffect, useRef, useState } from "react";
import { login, logout, signup, withdrawAccount, me } from "../api/auth";
import MainPage from "./MainPage";

const initialSignup = { id: "", password: "", name: "" };
const initialLogin = { id: "", password: "" };

function validateSignupForm({ id, password, name }) {
  const trimmedId = id.trim();
  const trimmedPassword = password.trim();
  const trimmedName = name.trim();

  if (!trimmedId) return "아이디를 입력해 주세요.";
  if (trimmedId.length > 20) return "아이디는 20자 이하여야 합니다.";
  if (!trimmedPassword) return "비밀번호를 입력해 주세요.";
  if (trimmedPassword.length < 4 || trimmedPassword.length > 20) {
    return "비밀번호는 4자 이상 20자 이하로 입력해 주세요.";
  }
  if (!trimmedName) return "이름을 입력해 주세요.";
  if (trimmedName.length > 20) return "이름은 20자 이하여야 합니다.";

  return "";
}

function validateLoginForm({ id, password }) {
  const trimmedId = id.trim();
  const trimmedPassword = password.trim();

  if (!trimmedId) return "아이디를 입력해 주세요.";
  if (trimmedId.length > 20) return "아이디는 20자 이하여야 합니다.";
  if (!trimmedPassword) return "비밀번호를 입력해 주세요.";
  if (trimmedPassword.length < 4 || trimmedPassword.length > 20) {
    return "비밀번호는 4자 이상 20자 이하로 입력해 주세요.";
  }

  return "";
}

export default function Login() {
  const [mode, setMode] = useState("login");
  const [view, setView] = useState("auth");
  const [signupForm, setSignupForm] = useState(initialSignup);
  const [loginForm, setLoginForm] = useState(initialLogin);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [user, setUser] = useState(null);
  const [lastInputUrl, setLastInputUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const toastTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (toastTimerRef.current) {
        window.clearTimeout(toastTimerRef.current);
      }
    };
  }, []);

  // 페이지 로드 시 현재 인증 상태 확인
  useEffect(() => {
    async function checkAuthStatus() {
      try {
        const currentUser = await me();
        setUser(currentUser);
        setView("main");
      } catch (err) {
        // 인증되지 않았거나 만료된 세션, 로그인 화면 유지
        setView("auth");
      }
    }

    checkAuthStatus();
  }, []);

  useEffect(() => {
    if (!error) {
      return;
    }

    const errorTimerId = window.setTimeout(() => {
      setError("");
    }, 3000);

    return () => {
      window.clearTimeout(errorTimerId);
    };
  }, [error]);

  function showToast(text, type = "error") {
    setToast({ text, type });
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
    toastTimerRef.current = window.setTimeout(() => {
      setToast(null);
    }, 2800);
  }

  async function handleSignup(event) {
    event.preventDefault();

    const validationMessage = validateSignupForm(signupForm);
    if (validationMessage) {
      showToast(validationMessage, "error");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const result = await signup(signupForm);
      setMessage(result?.message || "회원가입이 완료되었습니다.");
      showToast(result?.message || "회원가입이 완료되었습니다.", "success");
      setSignupForm(initialSignup);
      setMode("login");
    } catch (err) {
      setError(err.message);
      showToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();

    const validationMessage = validateLoginForm(loginForm);
    if (validationMessage) {
      showToast(validationMessage, "error");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const currentUser = await login(loginForm);
      setUser(currentUser);
      setView("main");
      setMessage(`${currentUser.name}님, 환영합니다.`);
      showToast(`${currentUser.name}님, 환영합니다.`, "success");
      setLoginForm(initialLogin);
    } catch (err) {
      setError("로그인 실패");
      showToast("로그인 실패", "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const seatSelectionStoragePrefix = "seatSelection:selectedSeats:";
      Object.keys(window.localStorage).forEach((key) => {
        if (key.startsWith(seatSelectionStoragePrefix)) {
          window.localStorage.removeItem(key);
        }
      });

      window.localStorage.removeItem("reservationFlow");
      const mainPageDetailsPrefix = "mainPage:performanceDetails:";
      Object.keys(window.localStorage).forEach((key) => {
        if (key.startsWith(mainPageDetailsPrefix)) {
          window.localStorage.removeItem(key);
        }
      });
    } catch {
      // Ignore storage cleanup errors.
    }

    setUser(null);
    setView("auth");
    setMode("login");

    try {
      await logout();
      setMessage("로그아웃되었습니다.");
      showToast("로그아웃되었습니다.", "success");
    } catch (err) {
      setError(err.message);
      showToast(err.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleWithdraw() {
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const result = await withdrawAccount();
      setUser(null);
      setView("auth");
      setMode("login");
      setMessage(result?.message || "회원 탈퇴가 완료되었습니다.");
      showToast(result?.message || "회원 탈퇴가 완료되었습니다.", "success");
    } catch (err) {
      setError(err.message);
      showToast(err.message, "error");
      throw err;
    } finally {
      setLoading(false);
    }
  }

  function handleUrlInputSubmit(urlValue) {
    const trimmedUrl = urlValue.trim();
    if (!trimmedUrl) {
      showToast("URL을 입력해 주세요.", "error");
      return;
    }

    setLastInputUrl(trimmedUrl);
  }

  return (
    <div className="page">
      {toast && (
        <div className={`toast ${toast.type === "success" ? "success" : "error"}`} role="alert" aria-live="polite">
          {toast.text}
        </div>
      )}

      {view === "main" ? (
        <MainPage
          user={user}
          loading={loading}
          lastInputUrl={lastInputUrl}
          onSubmitUrl={handleUrlInputSubmit}
          onLogout={handleLogout}
          onWithdraw={handleWithdraw}
        />
      ) : (
      <main className="auth-shell auth-single">
        <section className="brand-panel">
          <p className="tag">COPY TICKET</p>
          <h1>카피 티켓</h1>
          <p>
            카피 티켓은 티켓팅을 연습할 사이트의 링크를 넣으면, 해당 사이트의 화면을 그대로 가져와서 똑같은 화면에서 티켓팅 연습을 할 수 있게 해주는 서비스입니다. 
            <br></br><br></br>로그인 후, 연습할 사이트의 URL을 입력해 보세요!
          </p>

          <div className="auth-controls">
            <div className="mode-switch">
              <button
                className={mode === "login" ? "active" : ""}
                onClick={() => setMode("login")}
              >
                로그인
              </button>
              <button
                className={mode === "signup" ? "active" : ""}
                onClick={() => setMode("signup")}
              >
                회원가입
              </button>
            </div>

            {message && <div className="notice success">{message}</div>}
            {error && <div className="notice error">{error}</div>}

            {mode === "login" ? (
              <form onSubmit={handleLogin} className="auth-form">
                <label>
                  아이디
                  <input
                    placeholder="아이디를 입력하세요"
                    value={loginForm.id}
                    onChange={(event) =>
                      setLoginForm((prev) => ({ ...prev, id: event.target.value }))
                    }
                    required
                  />
                </label>

                <label>
                  비밀번호
                  <input
                    type="password"
                    placeholder="비밀번호를 입력하세요"
                    value={loginForm.password}
                    onChange={(event) =>
                      setLoginForm((prev) => ({ ...prev, password: event.target.value }))
                    }
                    required
                  />
                </label>

                <button type="submit" disabled={loading}>
                  {loading ? "처리 중..." : "로그인"}
                </button>
              </form>
            ) : (
              <form onSubmit={handleSignup} className="auth-form">
                <label>
                  아이디
                  <input
                    placeholder="사용할 아이디를 입력하세요"
                    value={signupForm.id}
                    onChange={(event) =>
                      setSignupForm((prev) => ({ ...prev, id: event.target.value }))
                    }
                    required
                  />
                </label>

                <label>
                  비밀번호
                  <input
                    type="password"
                    placeholder="비밀번호를 입력하세요"
                    value={signupForm.password}
                    onChange={(event) =>
                      setSignupForm((prev) => ({ ...prev, password: event.target.value }))
                    }
                    required
                  />
                </label>

                <label>
                  이름
                  <input
                    placeholder="이름을 입력하세요"
                    value={signupForm.name}
                    onChange={(event) =>
                      setSignupForm((prev) => ({ ...prev, name: event.target.value }))
                    }
                    required
                  />
                </label>

                <button type="submit" disabled={loading}>
                  {loading ? "처리 중..." : "회원가입"}
                </button>
              </form>
            )}
          </div>
        </section>
      </main>
      )}
    </div>
  );
}
