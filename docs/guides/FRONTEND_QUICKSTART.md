# 🚀 Quick Start Guide - Frontend Setup

## Overview

The frontend has been successfully integrated into your Spring Boot application. All static files are located in `src/main/resources/static/`.

## ✅ What Was Created

### HTML Pages
- **`index.html`** - Login page with email/password authentication
- **`hod-dashboard.html`** - HOD dashboard for managing professors and requests
- **`prof-dashboard.html`** - Professor dashboard for viewing and submitting documents

### CSS
- **`css/custom.css`** - Custom styles supplementing Tailwind CSS

### JavaScript Modules
- **`js/api.js`** - Centralized API service with JWT token management
- **`js/ui.js`** - UI helpers (modals, toasts, date formatting)
- **`js/auth.js`** - Login page logic
- **`js/hod.js`** - HOD dashboard functionality
- **`js/prof.js`** - Professor dashboard functionality

### Documentation
- **`README.md`** - Comprehensive documentation
- **`test-frontend.ps1`** - Test script for Windows

## 🏃 Running the Application

### Step 1: Start the Backend

```bash
mvn spring-boot:run
```

Or run from your IDE (IntelliJ IDEA, Eclipse, VS Code).

### Step 2: Access the Frontend

Open your browser and navigate to:
```
http://localhost:8080/index.html
```

### Step 3: Login

Use the credentials from your backend database:
- **HOD**: Will be redirected to `/hod-dashboard.html`
- **Professor**: Will be redirected to `/prof-dashboard.html`

## 🧪 Testing the Setup (Windows)

Run the test script:
```powershell
.\test-frontend.ps1
```

This will verify:
- Server is running
- All static files are accessible
- CORS configuration is correct

## 🔧 Configuration

### API Base URL

The frontend is configured to use `http://localhost:8080/api` by default.

If your backend runs on a different port or host, update `js/api.js`:

```javascript
const API_BASE_URL = 'http://your-host:port/api';
```

### Security Configuration

The `SecurityConfig.java` has been updated to allow access to:
- Static HTML pages (`/`, `/index.html`, `/hod-dashboard.html`, `/prof-dashboard.html`)
- Static resources (`/css/**`, `/js/**`)
- API endpoints with appropriate role-based access control

## 📋 Features by Role

### HOD Dashboard Features
✅ View, add, edit, and delete professors  
✅ Search and filter professor list  
✅ Create document requests with:
  - Course name
  - Document type
  - Allowed file extensions
  - Deadline
  - Assigned professor  
✅ View recent requests with submission status  
✅ Generate detailed reports per request  

### Professor Dashboard Features
✅ View all assigned document requests  
✅ Filter requests by status (All, Pending, Submitted, Overdue)  
✅ Upload documents with:
  - File type validation
  - File size validation (10MB max)
  - Upload progress tracking  
✅ Replace previously submitted documents  
✅ View notifications in real-time  
✅ Mark notifications as read  

## 🎨 UI/UX Features

- **Responsive Design**: Works on desktop, tablet, and mobile
- **Accessibility**: Semantic HTML, ARIA labels, keyboard navigation
- **Loading States**: Skeletons, spinners, progress bars
- **Error Handling**: Inline validation, toast notifications
- **Modals**: For forms and confirmations
- **Real-time Updates**: Notification polling every 30 seconds

## 🐛 Common Issues & Solutions

### Issue: Login redirects to error page
**Solution**: Check backend logs for authentication errors. Verify user exists in database.

### Issue: "Network error" on login
**Solution**: 
1. Ensure Spring Boot is running
2. Check `API_BASE_URL` in `js/api.js`
3. Verify CORS configuration in `SecurityConfig.java`

### Issue: Static files not loading
**Solution**:
1. Verify files are in `src/main/resources/static/`
2. Restart Spring Boot application
3. Clear browser cache (Ctrl+Shift+Del)

### Issue: File upload fails
**Solution**:
1. Check file size (<10MB)
2. Verify file extension matches allowed types
3. Check backend logs for multipart configuration errors

### Issue: Blank page after login
**Solution**:
1. Open browser DevTools (F12)
2. Check Console tab for JavaScript errors
3. Check Network tab for failed API requests
4. Verify JWT token is stored in localStorage

## 📱 Browser Compatibility

Tested and working on:
- ✅ Chrome/Edge (Latest)
- ✅ Firefox (Latest)
- ✅ Safari (Latest)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

## 🔒 Security Notes

- JWT tokens are stored in `localStorage`
- All API requests include `Authorization: Bearer {token}` header
- 401 responses automatically redirect to login
- Tokens are cleared on logout
- Role-based access control enforced on backend

## 📚 Next Steps

1. **Test all features** with different user roles
2. **Customize colors/branding** in `css/custom.css`
3. **Update API endpoints** if needed in `js/api.js`
4. **Add more features** by extending the JavaScript modules
5. **Deploy to production** with proper security measures

## 📖 Full Documentation

For detailed information, see: **`src/main/resources/static/README.md`**

---

## 💡 Tips

- Use browser DevTools (F12) to debug issues
- Check Console for JavaScript errors
- Check Network tab for API request/response details
- Inspect localStorage to see stored JWT token
- Use the test script (`test-frontend.ps1`) to verify setup

---

**Need help?** Check the README.md or review backend logs for errors.

**Al-Quds University — Archiving System**
