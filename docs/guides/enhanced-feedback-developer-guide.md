# Enhanced Feedback System - Developer Guide

## Overview
This guide covers the enhanced feedback system implemented in Task 8, including toast notifications, tooltips, and loading indicators.

## Table of Contents
1. [Enhanced Toast Notifications](#enhanced-toast-notifications)
2. [Tooltips](#tooltips)
3. [Loading Indicators](#loading-indicators)
4. [Best Practices](#best-practices)
5. [Examples](#examples)

---

## Enhanced Toast Notifications

### Basic Usage

```javascript
// Import (if using modules)
import { EnhancedToast } from './deanship-feedback.js';

// Show a basic toast
EnhancedToast.show('Operation successful!', 'success');
EnhancedToast.show('An error occurred', 'error');
EnhancedToast.show('Please review your changes', 'warning');
EnhancedToast.show('New updates available', 'info');
```

### Toast Types
- `success` - Green, checkmark icon
- `error` - Red, X icon
- `warning` - Yellow, warning icon
- `info` - Blue, info icon

### Advanced Options

```javascript
EnhancedToast.show('File uploaded successfully', 'success', {
    duration: 8000,              // Duration in milliseconds (default: 5000)
    action: 'view',              // Action identifier
    actionLabel: 'View File',    // Button text
    onAction: () => {            // Callback function
        navigateToFile();
    },
    pauseOnHover: true          // Pause countdown on hover (default: true)
});
```

### Toast Management

```javascript
// Clear all toasts
EnhancedToast.clearAll();

// Get active toast count
const count = EnhancedToast.toasts.length;
```

### Features
- **Stacking**: Maximum 5 toasts shown at once
- **Progress Bar**: Visual countdown at bottom of toast
- **Pause on Hover**: Auto-dismiss pauses when hovering
- **Action Buttons**: Optional action buttons with callbacks
- **Smooth Animations**: Slide-in from right with fade

---

## Tooltips

### Basic Usage

```html
<!-- Using data-tooltip attribute -->
<button data-tooltip="This is a helpful tooltip">
    Action Button
</button>

<!-- Icon-only buttons should always have tooltips -->
<button data-tooltip="Edit item">
    <svg><!-- edit icon --></svg>
</button>
```

### Programmatic Usage

```javascript
// Tooltips are automatically initialized
// No manual initialization needed

// Show tooltip manually (advanced)
Tooltip.show(element, 'Tooltip text');

// Hide tooltip manually
Tooltip.hide();
```

### Features
- **Auto-positioning**: Adjusts to stay within viewport
- **Keyboard accessible**: Works with Tab navigation
- **Smart placement**: Top or bottom based on space
- **Fade animation**: Smooth show/hide transitions

### Best Practices
1. Always add tooltips to icon-only buttons
2. Keep tooltip text concise (< 50 characters)
3. Use descriptive, action-oriented text
4. Don't duplicate visible button text

---

## Loading Indicators

### Basic Usage

```javascript
import { LoadingIndicator } from './deanship-feedback.js';

// Create loading indicator
const loader = new LoadingIndicator('containerId', {
    message: 'Loading data...',
    minDisplayTime: 500  // Minimum display time in ms
});

// Show loader
loader.show();

// Perform async operation
await fetchData();

// Hide loader (respects minimum display time)
await loader.hide();
```

### With Skeleton Loaders

```javascript
import { SkeletonLoader } from './deanship-feedback.js';

// Show skeleton while loading
SkeletonLoader.show('tableBody', 'table', {
    rows: 5,
    columns: 6
});

// Load data
const data = await fetchData();

// Render actual data
renderTable(data);
```

### Features
- **Minimum Display Time**: Prevents flicker for fast operations
- **Async/Await Support**: Works seamlessly with promises
- **Customizable**: Message and duration configurable
- **Smooth Transitions**: Fade-in/fade-out effects

---

## Best Practices

### Toast Notifications

#### ✅ Do
- Use appropriate toast types for context
- Keep messages concise and actionable
- Use action buttons for important follow-up actions
- Show toasts for user-initiated actions
- Use multiline for detailed error messages

#### ❌ Don't
- Show too many toasts at once (system limits to 5)
- Use toasts for critical errors (use modals instead)
- Make toast duration too short (< 3 seconds)
- Use toasts for passive information
- Rely solely on color to convey meaning

### Tooltips

#### ✅ Do
- Add tooltips to all icon-only buttons
- Use clear, descriptive text
- Keep text short and scannable
- Test with keyboard navigation
- Ensure tooltips don't cover important content

#### ❌ Don't
- Add tooltips to buttons with visible text labels
- Use long paragraphs in tooltips
- Rely on tooltips for critical information
- Use tooltips for mobile-only interfaces
- Duplicate button text in tooltip

### Loading Indicators

#### ✅ Do
- Use skeleton loaders for initial page loads
- Show loading indicators for operations > 500ms
- Provide meaningful loading messages
- Use minimum display time to prevent flicker
- Disable interactive elements while loading

#### ❌ Don't
- Show loading indicators for fast operations (< 500ms)
- Use generic "Loading..." for all operations
- Block entire UI for partial updates
- Forget to hide loading indicators on error
- Use loading indicators without timeout handling

---

## Examples

### Example 1: Form Submission with Toast

```javascript
async function handleFormSubmit(formData) {
    try {
        const response = await apiRequest('/api/submit', {
            method: 'POST',
            body: JSON.stringify(formData)
        });
        
        EnhancedToast.show('Form submitted successfully!', 'success', {
            duration: 5000,
            action: 'view',
            actionLabel: 'View Details',
            onAction: () => {
                navigateToDetails(response.id);
            }
        });
    } catch (error) {
        EnhancedToast.show(
            `Failed to submit form:\n${error.message}`,
            'error',
            { duration: 8000 }
        );
    }
}
```

### Example 2: Data Loading with Skeleton

```javascript
async function loadTableData() {
    const tbody = document.getElementById('tableBody');
    
    // Show skeleton loader
    tbody.innerHTML = SkeletonLoader.table(5, 6);
    
    try {
        const data = await fetchData();
        
        if (data.length === 0) {
            // Show empty state
            tbody.innerHTML = '<tr><td colspan="6"><div id="emptyState"></div></td></tr>';
            EmptyState.render('emptyState', {
                title: 'No Data Found',
                message: 'There are no records to display.',
                illustration: 'empty-box'
            });
        } else {
            // Render data
            renderTable(data);
        }
    } catch (error) {
        EnhancedToast.show('Failed to load data', 'error');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-red-500">Error loading data</td></tr>';
    }
}
```

### Example 3: Button with Tooltip and Loading State

```html
<button 
    id="saveBtn"
    data-tooltip="Save your changes"
    class="bg-blue-600 text-white px-4 py-2 rounded-md"
>
    Save
</button>
```

```javascript
async function handleSave() {
    const btn = document.getElementById('saveBtn');
    
    // Add loading state
    btn.classList.add('btn-loading');
    btn.disabled = true;
    
    try {
        await saveData();
        
        EnhancedToast.show('Changes saved successfully', 'success');
    } catch (error) {
        EnhancedToast.show('Failed to save changes', 'error');
    } finally {
        // Remove loading state
        btn.classList.remove('btn-loading');
        btn.disabled = false;
    }
}
```

### Example 4: Multi-step Operation with Progress

```javascript
async function performMultiStepOperation() {
    const steps = [
        { name: 'Validating data', fn: validateData },
        { name: 'Processing files', fn: processFiles },
        { name: 'Saving to database', fn: saveToDatabase }
    ];
    
    for (let i = 0; i < steps.length; i++) {
        const step = steps[i];
        
        // Show progress toast
        const toastId = EnhancedToast.show(
            `Step ${i + 1}/${steps.length}: ${step.name}`,
            'info',
            { duration: 10000 }
        );
        
        try {
            await step.fn();
        } catch (error) {
            EnhancedToast.show(
                `Failed at step ${i + 1}: ${error.message}`,
                'error'
            );
            return;
        }
    }
    
    EnhancedToast.show('All steps completed successfully!', 'success');
}
```

### Example 5: Undo Action with Toast

```javascript
async function deleteItem(itemId) {
    // Store item for undo
    const deletedItem = await getItem(itemId);
    
    // Delete item
    await apiRequest(`/api/items/${itemId}`, { method: 'DELETE' });
    
    // Show toast with undo action
    EnhancedToast.show('Item deleted', 'success', {
        duration: 8000,
        action: 'undo',
        actionLabel: 'Undo',
        onAction: async () => {
            try {
                await apiRequest('/api/items', {
                    method: 'POST',
                    body: JSON.stringify(deletedItem)
                });
                EnhancedToast.show('Item restored', 'success');
                refreshList();
            } catch (error) {
                EnhancedToast.show('Failed to restore item', 'error');
            }
        }
    });
    
    refreshList();
}
```

---

## Accessibility Considerations

### Keyboard Navigation
- All tooltips work with Tab key navigation
- Toasts can be dismissed with close button (keyboard accessible)
- Focus management maintained in modals and dropdowns

### Screen Readers
- Toasts have `role="alert"` for screen reader announcements
- Tooltips use `aria-label` or `aria-describedby`
- Loading states announced to screen readers

### Color Contrast
- All text meets WCAG AA standards (4.5:1 ratio)
- Icons have sufficient contrast
- Focus indicators clearly visible

### Reduced Motion
```css
@media (prefers-reduced-motion: reduce) {
    /* Animations disabled for users who prefer reduced motion */
}
```

---

## Troubleshooting

### Toast Not Showing
1. Check if `toastContainer` element exists in DOM
2. Verify EnhancedToast is imported/loaded
3. Check browser console for errors
4. Ensure CSS is loaded

### Tooltip Not Appearing
1. Verify `data-tooltip` attribute is set
2. Check if Tooltip.init() was called
3. Ensure element is visible and not disabled
4. Check z-index conflicts

### Loading Indicator Flickers
1. Increase `minDisplayTime` (default: 500ms)
2. Use skeleton loaders for longer operations
3. Debounce rapid state changes

---

## API Reference

### EnhancedToast

#### Methods
- `show(message, type, options)` - Show a toast notification
- `clearAll()` - Remove all active toasts
- `removeToast(element, id)` - Remove specific toast
- `pauseToast(id)` - Pause toast countdown
- `resumeToast(id)` - Resume toast countdown

#### Properties
- `toasts` - Array of active toast objects
- `maxToasts` - Maximum concurrent toasts (5)
- `toastIdCounter` - Auto-incrementing ID counter

### Tooltip

#### Methods
- `init()` - Initialize tooltip system (auto-called)
- `show(element, text)` - Show tooltip for element
- `hide()` - Hide active tooltip

### LoadingIndicator

#### Constructor
```javascript
new LoadingIndicator(containerId, options)
```

#### Methods
- `show()` - Display loading indicator
- `hide()` - Hide loading indicator (async, respects minDisplayTime)

#### Options
- `message` - Loading message text
- `minDisplayTime` - Minimum display duration in ms

---

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

All features use standard web APIs with no polyfills required.

---

## Performance Tips

1. **Limit Toast Count**: System automatically limits to 5 toasts
2. **Debounce Rapid Toasts**: Avoid showing toasts in loops
3. **Use Skeleton Loaders**: Better UX than loading spinners
4. **Cleanup Timers**: System automatically cleans up on toast removal
5. **CSS Animations**: Hardware-accelerated for smooth performance

---

## Related Documentation

- [Task 8 Implementation Summary](../tasks/task8-enhanced-feedback-implementation.md)
- [UI Components Guide](./ui-components-guide.md)
- [Accessibility Guidelines](./accessibility-guidelines.md)

---

## Support

For questions or issues:
1. Check this guide and examples
2. Review test page: `test-enhanced-toasts.html`
3. Check browser console for errors
4. Review implementation in `deanship-feedback.js`
