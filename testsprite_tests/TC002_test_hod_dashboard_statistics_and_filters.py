import requests

BASE_URL = "http://localhost:8080/api/hod"
AUTH_URL = "http://localhost:8080/api/auth/login"
HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"
TIMEOUT = 30

def test_hod_dashboard_statistics_and_filters():
    # Authenticate to get JWT token with HOD role
    credentials = {
        "email": HOD_EMAIL,
        "password": HOD_PASSWORD
    }
    try:
        auth_resp = requests.post(
            AUTH_URL,
            json=credentials,
            timeout=TIMEOUT
        )
        assert auth_resp.status_code == 200, f"Authentication failed: {auth_resp.text}"
        auth_json = auth_resp.json()
        token = auth_json.get("token") or auth_json.get("accessToken")
        assert token, "JWT token not found in authentication response"
    except Exception as e:
        raise AssertionError(f"Authentication request failed: {e}")

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    # First, retrieve valid academic years and semesters from API if possible
    # Assuming GET /academicYears returns list of years with semesters
    try:
        years_resp = requests.get(f"{BASE_URL}/academicYears", headers=headers, timeout=TIMEOUT)
        assert years_resp.status_code == 200, f"Failed to get academic years: {years_resp.text}"
        years_data = years_resp.json()
        assert isinstance(years_data, list) and len(years_data) > 0, "No academic years found"

        # Select first academic year and its first semester as test params
        academic_year = years_data[0].get("year")
        semesters = years_data[0].get("semesters")
        assert semesters and isinstance(semesters, list), "No semesters found for academic year"
        semester = semesters[0]
    except Exception as e:
        raise AssertionError(f"Error retrieving academic years/semesters: {e}")

    # Define the dashboard statistics URL with query parameters for year and semester
    dashboard_stats_url = f"{BASE_URL}/dashboard/statistics?academicYear={academic_year}&semester={semester}"

    try:
        # Request dashboard statistics for selected year and semester
        stats_resp = requests.get(dashboard_stats_url, headers=headers, timeout=TIMEOUT)
        assert stats_resp.status_code == 200, f"Dashboard statistics request failed: {stats_resp.text}"
        stats = stats_resp.json()

        # Validate response contains required keys and reasonable integer values >= 0
        required_fields = ["professorsCount", "coursesCount", "submissionsCount", "missingDocumentsCount", "overdueDocumentsCount"]
        for field in required_fields:
            assert field in stats, f"Missing field in dashboard statistics: {field}"
            value = stats[field]
            assert isinstance(value, int) and value >= 0, f"Invalid value for {field}: {value}"

    except Exception as e:
        raise AssertionError(f"Error validating dashboard statistics: {e}")

    # Now test dynamic update of filters - simulate changing academic year and semester if more than one available
    try:
        # If more than one academic year or semester, pick another to verify data changes
        if len(years_data) > 1 or len(semesters) > 1:
            # Try a different combination
            alt_academic_year = academic_year
            alt_semester = semester
            if len(semesters) > 1:
                alt_semester = semesters[1]
            elif len(years_data) > 1:
                alt_academic_year = years_data[1].get("year")
                alt_semester = years_data[1].get("semesters")[0]

            alt_stats_url = f"{BASE_URL}/dashboard/statistics?academicYear={alt_academic_year}&semester={alt_semester}"
            alt_resp = requests.get(alt_stats_url, headers=headers, timeout=TIMEOUT)
            assert alt_resp.status_code == 200, f"Alternate dashboard statistics request failed: {alt_resp.text}"
            alt_stats = alt_resp.json()

            # Check that statistics differ or at least are present and valid
            for field in required_fields:
                assert field in alt_stats, f"Missing field in alt dashboard statistics: {field}"
                alt_value = alt_stats[field]
                assert isinstance(alt_value, int) and alt_value >= 0, f"Invalid alt value for {field}: {alt_value}"
            
            # Ideally stats should be different when filters change, but if data is static, just confirm valid responses
            # We do not assert inequality here to avoid test flakiness if data is same
    except Exception as e:
        raise AssertionError(f"Error validating dashboard statistics filters: {e}")

    # Test filter interaction for one example filter:
    # Assuming an endpoint to get filtered professors or submissions exists, simulate a filter change.
    # Example: Filter by professorId (get list of professors first)
    try:
        profs_resp = requests.get(f"{BASE_URL}/professors?academicYear={academic_year}&semester={semester}", headers=headers, timeout=TIMEOUT)
        assert profs_resp.status_code == 200, f"Professors list request failed: {profs_resp.text}"
        profs = profs_resp.json()
        if isinstance(profs, list) and len(profs) > 0:
            professor_id = profs[0].get("id")
            # Now request submissions filtered by professorId
            submissions_url = f"{BASE_URL}/submissions/status?academicYear={academic_year}&semester={semester}&professorId={professor_id}"
            subs_resp = requests.get(submissions_url, headers=headers, timeout=TIMEOUT)
            assert subs_resp.status_code == 200, f"Submissions filtered request failed: {subs_resp.text}"
            subs_data = subs_resp.json()
            assert isinstance(subs_data, list), "Submission data should be a list"
    except Exception as e:
        raise AssertionError(f"Error testing dynamic filters for professors and submissions: {e}")

    # Further UI features mentioned (courses, file explorer, reports, navigation) are frontend-based and not covered by backend endpoint tests here.

test_hod_dashboard_statistics_and_filters()