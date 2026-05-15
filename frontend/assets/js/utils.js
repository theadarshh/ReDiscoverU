/* ═══════════════════════════════════════════════════════════════
   ReDiscoverU v41 — Shared Utilities
   ═══════════════════════════════════════════════════════════════ */

const API = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
  ? 'http://localhost:8080/api'
  : 'https://api.rediscoveru.life/api';

// ── Auth helpers ──────────────────────────────────────────────
function getToken()  { return localStorage.getItem('rdu_token'); }
function getName()   { return localStorage.getItem('rdu_name') || ''; }
function getEmail()  { return localStorage.getItem('rdu_email') || ''; }
function getRole()   { return localStorage.getItem('rdu_role') || ''; }
function getStatus() { return localStorage.getItem('rdu_status') || ''; }
// Access levels: NONE | LAUNCHPAD | LIFETIME
function getAccess() { return localStorage.getItem('rdu_access') || 'NONE'; }

function authHeader() {
  return { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' };
}

function saveSession(data) {
  localStorage.setItem('rdu_token',  data.token);
  localStorage.setItem('rdu_name',   data.name);
  localStorage.setItem('rdu_email',  data.email);
  localStorage.setItem('rdu_role',   data.role);
  const status = data.subscriptionStatus || data.accountStatus || 'PENDING';
  localStorage.setItem('rdu_status', status);
  // Map status to access level
  if (status === 'LIFETIME') localStorage.setItem('rdu_access', 'LIFETIME');
  else if (status === 'PAID' || status === 'LAUNCHPAD') localStorage.setItem('rdu_access', 'LAUNCHPAD');
  else localStorage.setItem('rdu_access', 'NONE');
}

function logout() { localStorage.clear(); window.location.href = '../login.html'; }
function logoutRoot() { localStorage.clear(); window.location.href = 'login.html'; }

function requireAuth(loginPath = '../login.html') {
  if (!getToken()) { window.location.href = loginPath; return false; }
  return true;
}
function requireAdmin() {
  if (!getToken() || getRole() !== 'ROLE_ADMIN') { window.location.href = '../login.html'; return false; }
  return true;
}
function requireLaunchpad(loginPath='../login.html') {
  if (!getToken()) { window.location.href = loginPath; return false; }
  const a = getAccess();
  if (a === 'NONE') { window.location.href = '../launchpad.html'; return false; }
  return true;
}

// ── UI helpers ────────────────────────────────────────────────
function showAlert(id, msg, type = 'error') {
  const el = document.getElementById(id);
  if (el) el.innerHTML = `<div class="alert alert-${type}">${msg}</div>`;
}
function clearAlert(id) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = '';
}
function showToast(msg, type='info') {
  let t = document.getElementById('global-toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'global-toast';
    t.className = 'toast';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.className = `toast ${type} show`;
  setTimeout(() => t.classList.remove('show'), 3000);
}

function formatINR(n) {
  return '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}
function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
function esc(s) {
  return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function imgSrc(url) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return API.replace('/api','') + url;
}

// ── Intersection Observer fade-up ─────────────────────────────
function initFadeUp() {
  const els = document.querySelectorAll('.fade-up:not(.visible)');
  if (!els.length) return;
  const io = new IntersectionObserver(entries => {
    entries.forEach((e, i) => {
      if (e.isIntersecting) {
        setTimeout(() => e.target.classList.add('visible'), (i % 4) * 80);
        io.unobserve(e.target);
      }
    });
  }, { threshold: 0.1 });
  els.forEach(el => io.observe(el));
}

// ── Modal helpers ─────────────────────────────────────────────
function openModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }

// ── Dashboard sidebar nav ─────────────────────────────────────
function initDashNav() {
  document.querySelectorAll('.dash-nav-item[data-page]').forEach(item => {
    item.addEventListener('click', () => showDashPage(item.dataset.page));
  });
}
function showDashPage(page) {
  document.querySelectorAll('.dash-page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.dash-nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById('page-' + page)?.classList.add('active');
  document.querySelector(`.dash-nav-item[data-page="${page}"]`)?.classList.add('active');
  if (typeof onPageChange === 'function') onPageChange(page);
}

document.addEventListener('DOMContentLoaded', () => {
  initFadeUp();
  initDashNav();
});
