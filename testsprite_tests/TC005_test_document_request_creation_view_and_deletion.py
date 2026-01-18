import requests
import json

BASE_URL = "http://localhost:8080/api/hod"
LOGIN_URL = f"{BASE_URL}/auth/login"
DOCUMENT_REQUEST_URL = f"{BASE_URL}/document-requests"
HEADERS = {"Content-Type": "application/json"}
TIMEOUT = 30

hod_credentials = {
    "email": "hod.ce@hod.alquds.edu",
    "password": "Hh@#2021"
}

def test_document_request_creation_view_and_deletion():
    session = requests.Session()
    try:
        # Authenticate to get JWT token
        resp = session.post(LOGIN_URL, json=hod_credentials, headers=HEADERS, timeout=TIMEOUT)
        assert resp.status_code == 200, f"Login failed with status code {resp.status_code}"
        token = resp.json().get("token")
        assert token, "JWT token not found in login response"
        auth_headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        # Step 1: Create a new document request for a professor in the HOD's department
        # As no explicit payload schema is provided, assume minimal required fields: professorId, documentType, dueDate
        # First, find a professor in the department to request document for
        prof_resp = session.get(f"{BASE_URL}/professors", headers=auth_headers, timeout=TIMEOUT)
        assert prof_resp.status_code == 200, f"Failed to fetch professors with status code {prof_resp.status_code}"
        professors = prof_resp.json()
        assert isinstance(professors, list) and len(professors) > 0, "No professors found in HOD department"
        professor_id = professors[0].get("id")
        assert professor_id, "Professor id missing"

        # Prepare the document request payload
        import datetime
        due_date = (datetime.datetime.now() + datetime.timedelta(days=30)).strftime("%Y-%m-%d")

        create_payload = {
            "professorId": professor_id,
            "documentType": "Transcript",
            "dueDate": due_date,
            "notes": "Test document request creation"
        }

        create_resp = session.post(DOCUMENT_REQUEST_URL, json=create_payload, headers=auth_headers, timeout=TIMEOUT)
        assert create_resp.status_code == 201, f"Document request creation failed with status {create_resp.status_code}"
        created_req = create_resp.json()
        request_id = created_req.get("id")
        assert request_id, "Created document request ID missing"

        # Step 2: View the created document request and validate fields
        get_resp = session.get(f"{DOCUMENT_REQUEST_URL}/{request_id}", headers=auth_headers, timeout=TIMEOUT)
        assert get_resp.status_code == 200, f"Failed to get created document request, status {get_resp.status_code}"
        fetched_req = get_resp.json()
        assert fetched_req.get("id") == request_id, "Fetched document request ID mismatch"
        assert fetched_req.get("professorId") == professor_id, "Professor ID mismatch in fetched document request"
        assert fetched_req.get("documentType") == create_payload["documentType"], "Document type mismatch"
        assert fetched_req.get("dueDate") == create_payload["dueDate"], "Due date mismatch"
        assert fetched_req.get("notes") == create_payload["notes"], "Notes mismatch"

        # Step 3: List all document requests for the department, confirm the created request is present
        list_resp = session.get(DOCUMENT_REQUEST_URL, headers=auth_headers, timeout=TIMEOUT)
        assert list_resp.status_code == 200, f"Failed to list document requests, status {list_resp.status_code}"
        requests_list = list_resp.json()
        assert any(req.get("id") == request_id for req in requests_list), "Created document request not found in listing"

    finally:
        # Clean up - delete the created document request
        if 'request_id' in locals():
            del_resp = session.delete(f"{DOCUMENT_REQUEST_URL}/{request_id}", headers=auth_headers, timeout=TIMEOUT)
            assert del_resp.status_code in (200, 204), f"Failed to delete document request with status {del_resp.status_code}"

test_document_request_creation_view_and_deletion()