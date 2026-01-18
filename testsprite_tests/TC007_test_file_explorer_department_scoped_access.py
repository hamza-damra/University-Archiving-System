import requests

BASE_URL = "http://localhost:8080/api/hod"
AUTH_URL = f"{BASE_URL}/auth/login"
FILE_EXPLORER_BROWSE_URL = f"{BASE_URL}/file-explorer/browse"
FILE_EXPLORER_DOWNLOAD_URL = f"{BASE_URL}/file-explorer/download"

HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"
TIMEOUT = 30

def test_file_explorer_department_scoped_access():
    session = requests.Session()
    try:
        # Authenticate to get JWT token
        auth_payload = {"username": HOD_EMAIL, "password": HOD_PASSWORD}
        auth_resp = session.post(AUTH_URL, json=auth_payload, timeout=TIMEOUT)
        assert auth_resp.status_code == 200, f"Authentication failed: {auth_resp.text}"
        auth_data = auth_resp.json()
        token = auth_data.get("token") or auth_data.get("accessToken")
        assert token, "JWT token missing in auth response"

        headers = {"Authorization": f"Bearer {token}"}

        # 1. Browse files within HOD department archive - expect success
        browse_params = {"path": "/"}  # Root of department archive
        browse_resp = session.get(FILE_EXPLORER_BROWSE_URL, headers=headers, params=browse_params, timeout=TIMEOUT)
        assert browse_resp.status_code == 200, f"Failed to browse department files: {browse_resp.text}"
        browse_data = browse_resp.json()
        assert isinstance(browse_data, dict) and "files" in browse_data, "Invalid browse response structure"
        files_list = browse_data.get("files", [])
        assert isinstance(files_list, list), "Files list should be a list"

        if files_list:
            file_item = files_list[0]
            file_path = file_item.get("path") or file_item.get("name")
            assert file_path, "File path/name missing"

            # 2. Download the file within department archive - expect success
            download_params = {"path": file_path}
            download_resp = session.get(FILE_EXPLORER_DOWNLOAD_URL, headers=headers, params=download_params, timeout=TIMEOUT)
            assert download_resp.status_code == 200, f"Failed to download file within department: {download_resp.text}"
            content_type = download_resp.headers.get("Content-Type", "")
            assert content_type.startswith("application/") or content_type.startswith("text/") or content_type == "application/octet-stream", \
                f"Unexpected content type for downloaded file: {content_type}"
            assert download_resp.content, "Downloaded file content is empty"

        # 3. Attempt browsing files outside the department archive - expect failure or empty
        invalid_browse_params = {"path": "../../"}
        invalid_browse_resp = session.get(FILE_EXPLORER_BROWSE_URL, headers=headers, params=invalid_browse_params, timeout=TIMEOUT)
        assert invalid_browse_resp.status_code in (400,403,404) or (invalid_browse_resp.status_code == 200 and not invalid_browse_resp.json().get("files")), \
            "Browsing outside department scope was not blocked"

        # 4. Attempt to download a file outside department scope - expect error response
        invalid_download_params = {"path": "../../etc/passwd"}
        invalid_download_resp = session.get(FILE_EXPLORER_DOWNLOAD_URL, headers=headers, params=invalid_download_params, timeout=TIMEOUT)
        assert invalid_download_resp.status_code in (400,403,404), "Download outside department scope was not blocked"

    finally:
        session.close()

test_file_explorer_department_scoped_access()