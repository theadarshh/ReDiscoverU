# ReDiscoverU v41 — Deployment Guide

## WHAT'S NEW IN v41
- ✅ 6-Day Launchpad (₹499) — dedicated page + Razorpay payment
- ✅ Lifetime Membership (₹4,999) — premium upgrade flow
- ✅ Locked premium sections with blur overlay
- ✅ Premium UI/UX — warm ivory + gold design system
- ✅ Removed testimonials completely
- ✅ Removed FAQ section
- ✅ YouTube video management (admin)
- ✅ Dual pricing (launchpadPrice + lifetimePrice in admin settings)
- ✅ Modern SaaS admin dashboard with vertical sidebar
- ✅ Interactive user dashboard with progress tracking

## PRICING (ADMIN CONFIGURABLE)
- 6-Day Launchpad: ₹499 (default)
- Lifetime Membership: ₹4,999 (default)
- Both configurable in Admin → Platform Settings

## ACCESS LEVELS
- NONE: No payment → redirect to launchpad
- LAUNCHPAD (PAID): 6-day content + sessions unlocked
- LIFETIME: All programs + everything unlocked forever

## LOCAL DEVELOPMENT

### Backend
```bash
cd backend
cp src/main/resources/application-example.properties src/main/resources/application.properties
# Fill in your values
mvn clean install -DskipTests
mvn spring-boot:run
# Runs at http://localhost:8080
```

### Frontend
```bash
cd frontend
python3 -m http.server 5500
# Open http://localhost:5500
```

## DEPLOY BACKEND → RENDER

1. Push `backend/` folder to GitHub
2. Render → New Web Service
3. Root directory: `backend`
4. Build: `mvn clean install -DskipTests`
5. Start: `java -Dserver.port=$PORT -jar target/*.jar`
6. Add env vars:
   ```
   DB_URL=jdbc:postgresql://YOUR_SUPABASE_URL/postgres
   DB_USER=postgres.YOUR_PROJECT
   DB_PASS=YOUR_PASSWORD
   JWT_SECRET=your-32-char-minimum-secret-key
   JWT_EXPIRY=86400000
   MAIL_USERNAME=yourmail@gmail.com
   MAIL_PASSWORD=your-gmail-app-password
   ADMIN_EMAIL=admin@rediscoveru.life
   ADMIN_PASSWORD=StrongPassword@2025
   ADMIN_NAME=Jayashankar Lingaiah
   RAZORPAY_KEY_ID=rzp_live_xxxxx
   RAZORPAY_KEY_SECRET=your_secret
   RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
   ```
7. Add Razorpay keys in Admin → Razorpay Config (encrypted storage)

## DEPLOY FRONTEND → HOSTINGER

1. Update API URL in `frontend/assets/js/utils.js`:
   ```js
   : 'https://YOUR-APP.onrender.com/api'
   ```
2. ZIP the contents of `frontend/` folder
3. Upload to Hostinger → File Manager → `public_html/`
4. Extract — ensure `index.html` is at root

## RAZORPAY WEBHOOK
- URL: `https://YOUR-APP.onrender.com/api/webhooks/razorpay`
- Events: `payment.captured`
- Secret: same as `RAZORPAY_WEBHOOK_SECRET` env var

## PAYMENT FLOW
Both product types use the same `/api/payment/order` endpoint.
Pass `productType: "LAUNCHPAD"` or `productType: "LIFETIME"` in the request body.

Backend uses `launchpadPrice` or `lifetimePrice` from platform_settings.
