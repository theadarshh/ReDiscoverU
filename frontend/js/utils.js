const API=(location.hostname==='localhost'||location.hostname==='127.0.0.1')?'http://localhost:8080/api':'https://api.rediscoveru.life/api';
const BASE=API.replace('/api','');
function imgUrl(p){if(!p)return '';if(p.startsWith('http'))return p;return BASE+p;}
function getToken(){return localStorage.getItem('rdu_token');}
function getName(){return localStorage.getItem('rdu_name')||'';}
function getEmail(){return localStorage.getItem('rdu_email')||'';}
function getRole(){return localStorage.getItem('rdu_role')||'';}
function getStatus(){return localStorage.getItem('rdu_status')||'';}
function authHeader(){return{'Authorization':'Bearer '+getToken(),'Content-Type':'application/json'};}
function saveSession(d){localStorage.setItem('rdu_token',d.token);localStorage.setItem('rdu_name',d.name);localStorage.setItem('rdu_email',d.email);localStorage.setItem('rdu_role',d.role);localStorage.setItem('rdu_status',d.subscriptionStatus||d.accountStatus||'PENDING');}
function logout(){localStorage.clear();window.location.href='../login.html';}
function logoutRoot(){localStorage.clear();window.location.href='login.html';}
function requireAuth(p){p=p||'../login.html';if(!getToken()){window.location.href=p;return false;}return true;}
function requireAdmin(){if(!getToken()||getRole()!=='ROLE_ADMIN'){window.location.href='../login.html';return false;}return true;}
function showAlert(id,msg,type){type=type||'error';var el=document.getElementById(id);if(el)el.innerHTML='<div class="alert alert-'+type+'">'+msg+'</div>';}
function clearAlert(id){var el=document.getElementById(id);if(el)el.innerHTML='';}
function formatINR(n){return '₹'+Number(n).toLocaleString('en-IN',{minimumFractionDigits:0,maximumFractionDigits:0});}
function formatDate(iso){if(!iso)return '—';return new Date(iso).toLocaleString('en-IN',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'});}
function typeLabel(t){return{SELF_PACED:'Self-Paced',LIVE:'Live Sessions',MENTORSHIP:'1-to-1 Mentorship'}[t]||t||'';}
function typeBadge(t){return{SELF_PACED:'badge-green',LIVE:'badge-blue',MENTORSHIP:'badge-gold'}[t]||'badge-neutral';}
function statusBadge(s){return{PAID:'badge-green',PENDING:'badge-gold'}[s]||'badge-neutral';}
function statusLabel(s){return{PAID:'Active',PENDING:'Pending Payment'}[s]||s||'';}
function initFadeUp(){var els=document.querySelectorAll('.fade-up:not(.visible)');if(!els.length)return;var io=new IntersectionObserver(function(entries){entries.forEach(function(e,i){if(e.isIntersecting){setTimeout(function(){e.target.classList.add('visible');},i*60);io.unobserve(e.target);}});},{threshold:0.06,rootMargin:'0px 0px -40px 0px'});els.forEach(function(el){io.observe(el);});}
function initDashNav(){document.querySelectorAll('.sidebar-link[data-page]').forEach(function(link){link.addEventListener('click',function(e){e.preventDefault();showDashPage(link.dataset.page);});});}
function showDashPage(page){document.querySelectorAll('.dash-page').forEach(function(p){p.classList.remove('active');});document.querySelectorAll('.sidebar-link').forEach(function(l){l.classList.remove('active');});var pageEl=document.getElementById('page-'+page);var linkEl=document.querySelector('.sidebar-link[data-page="'+page+'"]');if(pageEl)pageEl.classList.add('active');if(linkEl)linkEl.classList.add('active');if(typeof onPageChange==='function')onPageChange(page);}
document.addEventListener('DOMContentLoaded',function(){initFadeUp();initDashNav();});
