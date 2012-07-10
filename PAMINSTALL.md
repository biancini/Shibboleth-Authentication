Configurazione IdP e SP per autenticare gli utenti tramite HTTP Basic Authentication
====================================================================================

Scopo della configurazione
--------------------------

Scopo di questa pagina è descrivere la procedura per configurare un IdP e un SP di IDEM/Shibboleth in modo da permettere
l'autenticazione degli utente tramite HTTP Basic authentication.

L'autenticazione degi utenti tradizionalmente in IDEM/Shibboleth avviene per mezzo della presentazione, all'utente stesso,
di dei form web in cui vengano specificati nome utente e password. Questo meccanismo di autenticazione è molto efficace e funziona
ottimamente per tutte le applicazioni web.
In questi casi, infatti, l'utente punta il suo browser all'URL dell'applicazione a cui vuole accedere. L'applicazione, tramite i
meccanismi tipici del SP, reindirizza il browser dell'utente a una pagina dell'IdP in cui sia possibile specificare nome utente e
password per l'autenticazione.

Questo meccanismo di autenticazione non si adatta però bene a applicazioni che non siano sviluppate su tecnologue web. In questi casi,
infatti, l'accesso da parte dell'utente potrebbe non avvenire tramite un browser e quindi potrebbe non essere semplice eseguire la login
(che invece è basata proprio su una form HTTP mostrata dall'IdP al browser dell'utente).

Per ovviare a questo problema, in questa pagina verrà presentato un meccansimo per permettere all'IdP di verificare le credenziali degli
utenti tramite un meccanismo di autenticazione HTTP Basic. Questo meccanismo di autenticazione, sebbene non snatura l'essenza di
IDEM/Shibboleth, permette di essere più facilmente integrato in applicazioni non accedibili via web. A scopo di esempio, per esemplificare
la possibilità di autenticazione IDEM/Shibboleth di applicazioni non web, all'interno del progetto Garrbox sono in corso di realizzazione un
modulo PAM e NSS per permettere l'autenticazione di utenti Shibboleth come utenti di sistema di una macchina Linux.

Nel seguito di questa pagina le verie configurazioni per permettere autenticazione HTTP Basic su IDEM/Shibboleth sono presentate.
Il grosso delle configurazioni riguarderanno l'IdP, sono però anche presentate le modifiche da effettuare a livello di un SP perchè metta a
disposizione questo meccanismo di autenticazione.

Configurazioni lato Identity Provider (IdP)
-------------------------------------------

Come è ovvio che sia, la configurazione di un meccanismo di autenticazione degli utenti IDEM/Shibboleth tramite HTTP Basic authentication,
investe principalmente l'IdP.
Nel seguito sono presentati i passi necessari ad abilitare questo meccanismo di configurazione, così come eseguiti in un apposito ambiente
di test.
I passi descritti presuppongono di partire dalla configurazione standard e funzionante di un IdP che utilizzi LDAP per mantenere le informazioni
sugli attributi autorizzativi degli utenti.

I passi di configurazione da eseguire sono i seguenti:

### IdP in configurazione Apache+Tomcat

L'IdP utilizzato è stato quello rilasciato da Internet2 che consiste in una web application Java deployata solitamente sull'application
server Tomcat.
L'IdP deve essere configurato in modo che Tomcat sia accessibile attraverso il web server Apache HTTPd.

Questa configurazione, molto tipica, avviene eseguendo le seguenti configurazioni:

Nel file `server.xml` di configurazione di Tomcat, deve essere specificato un unico connector per AJP (il connector che permette a Tomcat
di dialogare con HTTPd):

    <!-- Define an AJP 1.3 Connector on port 8009 -->
    <Connector port="8009" address="127.0.0.1"
               enableLookups="false" redirectPort="443"
               protocol="AJP/1.3"
               tomcatAuthentication="false" />

Nella configurazione di Apache HTTPd, per il VirtualHost specifico, deve essere specificata la configurazione per effettuare da Proxy nei
confronti del connector AJP:

    <VirtualHost _default_:443>
          ProxyRequests off
          ProxyPass / ajp://127.0.0.1:8009/
          ProxyPassReverse / ajp://127.0.0.1:8009/
          SSLEngine on
          ....
    </VirtualHost>

A questo punto riavviando Tomcat e HTTPd sarà possibile verificare che tutto funzioni come voluto.

### Configurazione Apache per effettuare HTTP Basic authentication

Apache HTTPd deve essere configurato in modo da permettere HTTP Basic authentication degli utenti che cercano di accedere ad specifico
URL che verrà utilizzato dall'IdP per gli scopi descritti di autenticazione.

Questa configurazione di HTTPd prevede di abilitare la Basic autentication effettuando una verifica dell'utente sullo stesso server LDAP
utilizzato dall'IdP per autenticare gli utenti e per recuperare i dati di autorizzazione.

    <Location /idp/Authn/RemoteUser>
        AuthType Basic
        AuthName "Identity Provider Authentication"
        AuthBasicProvider ldap
        AuthLDAPURL ldap://127.0.0.1:389/ou=people,dc=example,dc=com?uid?sub
        AuthLDAPBindDN "cn=admin,dc=example,dc=com"
        AuthLDAPBindPassword password
        Require valid-user
    </Location>

I dati da utilizzare per la connessione all'LDAP sono gli stessi utilizzati dall'IdP e solitamente presenti nel file `conf/login.config`
nella home dell'IdP.

### Aggiunta dell'handler specifico per la Basic authentication

L'IdP a questo punto deve essere istruito perchè, oltre all'usuale handler di autenticazione, attivi un nuovo handler che permetta la HTTP
Basic authentication.

Questa configurazione avviene nel file `conf/handler.xml` nella directory dove è stato installato l'IdP.

    <ph:LoginHandler xsi:type="ph:RemoteUser">
        <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn</ph:AuthenticationMethod>
    </ph:LoginHandler>

### Configurazione del parametro authnMethod per la servlet RemoteUserAuthHandler
Infine l'IdP deve essere configurato in modo che la servlet che risponde all'handler RemoteUser utilizzi l'authentication method
specificato nella configurazione di Shibboleth (ovvero `urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn`).
Questa configurazione avviene a livello della web application presente su tomcat.

Il file `web.xml` deve essere modificato in modo che la configurazione per la servlet RemoteUserAuthHandler sia come il seguente:

    <servlet>
        <servlet-name>RemoteUserAuthHandler</servlet-name>
        <servlet-class>edu.internet2.middleware.shibboleth.idp.authn.provider.RemoteUserAuthServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
        <init-param>
            <param-name>authnMethod</param-name>
            <param-value>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn</param-value>
        </init-param>
    </servlet>

### Configurazioni lato Service Provider (SP)

Per autenticare gli utenti tramite HTTP Basic authentication, l'SP deve configurare un SessionInitiator specifico che utilizzo il
metodo autenticativo BascAuthn.
La configurazione avviene quindi secondo i seguenti passi:

### Configurazioni del file shibboleth2.xml

    <SessionInitiator type="Chaining" Location="/BasicLogin" id="WebDAVLogin"
                      entityID="https://identityprovider.fqdn.com/idp/shibboleth"
                      target="https://serviceprovider.fqdn.com/">
         <SessionInitiator type="SAML2" defaultACSIndex="3" template="bindingTemplate.html"
                       outgoingBindings="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
                       authnContextClassRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn"/>
    </SessionInitiator>

Il defaultACSIndex (in questo esempio impostato a 3) deve puntare al AssertionConsumerService che utilizza HTTP-Artifact.Nel file di configurazione shibboleth2.xml deve essere presente: 

    <md:AssertionConsumerService Location="/SAML2/Artifact" index="3"
           Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"/>

Come è possibile vedere l'index di questo AssertionConsumerService (3) corrisponde al valore specificato nel defaultACSIndex.

### Utilizzo della libreria libCurl corretta

Shibboleth utilizza, per le HTTP Assertion la libreria libCurl. Le versioni di Linux più recenti, soprattutto quelle basate su RH/Fedora,
utilizzano una libreria libCurl compilata in manieta compatibile con la Netscape Security Services. Shibbolet, invece, utilizza altri metodi
di sicurezza.

Per questo, affinchè i dialoghi tra IdP e SP funzionino correttamente, è necessario installare la libreria libcurl.so.4 nella cartella lib
della location dove è installato l'SP.
Questa libreria è scaricabile dal sito di Internet2, per maggiori informazioni si faccia riferimento alla pagina:

  [https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxRH6]

### Configurazioni di Apache per gestire più SessionInitiator
Il SessionInitiator configurato per la Basic authentication può essere configurato in aggiunta al SessionInitiator già configurato
(che usa WAYF o che punta direttamente a un IdP e autentica mostrando all'utente pagine web per il login).
In questo caso la configurazione di Apache può essere modificata in modo che una certa Location utilizzi l'autenticazione Shibboleth
su questo SessionInitiator.

    <Location />
        ShibRequireSessionWith BasicLogin
        AuthType shibboleth
        ShibRequireSession On
        ShibUseHeaders On
        Require valid-user 
    </Location>

