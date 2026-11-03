package co.remiges.tech.emailservice;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.validator.exception.CertificateException;

import co.remiges.tech.util.ConfigUtil;

public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EmailResponse sendEmailWithoutProxy(String recipient, String subject, String content, EmailSender sender, String apiKey, String apiUrl) {
        EmailResponse invalidSender = validateSender(sender);
        if (invalidSender != null) {
            return invalidSender;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header(authHeaderName(sender), authHeaderValue(sender, apiKey))
                    .POST(BodyPublishers.ofString(createEmailPayload(recipient, subject, content, sender)))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 202) {
                LOG.info("Email sent successfully with status code: " + response.statusCode());
                return new EmailResponse(true, "");
            } else {
                String errorMessage = "Email failed with status code: " + response.statusCode() + " and error: " + abbreviate(response.body());
                LOG.error(errorMessage);
                return new EmailResponse(false, errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to send email: " + e.getMessage();
            LOG.error(errorMessage, e);
            return new EmailResponse(false, errorMessage);
        }
    }

    public EmailResponse sendEmailWithProxy(
        String recipient, String subject, String content, EmailSender sender, String apiKey, String apiUrl,
        String proxyHost, String proxyPort, boolean skipTlsVerification) {
    EmailResponse invalidSender = validateSender(sender);
    if (invalidSender != null) {
        return invalidSender;
    }
    try {
        // Initialize HttpClient builder
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();

        // Skip TLS verification if specified
        if (skipTlsVerification) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                // No validation for client certificates
                            }
                
                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                // No validation for server certificates
                            }
                
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0]; // Return an empty array
                            }
                        }
                }, new java.security.SecureRandom());
                
                clientBuilder.sslContext(sslContext)
                             .sslParameters(new SSLParameters());
            } catch (Exception e) {
                LOG.error("Failed to configure SSL context for skipping TLS verification. Falling back to default settings.", e);
                // Proceed with default TLS settings
            }
        }

        // Configure proxy settings
        ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
        clientBuilder.proxy(proxySelector);

        // Build the HttpClient
        HttpClient client = clientBuilder.build();

        // Prepare the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header(authHeaderName(sender), authHeaderValue(sender, apiKey))
                .POST(HttpRequest.BodyPublishers.ofString(createEmailPayload(recipient, subject, content, sender)))
                .build();

        // Send the HTTP request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Log response body for debugging purposes
        LOG.debug(abbreviate(response.body()));

        // Process response
        if (response.statusCode() == 200 || response.statusCode() == 202) {
            LOG.info("Email sent successfully with status code: " + response.statusCode());
            return new EmailResponse(true, "");
        } else {
            String errorMessage = "Email failed with status code: " + response.statusCode() + " and error: " + abbreviate(response.body());
            LOG.error(errorMessage);
            return new EmailResponse(false, errorMessage);
        }
    } catch (Exception e) {
        // Handle any unexpected exceptions
        String errorMessage = "Failed to send email: " + e.getMessage();
        LOG.error(errorMessage, e);
        return new EmailResponse(false, errorMessage);
    }
}
    

    private String createEmailPayload(String recipient, String subject, String content, EmailSender sender) throws JsonProcessingException {
        if (sender.isV5()) {
            return createV5Payload(recipient, subject, content, sender);
        }
        Map<String, Object> from = new LinkedHashMap<>();
        from.put("fromEmail", sender.getFromAddress());
        from.put("fromName", sender.getFromName());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("personalizations", Collections.singletonList(Collections.singletonMap("recipient", recipient)));
        payload.put("from", from);
        payload.put("subject", subject);
        payload.put("content", content);
        payload.put("tags", sender.getTags());
        return MAPPER.writeValueAsString(payload);
    }

    /** The gateway can echo the request in its answer, so only the start of it is kept for the log. */
    private static String abbreviate(String text) {
        if (text == null || text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200) + "...";
    }

    /** v6 takes the key as a Bearer token, v5 and the legacy API in the api_key header. */
    private static String authHeaderName(EmailSender sender) {
        return sender.isV6() ? "Authorization" : "api_key";
    }

    private static String authHeaderValue(EmailSender sender, String apiKey) {
        return sender.isV6() ? "Bearer " + apiKey : apiKey;
    }

    /** Netcore v5 and v6 mail send shape. */
    private String createV5Payload(String recipient, String subject, String content, EmailSender sender) throws JsonProcessingException {
        Map<String, Object> from = new LinkedHashMap<>();
        from.put("email", sender.getFromAddress());
        from.put("name", sender.getFromName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "html");
        body.put("value", content);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("subject", subject);
        payload.put("content", Collections.singletonList(body));
        payload.put("personalizations", Collections.singletonList(
                Collections.singletonMap("to", Collections.singletonList(Collections.singletonMap("email", recipient)))));
        if (!ConfigUtil.isBlank(sender.getTags())) {
            payload.put("tags", Collections.singletonList(sender.getTags()));
        }
        if (sender.isV6()) {
            // 1 = OTP / transactional in the Netcore v6 API
            payload.put("priorityflag", 1);
        }
        return MAPPER.writeValueAsString(payload);
    }

    private EmailResponse validateSender(EmailSender sender) {
        if (sender == null || ConfigUtil.isBlank(sender.getFromAddress())) {
            String errorMessage = "Failed to send email: sender address is not configured";
            LOG.error(errorMessage);
            return new EmailResponse(false, errorMessage);
        }
        return null;
    }

    public static class EmailResponse {
        private final boolean success;
        private final String errorMessage;

        public EmailResponse(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
