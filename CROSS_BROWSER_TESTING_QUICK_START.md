# Cross-Browser Testing - Quick Start Guide

## Quick Access

### Testing Tool
Open in each browser: **`test-cross-browser-verification.html`**

### Full Guide
See: **`.kiro/specs/dean-file-explorer-filter-fix/cross-browser-testing-guide.md`**

## 3-Step Testing Process

### Step 1: Automated Tests (2 minutes per browser)
1. Open `test-cross-browser-verification.html`
2. Click "Run All Tests"
3. Verify all 10 tests pass
4. Click "Export Results"

### Step 2: Manual Tests (10 minutes per browser)
1. Login to Dean Dashboard: http://localhost:8080
2. Navigate to File Explorer page
3. Follow the 8 test scenarios in the checklist
4. Check off each scenario as completed
5. Click "Generate Manual Test Report"

### Step 3: Quick Console Check (30 seconds)
Open DevTools Console on Dean File Explorer page and paste:
```javascript
console.assert(document.querySelector('#fileExplorerTree') === null, 'No tree panel');
console.assert(document.querySelector('.file-explorer .grid').classList.contains('grid-cols-1'), 'Single column');
console.assert(window.fileExplorerInstance?.options.hideTree === true, 'hideTree enabled');
console.log('✓ All checks passed');
```

## 8 Test Scenarios (Quick Checklist)

- [ ] 1. **Academic Year Filter**: Change year → File Explorer clears
- [ ] 2. **Semester Filter**: Change semester → New data loads
- [ ] 3. **Tree View**: No tree panel visible, single-column layout
- [ ] 4. **Folder Navigation**: Click folder card → Navigate correctly
- [ ] 5. **Breadcrumbs**: Click breadcrumb → Navigate to that level
- [ ] 6. **Loading**: Loading indicator appears during data load
- [ ] 7. **Empty State**: Context message when no filters selected
- [ ] 8. **Errors**: Error message on API failure

## Browsers to Test

- [ ] **Chrome** (latest version)
- [ ] **Firefox** (latest version)
- [ ] **Edge** (latest version)

## Expected Results

✓ All automated tests pass
✓ All manual scenarios work correctly
✓ No console errors
✓ Consistent layout across browsers
✓ Fast performance (< 1 second)

## If Issues Found

1. Document the issue in test report
2. Note which browser(s) affected
3. Include screenshots if helpful
4. Report to development team

## Files

- **Testing Tool**: `test-cross-browser-verification.html`
- **Full Guide**: `.kiro/specs/dean-file-explorer-filter-fix/cross-browser-testing-guide.md`
- **Task Summary**: `.kiro/specs/dean-file-explorer-filter-fix/task-10-completion-summary.md`

---

**Total Testing Time**: ~15 minutes per browser = ~45 minutes for all three browsers
