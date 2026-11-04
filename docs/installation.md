# Installation

This page covers building the JAR, deploying it to Keycloak, adding the optional GeoIP database,
and running it in Docker.

## Requirements

| Item | Needed for |
|---|---|
| Java 17 or newer, Maven 3.8 or newer | Building |
| A running Keycloak 26 or newer (Quarkus distribution) | Deploying |
| An SMS gateway that accepts an HTTP GET | SMS Authentication |
| An HTTP email API (Netcore v5, v6 or the legacy format) | OTP email, custom reset email |
| MaxMind `GeoLite2-Country.mmdb` | IP Validator country check only |
| A captcha validation service and a login page script | Captcha Login Form only |

## 1. Build

```bash
cd keycloak-spis
mvn clean install
```

The result is `keycloak-spis/target/keycloak-remiges-spi-22.0.9.jar`.

The version in the file name comes from `<version>` in `keycloak-spis/pom.xml`. The Keycloak version the code is
compiled against is the property `version.org.keycloak` (26.0.2). To check another version, pass it on the command line,
for example `mvn clean package -Dversion.org.keycloak=26.8.1`.

## 2. Deploy the JAR

```bash
cp keycloak-spis/target/keycloak-remiges-spi-22.0.9.jar /opt/keycloak/providers/
```

### Dependencies placed beside the JAR

Only the IP Validator needs extra libraries. Copy them into the same `providers/` folder:

| File | Used for |
|---|---|
| `ipaddress-5.3.3.jar` | IP range and IPv6 comparison |
| `geoip2-4.0.1.jar` | GeoIP lookups |
| `maxmind-db-3.0.0.jar` | GeoIP database reader |
| `GeoLite2-Country.mmdb` | The country database |

If you do not use the IP Validator, you do not need them. The other four steps load without them.

### GeoIP database location

The database is read from `${KC_DIR}/providers/GeoLite2-Country.mmdb`. Set the environment variable `KC_DIR`
to the Keycloak home directory before starting Keycloak:

```bash
export KC_DIR=/opt/keycloak
```

If `KC_DIR` is not set, the path becomes `null/providers/GeoLite2-Country.mmdb` and the country check fails
with the "Geo IP Database Error" message. A free `GeoLite2-Country.mmdb` is available from MaxMind after you
create an account.

## 3. Rebuild Keycloak's provider index and restart

```bash
/opt/keycloak/bin/kc.sh build
/opt/keycloak/bin/kc.sh start
```

`kc.sh build` is required on the Quarkus distribution. Without it the new steps do not show up in the admin console.

## 4. Check that it loaded

In the admin console open **Authentication > Flows**, pick a flow, click **Add step** and search. You should find:

- SMS Authentication
- IP Validator
- Captcha Username Password Form
- Send Custom Reset Email
- user-session-validator

Then switch on the cookie cleanup: **Realm settings > Events > Event listeners**, add **login-cookie-cleanup**, save.
It is realm configuration, so repeat it in every realm and environment. See [Login Cookie Cleanup](login-cookie-cleanup.md).

If one is missing, see [Troubleshooting](troubleshooting.md#a-step-is-missing-from-the-add-step-list).

## 5. Install the theme files

Some steps render pages that belong to your **login theme**, not to the JAR's own theme folder. Copy or create
these in the theme your realm uses (`themes/<your-theme>/login/`):

| Template | Needed by | Source in this repo |
|---|---|---|
| `login-sms.ftl` | SMS Authentication (the code entry page) | Start from `login-sms.ftl-backup` |
| `login.ftl` | Captcha Login Form (adds the captcha image and field) | `src/main/resources/theme-resources/templates/login.ftl` |
| `sessionPopup.ftl`, `sessionError.ftl` | user-session-validator (the confirm and error pages) | Not in this repo, you write them |

The JAR also registers an email theme, `email-code-theme`, through `META-INF/keycloak-themes.json`.

> `login-sms.ftl` is referenced by the code but only a `login-sms.ftl-backup` is stored in the repository. Rename
> a copy of the backup to `login-sms.ftl` in your theme and adjust the look. See
> [SMS Authentication, the code page](sms-authentication.md#the-code-page).

## Docker

`keycloak-spis/Dockerfile` builds an image in two stages: it copies the JAR into a Keycloak 26.0.2 image,
runs `kc.sh build`, then starts Keycloak with `start --optimized`.

```bash
cd keycloak-spis
mvn clean install                         # the Dockerfile copies the JAR from target/
docker build -t keycloak-login-spi .
docker run --rm -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=change-me \
  -e KC_DIR=/opt/keycloak \
  keycloak-login-spi start-dev
```

For production use you must also provide the database, hostname and TLS settings that `start --optimized`
needs. The Dockerfile does not copy the IP Validator libraries or the GeoIP database, so add `COPY` lines for
them if you use that step.

## Compatibility notes

| Fact | Consequence |
|---|---|
| The code compiles against Keycloak **26.0.2** and was also compiled and run on **26.8.1** | Use Keycloak 26 or newer |
| The Dockerfile runs **Keycloak 26.0.2** | Same version as the build |
| The login cookie cleanup uses `org.keycloak.cookie.CookieProvider` | It exists from Keycloak 24, so Keycloak 22 is not supported |
| The remember-device cookie is written through the Keycloak response (`HttpResponse.addHeader`) | It no longer uses Keycloak 22 classes, but test it on your version before turning it on |
| `jakarta.ws.rs` is used throughout | It targets the Jakarta namespace of Keycloak 22 and later |

## Upgrading the JAR

1. Stop Keycloak, replace the old JAR in `providers/`, run `kc.sh build`, start Keycloak.
2. Existing saved step settings keep their values. New fields start blank and use their defaults.
3. Open each step's settings once after an upgrade and review the new fields.

## Uninstalling

Remove the JAR from `providers/`, run `kc.sh build` and restart. Remove the steps from your flows first,
because a flow that points to a missing step cannot run.
