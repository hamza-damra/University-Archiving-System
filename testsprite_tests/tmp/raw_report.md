
# TestSprite AI Testing Report(MCP)

---

## 1️⃣ Document Metadata
- **Project Name:** University-Archiving-System
- **Date:** 2026-01-13
- **Prepared by:** TestSprite AI Team

---

## 2️⃣ Requirement Validation Summary

#### Test TC001 verify_jwt_authentication_and_hod_role_validation
- **Test Code:** [TC001_verify_jwt_authentication_and_hod_role_validation.py](./TC001_verify_jwt_authentication_and_hod_role_validation.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 44, in <module>
  File "<string>", line 26, in test_verify_jwt_authentication_and_hod_role_validation
AssertionError: No accessToken found in login response

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/511f025f-5547-41a3-ac3a-2ebb38cbe9e1
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC002 test_hod_dashboard_statistics_and_filters
- **Test Code:** [TC002_test_hod_dashboard_statistics_and_filters.py](./TC002_test_hod_dashboard_statistics_and_filters.py)
- **Test Error:** Traceback (most recent call last):
  File "<string>", line 21, in test_hod_dashboard_statistics_and_filters
AssertionError: Authentication failed: {"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many login attempts. Please wait before trying again.","retryAfterSeconds":3},"timestamp":1768310157162}

During handling of the above exception, another exception occurred:

Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 118, in <module>
  File "<string>", line 26, in test_hod_dashboard_statistics_and_filters
AssertionError: Authentication request failed: Authentication failed: {"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many login attempts. Please wait before trying again.","retryAfterSeconds":3},"timestamp":1768310157162}

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/6f6b61a8-6801-4d2b-97a7-ae1118eba77f
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC003 test_professor_crud_operations_within_department
- **Test Code:** [TC003_test_professor_crud_operations_within_department.py](./TC003_test_professor_crud_operations_within_department.py)
- **Test Error:** Traceback (most recent call last):
  File "<string>", line 15, in authenticate
  File "/var/task/requests/models.py", line 1024, in raise_for_status
    raise HTTPError(http_error_msg, response=self)
requests.exceptions.HTTPError: 429 Client Error: Too Many Requests for url: http://localhost:8080/api/auth/login

During handling of the above exception, another exception occurred:

Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 105, in <module>
  File "<string>", line 27, in test_professor_crud_operations_within_department
  File "<string>", line 23, in authenticate
RuntimeError: Authentication failed: 429 Client Error: Too Many Requests for url: http://localhost:8080/api/auth/login

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/69a08740-f5f7-4bdf-80b0-3063a5caa033
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC004 test_submission_status_filtering_and_retrieval
- **Test Code:** [TC004_test_submission_status_filtering_and_retrieval.py](./TC004_test_submission_status_filtering_and_retrieval.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 83, in <module>
  File "<string>", line 18, in test_submission_status_filtering_and_retrieval
AssertionError: Login failed: 

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/fae19be3-7b72-42e0-8dd6-c7527f62eb60
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC005 test_document_request_creation_view_and_deletion
- **Test Code:** [TC005_test_document_request_creation_view_and_deletion.py](./TC005_test_document_request_creation_view_and_deletion.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 76, in <module>
  File "<string>", line 20, in test_document_request_creation_view_and_deletion
AssertionError: Login failed with status code 403

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/ce07549a-a4e7-41e8-b511-34a0663f03da
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC006 test_report_generation_and_pdf_export
- **Test Code:** [TC006_test_report_generation_and_pdf_export.py](./TC006_test_report_generation_and_pdf_export.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 74, in <module>
  File "<string>", line 19, in test_report_generation_and_pdf_export
AssertionError: Login failed: 

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/9878749d-b36e-4bc4-9974-ea17fd0a521e
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC007 test_file_explorer_department_scoped_access
- **Test Code:** [TC007_test_file_explorer_department_scoped_access.py](./TC007_test_file_explorer_department_scoped_access.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 62, in <module>
  File "<string>", line 18, in test_file_explorer_department_scoped_access
AssertionError: Authentication failed: 

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/65c05e68-5a07-48ad-9734-ca2a0ab79d12
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC008 test_notification_retrieval_and_management
- **Test Code:** [TC008_test_notification_retrieval_and_management.py](./TC008_test_notification_retrieval_and_management.py)
- **Test Error:** Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 85, in <module>
  File "<string>", line 18, in test_notification_retrieval_and_management
AssertionError: Authentication failed: {"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many login attempts. Please wait before trying again.","retryAfterSeconds":11},"timestamp":1768310149479}

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/5b0f4783-dc42-411f-9697-aad26ace845c
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC009 test_semester_based_operation_scoping
- **Test Code:** [TC009_test_semester_based_operation_scoping.py](./TC009_test_semester_based_operation_scoping.py)
- **Test Error:** Traceback (most recent call last):
  File "<string>", line 27, in test_semester_based_operation_scoping
AssertionError: Login failed: 

During handling of the above exception, another exception occurred:

Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 124, in <module>
  File "<string>", line 121, in test_semester_based_operation_scoping
AssertionError: Test failed: Login failed: 

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/0d725ce2-d3e7-46be-b697-1f345bf319ed
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---

#### Test TC010 test_error_handling_and_user_feedback
- **Test Code:** [TC010_test_error_handling_and_user_feedback.py](./TC010_test_error_handling_and_user_feedback.py)
- **Test Error:** Traceback (most recent call last):
  File "<string>", line 18, in authenticate
  File "/var/task/requests/models.py", line 1024, in raise_for_status
    raise HTTPError(http_error_msg, response=self)
requests.exceptions.HTTPError: 429 Client Error: Too Many Requests for url: http://localhost:8080/api/auth/login

During handling of the above exception, another exception occurred:

Traceback (most recent call last):
  File "/var/task/handler.py", line 258, in run_with_retry
    exec(code, exec_env)
  File "<string>", line 130, in <module>
  File "<string>", line 28, in test_error_handling_and_user_feedback
  File "<string>", line 24, in authenticate
Exception: Authentication failed: 429 Client Error: Too Many Requests for url: http://localhost:8080/api/auth/login

- **Test Visualization and Result:** https://www.testsprite.com/dashboard/mcp/tests/b24de6fe-9059-4ad4-bf44-fe6e04618daf/1298d934-8765-4a89-aa90-6b34faea0d89
- **Status:** ❌ Failed
- **Analysis / Findings:** {{TODO:AI_ANALYSIS}}.
---


## 3️⃣ Coverage & Matching Metrics

- **0.00** of tests passed

| Requirement        | Total Tests | ✅ Passed | ❌ Failed  |
|--------------------|-------------|-----------|------------|
| ...                | ...         | ...       | ...        |
---


## 4️⃣ Key Gaps / Risks
{AI_GNERATED_KET_GAPS_AND_RISKS}
---