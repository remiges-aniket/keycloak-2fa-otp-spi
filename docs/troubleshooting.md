# Troubleshooting

Find the symptom, then read the cause and the fix. Logs are in the Keycloak server log. Raise the log level for
`co.remiges.tech` to `DEBUG` to see more:

```bash
kc.sh start --log-level=INFO,co.remiges.tech:debug
```

## Installation

### A step is missing from the "Add step" list

| Possible cause | Fix |
|---|---|
| `kc.sh build` was not run after copying the JAR | Run `kc.sh build`, then restart |
| The JAR is not in `providers/` | Put it in `${KEYCLOAK_HOME}/providers/` |
| Keycloak failed to load the JAR | Read the start-up log for a `ClassNotFoundException` or `NoClassDefFoundError` |
| IP Validator missing and the log names `inet.ipaddr` or `com.maxmind` | Copy `ipaddress-5.3.3.jar`, `geoip2-4.0.1.jar` and `maxmind-db-3.0.0.jar` beside the JAR |

### The step loads but crashes on Keycloak 26

The code compiles against Keycloak 26.0.2. If a `NoSuchMethodError` or `ClassNotFoundException` mentions a Keycloak
class, that class changed or does not exist in your Keycloak. Rebuild against your version with
`mvn clean package -Dversion.org.keycloak=<version>`, and fix the compile errors. Keycloak 22 and 23 are not supported, because
the cookie cleanup needs the cookie API from Keycloak 24.

## SMS Authentication

### The login page after the password shows a template error

`login-sms.ftl` is missing from your login theme. Create it from `login-sms.ftl-backup`
([The code page](sms-authentication.md#the-code-page)).

### No SMS arrives

| Check | Fix |
|---|---|
| Is **Testing: Simulation Mode** on? | Turn it off. The log shows `SIMULATION MODE ... SMS to ... not sent` |
| Does the user have the mobile attribute? | The log shows `'mobileNumber' attribute for the user <...> is blank or not present` |
| Is the attribute name right? | Compare **User Attribute: Mobile Number** with the attribute on the user |
| Gateway status is not 200 or 202 | The log shows `SMS failed with status: ...`. Check URL, feed id, username, password, sender id |
| Text rejected by the gateway | DLT mismatch. Make **SMS: Message Text** match the registered template exactly |
| The log shows `Failed to send SMS: <ExceptionType>` | A network problem, proxy setting or TLS issue. Try the proxy and TLS fields |
| Behind a proxy | Turn on **Network: Use Proxy** and set host and port |

### No email arrives

| Check | Fix |
|---|---|
| **OTP Email: Sender Address** is empty | Set it. The log shows `sender address is not configured` |
| Wrong format for the URL | A `v6` URL needs format `v6`, a `v5` URL needs `v5` |
| Status 401 or 403 in the log | The API key is wrong, or the sender domain is not verified |
| The user has no email | Set the user's email attribute |

### "The code has expired" appears immediately

**OTP: Valid For (seconds)** is too small. It is in **seconds**, so use `300` for five minutes.

### Every code is rejected

| Possible cause | Fix |
|---|---|
| The user is typing the SMS text of an earlier code | A resend creates a new code and invalidates the old one |
| Simulation mode is on with a fixed OTP | Use the fixed code |
| Too many wrong attempts | Wait, or ask for a new code, which starts the count again |

### "Resend otp attempts exhausted"

The user used all **OTP: Max Resends**. They must wait **OTP: Wait After Max Resends (minutes)**.

### The {placeholder} appears in the message

The name is wrong, or the field does not list that placeholder. The log shows
`Unknown placeholder {name} in message template`. See [placeholders](configuration-reference.md#placeholders).

### The code page does not say where the code went

Your `login-sms.ftl` does not render `otpSendMsg`. Add it, see [The code page](sms-authentication.md#the-code-page).

### Login fails even though the SMS was sent

The user has no value in the email attribute. The step expects an email for the user even if the SMS works. The log shows `Email ID is null or invalid for user`.

## IP Validator

| Symptom | Cause and fix |
|---|---|
| Everyone is allowed | No `ValidIpWhitelist` and no `ValidISOGeoLocation` exist on the user or the `IPWhiteListRole` role. An empty rule allows everyone |
| The client-level rule is ignored | The attribute must be on the role named exactly `IPWhiteListRole` of the client that is logging in |
| Whitelist rejects a valid IP | Check the log line `IP Address <...> || whitelist <...>`. Behind a proxy, the address is the proxy's. Configure proxy headers |
| "GeoIP Database Error" | `KC_DIR` is not set, the file `${KC_DIR}/providers/GeoLite2-Country.mmdb` is missing, or the address is private |
| Every user looks Indian | Keycloak sees `127.0.0.1`, which the code treats as `IN`. Configure proxy headers |
| A range does not match | Use `start-end` with both ends valid, and no spaces inside one entry |

## Captcha Login Form

| Symptom | Cause and fix |
|---|---|
| Always "wrong captcha" | The validation URL is unreachable or does not answer `{"status":"success"}`. The log shows `CAPTCHA validation failed.` |
| The validation API receives an empty `captcha_id` | The login page does not post `captcha_secret` |
| Captcha image does not load | The URL in `login.ftl` still points to `localhost`, or the browser blocks it. Check the browser console and the Content-Security-Policy |
| "Encrypted payload is too large" | `data` or `s` exceeds the limits (4096 and 128 hex characters) |
| "AES-GCM decryption failed" | The secret differs between console and page, or the page encrypts with other parameters ([details](captcha-login-form.md#encrypted-credentials-optional)) |
| A warning says the built-in secret is used | **Credentials Encryption Secret** is blank. Set it in both places |
| "Invalid username or password" for a correct password, and no decrypt error in the log | The page posts both the encrypted fields and the plain password, or the JAR is older than 22.0.8 and Keycloak is newer than 26.0. Use the current JAR: it checks the decrypted values directly |
| AUTH_SESSION_ID is still present after login | See [Login Cookie Cleanup](login-cookie-cleanup.md#troubleshooting) |

## Send Custom Reset Email

| Symptom | Cause and fix |
|---|---|
| The page says the mail was sent but nothing arrives | The user is unknown or has no email (both show the same message), or the sender address is empty. Check the log |
| `Proxy host or port is null` | **Network: Use Proxy** is on but host or port is empty |
| The link in the email is missing | **Reset Email: Body** lost `{link}` |
| The mail shows raw `{brandingName}` | A typo in the placeholder name |

## User Session Validator

| Symptom | Cause and fix |
|---|---|
| Template not found for `sessionPopup.ftl` | Create `sessionPopup.ftl` and `sessionError.ftl` in your login theme |
| The popup appears on the first login | An old session still exists. It is counted until it expires or is removed |
| Choosing "continue" does nothing | The popup form must post a field `override_session` with the value `true` |

## Still stuck

Collect these before asking for help:

1. The Keycloak version and the JAR version.
2. The step's settings, with secrets removed.
3. The log lines from the failed login, with the level set to `DEBUG` for `co.remiges.tech`.
