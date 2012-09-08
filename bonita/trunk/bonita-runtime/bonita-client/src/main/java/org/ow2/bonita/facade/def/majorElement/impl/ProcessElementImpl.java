/**
 * Copyright (C) 2006  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.def.majorElement.impl;

import org.ow2.bonita.facade.def.majorElement.ProcessElement;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;

/** 
 * 
 * @author Pierre Vigneras
 */
public abstract class ProcessElementImpl extends NamedElementImpl {

  private static final long serialVersionUID = 1883364714773768271L;
  /**
   * This field can be null when the related element has not been defined in the process but 
   * in the package 
   */
  protected ProcessDefinitionUUID processDefinitionUUID;

  protected ProcessElementImpl() { }

  protected ProcessElementImpl(
      final String name, final ProcessDefinitionUUID processDefinitionUUID) {
    super(name);
    this.processDefinitionUUID = processDefinitionUUID;
  }

  protected ProcessElementImpl(ProcessElement processDefinitionRecord) {
    super(processDefinitionRecord);
    // If the record really comes from a process definition...
    if (processDefinitionRecord.getProcessDefinitionUUID() != null) {
      this.processDefinitionUUID = new ProcessDefinitionUUID(processDefinitionRecord.getProcessDefinitionUUID());
    }
  }

  public ProcessDefinitionUUID getProcessDefinitionUUID() {
    return processDefinitionUUID;
  }

}
