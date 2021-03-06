/**
 * Copyright (C) 2010  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.ow2.bonita.facade.rest;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Map;

import org.ow2.bonita.facade.exception.BonitaInternalException;
import org.ow2.bonita.facade.interceptor.InterceptorUtil;
import org.ow2.bonita.facade.rest.wrapper.RESTCommand;
import org.ow2.bonita.facade.rest.wrapper.RESTMap;
import org.ow2.bonita.facade.rest.wrapper.RESTObject;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.ExceptionManager;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class RESTClientAPIInterceptor implements InvocationHandler {

  private final Object api; 

  public RESTClientAPIInterceptor(final Object api) {
    this.api = api;    
  }

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		try {
			Class<?>[] params = method.getParameterTypes();
			final Method m = this.api.getClass().getMethod(method.getName(), params);
			if ((m.getName().equals("setVariable")
					|| m.getName().equals("setProcessInstanceVariable") || m.getName()
					.equals("setActivityInstanceVariable"))
					&& args.length == 3) {
					args[2] = new RESTObject((Serializable) args[2]);
			} else if (m.getName().equals("execute")
					&& (args.length == 1 || args.length == 2)) {
				if (args[0] instanceof Command<?>) {
					args[0] = getRESTCommand((Command<?>) args[0]);
				}
			} else if (m.getName().equals("instantiateProcess") && args.length == 3 &&
					args[1] instanceof Map<?, ?>){
				args[1] = getRESTMAP(((Map<?, ?>)args[1]));
			}	else if (m.getName().equals("getModifiedJavaObject") && args.length == 4) {
        args[2] = new RESTObject((Serializable) args[2]);
        args[3] = new RESTObject((Serializable) args[3]);
			}
			Object ret = m.invoke(this.api, args);
			return ret;
		} catch (final Exception e) {
      if (e instanceof InvocationTargetException) {
        final Throwable invocationExceptionCause = e.getCause();
        if (invocationExceptionCause instanceof RemoteException) {
          final RemoteException remoteException = (RemoteException) invocationExceptionCause;
          final Throwable remoteCause = getRemoteCause(remoteException);
          InterceptorUtil.manageInvokeExceptionCause(method, remoteCause);
        } else {
          throw invocationExceptionCause;
        }
      }
      String message = ExceptionManager.getInstance().getFullMessage("baa_CAPII_1", e);
      throw new BonitaInternalException(message, e);
    }
  }
	
	private <K,V> RESTMap<K, V> getRESTMAP(Map<K, V> map){		
		return new RESTMap<K, V>(map);
	}
	
	private <T> RESTCommand<T> getRESTCommand(Command<T> command){
		RESTCommand<T> retCommand = null;
		try {
			retCommand = new RESTCommand<T>(command);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return retCommand;
	}

  private Throwable getRemoteCause(final RemoteException e) {
    Throwable t = e;
    while (t instanceof RemoteException) {
      t = t.getCause();
    }
    return t;
  }

}
