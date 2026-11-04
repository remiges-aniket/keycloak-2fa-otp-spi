# Login Cookie Cleanup

Provider id `login-cookie-cleanup`. It is an **event listener**, not a login step.
Class: `co.remiges.tech.cookieservice.LoginCookieCleanupListener`.

It answers the penetration-test finding "session fixation": the `AUTH_SESSION_ID` cookie kept the same value before
and after login. Keycloak creates that cookie itself and cannot be told to rename or skip it, so the listener removes it
from the browser as soon as the login has completed.

## What it does

When Keycloak sends a `LOGIN` event, the listener adds expiring `Set-Cookie` headers to the same response for:

| Cookie | What it is for | Removed after login |
|---|---|---|
| `AUTH_SESSION_ID` | Tracks the login attempt in progress | Yes |
| `KC_RESTART` | Lets Keycloak restart an expired login attempt | Yes |
| `KEYCLOAK_LOCALES` | The language chosen on the login page | Yes |
| `KEYCLOAK_IDENTITY` | The single sign-on session. Signed, HttpOnly, new after login | **No** |
| `KEYCLOAK_SESSION` | Session marker read by the Keycloak JS adapter | **No** |

Any cookie of your own, for example a login-scope cookie that the web app reads, is not touched.

`KEYCLOAK_IDENTITY` is kept on purpose. A single-page app that holds its tokens in memory restores its login after
every page refresh by redirecting to Keycloak with `prompt=none`, and that only works while the SSO cookie exists.
Removing it would send the user to the login page, with captcha and OTP, on every refresh.

## When it fires

`LOGIN` is sent after **every** step has passed: captcha, IP check, OTP, session limit and any required action.
The listener therefore does not depend on where the steps sit in the flow. If a step fails, nothing is removed and the
user can continue the attempt.

If clearing a cookie fails, a warning is logged and the login still succeeds.

## Set it up

1. Deploy the JAR (see [Installation](installation.md)).
2. In the admin console open **Realm settings > Events > Event listeners**.
3. Add **login-cookie-cleanup** and save. Keep the listeners that are already there, for example `jboss-logging`.
4. Do this in **every realm** and **every environment** (dev, UAT, production). It is realm configuration, not part of the JAR.

There are no settings to fill in.

## Requirements

| Item | Needed |
|---|---|
| Keycloak 24 or newer | The cookie API (`org.keycloak.cookie.CookieProvider`) does not exist before 24. On Keycloak 22 this class cannot load |
| Java 17 | The project is compiled for Java 17 |

## Check that it works

1. Open the login page in a clean browser profile and open the developer tools, Application tab, Cookies.
2. Before login you see `AUTH_SESSION_ID`, `KC_RESTART` and possibly `KEYCLOAK_LOCALES`.
3. Log in completely, including the OTP.
4. After login the three cookies are gone. `KEYCLOAK_IDENTITY` and `KEYCLOAK_SESSION` are present with new values.
5. Refresh the web app. You stay logged in. Log out and log in again to confirm that works too.

## Expected effect on a second tab (not tested yet)

Cookies are shared by all tabs of a browser. If a second tab is half way through a login when the first tab completes
its login, the second tab loses its login attempt and shows "restart login". Nothing is lost beyond starting that login again.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| The cookies are still there after login | The listener is not added in the realm's Event listeners, or the JAR is old. Check the log for `login-cookie-cleanup` at start-up |
| The log shows `Could not expire the login-attempt cookies` | The Keycloak version has no cookie provider. Use Keycloak 24 or newer |
| Users are asked to log in again on every refresh | `KEYCLOAK_IDENTITY` is being removed, probably by a proxy or gateway rule. It must stay |
