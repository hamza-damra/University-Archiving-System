import requests
from requests.exceptions import RequestException

BASE_URL = "http://localhost:8080/api/hod"
LOGIN_URL = f"{BASE_URL}/auth/login"
REPORTS_GENERATE_URL = f"{BASE_URL}/reports/professor-submissions"
REPORTS_EXPORT_PDF_URL = f"{BASE_URL}/reports/professor-submissions/export"

HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"
TIMEOUT = 30


def test_report_generation_and_pdf_export():
    # Authenticate and get JWT token
    auth_payload = {"email": HOD_EMAIL, "password": HOD_PASSWORD}
    try:
        auth_resp = requests.post(LOGIN_URL, json=auth_payload, timeout=TIMEOUT)
        assert auth_resp.status_code == 200, f"Login failed: {auth_resp.text}"
        token = auth_resp.json().get("token")
        assert token, "JWT token not found in login response"
    except RequestException as e:
        assert False, f"Login request failed with exception: {e}"

    headers = {
        "Authorization": f"Bearer {token}"
    }

    # Define various filter combinations to test report generation
    filter_combinations = [
        {},  # No filters - default
        {"academicYear": "2024/2025"},
        {"academicYear": "2024/2025", "semester": "1"},
        {"academicYear": "2024/2025", "semester": "2", "departmentId": 1},
        {"semester": "1"},
        {"departmentId": 1},
        {"academicYear": "2023/2024", "semester": "2"},
    ]

    for filters in filter_combinations:
        # Step 1: Generate report data with filters
        try:
            resp = requests.get(REPORTS_GENERATE_URL, headers=headers, params=filters, timeout=TIMEOUT)
        except RequestException as e:
            assert False, f"Report generation request failed with exception: {e}"

        assert resp.status_code == 200, f"Report generation failed with filters {filters}, response: {resp.text}"
        data = resp.json()
        assert isinstance(data, dict), f"Expected JSON object for report data, got: {data}"
        # Validate essential keys in report data if present
        assert "report" in data or "submissions" in data or "summary" in data, f"Report data missing expected keys for filters {filters}"

        # Step 2: Export report as PDF (with same filters)
        try:
            resp_pdf = requests.get(REPORTS_EXPORT_PDF_URL, headers=headers, params=filters, timeout=TIMEOUT)
        except RequestException as e:
            assert False, f"PDF export request failed with exception: {e}"

        assert resp_pdf.status_code == 200, f"PDF export failed with filters {filters}, response: {resp_pdf.text}"
        content_type = resp_pdf.headers.get("Content-Type", "")
        content_disp = resp_pdf.headers.get("Content-Disposition", "")

        # Validate PDF mime type and content disposition headers
        assert "application/pdf" in content_type.lower(), f"Invalid Content-Type for PDF export: {content_type}"
        assert "attachment" in content_disp.lower() and ".pdf" in content_disp.lower(), f"Content-Disposition header invalid or missing for PDF export: {content_disp}"

        # Validate PDF content is non-empty
        content_length = len(resp_pdf.content)
        assert content_length > 100, f"PDF export content too small, length: {content_length}"

    print("All report generation and PDF export tests passed.")


test_report_generation_and_pdf_export()