/**
 * BackendPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package it.infn.mib.shibboleth.jaas.test.ws;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.junit.Ignore;

@Ignore
public interface BackendPort extends Remote {
    public String oncall(String loggeduser) throws RemoteException;
}
