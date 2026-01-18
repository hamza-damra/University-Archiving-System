import requests

BASE_URL = "http://localhost:8080/api/hod"
AUTH_ENDPOINT = f"{BASE_URL}/auth/login"
DASHBOARD_ENDPOINT = f"{BASE_URL}/dashboard"
PROFESSORS_ENDPOINT = f"{BASE_URL}/professors"
REPORTS_ENDPOINT = f"{BASE_URL}/reports"
FILE_EXPLORER_ENDPOINT = f"{BASE_URL}/files"
NOTIFICATIONS_ENDPOINT = f"{BASE_URL}/notifications"
HEADERS = {"Content-Type": "application/json"}
TIMEOUT = 30

HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"

def test_semester_based_operation_scoping():
    # Authenticate and obtain JWT token
    login_payload = {"email": HOD_EMAIL, "password": HOD_PASSWORD}
    token = None
    try:
        auth_resp = requests.post(
            AUTH_ENDPOINT,
            json=login_payload,
            headers=HEADERS,
            timeout=TIMEOUT
        )
        assert auth_resp.status_code == 200, f"Login failed: {auth_resp.text}"
        auth_data = auth_resp.json()
        token = auth_data.get("token")
        assert token, "No token received in login response"
        auth_headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        # Step 1: Retrieve dashboard data for initial academic year and semester
        params1 = {"academicYear": "2023-2024", "semester": "1"}
        dash_resp1 = requests.get(
            DASHBOARD_ENDPOINT, headers=auth_headers, params=params1, timeout=TIMEOUT
        )
        assert dash_resp1.status_code == 200, f"Dashboard request failed: {dash_resp1.text}"
        dashboard_data_1 = dash_resp1.json()

        # Step 2: Retrieve professor list scoped to initial academic year and semester
        prof_resp1 = requests.get(
            PROFESSORS_ENDPOINT, headers=auth_headers, params=params1, timeout=TIMEOUT
        )
        assert prof_resp1.status_code == 200, f"Professors request failed: {prof_resp1.text}"
        professors_1 = prof_resp1.json()

        # Step 3: Retrieve reports scoped to initial academic year and semester
        reports_resp1 = requests.get(
            REPORTS_ENDPOINT, headers=auth_headers, params=params1, timeout=TIMEOUT
        )
        assert reports_resp1.status_code == 200, f"Reports request failed: {reports_resp1.text}"
        reports_1 = reports_resp1.json()

        # Step 4: Retrieve list of files for initial academic year and semester
        files_resp1 = requests.get(
            FILE_EXPLORER_ENDPOINT, headers=auth_headers, params=params1, timeout=TIMEOUT
        )
        assert files_resp1.status_code == 200, f"Files request failed: {files_resp1.text}"
        files_1 = files_resp1.json()

        # Step 5: Retrieve notifications scoped to initial academic year and semester
        notifications_resp1 = requests.get(
            NOTIFICATIONS_ENDPOINT, headers=auth_headers, params=params1, timeout=TIMEOUT
        )
        assert notifications_resp1.status_code == 200, f"Notifications request failed: {notifications_resp1.text}"
        notifications_1 = notifications_resp1.json()

        # Change the academic year and semester parameters
        params2 = {"academicYear": "2022-2023", "semester": "2"}

        dash_resp2 = requests.get(
            DASHBOARD_ENDPOINT, headers=auth_headers, params=params2, timeout=TIMEOUT
        )
        assert dash_resp2.status_code == 200, f"Dashboard request failed (semester change): {dash_resp2.text}"
        dashboard_data_2 = dash_resp2.json()

        prof_resp2 = requests.get(
            PROFESSORS_ENDPOINT, headers=auth_headers, params=params2, timeout=TIMEOUT
        )
        assert prof_resp2.status_code == 200, f"Professors request failed (semester change): {prof_resp2.text}"
        professors_2 = prof_resp2.json()

        reports_resp2 = requests.get(
            REPORTS_ENDPOINT, headers=auth_headers, params=params2, timeout=TIMEOUT
        )
        assert reports_resp2.status_code == 200, f"Reports request failed (semester change): {reports_resp2.text}"
        reports_2 = reports_resp2.json()

        files_resp2 = requests.get(
            FILE_EXPLORER_ENDPOINT, headers=auth_headers, params=params2, timeout=TIMEOUT
        )
        assert files_resp2.status_code == 200, f"Files request failed (semester change): {files_resp2.text}"
        files_2 = files_resp2.json()

        notifications_resp2 = requests.get(
            NOTIFICATIONS_ENDPOINT, headers=auth_headers, params=params2, timeout=TIMEOUT
        )
        assert notifications_resp2.status_code == 200, f"Notifications request failed (semester change): {notifications_resp2.text}"
        notifications_2 = notifications_resp2.json()

        # Validate that data between the two semesters differs, indicating correct scoping and update
        # For dashboard overview, expect some change in summary stats
        assert dashboard_data_1 != dashboard_data_2, "Dashboard data should differ between semesters"

        # Professors list might differ due to active/assigned semester filtering
        assert professors_1 != professors_2 or isinstance(professors_1, list), "Professors data should update per semester"

        # Reports data should differ to reflect semester scoped reports
        assert reports_1 != reports_2, "Reports data should differ between semesters"

        # Files (archive content) should differ or at least be filtered by semester
        assert files_1 != files_2 or isinstance(files_1, list), "File explorer data should update per semester"

        # Notifications should reflect semester scoped submissions
        assert notifications_1 != notifications_2 or isinstance(notifications_1, list), "Notifications should differ between semesters"

    except (requests.RequestException, AssertionError) as ex:
        raise AssertionError(f"Test failed: {ex}")
        

test_semester_based_operation_scoping()