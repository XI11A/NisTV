# AFDS Sign-Up API & Flow Specification

> **Target URL**: `https://afds.hashhackers.com/signup`  
> **Environment Verified**: Termux Chromium Browser (Headless CDP & Live API Inspection)  
> **Document Purpose**: Complete technical reference and architecture blueprint for integrating the AFDS Sign-Up flow natively into the NisTV Android app.

---

## 1. Overview & Service Architecture

The HashHackers AFDS (Advanced File Distribution System / Archive) signup process is a 2-step email verification workflow:
1. **User Details Submission**: The client submits user information (first name, last name, email, invite code). The server validates the fields and dispatches a 6-digit numeric OTP to the user's email.
2. **OTP Verification & Account Provisioning**: The client submits the 6-digit code and email. The server creates the account and sends a welcome confirmation email.

### Base URLs & Fallback Strategy

The web application implements automatic dynamic failover between the direct API backend and a Cloudflare reverse proxy:

| Target | URL | Usage |
|---|---|---|
| **Primary Backend API** | `https://tga-hd.api.hashhackers.com` | Preferred direct endpoint |
| **Fallback Reverse Proxy** | `https://afds.hashhackers.com/proxyapi` | Used if direct backend is unreachable or blocked |

#### Healthcheck Probe
- **Endpoint**: `GET /alive`
- **Method**: `GET`
- **Response**: `HTTP 200 OK`
```json
{
  "status": "ok",
  "service": "tgarchive-hyperdrive-api",
  "timestamp": "2026-11-06T11:00:03.041Z"
}
```

---

## 2. API Endpoints Specification

### 2.1 Step 1: Request Verification Code

- **Route**: `POST /api/request-verification`
- **Full URLs**:
  - Primary: `https://tga-hd.api.hashhackers.com/api/request-verification`
  - Fallback: `https://afds.hashhackers.com/proxyapi/api/request-verification`
- **Headers**:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```

#### Request Payload
```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "adalovelace@gmail.com",
  "inviteCode": "WELCOME"
}
```

#### Field Specifications & Validation Rules
| Field | Type | Required | Default | Validation & Constraints |
|---|---|---|---|---|
| `firstName` | `String` | **Yes** | — | Non-empty trimmed string. |
| `lastName` | `String` | **Yes** | — | Non-empty trimmed string. |
| `email` | `String` | **Yes** | — | Must be a recognized provider domain (see whitelist below). Must not contain `+` alias character. |
| `inviteCode` | `String` | **Yes** | `"WELCOME"` | Case-sensitive/normalized invite code. Default public invite code is `"WELCOME"`. |

#### Email Provider Whitelist & Normalization Rules
1. **No `+` aliases**: The web client uses regex pattern `[^+]*` with title `"Email addresses with + sign are not allowed"`. The backend also rejects aliases.
2. **Provider Whitelist**: The backend validates domains and rejects generic, disposable, or unapproved domains with:  
   `"Please use a valid email provider (Gmail, Outlook, Yahoo, Proton, iCloud, etc.)."`  
   *Accepted providers*: Gmail (`@gmail.com`, `@googlemail.com`), Microsoft Outlook/Hotmail (`@outlook.com`, `@hotmail.com`), Yahoo (`@yahoo.com`), ProtonMail (`@proton.me`, `@protonmail.com`), Apple iCloud (`@icloud.com`).
3. **Gmail Normalization**:
   - Converts to lowercase and trims whitespace.
   - For `@gmail.com` and `@googlemail.com`, dots (`.`) in the local part are stripped (e.g., `ada.lovelace@gmail.com` -> `adalovelace@gmail.com`).
   - Domain is normalized to `@gmail.com`.

#### Responses
- **Success (`HTTP 200 OK`)**:
  ```json
  {
    "message": "Verification code sent to your email."
  }
  ```
- **Error Responses (`HTTP 400 Bad Request`)**:
  | Condition | Status | Response Body |
  |---|---|---|
  | Invalid/missing invite code | `400` | `{"error": "Invalid invite code."}` |
  | Missing first/last name | `400` | `{"error": "First and last name are required."}` |
  | Missing or malformed email | `400` | `{"error": "Please provide a valid email address."}` |
  | Unaccepted email provider | `400` | `{"error": "Please use a valid email provider (Gmail, Outlook, Yahoo, Proton, iCloud, etc.)."}` |
  | Rate limiting / rapid calls | `400` | `{"error": "Unable to process your request. Please try again later."}` |

---

### 2.2 Step 2: Verify Code & Complete Signup

- **Route**: `POST /api/verify`
- **Full URLs**:
  - Primary: `https://tga-hd.api.hashhackers.com/api/verify`
  - Fallback: `https://afds.hashhackers.com/proxyapi/api/verify`
- **Headers**:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```

#### Request Payload
```json
{
  "email": "adalovelace@gmail.com",
  "code": "123456"
}
```

#### Field Specifications & Validation Rules
| Field | Type | Required | Constraints |
|---|---|---|---|
| `email` | `String` | **Yes** | Must match the email submitted in Step 1. |
| `code` | `String` | **Yes** | 6 numeric digits matching regex `^[0-9]{6}$`. |

#### Responses
- **Success (`HTTP 200 OK`)**:
  ```json
  {
    "message": "Signup successful! You can now login."
  }
  ```
  *(Note: A welcome email is dispatched to the user. The response does not return an auth session token directly; see Section 4 for authentication flow).*
- **Error Responses (`HTTP 400 Bad Request`)**:
  | Condition | Status | Response Body |
  |---|---|---|
  | Missing email or code | `400` | `{"error": "Email and verification code are required."}` |
  | Invalid or expired code | `400` | `{"error": "Invalid verification code."}` |

#### Live Verification Test Case (Verified in Termux Chromium & API)
- **Account Registered**: `nissyssoftwares@gmail.com`
- **Request Verification Call**:
  ```http
  POST https://tga-hd.api.hashhackers.com/api/request-verification
  Content-Type: application/json

  {"firstName":"NisTV","lastName":"Softwares","email":"nissyssoftwares@gmail.com","inviteCode":"WELCOME"}
  ```
  Response (`HTTP 200 OK`): `{"message": "Verification code sent to your email."}`
- **Verify Code Call**:
  ```http
  POST https://tga-hd.api.hashhackers.com/api/verify
  Content-Type: application/json

  {"email":"nissyssoftwares@gmail.com","code":"828276"}
  ```
  Response (`HTTP 200 OK`): `{"message": "Signup successful! You can now login."}`
- **Subsequent Login OTP Check**:
  ```http
  POST https://tga-hd.api.hashhackers.com/request-login-otp
  Content-Type: application/json

  {"email":"nissyssoftwares@gmail.com"}
  ```
  Response (`HTTP 200 OK`): `{"message": "Login code sent to your email.", "login_type": "email"}`
  *(Confirmed account is active and ready for immediate login)*

---

## 3. Web UI Flow & Interaction Details

Based on live Termux Chromium DOM analysis of `https://afds.hashhackers.com/signup`:

```
┌────────────────────────────────────────────────────────┐
│                   [ AFDS Logo ]                        │
│               Create account Header                    │
│        Join free — no credit card needed               │
├────────────────────────────────────────────────────────┤
│           Step Indicator:  (1) ───○─── (2)             │
├────────────────────────────────────────────────────────┤
│ [Step 1: Details Form]                                 │
│ ┌──────────────────────┐  ┌──────────────────────────┐ │
│ │ First Name (e.g. Ada)│  │ Last Name (Lovelace)     │ │
│ └──────────────────────┘  └──────────────────────────┘ │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Email Address (you@example.com)                    │ │
│ └────────────────────────────────────────────────────┘ │
│  (Real-time preview: "📧 Will be saved as: ...")       │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Invite Code (Default pre-filled: "WELCOME")        │ │
│ └────────────────────────────────────────────────────┘ │
│ [ Continue -> (Submits /api/request-verification) ]    │
├────────────────────────────────────────────────────────┤
│ [Step 2: OTP Verification Form] (hidden by default)   │
│            Verification code sent to <email>           │
│ ┌────────────────────────────────────────────────────┐ │
│ │ [ 0 0 0 0 0 0 ] (6-digit spaced input)             │ │
│ └────────────────────────────────────────────────────┘ │
│ [ Verify & Create Account -> (Submits /api/verify) ]   │
│ [ <- Back to Details ]                                 │
├────────────────────────────────────────────────────────┤
│ [Footer Link] "Already have an account? Sign in"       │
└────────────────────────────────────────────────────────┘
```

---

## 4. Post-Signup Authentication Transition

In the AFDS architecture:
1. `/api/verify` provisions the user account and sends a welcome email.
2. It does **not** return a JWT Bearer token in the signup response.
3. To obtain the session JWT token (`afds_auth_token`), users log in via:
   - **Email OTP Login**:
     1. `POST /request-login-otp` with `{"email": "<normalizedEmail>"}`
     2. `POST /verify-login-otp` with `{"email": "<normalizedEmail>", "code": "<otp>"}` -> Returns `{"token": "JWT...", ...}`
   - **Google OAuth Login**:
     - `POST /auth/google` with Google ID token -> Returns `{"token": "JWT..."}`

### Recommended NisTV Flow Upon Successful Signup:
To provide a smooth experience in NisTV:
1. User completes signup verification (`/api/verify`).
2. NisTV immediately triggers `requestLoginOtp(email)` in the background or seamlessly prompts the user to enter the login OTP, or allows one-tap Google Sign-In with the newly registered Google email.
3. NisTV receives the JWT token, fetches user profile (`/profile`), and saves credentials to `UserPreferences`.

---

## 5. NisTV App Integration Blueprint (For Next Step)

### 5.1 Kotlin Data Models to Add

In `com.nis.nistv.data.download.provider.AfdsAuthService.kt`:

```kotlin
@Serializable
data class AfdsSignupRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val inviteCode: String = "WELCOME"
)

@Serializable
data class AfdsVerifySignupRequest(
    val email: String,
    val code: String
)

@Serializable
data class AfdsSignupResponse(
    val message: String? = null,
    val error: String? = null
)
```

### 5.2 `AfdsAuthService` Methods to Implement

```kotlin
suspend fun requestSignupVerification(
    firstName: String,
    lastName: String,
    email: String,
    inviteCode: String = "WELCOME"
): Result<AfdsSignupResponse> = withContext(Dispatchers.IO) {
    // Normalization logic:
    val normalizedEmail = normalizeEmail(email)
    val body = json.encodeToString(
        AfdsSignupRequest.serializer(),
        AfdsSignupRequest(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = normalizedEmail,
            inviteCode = inviteCode.trim()
        )
    )
    executeSignupRequest("$BASE_URL/api/request-verification", body)
}

suspend fun verifySignup(
    email: String,
    code: String
): Result<AfdsSignupResponse> = withContext(Dispatchers.IO) {
    val normalizedEmail = normalizeEmail(email)
    val body = json.encodeToString(
        AfdsVerifySignupRequest.serializer(),
        AfdsVerifySignupRequest(
            email = normalizedEmail,
            code = code.trim()
        )
    )
    executeSignupRequest("$BASE_URL/api/verify", body)
}
```

### 5.3 UI Integration Options in NisTV

1. **Option A (Tabbed inside `AfdsAuthGateDialog.kt`)**:
   - Add a top selector: `[ Sign In | Create Account ]`.
   - When "Create Account" is selected:
     - Step 0: Fields for First Name, Last Name, Email, Invite Code (prefilled with "WELCOME").
     - Step 1: 6-digit OTP code entry.
     - On completion: Option to automatically log in or prefill email in Sign In tab.
2. **Option B (Dedicated `AfdsSignUpDialog.kt`)**:
   - A standalone composable dialog styled with `NisSurfaceElevated`, `NisAccentCyan`, and `NisLiveEmerald`.
   - Accessible via a "Don't have an account? Sign up" text link at the bottom of `AfdsAuthGateDialog`.

---

## 6. Verification Summary Checklist

- [x] Website inspected using Termux Chromium browser (`chromium-browser 155.0.8059.79`).
- [x] DOM structure and input field constraints analyzed.
- [x] Live API requests and responses tested on both direct API and Cloudflare proxy.
- [x] Email provider whitelist and normalization requirements identified.
- [x] Rate limiting and error response schemas documented.
- [x] Clear blueprint created for data models and UI integration in NisTV.
