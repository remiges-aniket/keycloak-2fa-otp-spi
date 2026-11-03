# Provider reference

How each step behaves. For the list of admin console fields, see [`keycloak-spis/README.md`](../keycloak-spis/README.md).

## SMS Authentication (`sms-authenticator`)

Class: `co.remiges.tech.smsservice.SmsAuthenticator`

### Flow

1. The user passes the username and password step.
2. If **Remember Me** was ticked and a valid remember-device cookie exists, the step succeeds with no OTP.
3. If an unexpired code already exists in the session (for example the user reloaded the page), the code form is shown again and no new code is sent.
4. Otherwise a code is generated. Its length is the configured length, never below 4 digits.
5. The OTP is sent by SMS to the number in the mobile attribute, and by email to the address in the email attribute.
6. The user enters the code. A correct, unexpired code ends the step with success.

### Messages the user sees

The OTP screen shows one line for email (`Email sent to {maskedEmail}.`) and one for the mobile number
(`OTP also sent to your registered mobile number.` or `No mobile number found.`). All wording is configurable.

### Limits

| Setting | Behaviour |
|---|---|
| Valid For (seconds) | After this time the code is rejected with an expired-code error |
| Max Wrong Attempts | After this many wrong codes the user is stopped. Asking for a new code resets the count |
| Max Resends | After this many resends the user must wait |
| Wait After Max Resends (minutes) | Length of that wait. After it, the resend count starts again |

### Simulation mode

With **Testing: Simulation Mode** on, no SMS and no email is sent. Only the masked number and email are logged.
The OTP is never logged, so set **Testing: Fixed OTP** (digits only, at least 4) to be able to log in.
The fixed code is ignored when simulation mode is off.

### Remember device

If **Remember Device: Enable** is on and the user ticks Remember Me, a signed cookie (`COOKIE_2FA_ANSWERED`) is set for the configured number of days.
It is tied to the username, the browser's User-Agent and the client IP, and is sent over https only when Keycloak itself is reached over https.
It has no server-side expiry or revoke. Keep it off on Keycloak 26.

### User attributes

| Attribute (default) | Used for |
|---|---|
| `mobileNumber` | Destination of the SMS. Name configurable |
| `email` | Destination of the email. Name configurable |

### SMS gateway contract

The client sends one HTTP GET request, with every value URL-encoded:

```
{SMS Gateway: URL}?feedid=..&text=..&to=..&username=..&senderid=..&password=..&short=1
```

A response of 200 or 202 counts as sent. Anything else is logged with the status code and at most 200 characters of the body.
A network error is logged as its type only, because the exception text can contain the URL with the gateway password.
If **SMS Gateway: Sender Id** is blank, the gateway URL itself is sent as `senderid` (the original behaviour).

If the gateway uses DLT (India), **SMS: Message Text** must match the registered template exactly.
The default text keeps the original spacing for that reason.

### Email API

| Format | URL | How the key is sent |
|---|---|---|
| `v5` | `https://api.pepipost.com/v5/mail/send` | `api_key` header |
| `v6` | `https://emailapi.netcorecloud.net/v6/mail/send` | `Authorization: Bearer <key>`, mail marked as OTP priority |
| `legacy` | older gateway | `api_key` header, older request body |

The format must match the URL. Both a direct and a proxy path exist (see Network below).

### Network

| Field | Notes |
|---|---|
| Use Proxy, Proxy Host, Proxy Port | Route the email and SMS calls through an HTTP proxy. The port is only read when the proxy is on |
| Skip TLS Verification | Turns off certificate checking. Testing only, since the OTP and the gateway password can then be intercepted |

## IP Validator (`ip-validator`)

Class: `co.remiges.tech.ipcheckservice.ClientIPValidator`

Two independent checks, each switched on in the step's settings:

**IP whitelist.** The client IP must match the value of the attribute `ValidIpWhitelist`.
It is read from the user, or from the role `IPWhiteListRole` of the client. Entries are comma separated:
single IPs, ranges (`192.168.0.220-192.168.0.224`), IPv6, or `*.*.*.*`. Spaces are ignored.

**Country (GeoIP).** The country of the client IP, looked up in `GeoLite2-Country.mmdb`, must equal the attribute `ValidISOGeoLocation`
(an ISO code such as `IN`), set on the user or on the client role `ValidISOGeoLocation`.

Notes:
- Requests from `127.0.0.1` count as country `IN`. Behind a reverse proxy that does not forward the real client IP, every user looks like they are in India. Configure Keycloak's proxy headers.
- The database path is `${KC_DIR}/providers/GeoLite2-Country.mmdb`, so `KC_DIR` must be set.
- Denial messages are configurable: IP Not Whitelisted, Country Not Allowed, Geo IP Database Error (placeholders `{ip}`, `{country}`, `{allowedCountry}`).

## Captcha Username Password Form (`captcha-u-p-form`)

Class: `co.remiges.tech.captchaservice.CaptchaUsernamePasswordForm`

Replaces the standard username and password form. It checks the captcha by calling the configured **Captcha API: Validation URL**,
then validates the credentials. The login page can send the username and password encrypted with a shared secret.
Set **Login Page: Credentials Encryption Secret** to the same value as in the login page script (`login.ftl`).
If it is blank, the older built-in secret is used and a warning is logged.
The secret sits in the page script, so this protects the values in transit only, not from the user's own browser.

Setup also needs `frame-src 'self';` removed from Realm Settings > Security Defenses > Content-Security-Policy.

## Send Custom Reset Email (`reset-credential-email-custom`)

Class: `co.remiges.tech.emailservice.CustomResetCredentialEmail`

Replaces the "Send Reset Email" step. It sends the reset link through the HTTP email API, not the realm SMTP settings.
Keep `{link}` in the body template. The "sent" message is shown for unknown users and for users with no email too,
so the screen does not reveal which users exist.

## user-session-validator

Class: `co.remiges.tech.usersession.UserSessionAuthenticator`

After login, counts the user's active sessions. If one or more exist, a popup (`sessionPopup.ftl`) asks whether to end the older session or cancel this login.
The allowed number is fixed at 1 in the code. Place this step **after** the OTP step. Before it, anyone who knows a password could end that user's other sessions without passing the OTP.

## Templates and themes

| File | Purpose |
|---|---|
| `login.ftl` | Login page used with the captcha form |
| `email-code-form.ftl` | Code entry form |
| `html/code-email.ftl`, `text/code-email.ftl` | Email bodies |
| `messages/messages_en.properties` | English strings |
| `theme/email-code-theme` | Email theme registered in `keycloak-themes.json` |
| `login-sms.ftl-backup` | Earlier version of the SMS code form, kept for reference |

Two templates the code refers to are not in this repository and must exist in the active login theme:
`login-sms.ftl` (the SMS code form) and `sessionPopup.ftl` / `sessionError.ftl` (the session validator screens).
