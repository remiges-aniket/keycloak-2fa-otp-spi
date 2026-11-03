# Configuration Reference

Every setting of every step. All are edited in the admin console:
**Authentication > Flows > your flow > gear icon next to the step**.

- A field left **blank** uses its default.
- Fields whose type is *Password* are stored masked.
- The **Key** column is the name stored in the Keycloak database. It matters if you export a realm or script the REST API.

## Placeholders

Write `{name}` in a template field. A placeholder is replaced only in the fields that list it.
An unknown placeholder is left as typed and a warning is logged. In HTML bodies the values are HTML-escaped.

| Placeholder | Value | Usable in |
|---|---|---|
| `{brandingName}` | Branding: Name, else realm display name, else realm name | SMS, OTP email, reset email |
| `{code}` | The OTP | SMS text, OTP email body |
| `{firstName}` `{lastName}` `{username}` | The user's fields | SMS, OTP email, reset email |
| `{ttlMinutes}` | OTP validity in whole minutes (`ttl` seconds divided by 60, rounded down) | SMS text, OTP email body |
| `{link}` | The reset-password link | Reset email body |
| `{maskedEmail}` | The email with its middle hidden, for example `jo****oe@*******.com` | Message: OTP Sent To Email |
| `{retryMinutes}` | Minutes to wait before a resend is allowed | Message: Resend Limit Reached |
| `{ip}` | The client IP | IP Validator messages |
| `{country}` | Country found for the IP | Message: Country Not Allowed |
| `{allowedCountry}` | The allowed country code | Message: Country Not Allowed |

---

## SMS Authentication (`sms-authenticator`)

| Label | Key | Type | Default |
|---|---|---|---|
| Branding: Name | `brandingName` | String | blank |
| Email API: URL | `netcoreApiUrl` | String | blank |
| Email API: Key | `netcoreApiKey` | Password | blank |
| Email API: Format | `emailApiFormat` | List `legacy` `v5` `v6` | `legacy` |
| OTP Email: Sender Name | `otpEmailFromName` | String | blank (uses Branding: Name) |
| OTP Email: Sender Address | `otpEmailFromAddress` | String | blank. **Required** |
| OTP Email: Provider Tag | `otpEmailTags` | String | blank (uses sender name) |
| OTP Email: Subject | `otpEmailSubjectTemplate` | String | `OTP for Login to {brandingName}` |
| OTP Email: Body | `otpEmailBodyTemplate` | Text | the default OTP message, below |
| SMS: Message Text | `smsMessageTemplate` | Text | the default OTP message, below |
| SMS Gateway: URL | `smsApiUrl` | String | blank |
| SMS Gateway: Feed Id | `feedid` | String | blank |
| SMS Gateway: Sender Id | `smsSenderId` | String | blank (sends the gateway URL as sender id) |
| SMS Gateway: Username | `username` | String | blank |
| SMS Gateway: Password | `password` | Password | blank |
| OTP: Code Length (digits) | `length` | String | `6` (a value below 4 becomes 4) |
| OTP: Valid For (seconds) | `ttl` | String | `300` |
| OTP: Max Wrong Attempts | `validateOtpLimit` | String | `3` |
| OTP: Max Resends | `resendOtpLimit` | String | `3` |
| OTP: Wait After Max Resends (minutes) | `retryAfterTime` | String | `30` |
| Message: OTP Sent To Email | `messageOtpSentToEmail` | String | `Email sent to {maskedEmail}.` |
| Message: OTP Sent To Mobile | `messageOtpSentToMobile` | String | `OTP also sent to your registered mobile number.` |
| Message: No Mobile Number | `messageNoMobileFound` | String | `No mobile number found.` |
| Message: Resend Limit Reached | `messageResendLimitExhausted` | String | `Resend otp attempts exhausted. Try after {retryMinutes} minutes.` |
| User Attribute: Mobile Number | `userMobileAttribute` | String | `mobileNumber` |
| User Attribute: Email | `userEmailAttribute` | String | `email` |
| Remember Device: Enable | `setCookieFor2FA` | Boolean | off |
| Remember Device: Days | `cookieMaxAge` | String | `7` |
| Network: Use Proxy | `isProxyRequired` | Boolean | off |
| Network: Proxy Host | `proxyHost` | String | blank |
| Network: Proxy Port | `proxyPort` | String | blank |
| Network: Skip TLS Verification | `skiptls` | Boolean | off |
| Testing: Simulation Mode | `simulation` | Boolean | off |
| Testing: Fixed OTP | `simulationOtp` | Password | blank |

Default OTP message, used for both **SMS: Message Text** and **OTP Email: Body**:

```
{code} is the OTP to access {brandingName} platform for user:  {firstName} {lastName} username: {username}. OTP valid for {ttlMinutes} Minutes . Pls do not share it with anyone.
```

The two spaces after `user:` and the space before `.` in `Minutes .` are deliberate. They keep the text identical to a DLT-registered SMS template.

## Send Custom Reset Email (`reset-credential-email-custom`)

| Label | Key | Type | Default |
|---|---|---|---|
| Branding: Name | `brandingName` | String | blank |
| Email API: URL | `netcoreApiUrl` | String | blank |
| Email API: Key | `netcoreApiKey` | Password | blank |
| Email API: Format | `emailApiFormat` | List | `legacy` |
| Reset Email: Sender Name | `resetEmailFromName` | String | blank (uses Branding: Name) |
| Reset Email: Sender Address | `resetEmailFromAddress` | String | blank. **Required** |
| Reset Email: Provider Tag | `resetEmailTags` | String | blank |
| Reset Email: Subject | `resetEmailSubjectTemplate` | String | `Reset Credentials for {brandingName}` |
| Reset Email: Body | `resetEmailBodyTemplate` | Text | the HTML in [Custom Reset Email](custom-reset-email.md#defaults) |
| Message: Reset Email Sent | `messageResetEmailSent` | String | `Password reset mail sent to your registered email id` |
| User Attribute: Email | `userEmailAttribute` | String | `email` |
| Network: Use Proxy / Proxy Host / Proxy Port | `isProxyRequired` `proxyHost` `proxyPort` | Boolean / String / String | off / blank / blank |
| Network: Skip TLS Verification | `skiptls` | Boolean | off |

## Captcha Username Password Form (`captcha-u-p-form`)

| Label | Key | Type | Default |
|---|---|---|---|
| Captcha API: Validation URL | `validateCaptchaAPI` | String | blank |
| Message: Wrong Captcha | `incorrectCaptchaMsg` | String | blank |
| Login Page: Credentials Encryption Secret | `captchaEncryptionSecret` | Password | blank (uses a built-in legacy secret and warns) |

## IP Validator (`ip-validator`)

| Label | Key | Type | Default |
|---|---|---|---|
| Check IP Whitelist | `IP Validation` | Boolean | on |
| Check Country (Geo IP) | `Geo IP Validation` | Boolean | on |
| Message: IP Not Whitelisted | `messageIpNotWhitelisted` | String | `Your IP Address <{ip}> is not part of whitelist, therefore access is denied` |
| Message: Country Not Allowed | `messageGeoNotAllowed` | String | `Your IP Address <{ip}> belongs to <{country}> is not part of whitelisted Geo Location<{allowedCountry}> for you, therefore access is denied` |
| Message: Geo IP Database Error | `messageGeoDbError` | String | `GeoIP Database Error for your IP <{ip}>` |

Rules are read from attributes, not from these fields:

| Attribute | On | Meaning |
|---|---|---|
| `ValidIpWhitelist` | User, or role `IPWhiteListRole` of the client | Comma-separated IPs, ranges, IPv6, or `*.*.*.*` |
| `ValidISOGeoLocation` | User, or role `IPWhiteListRole` of the client | 2-letter country code |

## user-session-validator (`user-session-validator`)

No settings.

---

## Requirement choices per step

| Step | Offered |
|---|---|
| SMS Authentication | Required, Alternative, Disabled |
| IP Validator | Required, Alternative, Disabled |
| user-session-validator | Required, Alternative, Disabled |
| Captcha Username Password Form | Required |
| Send Custom Reset Email | Required |

## Other fixed names

| Name | Where | Value |
|---|---|---|
| Remember-device cookie | Browser | `COOKIE_2FA_ANSWERED` |
| Role holding client-level IP rules | Client roles | `IPWhiteListRole` |
| Environment variable for GeoIP | Server | `KC_DIR` |
| GeoIP database file | `${KC_DIR}/providers/` | `GeoLite2-Country.mmdb` |
| Code page template | Login theme | `login-sms.ftl` |
| Session pages | Login theme | `sessionPopup.ftl`, `sessionError.ftl` |
| Captcha form fields | Login page | `user_captcha`, `captcha_secret`, optionally `data` and `s` |
