package co.remiges.tech.ipcheckservice.util;

import java.net.InetAddress;

import org.jboss.logging.Logger;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;

/**
 * This utility class checks an IP to be present in the range of IP address passed.
 * @author bhavyag
 */
public class IpRangeCheckUtil {
	
	private static final Logger LOG = Logger.getLogger(IpRangeCheckUtil.class);

	private IpRangeCheckUtil() {
	}
	
	/**
	 * This method checks IP against a range of IP passed.
	 * @param validIpList : IP range against which the IP is to be checked
	 * @param ipToBeValidated : IP which needs to be validated agains the range.
	 * @return true if IP is within range else false. 
	 */
	public static boolean checkIP(String validIpList, String ipToBeValidated) {
		if (validIpList == null || ipToBeValidated == null) {
			return false;
		}
		String ipToCheck = ipToBeValidated.trim();
		for (String entry : validIpList.split(",")) {
			// entries are trimmed, so "10.0.0.1, 10.0.0.2" works
			String ip = entry.trim();
			if (ip.isEmpty()) {
				continue;
			}
			// equals, not ==, otherwise the wildcard never matched
			if (ip.equals("*.*.*.*")) {
				return true;
			}
			try {
				if (ip.contains("-")) {
					if (IpRangeCheck(ip.split("-"), ipToCheck)) {
						return true;
					}
				} else if (ip.contains(":")) {
					// IPv6 is compared as an address, not as text, because one address has several spellings
					IPAddress allowed = new IPAddressString(ip).getAddress();
					IPAddress input = new IPAddressString(ipToCheck).getAddress();
					LOG.debug("ipToBeValidated:[" + ipToCheck + "]||ipList:[" + ip + "]");
					if (allowed != null && allowed.equals(input)) {
						return true;
					}
				} else if (ipToCheck.equals(ip)) {
					return true;
				}
			} catch (Exception e) {
				// a bad entry in the list never grants access and does not stop the other entries from being checked
				LOG.error("Whitelist entry [" + ip + "] or input IP [" + ipToCheck + "] is invalid :: " + e.getLocalizedMessage());
			}
		}
		return false;
	}
	
	private static boolean IpRangeCheck(String ipRange[], String input) throws AddressStringException {
		if(ipRange.length!=2) {
			LOG.error("Invalid Range defined");
			return false;
		}else {
			
			return checkIPIsInGivenRange(input, ipRange[0], ipRange[1]);
		}
	}
	
	private static boolean checkIPIsInGivenRange (String inputIP, String rangeStartIP, String rangeEndIP) throws AddressStringException {		
	    IPAddress startIPAddress = new IPAddressString(rangeStartIP).getAddress();	    
	    IPAddress endIPAddress = new IPAddressString(rangeEndIP).getAddress();
	    IPAddressSeqRange ipRange = startIPAddress.toSequentialRange(endIPAddress);
	    IPAddress inputIPAddress = new IPAddressString(inputIP).toAddress();
	    return ipRange.contains(inputIPAddress);
	}
}
