package it.infn.mib.shibboleth.jaas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.security.auth.login.AppConfigurationEntry;

/**
 * This class represents a default implementation for
 * <code>javax.security.auth.login.Configuration</code>.
 * 
 * <p>
 * This object stores the runtime login configuration representation, and is the
 * amalgamation of multiple static login configurations that resides in files.
 * The algorithm for locating the login configuration file(s) and reading their
 * information into this <code>Configuration</code> object is:
 * 
 * <ol>
 * <li>
 * Loop through the <code>java.security.Security</code> properties,
 * <i>login.config.url.1</i>, <i>login.config.url.2</i>, ...,
 * <i>login.config.url.X</i>. These properties are set in the Java security
 * properties file, which is located in the file named
 * &lt;JAVA_HOME&gt;/lib/security/java.security. &lt;JAVA_HOME&gt; refers to the
 * value of the java.home system property, and specifies the directory where the
 * JRE is installed. Each property value specifies a <code>URL</code> pointing
 * to a login configuration file to be loaded. Read in and load each
 * configuration.
 * 
 * <li>
 * The <code>java.lang.System</code> property
 * <i>java.security.auth.login.config</i> may also be set to a <code>URL</code>
 * pointing to another login configuration file (which is the case when a user
 * uses the -D switch at runtime). If this property is defined, and its use is
 * allowed by the security property file (the Security property,
 * <i>policy.allowSystemProperty</i> is set to <i>true</i>), also load that
 * login configuration.
 * 
 * <li>
 * If the <i>java.security.auth.login.config</i> property is defined using "=="
 * (rather than "="), then ignore all other specified login configurations and
 * only load this configuration.
 * 
 * <li>
 * If no system or security properties were set, try to read from the file,
 * ${user.home}/.java.login.config, where ${user.home} is the value represented
 * by the "user.home" System property.
 * </ol>
 * 
 * <p>
 * The configuration syntax supported by this implementation is exactly that
 * syntax specified in the <code>javax.security.auth.login.Configuration</code>
 * class.
 * 
 * @see javax.security.auth.login.LoginContext
 */
public class ShibbolethConfigFile extends
		javax.security.auth.login.Configuration {
//	private static Logger logger = Logger.getLogger(ShibbolethConfigFile.class);
	private HashMap<String, LinkedList<AppConfigurationEntry>> configuration;

	/**
	 * Create a new <code>Configuration</code> object from the specified URI.
	 * 
	 * @param uri
	 *            Create a new Configuration object from this URI.
	 * @throws IOException
	 */
	public ShibbolethConfigFile() throws IOException {
		configuration = new HashMap<String, LinkedList<AppConfigurationEntry>>();
	}

	/**
	 * Retrieve an entry from the Configuration using an application name as an
	 * index.
	 * 
	 * <p>
	 * 
	 * @param applicationName
	 *            the name used to index the Configuration.
	 * @return an array of AppConfigurationEntries which correspond to the
	 *         stacked configuration of LoginModules for this application, or
	 *         null if this application has no configured LoginModules.
	 */
	public AppConfigurationEntry[] getAppConfigurationEntry(
			String applicationName) {

		LinkedList<AppConfigurationEntry> list = null;
		synchronized (configuration) {
			list = configuration.get(applicationName);
		}

		if (list == null || list.size() == 0)
			return null;

		AppConfigurationEntry[] entries = new AppConfigurationEntry[list.size()];
		Iterator<AppConfigurationEntry> iterator = list.iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			AppConfigurationEntry e = iterator.next();
			entries[i] = new AppConfigurationEntry(e.getLoginModuleName(),
					e.getControlFlag(), e.getOptions());
		}
		return entries;
	}

	public void appendEntry(String applicationName, AppConfigurationEntry entry) {
		synchronized (configuration) {
			LinkedList<AppConfigurationEntry> appConfigurationEntries = configuration.get(applicationName);
			if(appConfigurationEntries == null) {
				appConfigurationEntries = new LinkedList<AppConfigurationEntry>();
				configuration.put(applicationName, appConfigurationEntries);
			}
			appConfigurationEntries.add(entry);
		}
	}
}