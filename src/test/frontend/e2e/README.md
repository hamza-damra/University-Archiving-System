# E2E Tests

This directory contains end-to-end tests for the University Archiving System frontend.

## Setup

1. Install Playwright:
```bash
npm install
npx playwright install
```

2. Ensure the Spring Boot application is running on `http://localhost:8080`

## Running Tests

Run all E2E tests:
```bash
npm run test:e2e
```

Run with UI mode (interactive):
```bash
npm run test:e2e:ui
```

Run in headed mode (see browser):
```bash
npm run test:e2e:headed
```

## Test Files

- `auth.e2e.test.js` - Authentication flow tests (login, logout, session management)

## Test Data

The tests use the following test users (ensure these exist in your test database):

- **Admin**: admin@example.com / admin123
- **Deanship**: deanship@example.com / deanship123
- **HOD**: hod@example.com / hod123
- **Professor**: professor@example.com / professor123

## Configuration

Playwright configuration is in `playwright.config.js` at the project root.

The configuration automatically starts the Spring Boot server if it's not already running.
