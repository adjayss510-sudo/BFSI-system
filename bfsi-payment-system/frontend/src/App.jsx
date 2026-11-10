import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom';
import { getAuth, setAuth } from './api';
import { Login, Register, Dashboard, Pay, History, Admin } from './pages.jsx';

function Shell({ auth, onLogout, children }) {
  const nav = useNavigate();
  return (
    <>
      <header className="topbar">
        <b className="brand">BFSI Pay</b>
        <nav>
          {auth.role === 'CUSTOMER' && (
            <>
              <NavLink to="/">Dashboard</NavLink>
              <NavLink to="/pay">Pay</NavLink>
              <NavLink to="/history">History</NavLink>
            </>
          )}
          {auth.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
        </nav>
        <span className="who">{auth.email} ({auth.role})</span>
        <button className="ghost" onClick={() => { onLogout(); nav('/login'); }}>Logout</button>
      </header>
      <main className="container">{children}</main>
    </>
  );
}

export default function App() {
  const [auth, setAuthState] = useState(getAuth());
  const login = (a) => { setAuth(a); setAuthState(a); };
  const logout = () => { setAuth(null); setAuthState(null); };
  const home = auth?.role === 'ADMIN' ? '/admin' : '/';

  const guard = (el, role) =>
    !auth ? <Navigate to="/login" /> :
    auth.role !== role ? <Navigate to={home} /> :
    <Shell auth={auth} onLogout={logout}>{el}</Shell>;

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={auth ? <Navigate to={home} /> : <Login onLogin={login} />} />
        <Route path="/register" element={auth ? <Navigate to={home} /> : <Register onLogin={login} />} />
        <Route path="/" element={guard(<Dashboard />, 'CUSTOMER')} />
        <Route path="/pay" element={guard(<Pay />, 'CUSTOMER')} />
        <Route path="/history" element={guard(<History />, 'CUSTOMER')} />
        <Route path="/admin" element={guard(<Admin />, 'ADMIN')} />
        <Route path="*" element={<Navigate to={home} />} />
      </Routes>
    </BrowserRouter>
  );
}
