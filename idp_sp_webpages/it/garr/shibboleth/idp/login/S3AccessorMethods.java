package it.garr.shibboleth.idp.login;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class S3AccessorMethods {
	public static String getAccessKey(HttpServletRequest httpRequest) {
		String httpAuthorization = httpRequest.getHeader("Authorization");
		if (httpAuthorization == null) return null;
		if (!httpAuthorization.toLowerCase().startsWith("aws")) return null;
		httpAuthorization = httpAuthorization.replaceAll(" ", "").substring(3);
		
		return httpAuthorization.split(":")[0];
	}
	
	public static String getStringToSign(HttpServletRequest httpRequest) throws IOException{
		String stringToSign = httpRequest.getHeader("stringToSign");
		if (stringToSign == null) return null;
		BASE64Decoder b64Decoder = new BASE64Decoder();
		byte[] decodedStringToSign = b64Decoder.decodeBuffer(stringToSign);
				
		return new String(decodedStringToSign, "UTF-8");
	}
	
	public static String getIncomingAuthorization(HttpServletRequest httpRequest, String accessKey) {
		String httpAuthorization = httpRequest.getHeader("Authorization");
		if (httpAuthorization == null) return null;
		if (!httpAuthorization.toLowerCase().startsWith("aws")) return null;
		httpAuthorization = httpAuthorization.replaceAll(" ", "").substring(3);
		if (!httpAuthorization.startsWith(accessKey)) return null;
		
		return httpAuthorization.split(":")[1];
	}
	
	public static String getSecretKey(String accessKey) {
		return "PmcaneNFmZDmTAFKRRMkKCfSibBVKMcZJOKGCXMpHQ";
	}
	
	public static String getUsername(String accessKey) {
		return accessKey;
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
	
	public static String encryptSignature(String stringToSign, String key) {
		try {
			String signature = null;
			
			// UTF-8 encoding of stringToSign
			String stringToSignUTF8 = new String(stringToSign.getBytes(), "UTF-8");
			
			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
	
			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			
			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(stringToSignUTF8.getBytes());
			
			// compute the hmac on Base64
			BASE64Encoder b64Encoder = new BASE64Encoder();
			signature = b64Encoder.encode(rawHmac);
			
			return signature;
		} catch (Exception e) {
			return null; // Failed to generate HMAC
		}
	}
	
}
