// Jest setup file for frontend tests
import '@testing-library/jest-dom';

// Mock window.location
delete window.location;
window.location = { href: '' };

// Mock localStorage
const localStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => { store[key] = value; }),
        removeItem: jest.fn(key => { delete store[key]; }),
        clear: jest.fn(() => { store = {}; }),
        get length() { return Object.keys(store).length; }
    };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock fetch
global.fetch = jest.fn();

// Polyfill setImmediate for jsdom (some tests use it for flushing promises)
if (typeof global.setImmediate === 'undefined') {
    global.setImmediate = (callback, ...args) => setTimeout(callback, 0, ...args);
}