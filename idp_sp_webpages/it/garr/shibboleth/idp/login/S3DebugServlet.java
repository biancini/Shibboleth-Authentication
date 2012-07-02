package it.garr.shibboleth.idp.login;

import java.io.IOException;
import java.io.PrintWriter;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class S3DebugServlet extends HttpServlet {
	private static final long serialVersionUID = 1075071397951598158L;
	
	protected void doGet(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {

		PrintWriter out = httpResponse.getWriter();

		String stringToSign = S3AccessorMethods.createStringToSign(httpRequest);
		out.println("\n\n" + stringToSign);
		// StringToSign creation ends
		String secretKey = S3AccessorMethods.getSecretKey(S3AccessorMethods.getAccessKey(httpRequest));
		String mySign = S3AccessorMethods.encryptSignature(stringToSign, secretKey);
		out.println(mySign);
		out.println(S3AccessorMethods.getIncomingAuthorization(httpRequest, S3AccessorMethods.getAccessKey(httpRequest)));
		
		AmazonS3LoginServlet lh = new AmazonS3LoginServlet();
		try {
			lh.authenticateUser(httpRequest);
		}
		catch(LoginException e) {
			out.println("User NOT logged in");
		}
	}
	
}
