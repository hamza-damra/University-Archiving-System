# Al-Quds University Archiving System - Frontend

A clean, accessible, and responsive frontend for the Al-Quds University Document Archiving System, built with HTML, Tailwind CSS, and Vanilla JavaScript.

## 📋 Overview

This frontend integrates with the existing Spring Boot backend and provides separate interfaces for:
- **HOD (Head of Department)**: Manage professors and document requests
- **Professors**: View assigned requests and submit documents

## 🛠️ Tech Stack

- **HTML5**: Semantic markup for accessibility
- **Tailwind CSS**: Utility-first CSS framework (via CDN)
- **Vanilla JavaScript (ES6+)**: Modular, async/await, fetch API
- **No build tools required**: Static files served directly by Spring Boot

## 📁 Project Structure

```
src/main/resources/static/
├── index.html              # Login page
├── hod-dashboard.html      # HOD dashboard
├── prof-dashboard.html     # Professor dashboard
├── css/
│   └── custom.css         # Supplementary styles
└── js/
    ├── api.js             # Centralized API service
    ├── ui.js              # UI helper functions (modals, toasts)
    ├── auth.js            # Login page logic
    ├── hod.js             # HOD dashboard logic
    └── prof.js            # Professor dashboard logic
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Spring Boot application running on port 8080 (default)

### Running the Application

1. **Start the Spring Boot backend**:
   ```bash
   mvn spring-boot:run
   ```
   Or run the application from your IDE.

2. **Access the frontend**:
   Open your browser and navigate to:
   ```
   http://localhost:8080/index.html
   ```

3. **Login with credentials**:
   - Use the credentials configured in your backend
   - HOD users will be redirected to `/hod-dashboard.html`
   - Professor users will be redirected to `/prof-dashboard.html`

### CORS Configuration

Ensure your Spring Boot backend has CORS enabled for local development. The backend should allow:
- Origins: `http://localhost:8080`
- Methods: GET, POST, PUT, DELETE
- Headers: Authorization, Content-Type

If testing from a different port, update the `API_BASE_URL` in `js/api.js`:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

## 🎯 Features

### Login Page (`index.html`)
- Email and password authentication
- Client-side validation
- Role-based redirection (HOD/Professor)
- JWT token storage in localStorage
- Loading states and error handling

### HOD Dashboard (`hod-dashboard.html`)
- **Professor Management**:
  - View list of professors
  - Search/filter professors
  - Add new professors
  - Edit existing professors
  - Delete professors (with confirmation)
- **Document Request Management**:
  - Create new document requests
  - Assign requests to professors
  - Set deadlines and allowed file types
  - View recent requests with status
  - Generate reports for individual requests

### Professor Dashboard (`prof-dashboard.html`)
- **Assigned Requests**:
  - View all assigned document requests
  - Filter by status (All, Pending, Submitted, Overdue)
  - See deadline countdowns
  - Upload documents with progress tracking
  - Replace previously submitted documents
  - Client-side file validation (extension, size)
- **Notifications**:
  - Real-time notification badge
  - View unseen notifications
  - Mark notifications as read

## 🔌 API Integration

### Authentication Flow
1. User submits login form
2. POST `/api/auth/login` with credentials
3. Backend returns JWT token and user info
4. Token stored in localStorage
5. All subsequent requests include `Authorization: Bearer {token}` header

### API Endpoints Used

#### Auth
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user info

#### HOD
- `GET /api/hod/professors` - List professors
- `POST /api/hod/professors` - Create professor
- `PUT /api/hod/professors/{id}` - Update professor
- `DELETE /api/hod/professors/{id}` - Delete professor
- `GET /api/hod/requests` - List document requests
- `POST /api/hod/requests` - Create document request
- `GET /api/hod/report/{requestId}` - Get request report

#### Professor
- `GET /api/prof/requests` - List assigned requests
- `POST /api/prof/requests/{id}/submit` - Upload document (multipart)
- `GET /api/prof/notifications` - Get notifications
- `PUT /api/prof/notifications/{id}/seen` - Mark notification as seen

### Customizing API Endpoints

If your backend uses different endpoint paths, update the `js/api.js` file:

```javascript
// Change the base URL
const API_BASE_URL = 'http://your-server:port/api';

// Or modify individual endpoint paths in the exported objects
export const hod = {
    getProfessors: () => apiRequest('/your/custom/path', {
        method: 'GET',
    }),
    // ...
};
```

## ♿ Accessibility

The frontend follows accessibility best practices:

- ✅ Semantic HTML5 elements
- ✅ ARIA attributes for dynamic content
- ✅ Keyboard navigation support
- ✅ Focus management in modals
- ✅ Form labels and error messages
- ✅ Sufficient color contrast ratios
- ✅ Screen reader friendly alerts

### Accessibility Checklist

- [ ] All images have alt text (if applicable)
- [ ] Forms have proper labels
- [ ] Error messages are announced
- [ ] Modals trap focus
- [ ] Keyboard navigation works throughout
- [ ] Color is not the only indicator of status

## 📱 Responsive Design

The interface is fully responsive:
- **Desktop**: Multi-column layouts, tables
- **Tablet**: Adjusted grid columns
- **Mobile**: Stacked layouts, touch-friendly buttons

Breakpoints (Tailwind defaults):
- `sm`: 640px
- `md`: 768px
- `lg`: 1024px
- `xl`: 1280px

## 🧪 Testing

### Manual Testing Checklist

#### Login Flow
- [ ] Valid credentials log in successfully
- [ ] Invalid credentials show error message
- [ ] HOD redirected to HOD dashboard
- [ ] Professor redirected to Professor dashboard
- [ ] Token persists across page refreshes
- [ ] Logout clears token and redirects to login

#### HOD Features
- [ ] Professors list loads correctly
- [ ] Search filters professors
- [ ] Add professor form validates input
- [ ] Edit professor updates data
- [ ] Delete professor requires confirmation
- [ ] Create request form validates fields
- [ ] Request list shows correct statuses
- [ ] Report modal displays submission details

#### Professor Features
- [ ] Assigned requests load correctly
- [ ] Filters work (All, Pending, Submitted, Overdue)
- [ ] File upload validates extension
- [ ] File upload shows progress
- [ ] Large files rejected (>10MB)
- [ ] Notifications display correctly
- [ ] Notification badge updates

#### Cross-browser Testing
- [ ] Chrome/Edge (Chromium)
- [ ] Firefox
- [ ] Safari
- [ ] Mobile browsers

## 🎨 Customization

### Colors
The UI uses a neutral, official color palette. To customize:

Edit `css/custom.css` or use Tailwind classes:
- **Primary**: Blue (#3b82f6) - CTAs, links
- **Success**: Green - On-time submissions
- **Warning**: Yellow/Orange - Late submissions
- **Danger**: Red - Errors, overdue
- **Gray**: Neutral backgrounds and text

### Typography
Tailwind's default font stack is used. To change:

Add to `<head>` in HTML files:
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
  body {
    font-family: 'Inter', sans-serif;
  }
</style>
```

## 🐛 Troubleshooting

### Issue: "Network error - Please check your connection"
- **Cause**: Backend not running or incorrect API_BASE_URL
- **Fix**: Ensure Spring Boot is running on port 8080, or update API_BASE_URL in `js/api.js`

### Issue: "Unauthorized - Please log in again"
- **Cause**: JWT token expired or invalid
- **Fix**: Log out and log in again. Check backend token expiration settings.

### Issue: "CORS error in browser console"
- **Cause**: Backend not configured to allow frontend origin
- **Fix**: Add `@CrossOrigin` annotation in backend controllers or configure global CORS policy

### Issue: File upload fails
- **Cause**: Backend not accepting multipart/form-data or file size limit
- **Fix**: Check backend configuration for `spring.servlet.multipart.max-file-size`

### Issue: Modals or toasts not appearing
- **Cause**: Missing container elements in HTML
- **Fix**: Ensure `<div id="modalsContainer"></div>` and `<div id="toastContainer"></div>` exist

## 📝 License

This project is part of the Al-Quds University Archiving System.

## 👥 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review backend logs for API errors
3. Check browser console for frontend errors
4. Verify network requests in browser DevTools

---

**Al-Quds University — Archiving System**
