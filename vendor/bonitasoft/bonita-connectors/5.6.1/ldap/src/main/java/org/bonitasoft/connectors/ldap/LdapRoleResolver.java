/**
 * Copyright (C) 2009 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.ldap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.connectors.ldap.LdapAttribute;
import org.bonitasoft.connectors.ldap.LdapConnector;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.RoleResolver;

/**
 * @author Matthieu Chaffotte
 *
 */
public class LdapRoleResolver extends RoleResolver {

	private String host;
  private Long port;
  private String protocol;
  private String userName;
  private String password;
  private String baseObjectGroup;
  private String baseObjectPeople;
  private String scope;
  private String filter;
  private String derefAliases = "ALWAYS";
  private Long sizeLimit = 0L;
  private Long timeLimit = 0L;
  private String referralHandling = "ignore";

	public String getHost() {
  	return host;
  }

	public Long getPort() {
  	return port;
  }

	public String getProtocol() {
  	return protocol;
  }

	public String getUserName() {
  	return userName;
  }

	public String getPassword() {
  	return password;
  }

	public String getBaseObjectGroup() {
  	return baseObjectGroup;
  }

	public String getBaseObjectPeople() {
  	return baseObjectPeople;
  }

	public String getScope() {
  	return scope;
  }

	public String getFilter() {
  	return filter;
  }

	public String getDerefAliases() {
  	return derefAliases;
  }

	public Long getSizeLimit() {
  	return sizeLimit;
  }

	public Long getTimeLimit() {
  	return timeLimit;
  }

	public String getReferralHandling() {
  	return referralHandling;
  }

	public void setHost(String host) {
  	this.host = host;
  }

	public void setPort(Long port) {
  	this.port = port;
  }

	public void setProtocol(String protocol) {
  	this.protocol = protocol;
  }

	public void setUserName(String userName) {
  	this.userName = userName;
  }

	public void setPassword(String password) {
  	this.password = password;
  }

	public void setBaseObjectGroup(String baseObjectGroup) {
  	this.baseObjectGroup = baseObjectGroup;
  }

	public void setBaseObjectPeople(String baseObjectPeople) {
  	this.baseObjectPeople = baseObjectPeople;
  }

	public void setScope(String scope) {
  	this.scope = scope;
  }

	public void setFilter(String filter) {
  	this.filter = filter;
  }

	public void setDerefAliases(String derefAliases) {
  	this.derefAliases = derefAliases;
  }

	public void setSizeLimit(Long sizeLimit) {
  	this.sizeLimit = sizeLimit;
  }

	public void setTimeLimit(Long timeLimit) {
  	this.timeLimit = timeLimit;
  }

	public void setReferralHandling(String referralHandling) {
  	this.referralHandling = referralHandling;
  }

	private LdapConnector getGroupLdapConenctor() {
		LdapConnector ldap = new LdapConnector();
		ldap.setHost(host);
		ldap.setPort(port);
		ldap.setProtocol(protocol);
		ldap.setUserName(userName);
		ldap.setPassword(password);
    ldap.setAttributes("uniqueMember");
    ldap.setBaseObject(baseObjectGroup);
    ldap.setDerefAliases(derefAliases);
    ldap.setFilter(filter);
    ldap.setReferralHandling(referralHandling);
    ldap.setScope(scope);
    ldap.setSizeLimit(sizeLimit);
    ldap.setTimeLimit(timeLimit);
    return ldap;
	}

	@Override
	protected List<ConnectorError> validateValues() {
		List<ConnectorError> errors = super.validateValues();
		LdapConnector ldap = getGroupLdapConenctor();
		errors.addAll(ldap.validate());
		return errors;
	}

	@Override
	protected Set<String> getMembersSet(String roleId) throws Exception {
		final LdapConnector ldap = getGroupLdapConenctor();
    ldap.execute();

    final Set<String> returns = new HashSet<String>();
    List<List<LdapAttribute>> list = ldap.getLdapAttributeList();
    if (!list.isEmpty()) {
      for (List<LdapAttribute> members : list) {
        StringBuilder builder = new StringBuilder();
        if (members.size() > 1) {
          builder.append("(|");
        }
        for (LdapAttribute member : members) {
          builder.append("(");
          String value = member.getValue();
          int index = value.indexOf(",");
          builder.append(value.substring(0, index));
          builder.append(")");
        }
        if (members.size() > 1) {
          builder.append(")");
        }
        ldap.setBaseObject(baseObjectPeople);
        ldap.setFilter(builder.toString());
        ldap.setAttributes("uid");
        ldap.execute();
        list = ldap.getLdapAttributeList();
        for (List<LdapAttribute> uids : list) {
          returns.add(uids.get(0).getValue());
        }
      }
    }
		return returns;
	}

}
