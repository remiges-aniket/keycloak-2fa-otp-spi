# Security

What the suite does to protect your logins, the settings that weaken it, and what you should check before going live.

## Production checklist

- [ ] **Testing: Simulation Mode** is **off** on every SMS Authentication step.
- [ ] **Testing: Fixed OTP** is **blank**.
- [ ] **Network: Skip TLS Verification** is **off** on SMS Authentication and Send Custom Reset Email.
- [ ] **OTP Email: Sender Address** and **Reset Email: Sender Address** are set, on a domain verified with the email provider.
- [ ] **Login Page: Credentials Encryption Secret** is a long random value, identical in the console and the login page.
- [ ] **Remember Device: Enable** is off on Keycloak 26.
- [ ] **user-session-validator** comes **after** the OTP step.
- [ ] **login-cookie-cleanup** is added under Realm settings > Events > Event listeners, in every realm.
- [ ] The login page posts only the encrypted `data` and `s` fields, not the plain password field.
- [ ] Keycloak sees real client IPs (proxy headers configured) if you use the IP Validator.
- [ ] The login page is served over **https**.
- [ ] The SMS text matches the DLT registration (India).

## What the suite protects

| Area | Protection |
|---|---|
| OTP comparison | Constant-time byte comparison, so timing does not reveal digits. A missing code field counts as wrong |
| OTP guessing | Wrong-attempt limit, expiry, and a resend limit with a wait period |
| Reloading to skip | A reload while a valid code exists shows the code page again and does not mark the step as done |
| SMS request | Every value is URL-encoded, so a special character in a password or sender id cannot alter the query |
| Email and captcha requests | JSON is built with a JSON writer, so user text cannot change the structure |
| HTML email | User values (names, username, link) are HTML-escaped. Only the admin-written template is trusted |
| Logging | The OTP, gateway password, API key, reset link, captcha answer and decrypted credentials are not logged. Phone numbers and emails are masked. Exception text from a failed SMS call is reduced to its type |
| Error pages | Server-side exception text is never shown to the user |
| Account discovery | The reset-email step gives the same answer for unknown users, users without email, and real users |
| Whitelist parsing | A bad entry never grants access, and never stops other entries from being checked |
| Remember-device cookie | Signed with the realm key, bound to username, browser and IP, `HttpOnly`, `Secure` over https |
| Decryption endpoint | Encrypted payload size is limited, because each decryption is expensive |

## Settings that weaken security

| Setting | Risk |
|---|---|
| Testing: Fixed OTP (with Simulation Mode on) | Anyone who knows the code passes the OTP step for every user |
| Testing: Simulation Mode | No code is sent, so a real user cannot log in without the fixed code |
| Network: Skip TLS Verification | A trust-all certificate manager is installed. Someone on the network can read the OTP, the gateway password, the API key or the reset link |
| Credentials Encryption Secret left blank | A built-in secret that is in the source code is used |
| Whitelist `*.*.*.*` | Allows every address |
| Empty `ValidIpWhitelist` or `ValidISOGeoLocation` | That check allows everyone |

## Known limitations

| Limitation | Effect | Mitigation |
|---|---|---|
| OTP stored as plain text in the authentication session | Anyone with access to Keycloak's session store could read a live code | Keep the session store private. Codes expire |
| Captcha encryption secret is in the page script | The user can read it. It protects the network path only | Use https. Do not treat it as protection from the user |
| Zero IV in the credential encryption | Same plaintext and salt give the same ciphertext | The salt is random per request, which limits the impact |
| Remember-device cookie cannot be revoked | It lives until its age ends | Keep the days short |
| No rate limit by IP or user on the captcha service | Brute force is limited only by the captcha | Add limits at the captcha service and a Keycloak brute-force detection policy |
| `127.0.0.1` counts as country `IN` | A proxy that hides real IPs makes every user look Indian | Configure proxy headers |
| Code is built against Keycloak 26.0.2 | Newer Keycloak may change classes the code relies on | Test on your exact version. It was also compiled and run on 26.8.1 |
| The SSO cookie `KEYCLOAK_IDENTITY` stays in the browser after login | A scanner that expects every Keycloak cookie to disappear will still see it | It is signed, HttpOnly and new after login. The single-page app needs it to restore its session after a refresh |

## Order of steps

A sensible browser flow:

1. Captcha Username Password Form (identify the user, stop bots)
2. IP Validator (block disallowed networks and countries)
3. SMS Authentication (second factor)
4. user-session-validator (only after the second factor, because it can end other sessions)

## Reporting a vulnerability

Do not open a public issue for a security problem. Send the details privately to the maintainers of the repository.
