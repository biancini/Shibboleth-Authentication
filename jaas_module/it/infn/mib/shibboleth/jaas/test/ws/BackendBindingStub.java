/**
 * BackendBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package it.infn.mib.shibboleth.jaas.test.ws;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;

import org.apache.axis.AxisFault;
import org.apache.axis.NoEndPointException;
import org.apache.axis.client.Stub;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.utils.JavaUtils;

@SuppressWarnings("rawtypes")
public class BackendBindingStub extends Stub implements BackendPort {

	static OperationDesc[] _operations;

	static {
		_operations = new OperationDesc[1];

		OperationDesc oper;
		ParameterDesc param;
		oper = new OperationDesc();
		oper.setName("oncall");
		param = new ParameterDesc(new QName("", "name"), ParameterDesc.IN, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
		oper.addParameter(param);
		oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
		oper.setReturnClass(String.class);
		oper.setReturnQName(new QName("", "salutation"));
		oper.setStyle(Style.RPC);
		oper.setUse(Use.ENCODED);
		_operations[0] = oper;
	}

	public BackendBindingStub() throws AxisFault {
		this(null);
	}

	public BackendBindingStub(URL endpointURL, Service service) throws AxisFault {
		this(service);
		super.cachedEndpoint = endpointURL;
	}

	public BackendBindingStub(Service service) throws AxisFault {
		if (service == null) {
			super.service = new org.apache.axis.client.Service();
		} else {
			super.service = service;
		}
		((org.apache.axis.client.Service) super.service).setTypeMappingVersion("1.2");
	}

	protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
		try {
			org.apache.axis.client.Call _call = super._createCall();
			if (super.maintainSessionSet) _call.setMaintainSession(super.maintainSession);
			if (super.cachedUsername != null) _call.setUsername(super.cachedUsername);
			if (super.cachedPassword != null) _call.setPassword(super.cachedPassword);
			if (super.cachedEndpoint != null) _call.setTargetEndpointAddress(super.cachedEndpoint);
			if (super.cachedTimeout != null) _call.setTimeout(super.cachedTimeout);
			if (super.cachedPortName != null) _call.setPortName(super.cachedPortName);

			Enumeration keys = super.cachedProperties.keys();
			while (keys.hasMoreElements()) {
				java.lang.String key = (java.lang.String) keys.nextElement();
				_call.setProperty(key, super.cachedProperties.get(key));
			}
			
			return _call;
		}
		catch (Throwable _t) {
			throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
		}
	}

	public String oncall(String name) throws RemoteException {
		if (super.cachedEndpoint == null) throw new NoEndPointException();
		
		org.apache.axis.client.Call _call = createCall();
		_call.setOperation(_operations[0]);
		_call.setUseSOAPAction(true);
		_call.setSOAPActionURI("http://schemas.xmlsoap.org/soap/envelope/#Backend#oncall");
		_call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
		_call.setOperationName(new QName("http://schemas.xmlsoap.org/soap/envelope/", "oncall"));

		setRequestHeaders(_call);
		setAttachments(_call);
		try {
			Object _resp = _call.invoke(new Object[] {name});

			if (_resp instanceof RemoteException) {
				throw (RemoteException)_resp;
			}
			else {
				extractAttachments(_call);
				try {
					return (String) _resp;
				} catch (Exception _exception) {
					return (String) JavaUtils.convert(_resp, String.class);
				}
			}
		} catch (AxisFault axisFaultException) {
			throw axisFaultException;
		}
	}

}
