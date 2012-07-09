package it.garr.shibboleth.idp.retrieves3credentials;

import it.garr.shibboleth.idp.login.S3AccessorMethods;
import it.infn.mib.shibboleth.idp.LdapConfigServlet;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.session.SessionManager;
import edu.internet2.middleware.shibboleth.idp.session.ServiceInformation;
import edu.internet2.middleware.shibboleth.idp.session.Session;

public class AmazonS3RetrieveServlet extends HttpServlet{
	
	private static final long serialVersionUID = -5727998416723946990L;
	public static final String S3_AUTHN_CTX = "urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession";
	
	private final static Logger log = LoggerFactory.getLogger(AmazonS3RetrieveServlet.class);
	
	private SessionManager<Session> sessionManager = null;
	private String[] parameters = null;
	private String email = null;
	private static String secretKey = null;
	private String user = null;
	
	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		parameters = new String[8];
		parameters[0] = getServletConfig().getInitParameter("mailHost");
		parameters[1] = getServletConfig().getInitParameter("mailHostPort");
		parameters[2] = getServletConfig().getInitParameter("mailUser");
		parameters[3] = getServletConfig().getInitParameter("mailPass");
		parameters[4] = getServletConfig().getInitParameter("mailFrom");
		parameters[5] = null; // Mail recipient
		parameters[6] = getServletConfig().getInitParameter("mailSubject");
		parameters[7] = getServletConfig().getInitParameter("mailText");
		
		sessionManager = (SessionManager<Session>) getServletConfig().getServletContext().getAttribute("shibboleth.SessionManager");
	}
	
	/** {@inheritDoc} */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String entityId = request.getHeader("SP_EntityId");
		String sessionIndex = request.getHeader("SessionIndex");
		Session shibSession = sessionManager.getSession(sessionIndex);
		if(shibSession==null) throw new ServletException("Unable to retrive session informations.");
		else{
			try {
				ServiceInformation si = shibSession.getServicesInformation().get(entityId);
				user = si.getAuthenticationMethod().getAuthenticationPrincipal().getName();
				S3AccessorMethods.connectLdap(LdapConfigServlet.getLdapUrl(), LdapConfigServlet.getBaseDN(), LdapConfigServlet.getBindDN(), LdapConfigServlet.getCredential(), user);
			
				secretKey = S3AccessorMethods.getSecretKey(LdapConfigServlet.getSalt());
				parameters[5] = S3AccessorMethods.getMail();
				sendMail(parameters);
			} catch (NamingException e) {
				log.error("I can't get data from ldap for user " + user + "\n" + e);
			}
			catch (MessagingException e) {
				log.error("Errore nell'invio della secret via mail: " + e);
			}
			catch (Exception e) {
				log.error("Errore nell'invio della mail \n" + e);
			}
		}
	}
	
	private boolean sendMail(String[] parameters) throws MessagingException{
		if(getServletConfig().getInitParameter("useSSL").equals("true")) sendMailSSL(parameters);
		else sendMailNormal(parameters);
		
		return true;
	}
	
	private boolean sendMailSSL(String[] parameters) throws MessagingException {
		String host = parameters[0];
	    int port = Integer.valueOf(parameters[1]).intValue();
		final String user = parameters[2];
		final String pass = parameters[3];
		String from = parameters[4];
		String to = parameters[5];
		String subject = parameters[6];
		String text = parameters[7];
		
		if(host==null || user==null || pass==null || from==null || to==null || subject==null) throw new MessagingException("Errore invio mail, parametri mancanti.");
		
		text = text.replaceAll("\\$USER\\$", this.user);
		text = text.replaceAll("\\$SECRET_KEY\\$", secretKey);
		
		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.socketFactory.port", port);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", port);
		
		javax.mail.Session session = javax.mail.Session.getInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, pass);
			}
		  });
		//session.setDebug(true);
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		message.setContent(text,"text/html");
		Transport transport = session.getTransport("smtps");
		transport.connect(host, user, pass);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
		log.debug("Email sent to " + email);		

		return true;
	}
	
	private boolean sendMailNormal(String[] parameters) throws MessagingException {
		String host = parameters[0];
	    int port = Integer.valueOf(parameters[1]).intValue();
		final String user = parameters[2];
		final String pass = parameters[3];
		String from = parameters[4];
		String to = parameters[5];
		String subject = parameters[6];
		String text = parameters[7];
		
		if(host==null || user==null || pass==null || from==null || to==null || subject==null) throw new MessagingException("Errore invio mail, parametri mancanti.");
		
		//Get system properties
		Properties props = System.getProperties( );
		
		//Setup mail server
		props.put("mail.smtp.host", host);
		props.put("mail.debug", "false");
		props.put("mail.smtp.auth","true");
		
		//Get session
		javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);
		session.setDebug(true);
		session.setPasswordAuthentication(new URLName("smtp",host,port,"INBOX",user,pass), new PasswordAuthentication(user,pass));
		
		//Define message
		MimeMessage message = new MimeMessage(session);
		//Set the from address
		message.setFrom(new InternetAddress(from));
		//Set the to address
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		//Set the subject
		message.setSubject(subject);
		//Set the text content for body
		text = text.replaceAll("\\$USER\\$", this.user);
		text = text.replaceAll("\\$SECRET_KEY\\$", secretKey);
		message.setContent(text,"text/html");
		//Send message
		Transport tr = session.getTransport("smtp");
		tr.connect(host, user, pass);
		message.saveChanges(); // don't forget this
		tr.sendMessage(message, message.getAllRecipients());
		tr.close();

		return true;
	  } 
	
}
