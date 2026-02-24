# ✨ ReDiscoverU  
## A Premium Digital Mentorship Ecosystem

**Engineered with structure. Designed with intention. Built for growth.**

---

## 🚀 About The Project

**ReDiscoverU** is a full-stack digital mentorship platform designed to unify structured learning, live sessions, accountability, and community engagement into a single secure ecosystem.

Unlike traditional course platforms, ReDiscoverU operates on a **lifetime-access model**, providing continuous mentorship and recurring structured growth experiences.

This platform was built from scratch using secure backend architecture, scalable database modeling, and a premium editorial-grade interface.

---

## 🎯 Core Vision

ReDiscoverU is **not just a content delivery system**.

It is a **structured mentorship ecosystem** integrating:

- 📚 Programs & Recorded Learning  
- 🎥 Recurring Live Sessions  
- 🤝 Community Engagement  
- 💳 Secure One-Time Payment Model  
- 🛠️ Administrative Control Panel  
- 🔐 Enterprise-Level Authentication  

All under one **scalable architecture**.

---

## 🏛️ Technical Architecture

### 🔹 Backend Stack

- **Java 17**
- **Spring Boot**
- **Spring Security**
- **JWT Authentication**
- **MySQL**
- **JPA / Hibernate**
- **Razorpay Integration**
- **Gmail SMTP (OTP & Notifications)**

#### Security Implementation

- Stateless JWT authentication  
- Role-based access control (`USER` / `ADMIN`)  
- Webhook signature validation (HMAC SHA256)  
- Password encryption (BCrypt)  
- Environment-based secret configuration  
- Protected admin routes  

---

### 🔹 Frontend Architecture

- Responsive Web Interface  
- Premium Dark Editorial Theme  
- Role-Based Dashboard Rendering  
- Protected Route Logic  
- Dynamic Content Injection  
- Modular UI Components  

---

## 🔐 Authentication & Access Flow

```

User Registration
↓
OTP Verification (Email)
↓
Payment / Coupon Validation
↓
JWT Token Issued
↓
Authorized Access Granted

```

### Roles

- **ROLE_USER** → Programs, Sessions, Community Access  
- **ROLE_ADMIN** → Full Content & Platform Control  

---

## 💳 Payment Architecture

### Paid Flow

```

User selects program
↓
Backend creates Razorpay order
↓
Frontend triggers Razorpay Checkout
↓
Webhook verifies signature
↓
Payment marked SUCCESS
↓
User subscription activated

```

### Coupon Flow (100% Discount)

```

User applies coupon
↓
Backend validates with locking
↓
Payment record created (FREE)
↓
User subscription activated

```

---

## 🔁 Advanced Session Scheduling Engine

Supports intelligent recurrence patterns:

- `ONE_TIME`
- `DAILY`
- `WEEKDAYS`
- `WEEKENDS`
- `CUSTOM_DAYS`

Human-readable schedules are dynamically generated for user clarity.

---

## 📂 Content Management

Admins can upload directly from local system:

- 🎥 Videos (MP4)  
- 📄 PDF  
- 📝 DOCX  
- 📊 PPT  
- 🖼️ Images  

Files are stored securely and served via static resource mapping.

---

## 👤 User Features

- Lifetime membership access  
- View structured programs  
- Access recorded content  
- Download resources  
- Join recurring live sessions  
- Community WhatsApp access  
- Email notifications for sessions  

---

## 🛠️ Admin Capabilities

- Create & manage programs  
- Upload multimedia & documents  
- Schedule recurring sessions  
- Manage pricing  
- Manage WhatsApp groups  
- Publish motivational content  
- Control platform structure dynamically  

---

## 🗂️ Project Structure

```

ReDiscoverU/
│
├── backend/
│   ├── controllers/
│   ├── services/
│   ├── entities/
│   ├── repositories/
│   ├── security/
│   └── config/
│
├── frontend/
│   ├── index.html
│   ├── dashboard/
│   ├── admin/
│   ├── assets/
│   └── js/
│
└── README.md

```

---

## ⚙️ Environment Variables Required

```

DB_USER=
DB_PASS=
JWT_SECRET=

MAIL_USERNAME=
MAIL_PASSWORD=

RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=

ADMIN_EMAIL=
ADMIN_PASSWORD=
ADMIN_NAME=

````

---

## 🚀 Local Setup

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
````

Runs on:
**[http://localhost:8080](http://localhost:8080)**

### Frontend

Open the `frontend` directory and serve via a local server.

---

## 🧠 Engineering Principles Applied

* Clean separation of concerns
* Service-layer architecture
* Modular design
* Secure stateless authentication
* Scalable recurrence modeling
* Minimal over-engineering
* Production-ready configuration handling

---

## 🌟 Future Enhancements

* Mobile Application
* Analytics Dashboard
* AI-driven mentorship insights
* Session Recording Library
* Community engagement metrics

---

## 👨‍💻 Developed By

**Adarsh R**
Full-Stack Developer
System Thinker | Security-Focused Engineer | Product Builder

> *“Growth is intentional. Structure makes it sustainable.”*


