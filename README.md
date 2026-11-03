# Custom 2FA / OTP SPI for Keycloak

A set of Keycloak Service Provider Interface (SPI) extensions, built by Remiges Tech, that add
SMS and email one-time-password (OTP) login, IP and country restrictions, a captcha login form,
a single-active-session check, and password-reset email through an HTTP email API.

Everything is packaged as one JAR (`keycloak-remiges-spi`). After you install it, the steps below
appear in the admin console under **Authentication > Flows > Add step**. Texts, branding, sender
details and limits are all set in the admin console, so no rebuild is needed to change them.

## What is in the box

| Step name in the admin console | Provider id | What it does |
|---|---|---|
| SMS Authentication | `sms-authenticator` | Sends an OTP by SMS and by email, then checks the code the user enters |
| IP Validator | `ip-validator` | Allows login only from whitelisted IPs and, optionally, only from an allowed country |
| Captcha Username Password Form | `captcha-u-p-form` | Login form with a captcha. The password can arrive encrypted from the login page |
| Send Custom Reset Email | `reset-credential-email-custom` | Sends the password-reset link through an HTTP email API instead of the realm SMTP settings |
| user-session-validator | `user-session-validator` | Allows one active session per user and asks before ending the older one |

The full list of fields and placeholders for each step is in [`keycloak-spis/README.md`](keycloak-spis/README.md).
How each step behaves, with its flow and limits, is in [`docs/PROVIDERS.md`](docs/PROVIDERS.md).

## Requirements

| Item | Value |
|---|---|
| Java | 11 or newer to build (`<release>11</release>`) |
| Maven | 3.8 or newer |
| Keycloak libraries built against | 22.0.4 (`version.org.keycloak` in `keycloak-spis/pom.xml`) |
| Keycloak the JAR is deployed on | 26.0.2 in the supplied `Dockerfile` |
| SMS gateway | An HTTP gateway that accepts a GET request (see [SMS gateway contract](docs/PROVIDERS.md#sms-gateway-contract)) |
| Email provider | An HTTP email API: Netcore (Pepipost) v5 or v6, or the older `legacy` format |
| GeoIP (optional) | MaxMind `GeoLite2-Country.mmdb` |

See [Known limitations](#known-limitations) for the Keycloak version caveat.

## Build

```bash
cd keycloak-spis
mvn clean install
```

This produces `keycloak-spis/target/keycloak-remiges-spi-22.0.7.jar`.

## Install

1. Copy the JAR into `${KEYCLOAK_HOME}/providers/`.
2. If you use IP/GeoIP validation, also copy these into the same folder:
   `ipaddress-5.3.3.jar`, `geoip2-4.0.1.jar`, `maxmind-db-3.0.0.jar` and `GeoLite2-Country.mmdb`.
   Set the `KC_DIR` environment variable to the Keycloak home, because the database is read from `${KC_DIR}/providers/GeoLite2-Country.mmdb`.
3. If you use the captcha form, copy `login.ftl` from `keycloak-spis/src/main/resources/theme-resources/templates/` into the active login theme.
4. Run `kc.sh build`, then restart Keycloak. Without the build step the steps do not appear.

### Docker

The supplied `keycloak-spis/Dockerfile` copies the JAR into a Keycloak 26.0.2 image, runs `kc.sh build`
and starts it with `start --optimized`. Build the JAR first, because the image copies it from `target/`.

## Quick start: SMS OTP login

1. **Authentication > Flows**: open **browser**, choose **Action > Duplicate**, name it for example `browserWithSMSOtp`.
2. In the new flow, add the step **SMS Authentication** after the username and password step and set it to **Required**.
3. Click the gear icon, give it an alias, and fill in the email API, the SMS gateway and the OTP rules.
   Set **OTP Email: Sender Address**. No OTP email is sent while it is empty.
4. Give each user a mobile number in the attribute named in **User Attribute: Mobile Number** (default `mobileNumber`).
5. **Action > Bind flow > Browser flow**.
6. To test without sending anything, turn on **Testing: Simulation Mode** and set **Testing: Fixed OTP**. Turn both off before production.

## Repository layout

```
.
├── README.md                  this file
├── LICENSE                    Apache License 2.0
├── docs/PROVIDERS.md          how each step works, flows, limits, gateway contract
└── keycloak-spis/
    ├── pom.xml
    ├── Dockerfile
    ├── README.md              admin console field reference and security notes
    └── src/main/
        ├── java/co/remiges/tech/
        │   ├── smsservice/        SMS + email OTP authenticator and SMS gateway client
        │   ├── emailservice/      HTTP email API client and the custom reset-email step
        │   ├── ipcheckservice/    IP whitelist and GeoIP validation
        │   ├── captchaservice/    captcha login form
        │   ├── usersession/       single-session validator
        │   └── util/              branding, config, masking, template helpers
        └── resources/
            ├── META-INF/services/ registers the five authenticator factories
            ├── theme-resources/   login and email templates, messages_en.properties
            └── theme/             email-code-theme
```

## Security summary

- The OTP, the SMS gateway password, reset links and captcha answers are never written to the log. Phone numbers and emails are masked.
- OTP comparison uses a constant-time compare.
- All values sent to the SMS gateway are URL-encoded.
- User values in HTML email bodies are escaped.
- Simulation mode and Skip TLS Verification are off by default. Never enable either in production.

More in the Security notes section of [`keycloak-spis/README.md`](keycloak-spis/README.md#security-notes).

## Known limitations

- **Keycloak version.** The code is compiled against Keycloak 22.0.4 but the Dockerfile runs it on 26.0.2.
  Keycloak's SPI classes changed between those versions, so test every step on the version you deploy.
  The remember-device option uses Keycloak 22 classes. Keep it off on Keycloak 26.
- **One SMS gateway shape.** The SMS client calls one URL format only (see the gateway contract). Another provider needs a code change.
- **OTP is stored unhashed** in the authentication session until it is used or expires.
- **One active session** is fixed in the code for `user-session-validator`; it is not a setting.
- **No enrollment step.** The mobile number is read from a user attribute. Users without one get the OTP by email only.
- **No automated tests or CI yet.**

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
