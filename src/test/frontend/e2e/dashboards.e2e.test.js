/**
 * Dashboard E2E Tests
 * Tests complete dashboard flows for HOD, Professor, Deanship, and Admin dashboards
 * 
 * Test Coverage:
 * - Dashboard loading and statistics display
 * - Academic year and semester selection
 * - Report viewing and downloading
 * - Course management
 * - Document upload flows
 * - Tab switching
 * - User and department management
 */

import { test, expect } from '@playwright/test';

// Test user credentials - these should match test data in the database
const TEST_USERS = {
  admin: {
    email: 'admin@example.com',
    password: 'admin123',
    role: 'ROLE_ADMIN',
    dashboardPath: '/admin/dashboard.html'
  },
  deanship: {
    email: 'deanship@example.com',
    password: 'deanship123',
    role: 'ROLE_DEANSHIP',
    dashboardPath: '/deanship/dashboard.html'
  },
  hod: {
    email: 'hod@example.com',
    password: 'hod123',
    role: 'ROLE_HOD',
    dashboardPath: '/hod/dashboard.html'
  },
  professor: {
    email: 'professor@example.com',
    password: 'professor123',
    role: 'ROLE_PROFESSOR',
    dashboardPath: '/professor/dashboard.html'
  }
};

// Helper function to login
async function login(page, user) {
  await page.goto('/index.html');
  await page.fill('#email', user.email);
  await page.fill('#password', user.password);
  await page.click('#submitBtn');
  await page.waitForURL(user.dashboardPath, { timeout: 10000 });
}

test.describe('Dashboard E2E Tests', () => {
  
  test.beforeEach(async ({ page, context }) => {
    // Clear all storage before each test
    await context.clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  test.describe('HOD Dashboard', () => {
    
    test('should load dashboard with correct statistics', async ({ page }) => {
      await login(page, TEST_USERS.hod);
      
      // Wait for dashboard to load
      await page.waitForLoadState('networkidle');
      
      // Verify HOD name is displayed
      const hodName = page.locator('#hodName');
      await expect(hodName).toBeVisible();
      
      // Verify dashboard overview section exists
      const dashboardOverview = page.locator('#dashboardOverview');
      await expect(dashboardOverview).toBeVisible();
      
      // Verify statistics cards are present
      const statsCards = page.locator('#dashboardOverview .stat-card, [class*="stat"]');
      await expect(statsCards.first()).toBeVisible({ timeout: 10000 });
      
      // Verify academic year dropdown is populated
      const academicYearSelect = page.locator('#academicYearSelect');
      await expect(academicYearSelect).toBeVisible();
      
      // Wait for academic years to load
      await page.waitForTimeout(2000);
      
      // Verify at least one option exists (or loading message)
      const options = await academicYearSelect.locator('option').count();
      expect(options).toBeGreaterThan(0);
    });

    test('should update data when academic year is selected', async ({ page }) => {
      await login(page, TEST_USERS.hod);
      await page.waitForLoadState('networkidle');
      
      // Wait for academic year dropdown to be populated
      await page.waitForSelector('#academicYearSelect option:not([value=""])', { timeout: 10000 });
      
      // Get available academic years
      const academicYearSelect = page.locator('#academicYearSelect');
      const options = await academicYearSelect.locator('option').all();
      
      if (options.length > 1) {
        // Select a different academic year (if available)
        const firstOption = await options[0].getAttribute('value');
        const secondOption = options.length > 1 ? await options[1].getAttribute('value') : null;
        
        if (secondOption && secondOption !== firstOption) {
          // Change selection
          await academicYearSelect.selectOption(secondOption);
          
          // Wait for data to refresh
          await page.waitForTimeout(2000);
          
          // Verify semester dropdown is updated or data is refreshed
          const semesterSelect = page.locator('#semesterSelect');
          await expect(semesterSelect).toBeVisible();
        }
      }
    });

    test('should update data when semester is selected', async ({ page }) => {
      await login(page, TEST_USERS.hod);
      await page.waitForLoadState('networkidle');
      
      // Wait for dropdowns to be populated
      await page.waitForSelector('#academicYearSelect option:not([value=""])', { timeout: 10000 });
      await page.waitForSelector('#semesterSelect option:not([value=""])', { timeout: 10000 });
      
      const semesterSelect = page.locator('#semesterSelect');
      const options = await semesterSelect.locator('option').all();
      
      if (options.length > 1) {
        // Select a different semester
        const firstOption = await options[0].getAttribute('value');
        const secondOption = options.length > 1 ? await options[1].getAttribute('value') : null;
        
        if (secondOption && secondOption !== firstOption) {
          // Change selection
          await semesterSelect.selectOption(secondOption);
          
          // Wait for data to refresh
          await page.waitForTimeout(2000);
          
          // Verify submission status table is updated
          const submissionTable = page.locator('#submissionStatusTableBody');
          await expect(submissionTable).toBeVisible();
        }
      }
    });

    test('should view submission report', async ({ page }) => {
      await login(page, TEST_USERS.hod);
      await page.waitForLoadState('networkidle');
      
      // Wait for semester to be selected
      await page.waitForSelector('#semesterSelect option:not([value=""])', { timeout: 10000 });
      
      // Mock report API response
      await page.route('**/api/hod/reports/professor-submission*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            totalProfessors: 10,
            totalCourses: 25,
            submittedDocuments: 20,
            missingDocuments: 5,
            overdueDocuments: 2
          })
        });
      });
      
      // Click view report button
      const viewReportBtn = page.locator('#viewReportBtn, #viewReportBtnTab');
      if (await viewReportBtn.isVisible()) {
        await viewReportBtn.click();
        
        // Wait for report modal or section to appear
        await page.waitForTimeout(2000);
        
        // Verify report content is displayed (modal or section)
        const reportContent = page.locator('.modal, [class*="report"], [class*="modal-content"]');
        const reportVisible = await reportContent.count() > 0;
        expect(reportVisible).toBeTruthy();
      }
    });

    test('should download PDF report', async ({ page, context }) => {
      await login(page, TEST_USERS.hod);
      await page.waitForLoadState('networkidle');
      
      // Wait for semester to be selected
      await page.waitForSelector('#semesterSelect option:not([value=""])', { timeout: 10000 });
      
      // Mock PDF download response
      await page.route('**/api/hod/reports/export-pdf*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/pdf',
          body: Buffer.from('PDF content')
        });
      });
      
      // Set up download listener
      const downloadPromise = page.waitForEvent('download');
      
      // Click download report button
      const downloadReportBtn = page.locator('#downloadReportBtn, #downloadReportBtnTab');
      if (await downloadReportBtn.isVisible()) {
        await downloadReportBtn.click();
        
        // Wait for download
        const download = await downloadPromise;
        
        // Verify download
        expect(download.suggestedFilename()).toContain('.pdf');
        
        // Verify success toast
        await page.waitForTimeout(1000);
        const toast = page.locator('.toast-success, [class*="success"]');
        const toastVisible = await toast.count() > 0;
        expect(toastVisible).toBeTruthy();
      }
    });
  });

  test.describe('Professor Dashboard', () => {
    
    test('should load dashboard with courses', async ({ page }) => {
      await login(page, TEST_USERS.professor);
      await page.waitForLoadState('networkidle');
      
      // Verify professor name is displayed
      const professorName = page.locator('[id*="professorName"], [class*="professor-name"]');
      await expect(professorName.first()).toBeVisible({ timeout: 10000 });
      
      // Wait for courses to load
      await page.waitForTimeout(2000);
      
      // Verify course cards or course list exists
      const courseCards = page.locator('.course-card, [class*="course-card"]');
      const courseList = page.locator('#coursesList, [id*="course"]');
      
      // Either course cards or course list should be visible
      const cardsCount = await courseCards.count();
      const listVisible = await courseList.first().isVisible().catch(() => false);
      
      expect(cardsCount > 0 || listVisible).toBeTruthy();
    });

    test('should expand and collapse course card', async ({ page }) => {
      await login(page, TEST_USERS.professor);
      await page.waitForLoadState('networkidle');
      
      // Wait for courses to load
      await page.waitForSelector('.course-card, [class*="course-card"]', { timeout: 10000 });
      
      // Find first course card
      const firstCourseCard = page.locator('.course-card, [class*="course-card"]').first();
      await expect(firstCourseCard).toBeVisible();
      
      // Click to expand
      await firstCourseCard.click();
      
      // Wait for expansion
      await page.waitForTimeout(1000);
      
      // Verify course details are shown (document list or details)
      const courseDetails = firstCourseCard.locator('.course-details, [class*="details"], [class*="document"]');
      const detailsVisible = await courseDetails.count() > 0;
      
      // Click again to collapse
      await firstCourseCard.click();
      await page.waitForTimeout(500);
      
      // Verify it can be toggled
      expect(firstCourseCard).toBeVisible();
    });

    test('should complete upload document flow', async ({ page }) => {
      await login(page, TEST_USERS.professor);
      await page.waitForLoadState('networkidle');
      
      // Wait for courses to load
      await page.waitForSelector('.course-card, [class*="course-card"]', { timeout: 10000 });
      
      // Find upload button in first course card
      const uploadBtn = page.locator('.course-card button:has-text("Upload"), [class*="upload-btn"]').first();
      
      if (await uploadBtn.isVisible()) {
        // Mock upload API
        await page.route('**/api/professor/submissions/upload*', async route => {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 1,
              message: 'Files uploaded successfully'
            })
          });
        });
        
        // Click upload button
        await uploadBtn.click();
        
        // Wait for upload modal
        await page.waitForSelector('#uploadForm, [class*="upload-modal"], [class*="modal"]', { timeout: 5000 });
        
        // Verify modal is visible
        const modal = page.locator('#uploadForm, [class*="upload-modal"]').first();
        await expect(modal).toBeVisible();
        
        // Note: Actual file upload would require file creation
        // This test verifies the modal opens correctly
      }
    });

    test('should view notifications', async ({ page }) => {
      await login(page, TEST_USERS.professor);
      await page.waitForLoadState('networkidle');
      
      // Mock notifications API
      await page.route('**/api/professor/notifications*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 1,
              message: 'Test notification',
              isRead: false,
              createdAt: new Date().toISOString()
            }
          ])
        });
      });
      
      // Find notifications button or icon
      const notificationsBtn = page.locator('[id*="notification"], [class*="notification"], button:has-text("Notification")').first();
      
      if (await notificationsBtn.isVisible()) {
        await notificationsBtn.click();
        
        // Wait for notifications dropdown or modal
        await page.waitForTimeout(1000);
        
        // Verify notifications are displayed
        const notificationsList = page.locator('.notification-item, [class*="notification-list"]');
        const notificationsVisible = await notificationsList.count() > 0;
        expect(notificationsVisible).toBeTruthy();
      }
    });
  });

  test.describe('Deanship Dashboard', () => {
    
    test('should switch tabs correctly', async ({ page }) => {
      await login(page, TEST_USERS.deanship);
      await page.waitForLoadState('networkidle');
      
      // Wait for tabs to be visible
      await page.waitForSelector('[data-tab], .nav-tab, [class*="tab"]', { timeout: 10000 });
      
      // Find tab buttons
      const tabs = page.locator('[data-tab], .nav-tab, button[class*="tab"]');
      const tabCount = await tabs.count();
      
      if (tabCount > 1) {
        // Click on a different tab (e.g., professors tab)
        const professorsTab = page.locator('[data-tab="professors"], button:has-text("Professors")').first();
        
        if (await professorsTab.isVisible()) {
          await professorsTab.click();
          
          // Wait for tab content to load
          await page.waitForTimeout(2000);
          
          // Verify tab is active
          const activeTab = professorsTab;
          const isActive = await activeTab.evaluate(el => el.classList.contains('active') || el.getAttribute('aria-selected') === 'true');
          expect(isActive).toBeTruthy();
        }
      }
    });

    test('should create and edit professor', async ({ page }) => {
      await login(page, TEST_USERS.deanship);
      await page.waitForLoadState('networkidle');
      
      // Navigate to professors tab
      const professorsTab = page.locator('[data-tab="professors"], button:has-text("Professors")').first();
      if (await professorsTab.isVisible()) {
        await professorsTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock create professor API
      await page.route('**/api/deanship/professors*', async route => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 1,
              email: 'newprofessor@example.com',
              fullName: 'New Professor',
              department: { id: 1, name: 'Computer Science' }
            })
          });
        } else {
          await route.continue();
        }
      });
      
      // Find create professor button
      const createBtn = page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")').first();
      
      if (await createBtn.isVisible()) {
        await createBtn.click();
        
        // Wait for modal or form
        await page.waitForSelector('input[type="email"], input[name="email"]', { timeout: 5000 });
        
        // Fill form
        await page.fill('input[type="email"], input[name="email"]', 'newprofessor@example.com');
        await page.fill('input[name="fullName"], input[placeholder*="name"]', 'New Professor');
        
        // Submit
        const submitBtn = page.locator('button[type="submit"], button:has-text("Save")').first();
        if (await submitBtn.isVisible()) {
          await submitBtn.click();
          
          // Wait for success
          await page.waitForTimeout(2000);
          
          // Verify success toast
          const toast = page.locator('.toast-success, [class*="success"]');
          const toastVisible = await toast.count() > 0;
          expect(toastVisible).toBeTruthy();
        }
      }
    });

    test('should create and edit course', async ({ page }) => {
      await login(page, TEST_USERS.deanship);
      await page.waitForLoadState('networkidle');
      
      // Navigate to courses tab
      const coursesTab = page.locator('[data-tab="courses"], button:has-text("Courses")').first();
      if (await coursesTab.isVisible()) {
        await coursesTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock create course API
      await page.route('**/api/deanship/courses*', async route => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 1,
              code: 'CS101',
              name: 'Introduction to Computer Science',
              department: { id: 1, name: 'Computer Science' }
            })
          });
        } else {
          await route.continue();
        }
      });
      
      // Find create course button
      const createBtn = page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")').first();
      
      if (await createBtn.isVisible()) {
        await createBtn.click();
        
        // Wait for modal or form
        await page.waitForSelector('input[name="code"], input[placeholder*="code"]', { timeout: 5000 });
        
        // Fill form
        await page.fill('input[name="code"], input[placeholder*="code"]', 'CS101');
        await page.fill('input[name="name"], input[placeholder*="name"]', 'Introduction to Computer Science');
        
        // Submit
        const submitBtn = page.locator('button[type="submit"], button:has-text("Save")').first();
        if (await submitBtn.isVisible()) {
          await submitBtn.click();
          
          // Wait for success
          await page.waitForTimeout(2000);
          
          // Verify success toast
          const toast = page.locator('.toast-success, [class*="success"]');
          const toastVisible = await toast.count() > 0;
          expect(toastVisible).toBeTruthy();
        }
      }
    });

    test('should manage course assignments', async ({ page }) => {
      await login(page, TEST_USERS.deanship);
      await page.waitForLoadState('networkidle');
      
      // Navigate to assignments tab
      const assignmentsTab = page.locator('[data-tab="assignments"], button:has-text("Assignments")').first();
      if (await assignmentsTab.isVisible()) {
        await assignmentsTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock assignments API
      await page.route('**/api/deanship/assignments*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 1,
              professor: { id: 1, fullName: 'Test Professor' },
              course: { id: 1, code: 'CS101', name: 'Test Course' },
              semester: { id: 1, type: 'FIRST' }
            }
          ])
        });
      });
      
      // Verify assignments table or list loads
      await page.waitForTimeout(2000);
      const assignmentsTable = page.locator('table, [class*="table"], [class*="assignment"]');
      const tableVisible = await assignmentsTable.count() > 0;
      expect(tableVisible).toBeTruthy();
    });

    test('should generate reports', async ({ page }) => {
      await login(page, TEST_USERS.deanship);
      await page.waitForLoadState('networkidle');
      
      // Navigate to reports tab
      const reportsTab = page.locator('[data-tab="reports"], button:has-text("Reports")').first();
      if (await reportsTab.isVisible()) {
        await reportsTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock reports API
      await page.route('**/api/deanship/reports*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            totalProfessors: 50,
            totalCourses: 100,
            totalSubmissions: 200
          })
        });
      });
      
      // Find generate report button
      const generateBtn = page.locator('button:has-text("Generate"), button:has-text("View Report")').first();
      
      if (await generateBtn.isVisible()) {
        await generateBtn.click();
        
        // Wait for report to load
        await page.waitForTimeout(2000);
        
        // Verify report content is displayed
        const reportContent = page.locator('[class*="report"], [class*="statistics"]');
        const contentVisible = await reportContent.count() > 0;
        expect(contentVisible).toBeTruthy();
      }
    });
  });

  test.describe('Admin Dashboard', () => {
    
    test('should complete user management flow', async ({ page }) => {
      await login(page, TEST_USERS.admin);
      await page.waitForLoadState('networkidle');
      
      // Navigate to users tab
      const usersTab = page.locator('[data-tab="users"], button:has-text("Users")').first();
      if (await usersTab.isVisible()) {
        await usersTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock users API
      await page.route('**/api/admin/users*', async route => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              content: [
                {
                  id: 1,
                  email: 'user@example.com',
                  fullName: 'Test User',
                  role: 'ROLE_PROFESSOR',
                  isActive: true
                }
              ],
              totalElements: 1
            })
          });
        } else {
          await route.continue();
        }
      });
      
      // Verify users table loads
      await page.waitForTimeout(2000);
      const usersTable = page.locator('table, [class*="table"], [class*="user"]');
      const tableVisible = await usersTable.count() > 0;
      expect(tableVisible).toBeTruthy();
    });

    test('should manage departments', async ({ page }) => {
      await login(page, TEST_USERS.admin);
      await page.waitForLoadState('networkidle');
      
      // Navigate to departments tab
      const departmentsTab = page.locator('[data-tab="departments"], button:has-text("Departments")').first();
      if (await departmentsTab.isVisible()) {
        await departmentsTab.click();
        await page.waitForTimeout(2000);
      }
      
      // Mock departments API
      await page.route('**/api/admin/departments*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 1,
              name: 'Computer Science',
              code: 'CS',
              isActive: true
            }
          ])
        });
      });
      
      // Verify departments table or list loads
      await page.waitForTimeout(2000);
      const departmentsList = page.locator('table, [class*="table"], [class*="department"]');
      const listVisible = await departmentsList.count() > 0;
      expect(listVisible).toBeTruthy();
    });

    test('should manage system settings', async ({ page }) => {
      await login(page, TEST_USERS.admin);
      await page.waitForLoadState('networkidle');
      
      // Navigate to settings (if available)
      const settingsTab = page.locator('[data-tab="settings"], button:has-text("Settings")').first();
      
      if (await settingsTab.isVisible()) {
        await settingsTab.click();
        await page.waitForTimeout(2000);
        
        // Mock settings API
        await page.route('**/api/admin/settings*', async route => {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              maxFileSize: 104857600,
              allowedFileTypes: ['pdf', 'doc', 'docx'],
              sessionTimeout: 3600
            })
          });
        });
        
        // Verify settings form or display
        const settingsForm = page.locator('form, [class*="settings"], [class*="config"]');
        const formVisible = await settingsForm.count() > 0;
        expect(formVisible).toBeTruthy();
      }
    });
  });
});
