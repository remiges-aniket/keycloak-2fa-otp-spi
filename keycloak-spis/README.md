# keycloak-spis

This is an implementation of a Service Provider Interface (SPI) for Keycloak.<br>
Standard Keycloak configuration does not allow for SMS based OTP or IP level validation.<br>
This SPI manages to add this feature to Keycloak in a configurable manner.

## Use-Case
1. IP based validation (can be set at Client Level & User Level): 
    - Whitelist of Range of IP or List of IP is defined in the attributes against which the user IP is validated. 
    - GeoIP Validation : A User can be based on a single Geo Located Country(for e.g. IN). 

2. SMS Based OTP
  We want to enables SMS based OTP to optionally be a part of the Authentication flow. An SPI is written for this purpose.

## Dependencies
Certain external Libraries are to be included on KEYCOAK_HOME/providers directory. Following are those list:
1. ipaddress-5.3.3.jar : This is for IP Address range validation
2. geoip2-4.0.1.jar : For GeoIP Validation
3. maxmind-db-3.0.0.jar : For Geo IP Validation
4. GeoLite2-Country.mmdb : GeoIP country Database


## Building the Module from scratch

This is a Java maven project, so just execute `mvn clean install`.
This builds a `keycloak-remiges-spi-22.0.9.jar` in the target directory.


## Installing the module (scriptable)

It is not automatic, because you have to provide the scripts to automate the installation.

You can automatically load the provider in keycloak by putting the `keycloak-remiges-spi-22.0.9.jar` to the directory `${KEYCLOAK_HOME}/providers/`
Also place all the dependencies from above steps inlcuding JARs and geoip database on the same path


The new SPI fits in the registration flow of keycloaks registration.

**[A] IP/IP RANGE VALIDATION : Whitelist of Range of IP or List of IP is defined in the attributes against which the user IP is validated.**

Following properties are to be set in Keycloak for this to work: 
  1. __Authentication Flow level__  (Enable/Disable IP Validation):
     1. Login to Keycloak Admin Portal
     2. Select relevant Realm
     3. Under "Configure" section on Left Sidebar, select "Authentication" 
     4. Select the relevant flow like 'browerWithSMS_OTP' or 'browser' 
     5. If step "IP Validator" is present  
          - click on settings besides it and enable/disable it 
     6. If step "IP Validator" is not present, 
          - click on "Add Step" at relevant position in the flow, 
          - Search for "IP Validator" and Click Add. 
          - Go to setting besides "IP Validator" and add any alias and Enable/Disable 'IP Validation'
          
  2. __At Client Level__  (Valid IP Range): 
     1. Login to Keycloak Admin portal.
     2. Select relevant Realm 
     3. Under "Manage" section on the Left sidebar, select "Client"
     4. Click on Client where you want to set the attribute. For e.g. for Browser flow, select "account-console"
     5. Click on Roles
     6. If role "IPWhiteListRole" is not present, then add it, else select the role.
     7. In attributes for the Role, add the attribute "ValidIpWhitelist" along with value as the whitelist.
     8. Click on Save.
     
  3. __At User Level__  (Valid IP Range): 
     1. Login to Keycloak Admin Portal 
     2. Select relevant Realm 
     3. Under "Manage" section on the Left sidebar, select "Users" 
     4. Click on the user where you want to add IP Validation
     5. Click on "Attributes"
     6. Add the attribute key as "ValidIpWhitelist" and Value as IP range CSV e.g. "127.0.0.1-127.0.0.3,127.0.0.5,192.168.0.220-192.168.0.224" [without quotes]
     7. Click on Save and test.
     
     
 **[B] GEO LOCATION VALIDATION : IP is validated against Geo Location of IP using maxmind's geoIP2Lite DB**
 
 Following properties are to be set in Keycloak for this to work:
 1. __Authentication Flow level__  (Enable/Disable Geo Location based IP Validation): 
    1. Login to Keycloak Admin Portal
   	2. Select relevant Realm
    3. Under "Configure" section on Left Sidebar, select "Authentication"
    4. Select the relevant flow like 'browerWithSMS_OTP' or 'browser'
    5. If step "IP Validator" is present, 
        - click on settings besides it and enable/disable it
    6. If step "IP Validator" is not present, 
        - click on "Add Step" at relevant position in the flow,
        - Search for "IP Validator" and Click Add.
        - Go to setting besides "IP Validator" and add any alias and Enable/Disable 'Geo IP Validation'.
        
  2. __At Client Level__  (Valid Geo Location):
     1. Login to Keycloak Admin portal.
     2. Select relevant Realm 
     3. Under "Manage" section on the Left sidebar, select "Client"
     4. Click on Client where you want to set the attribute. For e.g. for Browser flow, select "account-console"
     5. Click on Roles
     6. If role "ValidISOGeoLocation" is not present, then add it, else select the role.
     7. In attributes for the Role, add the attribute "ValidISOGeoLocation" along with valid ISO Geo location e.g. "IN".
     8. Click on Save.
     
 2. __At User Level__  (Valid Geo Location):
     1. Login to Keycloak Admin Portal
     2. Select relevant Realm
     3. Under "Manage" section on the Left sidebar, select "Users" 
     4. Click on the user where you want to add IP Validation
     5. Click on "Attributes"
     6. Add the attribute key as "ValidISOGeoLocation" and Value as ISO Country code e.g. "IN" [without quotes]
     7. Click on Save and test.

## Configuring Recaptcha v2 for Login page

1. Create Recaptcha v2
    1. go to [ https://www.google.com/recaptcha/admin/create ]
    2. choose challenge(v2)
    3. add domain
    4. click on submit
    5. Note the Site Key and Site SECRET
2. Keycloak Authentication Flow
    1. In Authentication Flow > Add `Recaptcha Username Password Form`
    2. In Settings of `Recaptcha Username Password Form` add `alias` (any alias) , `Recaptcha Site Key` and `Recaptcha Secret`. This Key and secret were generated in step 1
    3. Now remove `frame-src 'self';` from Realm Settings> Security Defenses > Content-Security-Policy
3. Copy `login.ftl` to [Keycloak_Install_dir]/themes/base/login/
    

## Configurable messages, branding and sender (admin console)

Texts, branding and the email sender are not in the code. Set them in the admin console:
Authentication -> select the flow -> gear icon next to the step. No rebuild is needed.
A field left blank uses its default, shown in the field when a new config is created.
Existing saved configs keep their values, and new fields start blank and fall back to the defaults.

### Placeholders
Write `{name}` in a template. A value is only filled when the field lists that placeholder.
An unknown placeholder is left as typed and a warning is logged. User values (name, username, link) are HTML-escaped in email bodies.

| Placeholder | Value |
|---|---|
| `{brandingName}` | The Branding Name field. If blank, the realm display name, then the realm name |
| `{code}` | The OTP |
| `{firstName}` `{lastName}` `{username}` | User fields |
| `{ttlMinutes}` | OTP validity in minutes |
| `{link}` | Reset password link |
| `{retryMinutes}` | Minutes before resend is allowed again |
| `{maskedEmail}` | Email with the middle hidden, e.g. `jo****oe@*******.com` |
| `{ip}` `{country}` `{allowedCountry}` | IP validator messages |

### SMS Authentication step (`sms-authenticator`)
Each field has an example in its help text. Fields are grouped by label prefix.

| Label | What to enter | Example |
|---|---|---|
| Branding: Name | Name users see in the SMS and email. Fills `{brandingName}` | `Nuvama RTA` |
| Email API: URL | Mail-send URL of the email provider. Must match the format | `https://api.pepipost.com/v5/mail/send` |
| Email API: Key | API key of the email provider | Netcore dashboard > Settings > Integration |
| Email API: Format | `v5`, `v6` or `legacy`, matching the URL | `v5` |
| OTP Email: Sender Name / Sender Address / Provider Tag | Sender of the OTP email. **Sender Address is required** and must be on a verified domain | `RTA@nuvama.com` |
| OTP Email: Subject / Body | Email text, HTML allowed in the body | `OTP for Login to {brandingName}` |
| SMS: Message Text | OTP SMS text | see the default in the field |
| SMS Gateway: URL / Feed Id / Sender Id / Username / Password | Account details from the SMS gateway provider. Sender Id blank = the URL is sent as sender id (old behaviour) | `NUVAMA` |
| OTP: Code Length / Valid For (seconds) / Max Wrong Attempts / Max Resends / Wait After Max Resends (minutes) | OTP rules | `6` / `300` / `3` / `3` / `30` |
| Message: OTP Sent To Email / OTP Sent To Mobile / No Mobile Number / Resend Limit Reached | Lines on the OTP screen | |
| User Attribute: Mobile Number / Email | Names of the Keycloak user attributes | `mobileNumber` or `mobile_number` |
| Remember Device: Enable / Days | Skip the OTP on a remembered browser. Keep off on Keycloak 26 | |
| Network: Use Proxy / Proxy Host / Proxy Port / Skip TLS Verification | Network settings. Skip TLS is for testing only | `proxy.nuvama.local` / `8080` |
| Testing: Simulation Mode | No OTP is sent by SMS or email, only the masked number and email are logged. Use with Testing: Fixed OTP | off in production |
| Testing: Fixed OTP | Only with Simulation Mode on: every user gets this code instead of a random one, for automated tests. Digits only, at least 4. Blank = random codes | `123456`, blank in production |

If the SMS gateway uses DLT, the text of **SMS: Message Text** must match the registered DLT template.
The default keeps the old text and spacing, with only the brand name as a variable.

### Send Custom Reset Email step (`reset-credential-email-custom`)
Branding: Name, Email API: URL / Key / Format, Reset Email: Sender Name / Sender Address (required) / Provider Tag / Subject / Body,
Message: Reset Email Sent, User Attribute: Email, and the Network fields. Keep `{link}` in the body.
The "sent" message is also shown for unknown users and users with no email, so the screen does not show which users exist.

### Captcha step (`captcha-u-p-form`)
**Captcha API: Validation URL** is the service that checks the captcha, **Message: Wrong Captcha** is the error text, and
**Login Page: Credentials Encryption Secret** is the secret the login page uses to encrypt username and password.
The secret must be the same value as in the login page script. If blank, the old built-in secret is used and a warning is logged.
The secret is in the page script, so this protects the values only on the way to the server, not from the user's own browser.

### IP validator step (`ip-validator`)
Check IP Whitelist, Check Country (Geo IP), and three messages (IP Not Whitelisted, Country Not Allowed, Geo IP Database Error).
The whitelist accepts single IPs, ranges `a-b`, IPv6, and `*.*.*.*`, separated by commas. Spaces around entries are ignored.

## Security notes

- The OTP, the SMS gateway password, the captcha answer, reset links and decrypted credentials are not written to the log.
  Phone numbers and emails are masked in the log.
- "Simulation mode" sends no SMS and no email, and does not print the OTP. Set **Testing: Fixed OTP** to log in while it is on.
- **Testing: Fixed OTP** lets anyone who knows the code pass the OTP step. It works only while Simulation mode is on.
  Leave both off in production.
- **Skip TLS Verification** and **Simulation mode** are off by default. Skip TLS lets someone on the network read the OTP and the gateway password.
- Put the **User Session Validator** step after the OTP step. Before it, anyone who knows a password can end that user's other sessions without the OTP.
- With Geo IP validation, requests from `127.0.0.1` count as country `IN`. If a reverse proxy does not pass the real client IP,
  every user looks like they are in India. Configure the proxy headers in Keycloak.
- The "remember this device" cookie is signed and tied to username, browser and IP, but has no server-side expiry.
  It is sent only over https when Keycloak is reached over https.

## Moving to this version

1. Set **From Address** on the OTP email step and the reset email step. Without it no email is sent.
2. Set **Branding Name**, or leave it blank to use the realm display name.
3. Set **Credentials Encryption Secret** on the captcha step and in the login page.
4. If **Skip TLS Verification** was on in a saved config, it stays on until you change it. Turn it off.
5. Check the SMS template against the DLT registration.
