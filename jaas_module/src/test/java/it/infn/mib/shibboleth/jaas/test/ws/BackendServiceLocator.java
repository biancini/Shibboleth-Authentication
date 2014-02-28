/**
 * BackendServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package it.infn.mib.shibboleth.jaas.test.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Service;
import org.junit.Ignore;

@SuppressWarnings({"rawtypes", "unchecked"})
@Ignore
public class BackendServiceLocator extends Service implements BackendService {

	private static final long serialVersionUID = -5953657190235244955L;
    private String BackendPort_address = null;
    private String BackendPortWSDDServiceName = "BackendPort";

	public BackendServiceLocator(String address) {
		BackendPort_address =  address;
    }

    public BackendServiceLocator(EngineConfiguration config) {
        super(config);
    }

    public BackendServiceLocator(String wsdlLoc, QName sName) throws ServiceException {
        super(wsdlLoc, sName);
    }

    public String getBackendPortAddress() {
        return BackendPort_address;
    }

    public String getBackendPortWSDDServiceName() {
        return BackendPortWSDDServiceName;
    }

    public void setBackendPortWSDDServiceName(String name) {
        BackendPortWSDDServiceName = name;
    }

    public BackendPort getBackendPort() throws ServiceException {
       URL endpoint;
        try {
            endpoint = new URL(BackendPort_address);
        }
        catch (MalformedURLException e) {
            throw new ServiceException(e);
        }
        return getBackendPort(endpoint);
    }

    public BackendPort getBackendPort(URL portAddress) throws ServiceException {
        try {
            BackendBindingStub _stub = new BackendBindingStub(portAddress, this);
            _stub.setPortName(getBackendPortWSDDServiceName());
            return _stub;
        }
        catch (AxisFault e) {
            return null;
        }
    }

    public void setBackendPortEndpointAddress(String address) {
        BackendPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public Remote getPort(Class serviceEndpointInterface) throws ServiceException {
        try {
            if (BackendPort.class.isAssignableFrom(serviceEndpointInterface)) {
                BackendBindingStub _stub = new BackendBindingStub(new java.net.URL(BackendPort_address), this);
                _stub.setPortName(getBackendPortWSDDServiceName());
                return _stub;
            }
        }
        catch (Throwable t) {
            throw new ServiceException(t);
        }
        throw new ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public Remote getPort(QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("BackendPort".equals(inputPortName)) {
            return getBackendPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("urn:Backend", "BackendService");
    }

    private java.util.HashSet ports = null;

	public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("urn:Backend", "BackendPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
    	if ("BackendPort".equals(portName)) {
            setBackendPortEndpointAddress(address);
        }
        else 
        { // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
