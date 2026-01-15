/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';

// Mock dependencies before importing the module under test
global.fetch = jest.fn();
global.window = {
    location: { href: '', origin: 'http://localhost' },
    ...global.window
};

// Mock the API module
const mockApiRequest = jest.fn();

jest.mock('../../../main/resources/static/js/core/api.js', () => ({
    apiRequest: mockApiRequest
}));

// Mock the UI module
const mockShowToast = jest.fn();
const mockSetButtonLoading = jest.fn();

jest.mock('../../../main/resources/static/js/core/ui.js', () => ({
    showToast: mockShowToast,
    setButtonLoading: mockSetButtonLoading
}));

// Mock modern dropdown refresh helper used by the UI
global.refreshModernDropdown = jest.fn();

describe('Admin Reports (admin/admin-reports.js)', () => {
    let AdminReportsPage;

    function createSelect(id) {
        const el = document.createElement('select');
        el.id = id;
        document.body.appendChild(el);
        return el;
    }

    beforeEach(async () => {
        document.body.innerHTML = '';
        jest.clearAllMocks();

        // Required DOM nodes used by AdminReportsPage
        createSelect('reportDepartmentFilter');
        createSelect('reportAcademicYearFilter');
        createSelect('reportSemesterFilter');

        const container = document.createElement('div');
        container.id = 'reportsContainer';
        document.body.appendChild(container);

        const loading = document.createElement('div');
        loading.id = 'reportsLoading';
        document.body.appendChild(loading);

        const tableBody = document.createElement('tbody');
        tableBody.id = 'departmentTableBody';
        document.body.appendChild(tableBody);

        // Default API responses for initialize()
        mockApiRequest.mockImplementation((url) => {
            if (url === '/admin/departments') {
                return Promise.resolve([{ id: 10, name: 'Computer Science' }, { id: 20, name: 'Physics' }]);
            }
            if (url === '/deanship/academic-years') {
                return Promise.resolve([{ id: 1, yearCode: '2024-2025' }]);
            }
            if (url === '/admin/reports/filter-options') {
                return Promise.resolve({});
            }
            if (url.startsWith('/admin/reports/overview')) {
                return Promise.resolve({ overallStatistics: {}, departmentSummaries: [] });
            }
            return Promise.resolve({});
        });

        const module = await import('../../../main/resources/static/js/admin/admin-reports.js');
        AdminReportsPage = module.default;
    });

    it('should load filter options and populate department dropdown', async () => {
        const page = new AdminReportsPage();
        await page.initialize();

        expect(mockApiRequest).toHaveBeenCalledWith('/admin/departments', { method: 'GET' });
        expect(mockApiRequest).toHaveBeenCalledWith('/deanship/academic-years', { method: 'GET' });

        const deptSelect = document.getElementById('reportDepartmentFilter');
        expect(deptSelect).toBeInTheDocument();
        expect(deptSelect.options.length).toBeGreaterThan(1);
        expect(global.refreshModernDropdown).toHaveBeenCalled();
    });

    it('should include departmentId in report request when department filter changes', async () => {
        const page = new AdminReportsPage();
        await page.initialize();

        // Pretend a semester is selected already
        page.currentFilters.semesterId = '5';

        // Clear calls from initialization
        mockApiRequest.mockClear();

        const deptSelect = document.getElementById('reportDepartmentFilter');
        deptSelect.value = '20';
        deptSelect.dispatchEvent(new Event('change'));

        // Wait for async loadReport chain
        await Promise.resolve();
        await Promise.resolve();

        expect(mockApiRequest).toHaveBeenCalledWith(
            '/admin/reports/overview?semesterId=5&departmentId=20',
            { method: 'GET' }
        );
    });
});

