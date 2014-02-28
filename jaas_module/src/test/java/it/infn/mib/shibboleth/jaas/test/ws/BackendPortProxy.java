package it.infn.mib.shibboleth.jaas.test.ws;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.junit.Ignore;

@Ignore
public class BackendPortProxy implements BackendPort {
	
	private String _endpoint = null;
	private BackendPort backendPort = null;

	public BackendPortProxy(String endpoint) {
		_endpoint = endpoint;
		_initBackendPortProxy();
	}

	private void _initBackendPortProxy() {
		try {
			backendPort = (new BackendServiceLocator(_endpoint)).getBackendPort();
			if (backendPort != null) {
				if (_endpoint != null) ((Stub) backendPort)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
				else _endpoint = (String) ((Stub) backendPort)._getProperty("javax.xml.rpc.service.endpoint.address");
			}

		}
		catch (ServiceException serviceException) { }
	}

	public String getEndpoint() {
		return _endpoint;
	}

	public void setEndpoint(String endpoint) {
		_endpoint = endpoint;
		if (backendPort != null) ((Stub)backendPort)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
	}

	public BackendPort getBackendPort() {
		if (backendPort == null) _initBackendPortProxy();
		return backendPort;
	}

	public String oncall(String loggeduser) throws RemoteException {
		if (backendPort == null) _initBackendPortProxy();
		return backendPort.oncall(loggeduser);
	}

}