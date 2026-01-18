import requests

BASE_URL = "http://localhost:8080/api/hod"
LOGIN_ENDPOINT = "http://localhost:8080/api/auth/login"
TIMEOUT = 30

HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"


def test_verify_jwt_authentication_and_hod_role_validation():
    # Attempt accessing endpoint without token - expect 401 Unauthorized
    resp = requests.get(f"{BASE_URL}/dashboard", timeout=TIMEOUT)
    assert resp.status_code == 401 or resp.status_code == 403, f"Expected 401/403 for no token but got {resp.status_code}"

    # Attempt login as HOD to get JWT token
    login_payload = {
        "email": HOD_EMAIL,
        "password": HOD_PASSWORD
    }
    login_resp = requests.post(LOGIN_ENDPOINT, json=login_payload, timeout=TIMEOUT)
    assert login_resp.status_code == 200, f"Login failed with status {login_resp.status_code}"
    login_json = login_resp.json()

    token = login_json.get("accessToken")
    assert token is not None, "No accessToken found in login response"

    headers = {"Authorization": f"Bearer {token}"}

    # Access dashboard endpoint with valid token - expect 200 OK
    dashboard_resp = requests.get(f"{BASE_URL}/dashboard", headers=headers, timeout=TIMEOUT)
    assert dashboard_resp.status_code == 200, f"Dashboard access failed with valid token, status {dashboard_resp.status_code}"

    # Access another endpoint that requires HOD role, e.g. professors list
    professors_resp = requests.get(f"{BASE_URL}/professors", headers=headers, timeout=TIMEOUT)
    assert professors_resp.status_code == 200, f"Professors endpoint access failed, status {professors_resp.status_code}"

    # Access with invalid token - expect 401 or 403
    invalid_headers = {"Authorization": "Bearer invalidtoken123"}
    invalid_token_resp = requests.get(f"{BASE_URL}/dashboard", headers=invalid_headers, timeout=TIMEOUT)
    assert invalid_token_resp.status_code == 401 or invalid_token_resp.status_code == 403, f"Expected 401/403 for invalid token but got {invalid_token_resp.status_code}"


test_verify_jwt_authentication_and_hod_role_validation()
