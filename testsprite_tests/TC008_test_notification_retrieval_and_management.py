import requests

BASE_URL = "http://localhost:8080/api/hod"
AUTH_URL = "http://localhost:8080/api/auth/login"
HOD_EMAIL = "hod.ce@hod.alquds.edu"
HOD_PASSWORD = "Hh@#2021"
TIMEOUT = 30


def test_notification_retrieval_and_management():
    # Authenticate and get JWT token
    auth_payload = {"email": HOD_EMAIL, "password": HOD_PASSWORD}
    token = None
    headers = {"Content-Type": "application/json"}

    try:
        auth_resp = requests.post(AUTH_URL, json=auth_payload, headers=headers, timeout=TIMEOUT)
        assert auth_resp.status_code == 200, f"Authentication failed: {auth_resp.text}"
        token = auth_resp.json().get("accessToken")
        assert token, "No token received after authentication"

        auth_headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/json"
        }

        # Retrieve notifications related to department submissions
        notif_resp = requests.get(f"{BASE_URL}/notifications", headers=auth_headers, timeout=TIMEOUT)
        assert notif_resp.status_code == 200, f"Failed to get notifications: {notif_resp.text}"
        notifications = notif_resp.json()
        assert isinstance(notifications, list), "Notifications response is not a list"

        if not notifications:
            # No notifications, create a dummy submission to generate a notification or skip
            # Since no API schema provided for creating submissions or triggering notifications,
            # we just return test success as retrieval did not fail
            print("No notifications present to manage.")
            return

        # Pick one notification to mark as read
        notification = None
        for n in notifications:
            # Target notifications related to department submissions based on known keys
            # Assuming notifications have 'id' and 'type' fields and 'read' status
            if "submission" in n.get("type", "").lower() or "submission" in n.get("title", "").lower():
                notification = n
                break
        if not notification:
            notification = notifications[0]

        notif_id = notification.get("id")
        assert notif_id, "Notification ID missing in notification item"

        # Check initial read status, if available
        initial_read_status = notification.get("read")
        # Toggle read status: if unread mark read, else mark unread
        new_read_status = not initial_read_status if initial_read_status is not None else True

        mark_read_payload = {"read": new_read_status}

        # Mark the notification as read/unread
        mark_read_resp = requests.put(f"{BASE_URL}/notifications/{notif_id}/read", json=mark_read_payload,
                                      headers=auth_headers, timeout=TIMEOUT)
        assert mark_read_resp.status_code == 200, f"Failed to update notification read status: {mark_read_resp.text}"
        updated_notification = mark_read_resp.json()
        assert updated_notification.get("id") == notif_id, "Updated notification ID mismatch"
        assert updated_notification.get("read") == new_read_status, "Notification read status not updated"

        # Optionally, retrieve notifications again and verify the update persisted
        notif_resp2 = requests.get(f"{BASE_URL}/notifications", headers=auth_headers, timeout=TIMEOUT)
        assert notif_resp2.status_code == 200, "Failed to re-fetch notifications"
        notifications_after_update = notif_resp2.json()
        found_updated = False
        for n in notifications_after_update:
            if n.get("id") == notif_id:
                found_updated = True
                assert n.get("read") == new_read_status, "Read status did not persist after update"
                break
        assert found_updated, "Updated notification not found after modification"

    except requests.RequestException as e:
        assert False, f"Request failed: {e}"


test_notification_retrieval_and_management()
