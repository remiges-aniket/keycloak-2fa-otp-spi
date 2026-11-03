package co.remiges.tech.smsservice.smsgateway;

import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import com.webauthn4j.validator.exception.CertificateException;

import co.remiges.tech.Constants;
import co.remiges.tech.util.ConfigUtil;
import co.remiges.tech.util.MaskUtil;

import java.net.InetSocketAddress;
import java.net.ProxySelector;

// import com.vonage.client.VonageClient;
// import com.vonage.client.sms.SmsSubmissionResponse;
// import com.vonage.client.sms.messages.TextMessage;

// import com.vonage.client.VonageClient;
// import com.vonage.client.sms.MessageStatus;
// import com.vonage.client.sms.SmsSubmissionResponse;
// import com.vonage.client.sms.messages.TextMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.cert.X509Certificate;
// import org.jboss.logging.Logger;
/**
 * Sms Service Implementation for sending of SMS
 */
public class SmsServiceImpl implements ISmsService {

	//private static final SnsClient sns = SnsClient.create();

	private static final Logger LOG = Logger.getLogger(SmsServiceImpl.class);
	
	private String gatewayUrl;
	private String senderId;
	private String apiKey;
	private String apiSecret;
	// private VonageClient client;
	private String feedId;
    private String username;
    private String password;
    private String apiUrl;
    private String proxyHost;
    private int proxyPort;
    private boolean isProxyRequired;
    private String ttl;
    private boolean skipTlsVerification = true; // Set to true to skip TLS verification
    private HttpClient client;
    private HttpRequest request;
	SmsServiceImpl(Map<String, String> config) {
		this.gatewayUrl = config.get("smsApiUrl");
		this.senderId = ConfigUtil.get(config, Constants.CFG_SMS_SENDER_ID, gatewayUrl);
		this.feedId = config.get("feedid");//added 07/10
        this.ttl = config.get("ttl");
        this.username = config.get("username");
        this.password = config.get("password"); 
        this.proxyHost = config.get("proxyHost");
        this.isProxyRequired = Boolean.parseBoolean(config.get("isProxyRequired"));
        // the port is only needed, and only parsed, when a proxy is used
        this.proxyPort = isProxyRequired ? Integer.parseInt(config.get("proxyPort").trim()) : 0;
        this.skipTlsVerification = Boolean.parseBoolean(config.get("skiptls"));
        // this.apiUrl = config.get("apiUrl"); 

		
		LOG.info(String.format("SMS Parameters: Sms api Url<%s>, senderId<%s>, ttl<%s>, feedId<%s>, username<%s>",gatewayUrl,senderId,ttl,feedId,username));
	}

	
	@Override
public void send(String phoneNumber, String message) {
    try {
        LOG.info("Starting sending SMS to phone number: " + MaskUtil.maskPhone(phoneNumber));

        // every value is encoded, so a character such as & or # in the password or sender id cannot change the query
        String fullUrl = String.format("%s?feedid=%s&text=%s&to=%s&username=%s&senderid=%s&password=%s&short=1",
                gatewayUrl, encode(feedId), encode(message), encode(phoneNumber), encode(username), encode(senderId),
                encode(password));

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();

        // Optional TLS verification skipping
       if (skipTlsVerification) {
            LOG.warn("Skipping TLS verification for the HTTP client. This is not recommended for production use.");
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

        // Optional Proxy
        if (isProxyRequired) {
            ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort));
            clientBuilder.proxy(proxySelector);
        }

        HttpClient client = clientBuilder.build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 202) {
            LOG.info("SMS sent successfully.");
        } else {
            LOG.error("SMS failed with status: " + response.statusCode() + ", response: " + (response.body() != null && response.body().length() > 200 ? response.body().substring(0, 200) + "..." : response.body()));
        }

    } catch (Exception e) {
        // the exception can carry the request URL, which has the gateway password in its query, so only its type is logged
        LOG.error("Failed to send SMS: " + e.getClass().getSimpleName());
    }
}

private static String encode(String value) {
    return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
}



}