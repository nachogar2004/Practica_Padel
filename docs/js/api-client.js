/**
 * api-client.js — Módulo central de comunicación con el backend.
 *
 * Se encapsula en una IIFE (Immediately Invoked Function Expression) para evitar
 * contaminar el ámbito global con variables internas. Solo expone el objeto
 * público `window.AppApi` al resto de las páginas.
 *
 * Soporta dos modos de funcionamiento:
 *   - Mock (useMock: true):  simula respuestas sin llamar al servidor real.
 *                            Útil para probar la interfaz sin backend arrancado.
 *   - Real (useMock: false): realiza peticiones HTTP reales al servidor Spring Boot.
 */
(function () {

  /**
   * Configuración global del cliente.
   *
   * @property {boolean} useMock  - Si es true, todas las operaciones usan datos
   *                                locales sin hacer peticiones HTTP.
   * @property {string}  baseUrl  - URL raíz del backend. Debe coincidir con el
   *                                server.port y server.servlet.context-path de
   *                                application.properties.
   */
  const CONFIG = {
    useMock: false,
    baseUrl: 'http://localhost:8080/pistaPadel'
  };

  /**
   * Clave bajo la que se guarda la sesión del usuario en localStorage.
   * Usar una clave versionada evita conflictos si el formato de datos cambia.
   */
  const STORAGE_KEY = 'padel_session_v1';

  // ---------------------------------------------------------------------------
  // Gestión de sesión en localStorage
  // ---------------------------------------------------------------------------

  /**
   * Guarda los datos del usuario en localStorage tras un login exitoso.
   * Solo se persisten los campos necesarios para la UI; nunca la contraseña.
   *
   * @param {{ id: number, nombre: string, email: string, rol: string }} user
   *   Objeto usuario devuelto por el backend (ModeloUsuario).
   */
  function saveSession(user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      id: user.idUsuario ?? user.id,   // idUsuario es el nombre del campo Java
      nombre: user.nombre,
      email: user.email,
      rol: user.rol      // 'USER' o 'ADMIN' — usado por requireRole()
    }));
  }

  /**
   * Lee y devuelve la sesión guardada en localStorage.
   * Si no hay sesión activa, o si el JSON está corrupto, devuelve null.
   * El try/catch evita que un valor malformado rompa la página.
   *
   * @returns {{ id: number, nombre: string, email: string, rol: string } | null}
   */
  function getSession() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) {
      return null;
    }
  }

  /**
   * Elimina la sesión del localStorage.
   * Se llama siempre durante el logout, tanto si el servidor responde como si no.
   */
  function clearSession() {
    localStorage.removeItem(STORAGE_KEY);
  }

  // ---------------------------------------------------------------------------
  // Modo Mock — operaciones simuladas sin backend
  // ---------------------------------------------------------------------------

  /**
   * Simula el login buscando el usuario en la lista local `mockUsers`.
   * Aplica tres estrategias en orden:
   *   1. Coincidencia exacta de email y contraseña.
   *   2. Si el email contiene 'admin', devuelve el usuario ADMIN (conveniente para demos).
   *   3. Si no, devuelve el usuario USER genérico.
   * Esto facilita probar la UI con cualquier credencial sin ser estricto.
   *
   * @param {string} email
   * @param {string} password
   * @returns {Promise<{ ok: boolean, message?: string, user?: object }>}
   */
  /**
   * Simula el registro de un usuario en modo mock.
   * Solo valida que el email esté presente; no persiste nada de forma real.
   *
   * @param {{ email: string, [key: string]: any }} payload
   * @returns {Promise<{ ok: boolean, message?: string }>}
   */
  // Mock mode removed: registration/login go to real backend only

  // ---------------------------------------------------------------------------
  // Modo Real — peticiones HTTP al servidor Spring Boot
  // ---------------------------------------------------------------------------

  /**
   * Realiza el login contra el endpoint real del backend.
   *
   * Flujo:
   *   1. POST /auth/login con {email, password} en el cuerpo JSON.
   *   2. El servidor verifica las credenciales y crea una HttpSession.
   *   3. La respuesta incluye la cabecera Set-Cookie con el JSESSIONID.
   *   4. `credentials: 'include'` permite que el navegador guarde esa cookie.
   *   5. El cuerpo de la respuesta contiene el ModeloUsuario serializado.
   *   6. Se guarda en localStorage para que la UI pueda leer nombre y rol
   *      sin hacer una llamada extra al servidor.
   *
   * @param {string} email
   * @param {string} password
   * @returns {Promise<{ ok: boolean, message?: string, user?: object }>}
   */
  async function loginReal(email, password) {
    const response = await fetch(CONFIG.baseUrl + '/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',   // imprescindible: sin esto el navegador descarta el JSESSIONID
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      return { ok: false, message: 'No se pudo iniciar sesion.' };
    }

    // El backend devuelve directamente el ModeloUsuario como JSON.
    const user = await response.json();
    saveSession(user);
    return { ok: true, user: getSession() };
  }

  /**
   * Registra un nuevo usuario en el backend.
   *
   * El servidor asigna el rol USER por defecto si no se especifica.
   * Devuelve HTTP 201 si el registro fue exitoso, o 409 si el email ya existe.
   * `credentials: 'include'` se incluye para consistencia, aunque el registro
   * no crea sesión por sí solo.
   *
   * @param {{ nombre: string, email: string, password: string, [key: string]: any }} payload
   * @returns {Promise<{ ok: boolean, message?: string }>}
   */
  async function registerReal(payload) {
    const response = await fetch(CONFIG.baseUrl + '/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      return { ok: false, message: 'No se pudo registrar el usuario.' };
    }

    return { ok: true };
  }

  // ---------------------------------------------------------------------------
  // API pública — objeto expuesto a las páginas HTML
  // ---------------------------------------------------------------------------

  const AppApi = {
    /** Expone CONFIG para que las páginas puedan leer useMock si lo necesitan. */
    config: CONFIG,

    /**
     * Inicia sesión con email y contraseña.
     * Delega en loginMock o loginReal según CONFIG.useMock.
     *
     * @param {string} email
     * @param {string} password
     * @returns {Promise<{ ok: boolean, message?: string, user?: object }>}
     */
    async login(email, password) {
      return loginReal(email, password);
    },

    /**
     * Registra un nuevo usuario.
     * Delega en registerMock o registerReal según CONFIG.useMock.
     *
     * @param {{ nombre: string, email: string, password: string }} payload
     * @returns {Promise<{ ok: boolean, message?: string }>}
     */
    async register(payload) {
      return registerReal(payload);
    },

    /**
     * Cierra la sesión del usuario.
     *
     * En modo real, primero avisa al servidor (POST /auth/logout) para que
     * invalide la HttpSession y el JSESSIONID deje de ser válido. Si la
     * llamada falla (servidor caído, timeout, etc.), se captura el error
     * silenciosamente para no bloquear el cierre de sesión en el frontend.
     * En cualquier caso, siempre se limpia el localStorage.
     *
     * @returns {Promise<void>}
     */
    async logout() {
      if (!CONFIG.useMock) {
        try {
          await fetch(CONFIG.baseUrl + '/auth/logout', {
            method: 'POST',
            credentials: 'include'   // necesario para enviar el JSESSIONID al servidor
          });
        } catch (_) {
          // Aunque falle la llamada remota, limpiamos sesión local para desbloquear la UI.
        }
      }
      clearSession();
    },

    /**
     * Devuelve los datos de la sesión actual desde localStorage, o null si no
     * hay sesión activa. Las páginas usan esto para saber el nombre/rol del
     * usuario sin llamar al servidor.
     *
     * @returns {{ id: number, nombre: string, email: string, rol: string } | null}
     */
    getSession,

    /**
     * Comprueba que haya una sesión activa y que el usuario tenga el rol requerido.
     * Úsalo al inicio de cada página protegida para decidir si redirigir o no.
     *
     * Si no hay sesión → redirige a index.html (el usuario debe hacer login).
     * Si el rol no coincide → redirige a index.html?forceLogin=1 (debe cambiar
     *   de cuenta; el parámetro le indica a index.html que limpie la sesión actual).
     * Si todo está bien → devuelve { ok: true, user }.
     *
     * IMPORTANTE: esto es una protección de UI, no de seguridad. La seguridad
     * real la aplica el servidor comprobando el JSESSIONID en cada endpoint.
     *
     * @param {string} [role]  Rol requerido ('USER' o 'ADMIN'). Si se omite,
     *                         solo se comprueba que haya sesión.
     * @returns {{ ok: boolean, redirect?: string, user?: object }}
     */
    requireRole(role) {
      const session = getSession();
      if (!session) {
        return { ok: false, redirect: 'index.html' };
      }
      if (role && session.rol !== role) {
        // Si el rol no coincide, forzamos paso por login para permitir cambiar de cuenta.
        return { ok: false, redirect: 'index.html?forceLogin=1' };
      }
      return { ok: true, user: session };
    }
  };

  // Exponer el objeto público para que cualquier página pueda usar window.AppApi
  window.AppApi = AppApi;
})();
