# Development

For people who change the code.

## Build and layout

```bash
cd keycloak-spis
mvn clean install          # compile and package
mvn javadoc:javadoc        # API docs, written to keycloak-spis/ (see pom.xml reportOutputDirectory)
```

| Item | Value |
|---|---|
| Artifact | `keycloak-remiges-spi`, version `22.0.9` |
| Java release | 11 (`maven-compiler-plugin` `<release>11</release>`) |
| Keycloak version | property `version.org.keycloak` = `22.0.4` |
| Main dependencies | `keycloak-server-spi`, `keycloak-server-spi-private`, `keycloak-core`, `keycloak-services`, `ipaddress`, `geoip2`, `jakarta.ws.rs-api`, `jackson-databind` (provided), `lombok` |

Source lives under `keycloak-spis/src/main/java/co/remiges/tech`. See the [architecture](architecture.md#packages) for what each package does.

## Conventions used in the code

- **Config keys and defaults live in `Constants`.** Never write a key string in two places.
- **Read config through `ConfigUtil`.** It treats blank values as unset and offers `get`, `getFirst`, `getBoolean` and `getInt`.
- **Render texts through `TemplateUtil`.** Use `renderHtml` for anything that becomes HTML.
- **Brand through `BrandingUtil.withBranding`.** It fills a blank brand name from the realm.
- **Mask before logging.** Use `MaskUtil.maskPhone`. Never log an OTP, a password, a token or a reset link.
- **Do not show exception text to users.** Log it, then show a generic error page.
- **Each step has a Factory.** The factory declares the settings, the requirement choices and creates the authenticator.

## Adding a new step

1. Create the authenticator class implementing `org.keycloak.authentication.Authenticator`.
2. Create a factory implementing `AuthenticatorFactory`. Give it a unique `getId()`, a display name, help text, and the settings in `getConfigProperties()`.
3. Add every new key and default text to `Constants`.
4. Register the factory by adding its class name to
   `src/main/resources/META-INF/services/org.keycloak.authentication.AuthenticatorFactory`.
5. Build, deploy, run `kc.sh build`, and add the step to a test flow.
6. Document it: a guide under `docs/`, an entry in the [Configuration Reference](configuration-reference.md), and a row in the README table.

The factories currently registered:

```
co.remiges.tech.smsservice.SmsAuthenticatorFactory
co.remiges.tech.ipcheckservice.ClientIPValidationFactory
co.remiges.tech.captchaservice.CaptchaUsernamePasswordFormFactory
co.remiges.tech.usersession.UserSessionAuthenticatorFactory
co.remiges.tech.emailservice.CustomResetCredentialEmailFactory
```

## Adding another SMS gateway

The SMS call is behind the interface `ISmsService` (`void send(String phoneNumber, String message)`).
`SmsServiceFactory.get(config)` chooses the implementation:

- Simulation mode returns a lambda that only logs.
- Otherwise it returns `new SmsServiceImpl(config)`, which sends the HTTP GET described in
  [the SMS gateway contract](sms-authentication.md#sms-gateway-contract).

To support another provider, add a new `ISmsService` implementation and let `SmsServiceFactory` pick it,
for example from a new setting added to `SmsAuthenticatorFactory` and `Constants`.

## Adding a new email API format

`EmailSender` holds the format (`legacy`, `v5`, `v6`). `EmailService.createEmailPayload` builds the JSON, and
`authHeaderName` / `authHeaderValue` pick the auth header. Add a constant in `Constants`, extend
`EmailSender.apiFormatProperty()` and the two helpers, then add a payload builder.

## Adding a new placeholder

1. Add `PH_...` to `Constants`.
2. Put the value into the `Map<String, String>` passed to `TemplateUtil.render` where the text is built.
3. List the placeholder in the setting's help text and in the [placeholder table](configuration-reference.md#placeholders).

## Testing

The project has no automated tests yet. To test by hand:

1. Build and deploy to a disposable Keycloak.
2. Turn on **Testing: Simulation Mode** and set **Testing: Fixed OTP** to exercise the OTP step without a gateway.
3. Use a throwaway HTTP server as the SMS gateway and email API to inspect requests.
4. Check the log for the masked values and the absence of secrets.

Good candidates for the first unit tests are the pure helpers: `TemplateUtil`, `ConfigUtil`, `MaskUtil` and
`IpRangeCheckUtil.checkIP`.

## Repository hygiene

The repository currently tracks generated and IDE files that normally do not belong in git:

| Path | What it is |
|---|---|
| `keycloak-spis/target/` | Build output, including a built JAR |
| `keycloak-spis/apidocs/` | Generated Javadoc |
| `keycloak-spis/.metadata/` | An Eclipse workspace folder |
| `keycloak-spis/.classpath`, `.project`, `.factorypath` | Eclipse project files |

Add them to `.gitignore` and remove them from the index when you are ready.

## Release checklist

1. Update `<version>` in `keycloak-spis/pom.xml`.
2. Update the JAR name in `keycloak-spis/Dockerfile` and in the docs (`installation.md`, README quick start).
3. `mvn clean install`.
4. Test each step you changed on the Keycloak version you deploy.
5. Update [`docs/`](.) for any new field or behaviour.
6. Tag the release and attach the JAR.
