# ReDiscoverU — Production Deployment Guide

## Architecture

```
Browser
  ├── Frontend (Render Static) → https://rediscoveru.life
  └── Backend  (Render Docker) → https://api.rediscoveru.life
        └── MySQL DB (Render Managed DB or PlanetScale free tier)
```

---

## Option A — Deploy to Render (Recommended — Free tier available)

### Step 1 — Push code to GitHub

1. Create a new repository on GitHub: `github.com/yourname/rediscoveru`
2. In your project folder:

```bash
git init
git add .
git commit -m "Initial production deployment"
git remote add origin https://github.com/yourname/rediscoveru.git
git push -u origin main
```

Your `.gitignore` already excludes:
- `target/` (build artifacts)
- `uploads/` (user images — these live on the server disk)
- `.env`, `*.log`

---

### Step 2 — Deploy Backend to Render

1. Go to **render.com** → New → **Web Service**
2. Connect your GitHub repo
3. Settings:
   - **Name:** `rediscoveru-api`
   - **Root Directory:** `backend`
   - **Environment:** Docker
   - **Dockerfile path:** `Dockerfile`
   - **Plan:** Free (or Starter $7/mo for always-on)

4. Set these **Environment Variables** in Render dashboard:

| Variable | Value |
|---|---|
| `DB_URL` | From your MySQL provider (see Step 3) |
| `DB_USER` | Your MySQL username |
| `DB_PASS` | Your MySQL password |
| `JWT_SECRET` | Generate: `openssl rand -base64 48` |
| `PAYMENT_CONFIG_ENCRYPTION_KEY` | Generate: `openssl rand -base64 32` |
| `MAIL_USERNAME` | `rediscoveruadmin@gmail.com` |
| `MAIL_PASSWORD` | Gmail app password (`enql xojo geyi tasm`) |
| `ADMIN_EMAIL` | `rediscoveruadmin@gmail.com` |
| `ADMIN_PASSWORD` | `Coaching@2026` |
| `ADMIN_NAME` | `Jayashankar Lingaiah` |
| `APP_BASE_URL` | `https://rediscoveru.life` |
| `FILE_UPLOAD_DIR` | `/opt/render/project/uploads` |

5. Add a **Disk** in Render:
   - Mount path: `/opt/render/project/uploads`
   - Size: 1 GB

6. Click **Deploy** — Render will build the Docker image and start the API.

7. Your API will be live at: `https://rediscoveru-api.onrender.com`

8. In Render settings, set **Custom Domain**: `api.rediscoveru.life`

---

### Step 3 — MySQL Database

**Option A: Render PostgreSQL (easiest)**
> Note: requires changing the dialect and connector.
> Use PlanetScale or Aiven instead for MySQL.

**Option B: PlanetScale (free MySQL, recommended)**
1. Go to **planetscale.com** → Create database → `rediscoveru`
2. Create a branch `main`
3. Get connection string → use as `DB_URL`
4. PlanetScale connection string format:
   ```
   jdbc:mysql://aws.connect.psdb.cloud/rediscoveru?sslMode=VERIFY_IDENTITY&serverTimezone=UTC
   ```
5. Set `DB_USER` and `DB_PASS` from PlanetScale credentials

**Option C: Aiven free MySQL**
1. Go to **aiven.io** → Free tier → MySQL
2. Get connection details → set as env vars

---

### Step 4 — Deploy Frontend to Render

1. Render → New → **Static Site**
2. Connect same GitHub repo
3. Settings:
   - **Name:** `rediscoveru-frontend`
   - **Root Directory:** `frontend`
   - **Publish directory:** `.` (the frontend folder itself)
   - **Build command:** *(leave empty — no build needed)*
4. Set **Custom Domain**: `rediscoveru.life`
5. Click **Deploy**

The frontend auto-switches API URL based on hostname:
- `localhost` → `http://localhost:8080`
- `rediscoveru.life` → `https://api.rediscoveru.life`

---

### Step 5 — DNS (if you own rediscoveru.life)

In your domain registrar (GoDaddy / Namecheap / Cloudflare):

| Type | Name | Value |
|---|---|---|
| CNAME | `@` or `www` | `rediscoveru-frontend.onrender.com` |
| CNAME | `api` | `rediscoveru-api.onrender.com` |

Cloudflare recommended — free SSL, DDoS protection, CDN.

---

## Option B — Deploy to VPS (DigitalOcean / Hetzner)

If you prefer a VPS ($4–6/mo on Hetzner or $6/mo on DigitalOcean):

### 1. Install on server

```bash
# Connect to your VPS
ssh root@your-server-ip

# Install Java 17
apt update && apt install -y openjdk-17-jdk mysql-server nginx certbot python3-certbot-nginx

# Create app directory
mkdir -p /opt/rediscoveru/uploads/{mentors,motivation,static}
```

### 2. Set up MySQL

```sql
mysql -u root -p
CREATE DATABASE rediscoveru;
CREATE USER 'rdu'@'localhost' IDENTIFIED BY 'StrongPassword@2024';
GRANT ALL PRIVILEGES ON rediscoveru.* TO 'rdu'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Build and deploy JAR

```bash
# On your local machine
cd backend
mvn clean package -DskipTests

# Upload to server
scp target/rediscoveru-*.jar root@your-server-ip:/opt/rediscoveru/app.jar
scp -r ../frontend root@your-server-ip:/var/www/rediscoveru/
```

### 4. Create systemd service

```bash
cat > /etc/systemd/system/rediscoveru.service << 'EOF'
[Unit]
Description=ReDiscoverU Spring Boot App
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/rediscoveru
Environment="DB_URL=jdbc:mysql://localhost:3306/rediscoveru?useSSL=false&serverTimezone=UTC"
Environment="DB_USER=rdu"
Environment="DB_PASS=StrongPassword@2024"
Environment="JWT_SECRET=your-long-random-secret-here"
Environment="PAYMENT_CONFIG_ENCRYPTION_KEY=your-encryption-key"
Environment="MAIL_USERNAME=rediscoveruadmin@gmail.com"
Environment="MAIL_PASSWORD=enql xojo geyi tasm"
Environment="ADMIN_EMAIL=rediscoveruadmin@gmail.com"
Environment="ADMIN_PASSWORD=Coaching@2026"
Environment="ADMIN_NAME=Jayashankar Lingaiah"
Environment="APP_BASE_URL=https://rediscoveru.life"
Environment="FILE_UPLOAD_DIR=/opt/rediscoveru/uploads"
Environment="PORT=8080"
ExecStart=/usr/bin/java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable rediscoveru
systemctl start rediscoveru
systemctl status rediscoveru
```

### 5. Nginx config (reverse proxy + frontend)

```nginx
# /etc/nginx/sites-available/rediscoveru

# API
server {
    listen 80;
    server_name api.rediscoveru.life;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 200M;
    }
}

# Frontend
server {
    listen 80;
    server_name rediscoveru.life www.rediscoveru.life;
    root /var/www/rediscoveru/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Serve uploaded files from backend uploads dir
    location /uploads/ {
        alias /opt/rediscoveru/uploads/;
        expires 1h;
        add_header Cache-Control "public, no-transform";
    }
}
```

```bash
# Enable site and get SSL
ln -s /etc/nginx/sites-available/rediscoveru /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
certbot --nginx -d rediscoveru.life -d www.rediscoveru.life -d api.rediscoveru.life
```

---

## Razorpay Webhook Setup (Production)

After deployment, update Razorpay webhook URL:

1. Login to **dashboard.razorpay.com**
2. Settings → Webhooks → Add webhook
3. URL: `https://api.rediscoveru.life/api/webhooks/razorpay`
4. Events: check `payment.captured`
5. Copy the webhook secret → set as `RAZORPAY_WEBHOOK_SECRET` env var
6. OR: set via Admin Dashboard → Payment Config

---

## Post-deployment checklist

- [ ] Backend API responds: `curl https://api.rediscoveru.life/api/homepage/mentors`
- [ ] Frontend loads: `https://rediscoveru.life`
- [ ] Admin login works: `rediscoveruadmin@gmail.com`
- [ ] OTP email sends (test signup flow)
- [ ] Upload mentor image — verify it appears on homepage
- [ ] Set Razorpay live keys in Admin → Payment Config
- [ ] Update Razorpay webhook URL to production
- [ ] Test full payment flow with Razorpay test card

---

## Environment Variables Quick Reference

```bash
# Required — must set before first boot
DB_URL=jdbc:mysql://your-host/rediscoveru?useSSL=true&serverTimezone=UTC
DB_USER=your_db_user
DB_PASS=your_db_password
JWT_SECRET=minimum_32_character_random_string_here
PAYMENT_CONFIG_ENCRYPTION_KEY=any_32_char_random_string

# Email
MAIL_USERNAME=rediscoveruadmin@gmail.com
MAIL_PASSWORD=enql xojo geyi tasm

# Admin account (seeded on first boot)
ADMIN_EMAIL=rediscoveruadmin@gmail.com
ADMIN_PASSWORD=Coaching@2026
ADMIN_NAME=Jayashankar Lingaiah

# App config
APP_BASE_URL=https://rediscoveru.life
FILE_UPLOAD_DIR=/opt/render/project/uploads
PORT=8080
```

---

## Cost Summary

| Option | Cost | Notes |
|---|---|---|
| Render free tier | $0/mo | Spins down after 15 min inactivity |
| Render Starter | $7/mo | Always on, best for production |
| PlanetScale DB | $0/mo | Free MySQL, 5GB storage |
| Hetzner VPS (CX11) | €3.29/mo | Best value, full control |
| DigitalOcean Droplet | $6/mo | Good support, easy setup |
| Domain (rediscoveru.life) | ~$10/yr | GoDaddy / Namecheap |
| Cloudflare | Free | SSL, CDN, DDoS protection |

**Recommended for launch:** Render Starter ($7) + PlanetScale ($0) + Cloudflare (Free)
**Total: ~$7/month**
