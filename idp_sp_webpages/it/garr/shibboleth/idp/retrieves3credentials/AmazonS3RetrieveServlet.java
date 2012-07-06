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
	private String salt = null;
	private static String secretKey = null;
	
	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		parameters = new String[6];
		parameters[0] = getServletConfig().getInitParameter("mailHost");
		parameters[1] = getServletConfig().getInitParameter("mailHostPort");
		parameters[2] = getServletConfig().getInitParameter("mailUser");
		parameters[3] = getServletConfig().getInitParameter("mailPass");
		parameters[4] = getServletConfig().getInitParameter("mailFrom");
		parameters[5] = null;
		
		sessionManager = (SessionManager<Session>) getServletConfig().getServletContext().getAttribute("shibboleth.SessionManager");
	}
	
	/** {@inheritDoc} */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		//Session shibSession = (Session) request.getAttribute(Session.HTTP_SESSION_BINDING_ATTRIBUTE);
		String entityId = request.getHeader("SP_EntityId");
		String sessionIndex = request.getHeader("SessionIndex");
		Session shibSession = sessionManager.getSession(sessionIndex);
		if(shibSession==null) throw new ServletException("Unable to retrive session informations.");
		else{
			try {
				ServiceInformation si = shibSession.getServicesInformation().get(entityId);
				String user = si.getAuthenticationMethod().getAuthenticationPrincipal().getName();
				try {
					S3AccessorMethods.connectLdap(LdapConfigServlet.getLdapUrl(), LdapConfigServlet.getBaseDN(), LdapConfigServlet.getBindDN(), LdapConfigServlet.getCredential(), user);
				} catch (NamingException e) {
					log.error("I can't get data from ldap for user " + user + "\n" + e);
				}
				try {
					email = S3AccessorMethods.getMail();
					salt = LdapConfigServlet.getSalt();
					secretKey = S3AccessorMethods.getSecretKey(salt);
				} catch (Exception e) {
					log.error("Errore nell'invio della mail \n" + e);
				}
				parameters[5] = email;
				
				sendMail(parameters);
			} catch (MessagingException e) {
				log.error("Errore nell'invio della secret via mail: " + e);
			}
		}
	}
	
	private boolean sendMail(String[] parameters) throws MessagingException{
		int port = Integer.valueOf(parameters[1]).intValue();
		if(port == 25) sendMailNormal(parameters);
		else if(port == 465) sendMailSSL(parameters);
		
		return true;
	}
	
	private boolean sendMailSSL(String[] parameters) throws MessagingException {
		 
		String host = parameters[0];
	    int port = Integer.valueOf(parameters[1]).intValue();
		final String user = parameters[2];
		final String pass = parameters[3];
		String from = parameters[4];
		String to = parameters[5];
		if(host==null || user==null || pass==null || from == null || to == null) throw new MessagingException("Errore invio mail, parametri mancanti.");
 
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
		try {
 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject("GarrBox S3 SecretKey");
			
			message.setContent("<h1><font color='#0099CC'><b>&emsp;GarrBOX</b></font></h1><br/>" +
					   "<i>&emsp;&emsp;Gentile utente, come richiesto inviamo la SecretKey, utile per l'accesso ai servizi <b>S3</b> di GarrBOX.</i><br/><br/><br/>" +
					   "<b>&emsp;&emsp;SecretKey:</b> " + secretKey + "<br/><br/><br/><br/>" +
					   "&emsp;&emsp;<i>Cordiali saluti,<br/>&emsp;&emsp;Il Team di GarrBox</i>",
					   "text/html");
			
			Transport transport = session.getTransport("smtps");
			transport.connect(host, user, pass);
			message.saveChanges();
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
			log.debug("Email sent to " + email);
 
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}
	
	private static boolean sendMailNormal(String[] parameters) throws MessagingException {
	    String host = parameters[0];
	    int port = Integer.valueOf(parameters[1]).intValue();
		String user = parameters[2];
		String pass = parameters[3];
		String from = parameters[4];
		String to = parameters[5];
		if(host==null || user==null || pass==null || from == null || to == null) throw new MessagingException("Errore invio mail, parametri mancanti.");
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
			
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject("GarrBox S3 SecretKey");
			message.setContent("<h1><font color='#0099CC'><b>&emsp;GarrBOX</b></font></h1><br/>" +
					   "<i>&emsp;&emsp;Gentile utente, come richiesto inviamo la SecretKey, utile per l'accesso ai servizi <b>S3</b> di GarrBOX.</i><br/><br/><br/>" +
					   "<b>&emsp;&emsp;SecretKey:</b> " + secretKey + "<br/><br/><br/><br/>" +
					   "&emsp;&emsp;<i>Cordiali saluti,<br/>&emsp;&emsp;Il Team di GarrBox</i>",
					   "text/html");
			//Send message
			Transport tr = session.getTransport("smtp");
			tr.connect(host, user, pass);
			message.saveChanges();
			tr.sendMessage(message, message.getAllRecipients());
			tr.close();
		
			return true;
	  } 
	
}
