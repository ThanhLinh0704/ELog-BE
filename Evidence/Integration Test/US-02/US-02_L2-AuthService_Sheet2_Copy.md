# US-02 - L2 AuthService Sheet Copy Data

File này là bản dữ liệu riêng để copy vào sheet L2 của `Report 5.2_L2-IntegrationTests_Template.xlsx`.
Không thay thế hoặc chỉnh sửa file kết quả test hiện có.

## Dòng tiêu đề sheet

Copy vào các dòng đầu của sheet nếu muốn đổi sheet mẫu sang AuthService:

| Cell | Value |
|---|---|
| A1 | L2-AuthService \| methods: login, refresh, logout; security: JWT validation, role authorization |
| A2 | HOW TO USE: (1) Review Introduction sheet first (2) Copy rows below (3) Replace sample rows (4) Keep Status/Defect ID from US-02 evidence |

## Bảng copy vào Excel

Copy toàn bộ block TSV dưới đây và paste vào Excel từ ô `A4`.

```tsv
Test ID	Coverage Technique	SRS Reference	Feature	Priority	Services Involved	Infrastructure (Testcontainers)	External Mocks (WireMock)	Given (DB State / Setup)	When (Action / HTTP Call)	Then (Expected DB State + Events + Response)	Negative?	Status	Defect ID	Notes
▶ Block: login() | credentials, disabled account, refresh token creation														
L2-AUTH-01	Happy Path + Transaction Boundary	NFR-SEC; UC-01 precondition	Security / Auth	P1	ELog-BE AuthController/AuthService + Security + MySQL	MySQL 8 Testcontainer	None	users.admin exists; username=admin; password_hash matches Admin@2025; is_active=true; refresh_tokens may be empty	POST /api/auth/login body: {"username":"admin","password":"Admin@2025"}	HTTP 200; response.success=true; data.accessToken present; data.refreshToken present; data.tokenType=Bearer; data.expiresIn=900; data.username=admin; data.roles contains SYSTEM_ADMIN; DB refresh_tokens contains the issued refresh token for admin	No	Pass		Evidence: TASK-02-05 TC-01 PASS; local evidence ran on MySQL localhost:3307 / elog_db
L2-AUTH-02	Error Path	NFR-SEC; UC-01 precondition	Security / Auth	P1	ELog-BE AuthController/AuthService + Security + MySQL	MySQL 8 Testcontainer	None	users.admin exists; is_active=true; password_hash does not match WrongPass123	POST /api/auth/login body: {"username":"admin","password":"WrongPass123"}	HTTP 401; response.success=false; error.code=INVALID_CREDENTIALS; no valid accessToken returned; no new usable session should be created	Yes	Pass		Evidence: TASK-02-05 TC-02 PASS; verifies wrong password is rejected
L2-AUTH-03	Error Path	NFR-SEC; UC-01 precondition	Security / Auth	P1	ELog-BE AuthController/AuthService + Security + MySQL	MySQL 8 Testcontainer	None	Setup DB: UPDATE users SET is_active=0 WHERE username='driver01'; driver01 password remains Dev@2025	POST /api/auth/login body: {"username":"driver01","password":"Dev@2025"}	Expected HTTP 403; response.success=false; error.code=ACCOUNT_DISABLED; disabled account must be distinguishable from wrong password; restore DB: UPDATE users SET is_active=1 WHERE username='driver01'	Yes	Fail	BUG-AUTH-01	Evidence: TASK-02-05 TC-03 FAIL; actual HTTP 401 INVALID_CREDENTIALS; root cause in UserDetailsServiceImpl.findByUsernameAndIsActiveTrue
▶ Block: JWT authorization | valid token, missing token, wrong role, tampered signature														
L2-AUTH-04	Happy Path	UC-01; NFR-SEC; Role-based access control	Security / Auth	P1	ELog-BE JwtAuthFilter/UserController + MySQL	MySQL 8 Testcontainer	None	admin is active; valid accessToken issued from L2-AUTH-01; token contains role SYSTEM_ADMIN	GET /api/users with Authorization: Bearer <admin accessToken>	HTTP 200; response.success=true; user list returned; pagination present; password_hash is not returned in response	No	Pass		Evidence: TASK-02-05 TC-04 PASS; User Management endpoint is used to verify role authorization
L2-AUTH-05	Error Path	UC-01; NFR-SEC; Role-based access control	Security / Auth	P1	ELog-BE JwtAuthFilter/UserController + MySQL	MySQL 8 Testcontainer	None	No Authorization header is provided	GET /api/users without Authorization header	HTTP 401; response.success=false; error.code=AUTHENTICATION_FAILED; controller action is not executed	Yes	Pass		Evidence: TASK-02-05 TC-05 PASS; protected endpoint requires token
L2-AUTH-06	Error Path	UC-01; NFR-SEC; Role-based access control	Security / Auth	P1	ELog-BE JwtAuthFilter/UserController + MySQL	MySQL 8 Testcontainer	None	dispatcher01 exists; is_active=true; valid accessToken issued for role DISPATCHER	GET /api/users with Authorization: Bearer <dispatcher01 accessToken>	HTTP 403; response.success=false; error.code=ACCESS_DENIED; user list is not returned	Yes	Pass		Evidence: TASK-02-05 TC-06 PASS; valid token with wrong role is forbidden
L2-AUTH-09	Error Path	NFR-SEC	Security / Auth	P1	ELog-BE JwtUtils/JwtAuthFilter/UserController + MySQL	MySQL 8 Testcontainer	None	Valid admin accessToken exists, then signature segment is manually modified/tampered	GET /api/users with Authorization: Bearer <tampered accessToken>	HTTP 401; response.success=false; error.code=AUTHENTICATION_FAILED; no user list returned; invalid signature is handled without server error	Yes	Pass		Evidence: TASK-02-05 TC-09 PASS; BUG-AUTH-02 was found and fixed by catching SignatureException in JwtUtils.validateToken()
▶ Block: refresh/logout() | refresh token lifecycle														
L2-AUTH-07	Happy Path + Transaction Boundary	NFR-SEC	Security / Auth	P1	ELog-BE AuthController/AuthService + MySQL	MySQL 8 Testcontainer	None	Valid refreshToken exists in refresh_tokens for admin; refreshToken.expiry_date > NOW()	POST /api/auth/refresh body: {"refreshToken":"<valid refreshToken from L2-AUTH-01>"}	HTTP 200; response.success=true; data.accessToken present and usable; data.expiresIn=900; existing refresh token remains valid until logout/expiry	No	Pass		Evidence: TASK-02-05 TC-07 PASS; verifies DB-backed refresh token can issue a new access token
L2-AUTH-08	Transaction Boundary + Error Path	NFR-SEC	Security / Auth	P1	ELog-BE AuthController/AuthService + MySQL	MySQL 8 Testcontainer	None	Valid refreshToken exists in refresh_tokens for admin from L2-AUTH-01	Step 1: POST /api/auth/logout body: {"refreshToken":"<valid refreshToken>"}; Step 2: POST /api/auth/refresh body: {"refreshToken":"<same refreshToken>"}	Logout returns HTTP 200 and success=true; DB refresh_tokens no longer contains that token; second refresh returns HTTP 401 with error.code=TOKEN_INVALID	Yes	Pass		Evidence: TASK-02-05 TC-08 PASS; verifies logout invalidates refresh token at DB level
▶ Template Row														
L2-AUTH-[NN]	[Happy Path | Error Path | Transaction Boundary]	[NFR-SEC | UC-01 precondition | Role-based access control]	Security / Auth	P2	ELog-BE Auth/Security + MySQL	MySQL 8 Testcontainer	None	[users/refresh_tokens state before test]	[POST /api/auth/login OR POST /api/auth/refresh OR POST /api/auth/logout OR GET protected endpoint]	[HTTP status + response code/body + DB state in refresh_tokens/users]	[Yes/No]	[Not Run/Pass/Fail]	[BUG-AUTH-xx if Fail]	[Evidence file, TC number, setup/restore note]
```

## Gợi ý đổi tên sheet

Đổi tên sheet từ mẫu hiện tại thành `L2-AuthService`.
