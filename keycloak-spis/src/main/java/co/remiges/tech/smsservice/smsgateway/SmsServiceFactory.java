package co.remiges.tech.smsservice.smsgateway;

import java.util.Map;

import org.jboss.logging.Logger;

import co.remiges.tech.util.MaskUtil;

/**
 * SMS Service factory
 */
public class SmsServiceFactory {

	private static final Logger LOG = Logger.getLogger(SmsServiceFactory.class);

	private SmsServiceFactory() {		
	}
	
	/**
	 * Sms Service class which checks for simulation mode and decides whether to send SMS or just print it in logs
	 * @param config Config of properties
	 * @return SmsServiceImpl
	 */
	public static ISmsService get(Map<String, String> config) {
		if (Boolean.parseBoolean(config.getOrDefault("simulation", "false"))) {
			// the text holds the OTP, so it is not written to the log
			return (phoneNumber, message) ->
				LOG.warn(String.format("***** SIMULATION MODE ***** SMS to %s not sent (%d characters)", MaskUtil.maskPhone(phoneNumber), message.length()));
		} else {
			return new SmsServiceImpl(config);
		}
	}
}
