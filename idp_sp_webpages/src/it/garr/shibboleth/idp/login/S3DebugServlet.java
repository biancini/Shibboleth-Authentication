package it.garr.shibboleth.idp.login;

import it.garr.shibboleth.idp.S3AccessorMethods;

import java.io.IOException;
import java.io.PrintWriter;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class S3DebugServlet extends HttpServlet {
	private static final long serialVersionUID = 1075071397951598158L;
	
	private static String salt = null;
	
	/** {@inheritDoc} */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		//salt = getServletConfig().getInitParameter("salt");
		salt = "770A8A65DA156D24EE2A093277530142";
	}
	
	protected void doGet(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {

		PrintWriter out = httpResponse.getWriter();

		String stringToSign = S3AccessorMethods.createStringToSign(httpRequest);
		out.println("\n\n" + stringToSign);
		
		try {
			String secretKey = S3AccessorMethods.getSecretKey(S3AccessorMethods.getAccessKey(httpRequest), salt);
			String mySign = S3AccessorMethods.encryptSignature(stringToSign, secretKey);
			out.println(mySign);
			out.println(S3AccessorMethods.getIncomingAuthorization(httpRequest, S3AccessorMethods.getAccessKey(httpRequest)));
		} catch (Exception e1) {
			out.println("Error while calling S4 methods");
			e1.printStackTrace();
		}
		
		AmazonS3LoginServlet lh = new AmazonS3LoginServlet();
		try {
			lh.authenticateUser(httpRequest);
		}
		catch(LoginException e) {
			out.println("User NOT logged in");
		}
	}
	
}
