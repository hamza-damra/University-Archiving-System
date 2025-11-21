# Error Handling Quick Reference

## Overview

This guide provides a quick reference for the error handling and loading states implementation across all deanship page modules.

## Error Handling Methods

### handleApiError(error, action)

Handles API-specific errors with appropriate user feedback.

**Parameters:**
- `error`: The error object from the API call
- `action`: String describing the action being performed (e.g., "load professors")

**Error Types Handled:**

| Status Code | User Message | Action |
|-------------|--------------|--------|
| 401 | "Your session has expired. Please log in again." | Redirect to login after 2s |
| 403 | "You do not have permission to perform this action." | Show toast |
| 500 | "Server error. Please try again later." | Show toast |
| NetworkError | "Network error. Please check your connection and try again." | Show toast |
| Other | Custom or default message | Show toast |

**Usage Example:**
```javascript
try {
    const data = await apiRequest('/api/endpoint');
    // Process data
} catch (error) {
    this.handleApiError(error, 'load data');
}
```

### handleError(message, error)

Handles general errors with custom messages.

**Parameters:**
- `message`: Custom error message to display to user
- `error`: The error object for logging

**Usage Example:**
```javascript
try {
    // Some operation
} catch (error) {
    this.handleError('Failed to initialize page. Please refresh.', error);
}
```

## Loading States

### Dashboard Page

**Methods:**
- `showPageLoading(show)`: Page-level loading state
- `showCardsLoading(show)`: Card-level loading state

**Usage:**
```javascript
this.showPageLoading(true);
await this.loadData();
this.showPageLoading(false);
```

### Data Management Pages

**Method:**
- `showLoading(show)`: Shows/hides loading spinner

**Usage:**
```javascript
this.showLoading(true);
const data = await apiRequest('/api/endpoint');
this.showLoading(false);
```

### File Explorer Page

**Method:**
- `showLoading(show)`: Opacity-based loading state

**Usage:**
```javascript
this.showLoading(true);
await this.fileExplorer.loadRoot(yearId, semesterId);
this.showLoading(false);
```

## Empty State Handling

**Method:**
- `showEmptyState(show)`: Shows/hides empty state message

**When to Use:**
- No data available after successful API call
- Academic context not selected (for context-dependent pages)

**Usage:**
```javascript
if (this.data.length === 0) {
    this.showEmptyState(true);
} else {
    this.showEmptyState(false);
    this.renderTable();
}
```

## Toast Notifications

**Function:** `showToast(message, type, duration)`

**Parameters:**
- `message`: Message to display
- `type`: 'success', 'error', 'warning', or 'info'
- `duration`: Duration in milliseconds (default: 5000)

**Usage Examples:**
```javascript
// Success
showToast('Professor created successfully', 'success');

// Error
showToast('Failed to load data', 'error');

// Warning
showToast('Please select an academic year first', 'warning');

// Info
showToast('Loading data...', 'info');
```

## Console Logging

**Pattern:**
```javascript
console.error('[ModuleName] Error description:', error);
```

**Examples:**
```javascript
console.error('[Dashboard] Failed to load stats:', error);
console.error('[Professors] Error creating professor:', error);
console.log('[AcademicYears] Initialized successfully');
```

## Common Patterns

### Pattern 1: Data Loading with Error Handling

```javascript
async loadData() {
    try {
        this.showLoading(true);
        
        const data = await apiRequest('/api/endpoint');
        
        this.showLoading(false);
        this.renderData(data);
        
    } catch (error) {
        console.error('[Module] Failed to load data:', error);
        this.showLoading(false);
        this.handleApiError(error, 'load data');
        this.showEmptyState(true);
    }
}
```

### Pattern 2: CRUD Operation with Error Handling

```javascript
async createItem(closeModal) {
    try {
        await apiRequest('/api/items', {
            method: 'POST',
            body: JSON.stringify(itemData)
        });
        
        showToast('Item created successfully', 'success');
        closeModal();
        await this.loadItems();
        
    } catch (error) {
        console.error('[Module] Error creating item:', error);
        this.handleApiError(error, 'create item');
    }
}
```

### Pattern 3: Initialization with Error Handling

```javascript
async initialize() {
    try {
        this.showPageLoading(true);
        
        await this.layout.initialize();
        await this.loadData();
        this.setupEventListeners();
        
        this.showPageLoading(false);
        console.log('[Module] Initialized successfully');
        
    } catch (error) {
        console.error('[Module] Initialization error:', error);
        this.showPageLoading(false);
        this.handleError('Failed to initialize page. Please refresh.', error);
    }
}
```

### Pattern 4: Context-Dependent Loading

```javascript
async handleContextChange(context) {
    if (!context.semesterId) {
        this.showContextMessage(true);
        this.showContent(false);
        return;
    }
    
    try {
        this.showContextMessage(false);
        this.showLoading(true);
        
        await this.loadData(context.semesterId);
        
        this.showLoading(false);
        this.showContent(true);
        
    } catch (error) {
        console.error('[Module] Failed to load data:', error);
        this.showLoading(false);
        this.handleApiError(error, 'load data');
    }
}
```

## Testing Checklist

### Network Errors
- [ ] Disconnect network during data load
- [ ] Verify toast notification appears
- [ ] Verify console logs error
- [ ] Verify page remains functional

### Authentication Errors
- [ ] Clear auth token
- [ ] Try to access page
- [ ] Verify redirect to login
- [ ] Verify toast notification

### Loading States
- [ ] Verify loading indicator appears
- [ ] Verify UI is disabled during loading
- [ ] Verify loading clears after completion

### Empty States
- [ ] Clear academic context
- [ ] Verify empty state message
- [ ] Verify helpful guidance text

### Server Errors
- [ ] Trigger server error
- [ ] Verify error message
- [ ] Verify page remains functional

## Best Practices

1. **Always wrap API calls in try-catch blocks**
2. **Show loading state before async operations**
3. **Hide loading state in both success and error cases**
4. **Use descriptive action strings in handleApiError**
5. **Log errors to console for debugging**
6. **Display user-friendly messages in toasts**
7. **Handle empty states gracefully**
8. **Provide context-specific error messages**
9. **Don't overwhelm users with multiple error toasts**
10. **Test all error scenarios manually**

## Module-Specific Notes

### Dashboard
- Silent failures for stats loading (no toast)
- Cards show default values on error
- Page-level and card-level loading states

### Academic Years
- Reloads layout academic years after CRUD operations
- Validates year ranges before submission

### Professors & Courses
- Supports search and filter functionality
- Maintains filter state on error

### Course Assignments
- Requires academic context
- Shows context message when not selected

### Reports
- Button loading state during report fetch
- Hides report on context change

### File Explorer
- Opacity-based loading state
- Integrates with existing FileExplorer component

## Support

For questions or issues with error handling:
1. Check console logs for detailed error information
2. Verify network connectivity
3. Check authentication status
4. Review this guide for proper usage patterns
5. Test in isolation to identify the specific error

## Updates

This error handling implementation can be extended by:
- Adding more specific error types
- Implementing retry logic
- Adding error tracking/monitoring
- Creating custom error classes
- Implementing offline support
