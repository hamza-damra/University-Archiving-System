import requests
from requests.exceptions import RequestException

BASE_URL = "http://localhost:8080/api/hod"
AUTH_URL = "http://localhost:8080/api/auth/login"
HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"
TIMEOUT = 30


def authenticate():
    try:
        resp = requests.post(
            AUTH_URL,
            json={"email": HOD_EMAIL, "password": HOD_PASSWORD},
            timeout=TIMEOUT,
        )
        resp.raise_for_status()
        data = resp.json()
        token = data.get("token")
        assert token, "Authentication token not received"
        return token
    except (RequestException, AssertionError) as e:
        raise Exception(f"Authentication failed: {e}")


def test_error_handling_and_user_feedback():
    token = authenticate()
    headers = {"Authorization": f"Bearer {token}"}

    # Helper function to assert error response structure and status
    def assert_error(resp, expected_status, expected_message_contains=None):
        assert resp.status_code == expected_status, f"Expected status {expected_status}, got {resp.status_code}"
        try:
            err_json = resp.json()
        except Exception:
            raise AssertionError("Response is not in JSON format")
        assert "error" in err_json or "message" in err_json, "Error message missing in response"
        if expected_message_contains:
            msg = err_json.get("error") or err_json.get("message") or ""
            assert expected_message_contains.lower() in msg.lower(), f"Expected message to contain '{expected_message_contains}', got '{msg}'"

    # 1. Unauthorized access: Call protected endpoint without token
    response = requests.get(f"{BASE_URL}/dashboard-overview", timeout=TIMEOUT)
    assert_error(response, 401, "Unauthorized")

    # 2. Invalid token: call protected endpoint with invalid token
    invalid_headers = {"Authorization": "Bearer invalid.token.value"}
    response = requests.get(f"{BASE_URL}/dashboard-overview", headers=invalid_headers, timeout=TIMEOUT)
    assert_error(response, 401, "Unauthorized")

    # 3. Access with insufficient role (simulate by using a malformed token or invalid role token if possible)
    # Since we only have HOD token, simulate by calling user-management with malformed token
    response = requests.get(f"{BASE_URL}/professors", headers=invalid_headers, timeout=TIMEOUT)
    assert_error(response, 401, "Unauthorized")

    # 4. Invalid input error: Create professor with invalid payload (missing required fields)
    invalid_prof_payload = {"email": "invalid-email-format", "name": "", "departmentId": -1}  # invalid email, empty name, invalid deptId
    response = requests.post(f"{BASE_URL}/professors", headers=headers, json=invalid_prof_payload, timeout=TIMEOUT)
    assert_error(response, 400, "validation")

    # 5. Delete non-existing professor (invalid UUID)
    fake_professor_id = "00000000-0000-0000-0000-000000000000"
    response = requests.delete(f"{BASE_URL}/professors/{fake_professor_id}", headers=headers, timeout=TIMEOUT)
    assert_error(response, 404, "not found")

    # 6. Prevent deletion due to dependencies: Try to delete a department with active professors. We must get one department with dependencies first.
    # Get departments and pick one. If no departments, skip.
    resp_depts = requests.get(f"{BASE_URL}/departments", headers=headers, timeout=TIMEOUT)
    if resp_depts.status_code == 200 and resp_depts.json():
        departments = resp_depts.json()
        dep_with_dependency = None
        for dep in departments:
            dep_id = dep.get("id")
            # check if department has professors
            prof_resp = requests.get(f"{BASE_URL}/professors?departmentId={dep_id}", headers=headers, timeout=TIMEOUT)
            if prof_resp.status_code == 200 and prof_resp.json():
                dep_with_dependency = dep_id
                break
        if dep_with_dependency:
            del_resp = requests.delete(f"{BASE_URL}/departments/{dep_with_dependency}", headers=headers, timeout=TIMEOUT)
            assert_error(del_resp, 409, "conflict")

    # 7. Invalid query parameter: Request reports with invalid filter parameter (wrong type)
    invalid_query_params = {"academicYear": "not-a-year", "semester": "invalid-semester"}
    response = requests.get(f"{BASE_URL}/reports", headers=headers, params=invalid_query_params, timeout=TIMEOUT)
    assert_error(response, 400, "invalid")

    # 8. Access file explorer with unauthorized department id (simulate by using non-existing or wrong department id)
    # Since we cannot guarantee department IDs, use a random id
    forbidden_dep_id = "99999999-9999-9999-9999-999999999999"
    response = requests.get(f"{BASE_URL}/file-explorer/{forbidden_dep_id}", headers=headers, timeout=TIMEOUT)
    # Expected 403 Forbidden or 404 Not Found depending on implementation
    assert response.status_code in (403, 404)

    # 9. Submission status with invalid filter values (e.g., invalid course id)
    response = requests.get(f"{BASE_URL}/submission-status", headers=headers, params={"courseId": "invalid-course-id"}, timeout=TIMEOUT)
    assert_error(response, 400, "invalid")

    # 10. Report export with missing required parameters
    response = requests.get(f"{BASE_URL}/reports/export", headers=headers, timeout=TIMEOUT)
    assert_error(response, 400, "missing")

    # 11. Test expired JWT (simulate by tampering token expiry time if possible)
    # We cannot generate expired token here; simulate by using malformed but well-formed token structure
    expired_token = token[:-5] + "abcde"  # corrupted token
    expired_headers = {"Authorization": f"Bearer {expired_token}"}
    response = requests.get(f"{BASE_URL}/dashboard-overview", headers=expired_headers, timeout=TIMEOUT)
    assert_error(response, 401, "expired")

    # 12. Invalid method: POST on GET-only endpoint (reports tab overview)
    response = requests.post(f"{BASE_URL}/dashboard-overview", headers=headers, timeout=TIMEOUT)
    assert response.status_code in (405, 404)

    # 13. Partial input for required fields in professor creation (e.g., missing required fields)
    partial_payload = {"email": "professor@university.edu"}  # missing other required fields
    response = requests.post(f"{BASE_URL}/professors", headers=headers, json=partial_payload, timeout=TIMEOUT)
    assert_error(response, 400, "required")

    # 14. Filter professors with invalid filter combination (e.g., page with negative number)
    response = requests.get(f"{BASE_URL}/professors", headers=headers, params={"page": -1}, timeout=TIMEOUT)
    assert_error(response, 400, "invalid")

    # 15. Validate notification management error on invalid notification id
    invalid_notify_id = "00000000-0000-0000-0000-000000000000"
    resp_mark_read = requests.put(f"{BASE_URL}/notifications/{invalid_notify_id}/mark-read", headers=headers, timeout=TIMEOUT)
    assert_error(resp_mark_read, 404, "not found")


test_error_handling_and_user_feedback()