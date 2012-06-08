/**
 * BackendService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package it.infn.mib.shibboleth.jaas.test.ws;

import java.net.URL;

import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;

public interface BackendService extends Service {
	
    public String getBackendPortAddress();

    public BackendPort getBackendPort() throws ServiceException;
    public BackendPort getBackendPort(URL portAddress) throws javax.xml.rpc.ServiceException;
    
}
