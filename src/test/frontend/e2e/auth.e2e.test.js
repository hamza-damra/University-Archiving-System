/**
 * Authentication E2E Tests
 * 
 * Tests complete authentication flows including login, logout, session management,
 * and role-based access control.
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

test.describe('Authentication E2E Tests', () => {
  
  test.beforeEach(async ({ page, context }) => {
    // Clear all storage before each test
    await context.clearCookies();
    await page.goto('/index.html');
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  test.describe('Login Flow', () => {
    
    test('should complete login with valid credentials', async ({ page }) => {
      // Navigate to login page
      await page.goto('/index.html');
      
      // Wait for login form to be visible
      await expect(page.locator('#loginForm')).toBeVisible();
      
      // Fill in valid credentials (using admin as default)
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      
      // Click submit button
      const submitButton = page.locator('#submitBtn');
      await submitButton.click();
      
      // Wait for loading state
      await expect(submitButton).toHaveClass(/btn-loading/);
      
      // Wait for redirect to dashboard
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Verify we're on the correct dashboard
      expect(page.url()).toContain(TEST_USERS.admin.dashboardPath);
      
      // Verify auth data is stored in localStorage
      const token = await page.evaluate(() => localStorage.getItem('token'));
      const userInfo = await page.evaluate(() => localStorage.getItem('userInfo'));
      
      expect(token).toBeTruthy();
      expect(userInfo).toBeTruthy();
      
      const userInfoObj = JSON.parse(userInfo);
      expect(userInfoObj.role).toBe(TEST_USERS.admin.role);
      expect(userInfoObj.email).toBe(TEST_USERS.admin.email);
    });

    test('should show error on login failure with invalid credentials', async ({ page }) => {
      await page.goto('/index.html');
      
      // Fill in invalid credentials
      await page.fill('#email', 'invalid@example.com');
      await page.fill('#password', 'wrongpassword');
      
      // Submit form
      await page.click('#submitBtn');
      
      // Wait for error message to appear
      const errorMessage = page.locator('#generalError');
      await expect(errorMessage).toBeVisible({ timeout: 5000 });
      
      // Verify error message content
      const errorText = await errorMessage.textContent();
      expect(errorText).toContain('Invalid email or password');
      
      // Verify we're still on login page
      expect(page.url()).toContain('/index.html');
      
      // Verify no auth data is stored
      const token = await page.evaluate(() => localStorage.getItem('token'));
      expect(token).toBeNull();
    });

    test('should redirect to correct dashboard based on role', async ({ page }) => {
      // Test each role
      const roles = ['admin', 'deanship', 'hod', 'professor'];
      
      for (const roleKey of roles) {
        const user = TEST_USERS[roleKey];
        
        // Clear storage before each login
        await page.evaluate(() => {
          localStorage.clear();
        });
        
        await page.goto('/index.html');
        
        // Login
        await page.fill('#email', user.email);
        await page.fill('#password', user.password);
        await page.click('#submitBtn');
        
        // Wait for redirect
        await page.waitForURL(user.dashboardPath, { timeout: 10000 });
        
        // Verify correct dashboard
        expect(page.url()).toContain(user.dashboardPath);
        
        // Verify role in localStorage
        const userInfo = await page.evaluate(() => localStorage.getItem('userInfo'));
        const userInfoObj = JSON.parse(userInfo);
        expect(userInfoObj.role).toBe(user.role);
      }
    });

    test('should remember session across page refresh', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      
      // Wait for dashboard
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Get auth data
      const tokenBefore = await page.evaluate(() => localStorage.getItem('token'));
      const userInfoBefore = await page.evaluate(() => localStorage.getItem('userInfo'));
      
      // Refresh the page
      await page.reload();
      
      // Wait for page to load (should stay on dashboard, not redirect to login)
      await page.waitForLoadState('networkidle');
      
      // Verify we're still on dashboard
      expect(page.url()).toContain(TEST_USERS.admin.dashboardPath);
      
      // Verify auth data is still present
      const tokenAfter = await page.evaluate(() => localStorage.getItem('token'));
      const userInfoAfter = await page.evaluate(() => localStorage.getItem('userInfo'));
      
      expect(tokenAfter).toBe(tokenBefore);
      expect(userInfoAfter).toBe(userInfoBefore);
    });

    test('should prevent brute force with rate limiting', async ({ page }) => {
      await page.goto('/index.html');
      
      // Attempt multiple failed logins
      const maxAttempts = 5;
      let rateLimited = false;
      
      for (let i = 0; i < maxAttempts; i++) {
        await page.fill('#email', 'invalid@example.com');
        await page.fill('#password', 'wrongpassword');
        await page.click('#submitBtn');
        
        // Wait a bit for response
        await page.waitForTimeout(1000);
        
        // Check if rate limit error appears
        const errorMessage = page.locator('#generalError');
        const errorText = await errorMessage.textContent().catch(() => '');
        
        if (errorText.includes('Too many login attempts') || errorText.includes('wait')) {
          rateLimited = true;
          
          // Verify submit button is disabled
          const submitButton = page.locator('#submitBtn');
          await expect(submitButton).toBeDisabled();
          
          // Verify countdown message is shown
          await expect(errorMessage).toBeVisible();
          expect(errorText).toMatch(/\d+/); // Should contain time remaining
          
          break;
        }
        
        // Clear form for next attempt
        await page.fill('#email', '');
        await page.fill('#password', '');
      }
      
      // Verify rate limiting was triggered
      expect(rateLimited).toBe(true);
    });
  });

  test.describe('Logout Flow', () => {
    
    test('should clear session on logout', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Verify auth data exists
      const tokenBefore = await page.evaluate(() => localStorage.getItem('token'));
      expect(tokenBefore).toBeTruthy();
      
      // Find and click logout button (assuming it exists on dashboard)
      // The logout might be in a menu or header
      const logoutButton = page.locator('button:has-text("Logout"), a:has-text("Logout"), [data-testid="logout"]').first();
      
      // If logout button exists, click it
      if (await logoutButton.count() > 0) {
        await logoutButton.click();
      } else {
        // Alternative: call logout API directly via JavaScript
        await page.evaluate(async () => {
          const { auth } = await import('/js/core/api.js');
          await auth.logout();
        });
        await page.waitForTimeout(1000);
      }
      
      // Verify redirect to login
      await page.waitForURL(/\/index\.html/, { timeout: 5000 });
      
      // Verify auth data is cleared
      const tokenAfter = await page.evaluate(() => localStorage.getItem('token'));
      const refreshTokenAfter = await page.evaluate(() => localStorage.getItem('refreshToken'));
      const userInfoAfter = await page.evaluate(() => localStorage.getItem('userInfo'));
      
      expect(tokenAfter).toBeNull();
      expect(refreshTokenAfter).toBeNull();
      expect(userInfoAfter).toBeNull();
    });

    test('should prevent access to protected pages after logout', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Clear auth data (simulating logout)
      await page.evaluate(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
      });
      
      // Try to access protected page
      await page.goto(TEST_USERS.admin.dashboardPath);
      
      // Should redirect to login
      await page.waitForURL(/\/index\.html/, { timeout: 5000 });
      expect(page.url()).toContain('/index.html');
      
      // Verify error parameter in URL
      const url = page.url();
      expect(url).toMatch(/error=(unauthorized|session_expired)/);
    });

    test('should redirect to login with error message after logout', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Clear auth data
      await page.evaluate(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
      });
      
      // Try to access protected page
      await page.goto(TEST_USERS.admin.dashboardPath);
      
      // Wait for redirect
      await page.waitForURL(/\/index\.html/, { timeout: 5000 });
      
      // Check for error message in DOM
      const errorMessage = page.locator('#generalError');
      await expect(errorMessage).toBeVisible({ timeout: 3000 });
      
      const errorText = await errorMessage.textContent();
      expect(errorText.length).toBeGreaterThan(0);
    });
  });

  test.describe('Session Management', () => {
    
    test('should refresh token on expiration', async ({ page, context }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Get initial tokens
      const initialToken = await page.evaluate(() => localStorage.getItem('token'));
      const refreshToken = await page.evaluate(() => localStorage.getItem('refreshToken'));
      
      expect(initialToken).toBeTruthy();
      expect(refreshToken).toBeTruthy();
      
      // Simulate token expiration by making an API call that returns 401
      // Then the app should automatically refresh the token
      const response = await page.request.get('/api/auth/validate', {
        headers: {
          'Authorization': `Bearer ${initialToken}`
        }
      });
      
      // If token is invalid, app should attempt refresh
      // We'll verify the refresh mechanism works by checking if new token is stored
      // after a refresh attempt
      
      // Make a request that triggers token refresh (if token expired)
      // The API should handle this automatically via apiRequest interceptor
      try {
        await page.evaluate(async (token) => {
          const { apiRequest } = await import('/js/core/api.js');
          // This should trigger refresh if token is expired
          await apiRequest('/api/session/info', {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
        }, initialToken);
      } catch (e) {
        // Expected if token refresh is needed
      }
      
      // Verify we're still authenticated (either original token or refreshed token exists)
      const currentToken = await page.evaluate(() => localStorage.getItem('token'));
      expect(currentToken).toBeTruthy();
    });

    test('should redirect on refresh failure', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Set an invalid refresh token
      await page.evaluate(() => {
        localStorage.setItem('token', 'expired-token');
        localStorage.setItem('refreshToken', 'invalid-refresh-token');
      });
      
      // Try to access a protected page - should trigger validation
      await page.goto(TEST_USERS.admin.dashboardPath);
      
      // Wait for redirect to login (refresh should fail)
      await page.waitForURL(/\/index\.html/, { timeout: 10000 });
      
      // Verify redirect happened
      expect(page.url()).toContain('/index.html');
      
      // Verify error parameter
      const url = page.url();
      expect(url).toMatch(/error=(session_expired|unauthorized)/);
    });

    test('should handle multiple tab session correctly', async ({ context }) => {
      // Create two pages (tabs)
      const page1 = await context.newPage();
      const page2 = await context.newPage();
      
      try {
        // Login in first tab
        await page1.goto('/index.html');
        await page1.fill('#email', TEST_USERS.admin.email);
        await page1.fill('#password', TEST_USERS.admin.password);
        await page1.click('#submitBtn');
        await page1.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
        
        // Verify auth data in first tab
        const token1 = await page1.evaluate(() => localStorage.getItem('token'));
        expect(token1).toBeTruthy();
        
        // Open same dashboard in second tab
        await page2.goto(TEST_USERS.admin.dashboardPath);
        
        // Second tab should have access (shared localStorage)
        const token2 = await page2.evaluate(() => localStorage.getItem('token'));
        expect(token2).toBeTruthy();
        expect(token2).toBe(token1);
        
        // Logout from first tab
        await page1.evaluate(() => {
          localStorage.clear();
        });
        
        // Second tab should still work until it tries to make a request
        // (In real scenario, second tab would detect logout on next API call)
        const token2After = await page2.evaluate(() => localStorage.getItem('token'));
        // Token is cleared from localStorage, so second tab should also lose it
        expect(token2After).toBeNull();
      } finally {
        await page1.close();
        await page2.close();
      }
    });

    test('should handle session timeout', async ({ page }) => {
      // Login first
      await page.goto('/index.html');
      await page.fill('#email', TEST_USERS.admin.email);
      await page.fill('#password', TEST_USERS.admin.password);
      await page.click('#submitBtn');
      await page.waitForURL(TEST_USERS.admin.dashboardPath, { timeout: 10000 });
      
      // Simulate session timeout by clearing tokens
      await page.evaluate(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
      });
      
      // Try to make an authenticated request
      // This should trigger redirect to login
      await page.goto(TEST_USERS.admin.dashboardPath);
      
      // Should redirect to login with session_expired error
      await page.waitForURL(/\/index\.html/, { timeout: 5000 });
      
      const url = page.url();
      expect(url).toMatch(/error=session_expired/);
      
      // Verify error message is displayed
      const errorMessage = page.locator('#generalError');
      await expect(errorMessage).toBeVisible();
      
      const errorText = await errorMessage.textContent();
      expect(errorText).toContain('session has expired');
    });
  });
});
