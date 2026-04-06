/* ═══════════════════════════════════════════════════════════════
   ReDiscoverU — Shared Utilities
   ═══════════════════════════════════════════════════════════════ */

// ── URL Configuration ─────────────────────────────────────────────
// API  → all /api/* REST calls    e.g. http://localhost:8080/api
// BASE → static file root         e.g. http://localhost:8080
// Both auto-switch for local dev vs production.

const _HOST = window.location.hostname;
const _IS_LOCAL = (_HOST === '' || _HOST === 'localhost' || _HOST === '127.0.0.1');

const API  = _IS_LOCAL
  ? 'http://localhost:8080/api'
  : 'https://api.rediscoveru.life/api';

// BASE is the backend root — used to build image URLs like:
//   BASE + '/uploads/mentors/mentor-1.jpg'
//   → 'http://localhost:8080/uploads/mentors/mentor-1.jpg'
const BASE = _IS_LOCAL
  ? 'http://localhost:8080'
  : 'https://api.rediscoveru.life';

console.log('[ReDiscoverU] ENV:', _HOST || 'file://', '| API:', API, '| BASE:', BASE);

// ── Image URL helper ──────────────────────────────────────────────
// Always returns a full absolute URL for displaying images.
// Handles: relative paths (/uploads/...), absolute http URLs, and empty.
function imgUrl(path) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  // Relative path from backend — prepend backend base URL
  return BASE + (path.startsWith('/') ? path : '/' + path);
}

// ── Auth helpers ──────────────────────────────────────────────────
function getToken()  { return localStorage.getItem('rdu_token'); }
function getName()   { return localStorage.getItem('rdu_name') || ''; }
function getEmail()  { return localStorage.getItem('rdu_email') || ''; }
function getRole()   { return localStorage.getItem('rdu_role') || ''; }
function getStatus() { return localStorage.getItem('rdu_status') || ''; }

function authHeader() {
  return {
    'Authorization': 'Bearer ' + getToken(),
    'Content-Type': 'application/json'
  };
}

function saveSession(data) {
  localStorage.setItem('rdu_token',  data.token);
  localStorage.setItem('rdu_name',   data.name);
  localStorage.setItem('rdu_email',  data.email);
  localStorage.setItem('rdu_role',   data.role);
  const status = data.subscriptionStatus || data.accountStatus || 'PENDING';
  localStorage.setItem('rdu_status', status);
}

function logout() {
  localStorage.clear();
  window.location.href = '../login.html';
}

function logoutRoot() {
  localStorage.clear();
  window.location.href = 'login.html';
}

// ── Route guards ──────────────────────────────────────────────────
function requireAuth(loginPath = '../login.html') {
  if (!getToken()) { window.location.href = loginPath; return false; }
  return true;
}

function requireActive(loginPath = '../login.html', payPath = '../programs.html') {
  if (!getToken()) { window.location.href = loginPath; return false; }
  if (getStatus() !== 'PAID') { window.location.href = payPath + '?wall=1'; return false; }
  return true;
}

function requireAdmin() {
  if (!getToken() || getRole() !== 'ROLE_ADMIN') {
    window.location.href = '../login.html';
    return false;
  }
  return true;
}

// ── UI helpers ────────────────────────────────────────────────────
function showAlert(id, msg, type = 'error') {
  const el = document.getElementById(id);
  if (el) el.innerHTML = `<div class="alert alert-${type}">${msg}</div>`;
}
function clearAlert(id) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = '';
}

function formatINR(n) {
  return '₹' + Number(n).toLocaleString('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

function typeLabel(t) {
  return { SELF_PACED: 'Self-Paced', LIVE: 'Live Sessions', MENTORSHIP: '1-to-1 Mentorship' }[t] || t;
}

function typeBadge(t) {
  return { SELF_PACED: 'badge-green', LIVE: 'badge-blue', MENTORSHIP: 'badge-gold' }[t] || 'badge-neutral';
}

function statusBadge(s) {
  return { PAID: 'badge-green', PENDING: 'badge-gold' }[s] || 'badge-neutral';
}

function statusLabel(s) {
  return { PAID: 'Active', PENDING: 'Pending Payment' }[s] || s;
}

// ── IntersectionObserver fade-in ──────────────────────────────────
function initFadeUp() {
  const els = document.querySelectorAll('.fade-up:not(.visible)');
  if (!els.length) return;
  const io = new IntersectionObserver(entries => {
    entries.forEach((e, i) => {
      if (e.isIntersecting) {
        setTimeout(() => e.target.classList.add('visible'), 0);
        io.unobserve(e.target);
      }
    });
  }, { threshold: 0.06 });
  els.forEach((el, i) => {
    el.style.transitionDelay = (i % 5) * 0.08 + 's';
    io.observe(el);
  });
}

// ── Dashboard sidebar routing ─────────────────────────────────────
function initDashNav() {
  document.querySelectorAll('.sidebar-link[data-page]').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      showDashPage(link.dataset.page);
    });
  });
}

function showDashPage(page) {
  document.querySelectorAll('.dash-page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
  const pageEl = document.getElementById('page-' + page);
  const linkEl = document.querySelector(`.sidebar-link[data-page="${page}"]`);
  if (pageEl) pageEl.classList.add('active');
  if (linkEl) linkEl.classList.add('active');
  if (typeof onPageChange === 'function') onPageChange(page);
}

document.addEventListener('DOMContentLoaded', () => {
  initFadeUp();
  initDashNav();
});
