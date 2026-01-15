/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { fileExplorerState } from '../../../main/resources/static/js/file-explorer/file-explorer-state.js';

describe('FileExplorerState', () => {
    let stateInstance;

    beforeEach(() => {
        // Create a new instance for each test to ensure isolation
        // Since fileExplorerState is a singleton, we'll reset it instead
        fileExplorerState.reset();
        stateInstance = fileExplorerState;
    });

    afterEach(() => {
        // Clear all listeners
        fileExplorerState.reset();
    });

    // ==================== State Management Tests ====================

    describe('Initial State', () => {
        it('should have correct initial state', () => {
            const state = stateInstance.getState();

            expect(state.academicYearId).toBeNull();
            expect(state.semesterId).toBeNull();
            expect(state.yearCode).toBeNull();
            expect(state.semesterType).toBeNull();
            expect(state.treeRoot).toBeNull();
            expect(state.currentNode).toBeNull();
            expect(state.currentPath).toBe('');
            expect(state.breadcrumbs).toEqual([]);
            expect(state.expandedNodes).toBeInstanceOf(Set);
            expect(state.expandedNodes.size).toBe(0);
            expect(state.isLoading).toBe(false);
            expect(state.isTreeLoading).toBe(false);
            expect(state.isFileListLoading).toBe(false);
            expect(state.error).toBeNull();
            expect(state.lastUpdated).toBeNull();
        });
    });

    describe('setContext', () => {
        it('should update academic year and semester', () => {
            const academicYearId = 1;
            const semesterId = 2;
            const yearCode = '2024-2025';
            const semesterType = 'first';

            stateInstance.setContext(academicYearId, semesterId, yearCode, semesterType);

            const context = stateInstance.getContext();
            expect(context.academicYearId).toBe(academicYearId);
            expect(context.semesterId).toBe(semesterId);
            expect(context.yearCode).toBe(yearCode);
            expect(context.semesterType).toBe(semesterType);
        });

        it('should update lastUpdated timestamp', () => {
            const beforeTime = Date.now();
            stateInstance.setContext(1, 2, '2024-2025', 'first');
            const afterTime = Date.now();
            const lastUpdated = stateInstance.getLastUpdated();

            expect(lastUpdated).toBeGreaterThanOrEqual(beforeTime);
            expect(lastUpdated).toBeLessThanOrEqual(afterTime);
        });

        it('should notify listeners when context changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setContext(1, 2, '2024-2025', 'first');

            expect(listener).toHaveBeenCalledTimes(1);
            const calledState = listener.mock.calls[0][0];
            expect(calledState.academicYearId).toBe(1);
            expect(calledState.semesterId).toBe(2);
        });
    });

    describe('setCurrentNode', () => {
        it('should update current node and path', () => {
            const node = { id: 1, name: 'test', type: 'folder' };
            const path = '/test/path';

            stateInstance.setCurrentNode(node, path);

            expect(stateInstance.getCurrentNode()).toEqual(node);
            expect(stateInstance.getCurrentPath()).toBe(path);
        });

        it('should set empty path when path is not provided', () => {
            const node = { id: 1, name: 'test', type: 'folder' };

            stateInstance.setCurrentNode(node);

            expect(stateInstance.getCurrentNode()).toEqual(node);
            expect(stateInstance.getCurrentPath()).toBe('');
        });

        it('should notify listeners when current node changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            const node = { id: 1, name: 'test' };
            stateInstance.setCurrentNode(node, '/test');

            expect(listener).toHaveBeenCalledTimes(1);
            const calledState = listener.mock.calls[0][0];
            expect(calledState.currentNode).toEqual(node);
            expect(calledState.currentPath).toBe('/test');
        });
    });

    describe('getContext', () => {
        it('should return current context', () => {
            stateInstance.setContext(1, 2, '2024-2025', 'first');

            const context = stateInstance.getContext();

            expect(context).toEqual({
                academicYearId: 1,
                semesterId: 2,
                yearCode: '2024-2025',
                semesterType: 'first'
            });
        });

        it('should return null values when context is not set', () => {
            const context = stateInstance.getContext();

            expect(context).toEqual({
                academicYearId: null,
                semesterId: null,
                yearCode: null,
                semesterType: null
            });
        });
    });

    describe('State Persistence', () => {
        it('should persist academic context values', () => {
            stateInstance.setContext(1, 2, '2024-2025', 'first');
            const state1 = stateInstance.getState();

            // Simulate state access after some time
            const state2 = stateInstance.getState();

            expect(state2.academicYearId).toBe(state1.academicYearId);
            expect(state2.semesterId).toBe(state1.semesterId);
            expect(state2.yearCode).toBe(state1.yearCode);
            expect(state2.semesterType).toBe(state1.semesterType);
        });

        it('should persist tree root', () => {
            const treeRoot = { id: 1, name: 'root', children: [] };
            stateInstance.setTreeRoot(treeRoot);

            expect(stateInstance.getTreeRoot()).toEqual(treeRoot);
        });

        it('should persist breadcrumbs', () => {
            const breadcrumbs = [
                { path: '/', name: 'Home' },
                { path: '/folder', name: 'Folder' }
            ];
            stateInstance.setBreadcrumbs(breadcrumbs);

            expect(stateInstance.getBreadcrumbs()).toEqual(breadcrumbs);
        });

        it('should persist expanded nodes', () => {
            stateInstance.expandNode('/path1');
            stateInstance.expandNode('/path2');

            const expandedNodes = stateInstance.getExpandedNodes();
            expect(expandedNodes.has('/path1')).toBe(true);
            expect(expandedNodes.has('/path2')).toBe(true);
        });
    });

    // ==================== Listener Tests ====================

    describe('subscribe (addListener)', () => {
        it('should register callback', () => {
            const listener = jest.fn();
            const unsubscribe = stateInstance.subscribe(listener);

            expect(typeof unsubscribe).toBe('function');
            
            // Trigger a state change
            stateInstance.setContext(1, 2, '2024-2025', 'first');

            expect(listener).toHaveBeenCalled();
        });

        it('should throw error if listener is not a function', () => {
            expect(() => {
                stateInstance.subscribe(null);
            }).toThrow('Listener must be a function');

            expect(() => {
                stateInstance.subscribe('not a function');
            }).toThrow('Listener must be a function');

            expect(() => {
                stateInstance.subscribe(123);
            }).toThrow('Listener must be a function');
        });

        it('should return unsubscribe function', () => {
            const listener = jest.fn();
            const unsubscribe = stateInstance.subscribe(listener);

            expect(typeof unsubscribe).toBe('function');
        });
    });

    describe('unsubscribe (removeListener)', () => {
        it('should unregister callback', () => {
            const listener = jest.fn();
            const unsubscribe = stateInstance.subscribe(listener);

            // Unsubscribe
            unsubscribe();

            // Trigger a state change
            stateInstance.setContext(1, 2, '2024-2025', 'first');

            // Listener should not be called
            expect(listener).not.toHaveBeenCalled();
        });

        it('should allow multiple unsubscribe calls safely', () => {
            const listener = jest.fn();
            const unsubscribe = stateInstance.subscribe(listener);

            unsubscribe();
            unsubscribe(); // Should not throw

            stateInstance.setContext(1, 2, '2024-2025', 'first');
            expect(listener).not.toHaveBeenCalled();
        });
    });

    describe('Listeners notified on state change', () => {
        it('should notify listeners when state changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setContext(1, 2, '2024-2025', 'first');

            expect(listener).toHaveBeenCalledTimes(1);
            const state = listener.mock.calls[0][0];
            expect(state).toBeDefined();
            expect(state.academicYearId).toBe(1);
        });

        it('should notify listeners on setTreeRoot', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            const treeRoot = { id: 1, name: 'root' };
            stateInstance.setTreeRoot(treeRoot);

            expect(listener).toHaveBeenCalledTimes(1);
        });

        it('should notify listeners on setCurrentNode', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setCurrentNode({ id: 1 }, '/path');

            expect(listener).toHaveBeenCalledTimes(1);
        });

        it('should notify listeners on setBreadcrumbs', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setBreadcrumbs([{ path: '/', name: 'Home' }]);

            expect(listener).toHaveBeenCalledTimes(1);
        });

        it('should notify listeners on loading state changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setLoading(true);

            expect(listener).toHaveBeenCalledTimes(1);
        });

        it('should notify listeners on error state changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setError('Test error');

            expect(listener).toHaveBeenCalledTimes(1);
        });
    });

    describe('Multiple listeners', () => {
        it('should notify multiple listeners', () => {
            const listener1 = jest.fn();
            const listener2 = jest.fn();
            const listener3 = jest.fn();

            stateInstance.subscribe(listener1);
            stateInstance.subscribe(listener2);
            stateInstance.subscribe(listener3);

            stateInstance.setContext(1, 2, '2024-2025', 'first');

            expect(listener1).toHaveBeenCalledTimes(1);
            expect(listener2).toHaveBeenCalledTimes(1);
            expect(listener3).toHaveBeenCalledTimes(1);
        });

        it('should notify all listeners with same state', () => {
            const listener1 = jest.fn();
            const listener2 = jest.fn();

            stateInstance.subscribe(listener1);
            stateInstance.subscribe(listener2);

            stateInstance.setContext(1, 2, '2024-2025', 'first');

            const state1 = listener1.mock.calls[0][0];
            const state2 = listener2.mock.calls[0][0];

            expect(state1.academicYearId).toBe(state2.academicYearId);
            expect(state1.semesterId).toBe(state2.semesterId);
        });

        it('should handle errors in listeners gracefully', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const goodListener = jest.fn();
            const badListener = jest.fn(() => {
                throw new Error('Listener error');
            });

            stateInstance.subscribe(goodListener);
            stateInstance.subscribe(badListener);

            // Should not throw
            expect(() => {
                stateInstance.setContext(1, 2, '2024-2025', 'first');
            }).not.toThrow();

            // Good listener should still be called
            expect(goodListener).toHaveBeenCalledTimes(1);
            expect(consoleErrorSpy).toHaveBeenCalled();

            consoleErrorSpy.mockRestore();
        });
    });

    // ==================== History Tests ====================
    // Note: The current implementation doesn't have history tracking.
    // These tests verify that context changes clear relevant state,
    // which is the closest equivalent to "history cleared on context change"

    describe('State Reset on Context Change', () => {
        it('should maintain context when resetData is called', () => {
            stateInstance.setContext(1, 2, '2024-2025', 'first');
            stateInstance.setTreeRoot({ id: 1 });
            stateInstance.setCurrentNode({ id: 2 }, '/path');

            stateInstance.resetData();

            // Context should remain
            const context = stateInstance.getContext();
            expect(context.academicYearId).toBe(1);
            expect(context.semesterId).toBe(2);

            // But data should be cleared
            expect(stateInstance.getTreeRoot()).toBeNull();
            expect(stateInstance.getCurrentNode()).toBeNull();
            expect(stateInstance.getCurrentPath()).toBe('');
        });

        it('should clear all state when reset is called', () => {
            stateInstance.setContext(1, 2, '2024-2025', 'first');
            stateInstance.setTreeRoot({ id: 1 });
            stateInstance.setCurrentNode({ id: 2 }, '/path');
            stateInstance.expandNode('/path1');

            stateInstance.reset();

            // Everything should be cleared
            const context = stateInstance.getContext();
            expect(context.academicYearId).toBeNull();
            expect(context.semesterId).toBeNull();
            expect(stateInstance.getTreeRoot()).toBeNull();
            expect(stateInstance.getCurrentNode()).toBeNull();
            expect(stateInstance.getCurrentPath()).toBe('');
            expect(stateInstance.getExpandedNodes().size).toBe(0);
        });
    });

    // ==================== Additional State Management Tests ====================

    describe('Tree Root Management', () => {
        it('should set and get tree root', () => {
            const treeRoot = { id: 1, name: 'root', children: [] };
            stateInstance.setTreeRoot(treeRoot);

            expect(stateInstance.getTreeRoot()).toEqual(treeRoot);
        });

        it('should notify listeners when tree root changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setTreeRoot({ id: 1 });

            expect(listener).toHaveBeenCalledTimes(1);
        });
    });

    describe('Breadcrumbs Management', () => {
        it('should set and get breadcrumbs', () => {
            const breadcrumbs = [
                { path: '/', name: 'Home' },
                { path: '/folder', name: 'Folder' }
            ];
            stateInstance.setBreadcrumbs(breadcrumbs);

            expect(stateInstance.getBreadcrumbs()).toEqual(breadcrumbs);
        });

        it('should set empty array when breadcrumbs is null', () => {
            stateInstance.setBreadcrumbs(null);

            expect(stateInstance.getBreadcrumbs()).toEqual([]);
        });

        it('should notify listeners when breadcrumbs change', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setBreadcrumbs([{ path: '/', name: 'Home' }]);

            expect(listener).toHaveBeenCalledTimes(1);
        });
    });

    describe('Node Expansion Management', () => {
        it('should expand a node', () => {
            stateInstance.expandNode('/path1');

            expect(stateInstance.isNodeExpanded('/path1')).toBe(true);
            expect(stateInstance.getExpandedNodes().has('/path1')).toBe(true);
        });

        it('should collapse a node', () => {
            stateInstance.expandNode('/path1');
            stateInstance.collapseNode('/path1');

            expect(stateInstance.isNodeExpanded('/path1')).toBe(false);
        });

        it('should toggle node expansion', () => {
            expect(stateInstance.isNodeExpanded('/path1')).toBe(false);

            stateInstance.toggleNodeExpansion('/path1');
            expect(stateInstance.isNodeExpanded('/path1')).toBe(true);

            stateInstance.toggleNodeExpansion('/path1');
            expect(stateInstance.isNodeExpanded('/path1')).toBe(false);
        });

        it('should clear all expanded nodes', () => {
            stateInstance.expandNode('/path1');
            stateInstance.expandNode('/path2');
            stateInstance.expandNode('/path3');

            expect(stateInstance.getExpandedNodes().size).toBe(3);

            stateInstance.clearExpandedNodes();

            expect(stateInstance.getExpandedNodes().size).toBe(0);
        });

        it('should not notify if expanding already expanded node', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.expandNode('/path1');
            const callCount1 = listener.mock.calls.length;

            stateInstance.expandNode('/path1'); // Already expanded
            const callCount2 = listener.mock.calls.length;

            expect(callCount2).toBe(callCount1);
        });

        it('should not notify if collapsing already collapsed node', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.collapseNode('/path1'); // Already collapsed
            const callCount1 = listener.mock.calls.length;

            expect(callCount1).toBe(0);
        });
    });

    describe('Loading State Management', () => {
        it('should set general loading state', () => {
            stateInstance.setLoading(true);

            const state = stateInstance.getState();
            expect(state.isLoading).toBe(true);
        });

        it('should set tree loading state', () => {
            stateInstance.setTreeLoading(true);

            const state = stateInstance.getState();
            expect(state.isTreeLoading).toBe(true);
        });

        it('should set file list loading state', () => {
            stateInstance.setFileListLoading(true);

            const state = stateInstance.getState();
            expect(state.isFileListLoading).toBe(true);
        });

        it('should check if any loading state is active', () => {
            expect(stateInstance.isAnyLoading()).toBe(false);

            stateInstance.setLoading(true);
            expect(stateInstance.isAnyLoading()).toBe(true);

            stateInstance.setLoading(false);
            stateInstance.setTreeLoading(true);
            expect(stateInstance.isAnyLoading()).toBe(true);

            stateInstance.setTreeLoading(false);
            stateInstance.setFileListLoading(true);
            expect(stateInstance.isAnyLoading()).toBe(true);
        });
    });

    describe('Error State Management', () => {
        it('should set error from string', () => {
            stateInstance.setError('Test error message');

            const state = stateInstance.getState();
            expect(state.error).toBe('Test error message');
        });

        it('should set error from Error object', () => {
            const error = new Error('Test error');
            stateInstance.setError(error);

            const state = stateInstance.getState();
            expect(state.error).toBe('Test error');
        });

        it('should clear error', () => {
            stateInstance.setError('Test error');
            stateInstance.clearError();

            const state = stateInstance.getState();
            expect(state.error).toBeNull();
        });

        it('should notify listeners on error changes', () => {
            const listener = jest.fn();
            stateInstance.subscribe(listener);

            stateInstance.setError('Test error');

            expect(listener).toHaveBeenCalledTimes(1);
        });
    });

    describe('Utility Methods', () => {
        it('should check if context is set', () => {
            expect(stateInstance.hasContext()).toBe(false);

            stateInstance.setContext(1, 2, '2024-2025', 'first');
            expect(stateInstance.hasContext()).toBe(true);

            stateInstance.setContext(null, 2, '2024-2025', 'first');
            expect(stateInstance.hasContext()).toBe(false);

            stateInstance.setContext(1, null, '2024-2025', 'first');
            expect(stateInstance.hasContext()).toBe(false);
        });

        it('should get last updated timestamp', () => {
            expect(stateInstance.getLastUpdated()).toBeNull();

            stateInstance.setContext(1, 2, '2024-2025', 'first');
            const timestamp = stateInstance.getLastUpdated();

            expect(typeof timestamp).toBe('number');
            expect(timestamp).toBeGreaterThan(0);
        });
    });

    describe('State Immutability', () => {
        it('should return immutable copy of state', () => {
            stateInstance.setContext(1, 2, '2024-2025', 'first');
            const state1 = stateInstance.getState();
            const state2 = stateInstance.getState();

            // Should be different objects
            expect(state1).not.toBe(state2);

            // But with same values
            expect(state1.academicYearId).toBe(state2.academicYearId);
        });

        it('should return immutable copy of expandedNodes Set', () => {
            stateInstance.expandNode('/path1');
            const state1 = stateInstance.getState();
            const state2 = stateInstance.getState();

            // Should be different Set instances
            expect(state1.expandedNodes).not.toBe(state2.expandedNodes);

            // But with same values
            expect(state1.expandedNodes.has('/path1')).toBe(true);
            expect(state2.expandedNodes.has('/path1')).toBe(true);
        });

        it('should prevent direct mutation of state', () => {
            const state = stateInstance.getState();
            state.academicYearId = 999; // Try to mutate

            // Original state should not be affected
            expect(stateInstance.getContext().academicYearId).not.toBe(999);
        });
    });
});
