import requests

BASE_URL = "http://localhost:8080/api/hod"
USERNAME = "hod.ce@hod.alquds.edu"
PASSWORD = "Hh@#2021"
TIMEOUT = 30

def test_submission_status_filtering_and_retrieval():
    session = requests.Session()
    token = None
    try:
        # Authenticate and obtain JWT token
        login_resp = session.post(
            f"{BASE_URL}/auth/login",
            json={"username": USERNAME, "password": PASSWORD},
            timeout=TIMEOUT
        )
        assert login_resp.status_code == 200, f"Login failed: {login_resp.text}"
        login_data = login_resp.json()
        assert "token" in login_data, "JWT token not found in login response"
        token = login_data["token"]
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        }

        # Prepare filter parameters for submission status
        # We need to test filtering by course, document type, and submission status
        # Since no specific values are given, fetch possible courses and document types first to get valid filter values

        # 1. Retrieve courses for the HOD's department
        courses_resp = session.get(f"{BASE_URL}/courses", headers=headers, timeout=TIMEOUT)
        assert courses_resp.status_code == 200, f"Getting courses failed: {courses_resp.text}"
        courses = courses_resp.json()
        assert isinstance(courses, list), "Courses response is not a list"

        if not courses:
            raise AssertionError("No courses found for department to use as filter")

        course_id = courses[0].get("id")
        assert course_id is not None, "Course ID missing in course data"

        # 2. Retrieve document types for filtering - assuming API endpoint exists for document types
        doc_types_resp = session.get(f"{BASE_URL}/document-types", headers=headers, timeout=TIMEOUT)
        assert doc_types_resp.status_code == 200, f"Getting document types failed: {doc_types_resp.text}"
        document_types = doc_types_resp.json()
        assert isinstance(document_types, list), "Document types response is not a list"

        if not document_types:
            raise AssertionError("No document types found to use as filter")

        document_type_id = document_types[0].get("id")
        assert document_type_id is not None, "Document type ID missing in document type data"

        # 3. Possible submission statuses to filter by, assuming these are the values accepted
        submission_statuses = ["pending", "approved", "rejected"]  # typical statuses, adjust if API docs specify differently
        submission_status = submission_statuses[0]

        # Prepare filter payload or query params - guessing API spec based on usual patterns
        params = {
            "courseId": course_id,
            "documentTypeId": document_type_id,
            "submissionStatus": submission_status
        }

        # Retrieve filtered submission status data
        submissions_resp = session.get(f"{BASE_URL}/submission-status", headers=headers, params=params, timeout=TIMEOUT)
        assert submissions_resp.status_code == 200, f"Submission status filtering request failed: {submissions_resp.text}"
        submissions_data = submissions_resp.json()
        assert isinstance(submissions_data, list), "Submission status response is not a list"

        # Validate that all returned submissions match the filters applied
        for submission in submissions_data:
            # Assuming submission contains keys courseId, documentTypeId, and status keys to verify filtering
            assert submission.get("courseId") == course_id, f"Submission courseId mismatch: expected {course_id}, got {submission.get('courseId')}"
            assert submission.get("documentTypeId") == document_type_id, f"Submission documentTypeId mismatch: expected {document_type_id}, got {submission.get('documentTypeId')}"
            assert submission.get("status") == submission_status, f"Submission status mismatch: expected {submission_status}, got {submission.get('status')}"

    finally:
        session.close()

test_submission_status_filtering_and_retrieval()