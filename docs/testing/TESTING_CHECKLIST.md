# ✅ Post-Delivery Checklist

Use this checklist to verify the frontend integration and test all features.

## 📦 1. File Verification

- [ ] All files are in `src/main/resources/static/`
- [ ] HTML files: `index.html`, `hod-dashboard.html`, `prof-dashboard.html`
- [ ] CSS file: `css/custom.css`
- [ ] JS files: `js/api.js`, `js/ui.js`, `js/auth.js`, `js/hod.js`, `js/prof.js`
- [ ] Documentation: `README.md` in static folder
- [ ] Root docs: `DELIVERY_SUMMARY.md`, `FRONTEND_QUICKSTART.md`, `ARCHITECTURE.md`
- [ ] Test script: `test-frontend.ps1`

## 🔧 2. Configuration Verification

- [ ] SecurityConfig.java updated to allow static resources
- [ ] CORS configuration enables all required headers
- [ ] API_BASE_URL in `js/api.js` matches backend (default: `http://localhost:8080/api`)
- [ ] Backend endpoints match API calls in JavaScript

## 🚀 3. Run Application

- [ ] Start Spring Boot: `mvn spring-boot:run`
- [ ] No compilation errors
- [ ] Application starts successfully on port 8080
- [ ] Run test script: `.\test-frontend.ps1` (optional)
- [ ] All static files accessible

## 🔐 4. Test Authentication

### Login Page (index.html)
- [ ] Page loads at `http://localhost:8080/index.html`
- [ ] Email field validates format
- [ ] Password field is required
- [ ] Empty form shows validation errors
- [ ] Invalid credentials show error message
- [ ] Valid HOD credentials redirect to `/hod-dashboard.html`
- [ ] Valid Professor credentials redirect to `/prof-dashboard.html`
- [ ] JWT token saved in localStorage
- [ ] User info saved in localStorage

### Logout
- [ ] Logout button works on both dashboards
- [ ] Token cleared from localStorage
- [ ] Redirects to login page

## 👔 5. Test HOD Dashboard

### Page Load
- [ ] HOD dashboard loads after login
- [ ] HOD name displays in header
- [ ] No console errors (F12 → Console tab)

### Professor Management
- [ ] Professors list loads automatically
- [ ] Search box filters professors by name/email
- [ ] "Add Professor" button opens modal
- [ ] Add professor form validates required fields
- [ ] Creating professor shows success toast
- [ ] New professor appears in list
- [ ] Edit button opens modal with existing data
- [ ] Updating professor shows success toast
- [ ] Delete button shows confirmation dialog
- [ ] Deleting professor removes from list
- [ ] Professor dropdown in request form populates

### Document Request Creation
- [ ] All form fields are present
- [ ] Course name is required
- [ ] Document type dropdown has options
- [ ] File extensions field accepts comma-separated values
- [ ] Deadline picker works
- [ ] Professor dropdown is populated
- [ ] Form validates before submission
- [ ] Creating request shows success toast
- [ ] Form clears after successful submission

### Request List
- [ ] Recent requests table loads
- [ ] Table shows: course, type, professor, deadline, status
- [ ] Status badges are color-coded correctly
- [ ] "View Report" button opens modal
- [ ] Report modal shows submission details
- [ ] Report shows correct on-time/late status

## 👨‍🏫 6. Test Professor Dashboard

### Page Load
- [ ] Professor dashboard loads after login
- [ ] Professor name displays in header
- [ ] No console errors

### Request List
- [ ] Assigned requests load automatically
- [ ] Requests display in card grid
- [ ] Each card shows: course, type, deadline, status, allowed extensions
- [ ] Deadline countdown displays correctly
- [ ] Status badges are accurate (Pending, Overdue, Submitted)

### Filters
- [ ] "All" filter shows all requests
- [ ] "Pending" filter shows only unsubmitted
- [ ] "Submitted" filter shows only submitted
- [ ] "Overdue" filter shows only overdue unsubmitted
- [ ] Active filter button is highlighted

### File Upload
- [ ] "Upload Document" button opens modal
- [ ] Click to select file works
- [ ] Drag and drop works (test by dragging file)
- [ ] Selected file name and size display
- [ ] Invalid file extension shows error
- [ ] File > 10MB shows error
- [ ] Upload shows progress bar
- [ ] Successful upload shows toast
- [ ] Modal closes after upload
- [ ] Request status updates to "Submitted"
- [ ] "Replace Document" button appears after submission

### Notifications
- [ ] Notification bell icon visible in header
- [ ] Badge appears when unseen notifications exist
- [ ] Clicking bell opens notifications panel
- [ ] Notifications display with message and date
- [ ] Unseen notifications highlighted (blue background)
- [ ] Clicking notification marks it as seen
- [ ] Badge disappears when all notifications seen
- [ ] Close button hides panel

## 📱 7. Test Responsiveness

### Desktop (>1024px)
- [ ] Login page centered
- [ ] HOD dashboard: 1/3 + 2/3 column layout
- [ ] Professor dashboard: 3-column card grid
- [ ] Tables display properly
- [ ] All buttons visible

### Tablet (768px - 1024px)
- [ ] Login page centered
- [ ] HOD dashboard: columns stack on smaller tablets
- [ ] Professor dashboard: 2-column card grid
- [ ] Tables scroll horizontally

### Mobile (<768px)
- [ ] Login page full width with padding
- [ ] HOD dashboard: single column
- [ ] Professor dashboard: single column card grid
- [ ] Header collapses appropriately
- [ ] Buttons are touch-friendly (min 44x44px)
- [ ] Modals are readable

## ♿ 8. Test Accessibility

### Keyboard Navigation
- [ ] Tab key navigates through all interactive elements
- [ ] Enter key submits forms
- [ ] Escape key closes modals
- [ ] Focus visible on all elements
- [ ] No keyboard traps

### Screen Reader (Optional)
- [ ] Form labels read correctly
- [ ] Error messages announced
- [ ] Button purposes clear
- [ ] Modal titles announced

### Visual
- [ ] Text contrast is readable
- [ ] Focus states visible
- [ ] Color not sole indicator of status
- [ ] Text resizes without breaking layout

## 🌐 9. Test Cross-Browser

- [ ] Chrome/Edge (Chromium) - latest
- [ ] Firefox - latest
- [ ] Safari - latest (if on Mac)
- [ ] Mobile Safari (if testing on iPhone)
- [ ] Chrome Mobile (if testing on Android)

## 🐛 10. Error Handling

### Network Errors
- [ ] Backend stopped → shows "Network error" message
- [ ] Invalid endpoint → shows appropriate error
- [ ] Timeout → shows error message

### Authentication Errors
- [ ] Expired token → redirects to login
- [ ] Invalid token → redirects to login
- [ ] Wrong role access → shows error or redirects

### Validation Errors
- [ ] Server validation errors display in UI
- [ ] Client-side validation prevents submission
- [ ] File validation works before upload

## 📊 11. Performance Check

- [ ] Login page loads < 1 second
- [ ] Dashboard loads < 2 seconds
- [ ] Professor list loads < 1 second
- [ ] Requests list loads < 1 second
- [ ] Search responds instantly (debounced)
- [ ] File upload shows progress
- [ ] No memory leaks (test with DevTools)

## 🔒 12. Security Verification

- [ ] JWT token stored in localStorage
- [ ] Token included in all API requests (check Network tab)
- [ ] 401 responses trigger redirect to login
- [ ] Role-based access enforced (try accessing wrong dashboard)
- [ ] File uploads validated client-side
- [ ] Passwords not visible in browser
- [ ] No sensitive data in console logs

## 📝 13. Documentation Review

- [ ] Read `FRONTEND_QUICKSTART.md`
- [ ] Review `src/main/resources/static/README.md`
- [ ] Understand `ARCHITECTURE.md`
- [ ] Check `DELIVERY_SUMMARY.md` for completeness

## 🎯 14. Optional Enhancements (Future)

- [ ] Add user profile page
- [ ] Implement CSV export for reports
- [ ] Add date range filters for requests
- [ ] Add bulk actions (select multiple)
- [ ] Add document preview
- [ ] Implement real-time WebSocket notifications
- [ ] Add dark mode
- [ ] Add print-friendly views
- [ ] Add email notifications
- [ ] Add audit log viewing

---

## ✅ Sign-Off

Once all items are checked:

**Tested by:** ___________________________  
**Date:** ___________________________  
**Status:** ⬜ Passed  ⬜ Issues Found (see below)  

### Issues Found (if any):
```
1. 
2. 
3. 
```

### Notes:
```



```

---

**Need Help?**
- Check browser console (F12) for errors
- Review Network tab for failed requests
- Check backend logs
- Refer to README.md for troubleshooting
- Review ARCHITECTURE.md for system design

**Ready for Production?**
- [ ] All tests passed
- [ ] No critical issues
- [ ] Documentation reviewed
- [ ] Team trained on features
- [ ] Deployment plan ready
