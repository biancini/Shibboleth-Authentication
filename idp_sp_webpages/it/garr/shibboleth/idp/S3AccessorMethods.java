package it.garr.shibboleth.idp;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.SearchFilter;

public class S3AccessorMethods {
	private static TimeLimitedCacheMap cacheSecrets = null;
	private static final Logger log = LoggerFactory.getLogger(S3AccessorMethods.class);
	
	public static String getAccessKey(HttpServletRequest httpRequest) throws IOException {
		String httpAuthorization = httpRequest.getHeader("Authorization");
		if (httpAuthorization == null) throw new IOException("Missing HTTP-Authorization header in request.");
		if (!httpAuthorization.toLowerCase().startsWith("aws"))  throw new IOException("HTTP-Authorization header does not contains AWS.");
		httpAuthorization = httpAuthorization.substring(4);
		
		return decode64(httpAuthorization.split(":")[0]);
	}
	
	public static String getStoredAccessKey(String entityId, String username) throws UnsupportedEncodingException {
		return encode64((entityId + "!" + username).getBytes("UTF-8"));
	}
	
	public static String getMail(String accessKey) throws Exception {
		if (cacheSecrets == null || !cacheSecrets.containsKey(accessKey)) connectLdap(accessKey);
		HashMap<String, String> userParameters = cacheSecrets.get(accessKey);		

		if(userParameters == null || !userParameters.containsKey("mail") || userParameters.get("mail")==null){
			throw new Exception("Email parameter has not been retreived from LDAP.");
		}
		
		return userParameters.get("mail");
	}
	
	public static String getStringToSign(HttpServletRequest httpRequest) throws IOException {
		String stringToSign = httpRequest.getHeader("stringToSign");
		if (stringToSign == null) return null;
				
		return decode64(stringToSign);
	}
	
	public static String getIncomingAuthorization(HttpServletRequest httpRequest, String accessKey) throws Exception {
		String httpAuthorization = httpRequest.getHeader("Authorization");
		if (httpAuthorization == null) throw new Exception("Missing HTTP-Authorization header in request.");
		if (!httpAuthorization.toLowerCase().startsWith("aws")) throw new Exception("HTTP-Authorization header does not contains AWS.");
		httpAuthorization = httpAuthorization.substring(4);
		if (!httpAuthorization.startsWith(encode64(accessKey.getBytes())))  throw new Exception("Requesting authorization for a different access key.");
		
		return httpAuthorization.split(":")[1];
	}
	
	public static String getUsername(String accessKey) throws Exception {
		if (cacheSecrets == null || !cacheSecrets.containsKey(accessKey)) connectLdap(accessKey);
		HashMap<String, String> userParameters = cacheSecrets.get(accessKey);
		if (userParameters == null || !userParameters.containsKey("uid")) throw new Exception("User parameters not loaded.");
		return userParameters.get("uid");
	}
	
	@SuppressWarnings("unchecked")
	public static String createStringToSign(HttpServletRequest httpRequest) {
		String httpMethod = httpRequest.getMethod();
		String currentUri = httpRequest.getRequestURI();
		String currentQueryString = httpRequest.getQueryString();
		String date = httpRequest.getHeader("Date");
		String contentMD5 = httpRequest.getHeader("Content-MD5");
		String contentType = httpRequest.getHeader("Content-Type");

		Vector<String> canonicalizedAmzHeadersVec = new Vector<String>();
		Vector<String> canonicalizedResourceVec = new Vector<String>();
		String[] subResources = { "acl", "lifecycle", "location", "logging",
				"notification", "partNumber", "policy", "requestPayment",
				"torrent", "uploadId", "uploads", "versionId", "versioning",
				"versions", "website" };

		String canonicalizedAmzHeaders = "";
		String canonicalizedResources = "";
		String stringToSign = "";

		// CanonicalizedAmzHeaders creation begins
		Enumeration<String> headerNames = httpRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = (String) headerNames.nextElement().toLowerCase();
			String currentHeader = httpRequest.getHeader(headerName);

			if (headerName.startsWith("x-amz")) {
				canonicalizedAmzHeadersVec.add(headerName + ":" + currentHeader.toLowerCase().replace(" ", ""));
			}
		}
		if (canonicalizedAmzHeadersVec.size() > 0) {
			Collections.sort(canonicalizedAmzHeadersVec);
			for (String amzHeader : canonicalizedAmzHeadersVec) {
				canonicalizedAmzHeaders += (amzHeader + "\n");
			}
		}
		// CanonicalizedAmzHeaders creation ends

		// CanonicalizedResource creation begins
		canonicalizedResources = "";
		
		if(currentQueryString != null){
			String[] splittedQueryString = currentQueryString.split("&");
			for (int i = 0; i < splittedQueryString.length; i++) {
				for (int j = 0; j < subResources.length; j++) {
					if (splittedQueryString[i].startsWith(subResources[j])) {
						canonicalizedResourceVec.add(splittedQueryString[i]
								.toLowerCase().replace(" ", ""));
					}
				}
			}
			
			Collections.sort(canonicalizedResourceVec);
			if(canonicalizedResourceVec.size() > 0){
				for (String resource : canonicalizedResourceVec) {
					canonicalizedResources += (canonicalizedResources.equals("") ? "?" : "&") + resource;
				}
			}
		}
		
		canonicalizedResources = currentUri + canonicalizedResources;
		// CanonicalizedResource creation ends

		// StringToSign creation begins
		stringToSign = httpMethod + "\n" +
			   (contentMD5 == null ? "" : contentMD5) + "\n" +
			   (contentType == null ? "" : contentType) + "\n" +
			   (date == null ? "" : date) + "\n" +
			   canonicalizedAmzHeaders +
			   canonicalizedResources;
		
		return stringToSign;
	}
	
	public static String encryptSignature(String stringToSign, String secretKey) throws Exception{
		String signature = null;	
		byte[] rawHmac = encryptHSHA1(stringToSign, secretKey);
		signature = encode64(rawHmac);
		return signature;
	}
	
	public static void connectLdap(String accessKey) throws IOException, NamingException {
		String ldapUrl = LdapConfigServlet.getLdapUrl();
		String baseDN = LdapConfigServlet.getBaseDN();
		String bindDN = LdapConfigServlet.getBindDN();
		String credential = LdapConfigServlet.getCredential();
		String user = getUid(accessKey);
		
		log.debug("Trying to connect to ldap: " + LdapConfigServlet.getLdapUrl() + " with baseDN: " + LdapConfigServlet.getBaseDN());
		String[] ldapParameters = new String[]{"uid", "eduPersonPrincipalName", "userPassword", "mail"};
		
		// Connection with LDAP and query for users
		LdapConfig ldapConfig = new LdapConfig(ldapUrl, baseDN);
		ldapConfig.setBindDn(bindDN);
		ldapConfig.setBindCredential(credential);
		Ldap ldap = new Ldap(ldapConfig);
		
		// Uid extracted from decoded accessKey
		String uid = user;
		Iterator<SearchResult> results = ldap.search(new SearchFilter("uid="+uid), ldapParameters);
		if (!results.hasNext()) throw new IOException("No users returned from LDAP.");
		
		HashMap<String, String> userParameters = new HashMap<String, String>();
		
		SearchResult curResult = results.next();
		for (String curParameter : ldapParameters) {
			Object curParameterValue = curResult.getAttributes().get(curParameter).get();
			if (curParameterValue instanceof String) userParameters.put(curParameter, (String) curParameterValue);
			else if (curParameterValue instanceof byte[]) userParameters.put(curParameter, new String((byte[]) curParameterValue));
		}
		
		if (results.hasNext()) {
			userParameters = null;
			log.error("More than one user returned from LDAP for uid = " + uid + ".");
			throw new IOException("More than one user returned from LDAP.");
		}
		
		log.debug("Putting value for user " + accessKey + " into cache.");
		if (cacheSecrets == null) cacheSecrets = new TimeLimitedCacheMap(2*60, 15, 2*60, TimeUnit.MINUTES);
		cacheSecrets.put(accessKey, userParameters);
	}
	
	public static String printUserParameters(String accessKey) throws IOException, NamingException {
		if (cacheSecrets == null || !cacheSecrets.containsKey(accessKey)) connectLdap(accessKey);
		HashMap<String, String> userParameters = cacheSecrets.get(accessKey);
		
		String[] ldapParameters = new String[]{"uid", "eduPersonPrincipalName", "mail"};
		String stringUserParameters = "";
		
		for (String curParameter : ldapParameters)
			stringUserParameters += "[" + curParameter + "] => " + userParameters.get(curParameter) + "\n";
		
		return stringUserParameters;
	}
		
	public static String getSecretKey(String accessKey, String salt) throws Exception{
		if (cacheSecrets == null || !cacheSecrets.containsKey(accessKey)) connectLdap(accessKey);
		HashMap<String, String> userParameters = cacheSecrets.get(accessKey);
		
		if (userParameters == null || !userParameters.containsKey("userPassword") || !userParameters.containsKey("eduPersonPrincipalName") ||
				userParameters.get("userPassword") == null || userParameters.get("eduPersonPrincipalName") == null || salt == null)
			throw new Exception("Parameters to getSecretKey are null.");		
		
		return encryptAES(calculateSHA1(userParameters.get("userPassword") + "!" + userParameters.get("eduPersonPrincipalName")), salt);
	}
	
	public static String getUid(String accessKey) throws IOException {
		return accessKey.split("!")[1];
	}
	
	private static String encode64(byte[] messege){
		BASE64Encoder b64Encoder = new BASE64Encoder();
		String signature = b64Encoder.encode(messege);
		
		return signature;
	}
	
	public static String decode64(String encodedString) throws IOException{
		BASE64Decoder decode = new BASE64Decoder();
		byte[] decodedString = decode.decodeBuffer(encodedString);
		
		return new String(decodedString, "UTF-8");
	}
	
	private static String calculateSHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  { 
	    MessageDigest md;
	    md = MessageDigest.getInstance("SHA-1");
	    byte[] sha1hash = new byte[40];
	    md.update(text.getBytes("UTF-8"), 0, text.length());
	    sha1hash = md.digest();
	    return convertToHex(sha1hash);
	}
	
	private static String convertToHex(byte[] data) { 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        String computedSHA = buf.toString();
        //
        return computedSHA;
    } 
	
	private static byte[] encryptHSHA1(String messege, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		String messegeUTF8 = new String(messege.getBytes(), "UTF-8");
		
		// get an hmac_sha1 key from the raw key bytes
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
	
		// get an hmac_sha1 Mac instance and initialize with the signing key
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signingKey);
		
		// compute the hmac on input data bytes
		return mac.doFinal(messegeUTF8.getBytes());
	}
	
	private static String encryptAES(String plainText, String encryptionKey) throws Exception {
	      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
	      SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
	      cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(new byte[cipher.getBlockSize()]));
	    
	      String computedAes = encode64(cipher.doFinal(plainText.getBytes("UTF-8"))); 
		  //log.debug("ASSOLUTAMENTE DA CANCELLARE DOPO LO SVILUPPO computedAes: " + computedAes);
	      return computedAes;
	}
	
}
