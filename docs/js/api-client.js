(function () {
  const CONFIG = {
    useMock: true,
    baseUrl: 'http://localhost:8080/api'
  };

  const STORAGE_KEY = 'padel_demo_session_v1';

  const mockUsers = [
    { id: 1, nombre: 'Usuario Demo', email: 'demo@padel.local', password: 'demo123', rol: 'USER' },
    { id: 2, nombre: 'Admin Demo', email: 'admin@padel.local', password: 'admin123', rol: 'ADMIN' }
  ];

  function saveSession(user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      id: user.id,
      nombre: user.nombre,
      email: user.email,
      rol: user.rol
    }));
  }

  function getSession() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) {
      return null;
    }
  }

  function clearSession() {
    localStorage.removeItem(STORAGE_KEY);
  }

  async function loginMock(email, password) {
    const byCredentials = mockUsers.find((u) => u.email.toLowerCase() === email.toLowerCase() && u.password === password);
    const byAdminHint = !byCredentials && email.toLowerCase().includes('admin')
      ? mockUsers.find((u) => u.rol === 'ADMIN')
      : null;
    const byUserHint = !byCredentials && !email.toLowerCase().includes('admin')
      ? mockUsers.find((u) => u.rol === 'USER')
      : null;

    const found = byCredentials || byAdminHint || byUserHint;
    if (!found) {
      return { ok: false, message: 'Credenciales no validas (modo demo).' };
    }

    saveSession(found);
    return { ok: true, user: getSession() };
  }

  async function registerMock(payload) {
    if (!payload?.email) {
      return { ok: false, message: 'Email obligatorio.' };
    }
    return { ok: true };
  }

  async function loginReal(email, password) {
    const response = await fetch(CONFIG.baseUrl + '/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      return { ok: false, message: 'No se pudo iniciar sesion.' };
    }

    const data = await response.json();
    saveSession(data.user);
    return { ok: true, user: getSession() };
  }

  async function registerReal(payload) {
    const response = await fetch(CONFIG.baseUrl + '/users/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      return { ok: false, message: 'No se pudo registrar el usuario.' };
    }

    return { ok: true };
  }

  const AppApi = {
    config: CONFIG,

    async login(email, password) {
      if (CONFIG.useMock) return loginMock(email, password);
      return loginReal(email, password);
    },

    async register(payload) {
      if (CONFIG.useMock) return registerMock(payload);
      return registerReal(payload);
    },

    logout() {
      clearSession();
    },

    getSession,

    requireRole(role) {
      const session = getSession();
      if (!session) {
        return { ok: false, redirect: 'index.html' };
      }
      if (role && session.rol !== role) {
        return { ok: false, redirect: session.rol === 'ADMIN' ? 'admin.html' : 'dashboard.html' };
      }
      return { ok: true, user: session };
    }
  };

  window.AppApi = AppApi;
})();
