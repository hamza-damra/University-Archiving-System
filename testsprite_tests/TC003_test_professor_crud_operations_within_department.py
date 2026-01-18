import requests
import uuid

BASE_URL = "http://localhost:8080/api/hod"
HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"

TIMEOUT = 30


def authenticate(email: str, password: str) -> str:
    auth_url = "http://localhost:8080/api/auth/login"
    try:
        resp = requests.post(auth_url, json={"email": email, "password": password}, timeout=TIMEOUT)
        resp.raise_for_status()
        json_resp = resp.json()
        token = json_resp.get("token")
        if not token:
            token = json_resp.get("accessToken")
        assert token, "Authentication token not found in response"
        return token
    except Exception as e:
        raise RuntimeError(f"Authentication failed: {e}")


def test_professor_crud_operations_within_department():
    token = authenticate(HOD_EMAIL, HOD_PASSWORD)
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    # 1. Get HOD department info (to scope professors)
    dept_resp = requests.get(f"{BASE_URL}/profile", headers=headers, timeout=TIMEOUT)
    dept_resp.raise_for_status()
    profile_data = dept_resp.json()
    department_id = profile_data.get("departmentId")
    assert department_id, "Department ID not found in HOD profile"

    # Assume there is an academic year and semester selection endpoint or fixed values for test scope:
    # For testing, assume params for semester scoping:
    academic_year = profile_data.get("academicYear", "2025-2026")
    semester = profile_data.get("semester", "Fall")

    professor_id = None
    professor_payload = {
        "name": "Test Professor " + str(uuid.uuid4())[:8],
        "email": f"testprof{uuid.uuid4().hex[:6]}@faculty.alquds.edu",
        "phone": "0599123456",
        "departmentId": department_id,
        "academicYear": academic_year,
        "semester": semester,
        "title": "Associate Professor"
    }

    try:
        # CREATE Professor
        create_resp = requests.post(f"{BASE_URL}/professors", headers=headers, json=professor_payload, timeout=TIMEOUT)
        create_resp.raise_for_status()
        created_professor = create_resp.json()
        professor_id = created_professor.get("id")
        assert professor_id, "Created professor ID not returned"
        assert created_professor["email"] == professor_payload["email"]
        assert created_professor["departmentId"] == department_id

        # READ - Get professor by ID and verify fields and scoping
        get_resp = requests.get(f"{BASE_URL}/professors/{professor_id}", headers=headers, timeout=TIMEOUT)
        get_resp.raise_for_status()
        professor_data = get_resp.json()
        assert professor_data["id"] == professor_id
        assert professor_data["departmentId"] == department_id
        assert professor_data["academicYear"] == academic_year
        assert professor_data["semester"] == semester

        # UPDATE professor - modify phone and title
        updated_payload = {
            "phone": "0599765432",
            "title": "Professor"
        }
        update_resp = requests.put(f"{BASE_URL}/professors/{professor_id}", headers=headers, json=updated_payload, timeout=TIMEOUT)
        update_resp.raise_for_status()
        updated_professor = update_resp.json()
        assert updated_professor["phone"] == updated_payload["phone"]
        assert updated_professor["title"] == updated_payload["title"]

        # LIST professors in department and semester - Verify the created professor appears
        params = {"departmentId": department_id, "academicYear": academic_year, "semester": semester}
        list_resp = requests.get(f"{BASE_URL}/professors", headers=headers, params=params, timeout=TIMEOUT)
        list_resp.raise_for_status()
        prof_list = list_resp.json()
        assert any(p["id"] == professor_id for p in prof_list), "Created professor not found in filtered professor list"

        # VALIDATION - try creating professor with duplicate email - should fail
        dup_resp = requests.post(f"{BASE_URL}/professors", headers=headers, json=professor_payload, timeout=TIMEOUT)
        assert dup_resp.status_code == 400 or dup_resp.status_code == 409, "Duplicate professor creation should fail"

    finally:
        # CLEANUP - DELETE professor
        if professor_id:
            del_resp = requests.delete(f"{BASE_URL}/professors/{professor_id}", headers=headers, timeout=TIMEOUT)
            # Deletion might be soft delete; accept 200 or 204 or 202
            assert del_resp.status_code in [200, 202, 204], "Professor deletion failed"


test_professor_crud_operations_within_department()