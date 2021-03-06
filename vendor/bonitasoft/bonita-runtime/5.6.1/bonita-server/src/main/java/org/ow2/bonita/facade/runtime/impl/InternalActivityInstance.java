/**
 * Copyright (C) 2007  Bull S. A. S.
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
package org.ow2.bonita.facade.runtime.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.AssignUpdate;
import org.ow2.bonita.facade.runtime.VariableUpdate;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.VariableUtil;

/**
 * @author Pierre Vigneras
 */
public class InternalActivityInstance extends ActivityInstanceImpl {

  private static final long serialVersionUID = -8515098234372896097L;
  protected long dbid;
  protected Map<String, Variable> variables;

  protected InternalActivityInstance() { }

  public InternalActivityInstance(final ActivityInstanceUUID uuid, final ActivityDefinition activityDefinition, final ProcessInstanceUUID instanceUUID,
      final ProcessInstanceUUID rootInstanceUUID, final String iterationId, final String activityInstanceId, final String loopId) {
    super(uuid, activityDefinition, activityDefinition.getProcessDefinitionUUID(), instanceUUID, rootInstanceUUID, iterationId, activityInstanceId, loopId);
  }

  public InternalActivityInstance(final ActivityInstance src) {
    super(src);
    setVariables(VariableUtil.createVariableMap(src.getProcessDefinitionUUID(), src.getVariablesBeforeStarted()));
    this.variableUpdates = null;
    for (VariableUpdate varUpdate : src.getVariableUpdates()) {
      final Serializable value = varUpdate.getValue();
      addVariableUpdate(new InternalVariableUpdate(varUpdate.getDate(), varUpdate.getUserId(), varUpdate.getName(), VariableUtil.createVariable(src.getProcessDefinitionUUID(), varUpdate.getName(), value)));
    }
  }

  public void setExpectedEndDate(final Date expectedEndDate) {
    this.expectedEndDate = Misc.getTime(expectedEndDate);
  }

  public void setReadyDate(final Date readyDate) {
    this.readyDate = Misc.getTime(readyDate); 
  }
  
  public void setEndedDate(final Date endedDate) {
    this.endedDate = Misc.getTime(endedDate);
  }
  
  public void addVariableUpdate(final VariableUpdate varUpdate) {
    if (this.variableUpdates == null) {
      this.variableUpdates = new ArrayList<VariableUpdate>();
    }
    this.variableUpdates.add(varUpdate);
  }
  
  public void setVariableValue(final String variableName, final Variable variable) {
    updateLastUpdateDate();
    this.variables.put(variableName, variable);
  }

  public void updateLastUpdateDate() {
    this.lastUpdate = System.currentTimeMillis();
    final Querier querier = EnvTool.getJournalQueriers();
    querier.getProcessInstance(getProcessInstanceUUID()).updateLastUpdateDate();
  }

  public void setTaskAssign(ActivityState taskState, String loggedInUserId, String assignedUserId) {
    Set<String> previouslyActiveUsers = new HashSet<String>();
    if(isTaskAssigned()){
      previouslyActiveUsers.add(getTaskUser());
    } else {
      previouslyActiveUsers.addAll(getTaskCandidates());
    }
    updateLastUpdateDate();
    if (getAssignUpdates() == null) {
      this.assignUpdates = new ArrayList<AssignUpdate>();
    }
    this.userId = assignedUserId;
    this.getAssignUpdates().add(new AssignUpdateImpl(new Date(), taskState, loggedInUserId, this.candidates, this.userId));
    if (assignedUserId != null) {
      final Querier querier = EnvTool.getJournalQueriers();
      final InternalProcessInstance instance = querier.getProcessInstance(getRootInstanceUUID());
      if(previouslyActiveUsers!=null && !previouslyActiveUsers.isEmpty()){
        instance.removeActiveUsers(previouslyActiveUsers);
      }
      instance.addInvolvedUser(assignedUserId);
      if(assignedUserId!=null){
        instance.addActiveUser(assignedUserId);
      }
    }
  }

  public void setTaskAssign(ActivityState taskState, String loggedInUserId, Set<String> candidates) {
    Set<String> previouslyActiveUsers = new HashSet<String>();
    if(isTaskAssigned()){
      previouslyActiveUsers.add(getTaskUser());
    } else {
      previouslyActiveUsers.addAll(getTaskCandidates());
    }
    
    updateLastUpdateDate();
    if (getAssignUpdates() == null) {
      this.assignUpdates = new ArrayList<AssignUpdate>();
    }
    
    this.userId = null;
    this.candidates = candidates;
    
    this.getAssignUpdates().add(new AssignUpdateImpl(new Date(), taskState, loggedInUserId, this.candidates, this.userId));
    
    if (candidates != null && !candidates.isEmpty()) {
      final Querier querier = EnvTool.getJournalQueriers();
      final InternalProcessInstance instance = querier.getProcessInstance(getRootInstanceUUID()); 
      if(previouslyActiveUsers!=null && !previouslyActiveUsers.isEmpty()){
        instance.removeActiveUsers(previouslyActiveUsers);
      }
      instance.addInvolvedUsers(candidates);
      if(candidates!=null && !candidates.isEmpty()){
        instance.addActiveUsers(candidates);
      }
    }
  }

  public void setSubflowProcessInstanceUUID(final ProcessInstanceUUID subflowProcessInstanceUUID) {
    this.subflowProcessInstanceUUID = subflowProcessInstanceUUID;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public void setDynamicDescription(String dynamicDescription) {
    this.dynamicDescription = dynamicDescription;
  }

  public void setDynamicLabel(String dynamicLabel) {
    this.dynamicLabel = dynamicLabel;
  }

  public void setDynamicExecutionSummary(String executionSummary) {
    this.executionSummary = executionSummary;
  }

  public void setActivityState(ActivityState newState, String userId) {
    // Perform the update first
    if (ActivityState.FINISHED.equals(newState) || ActivityState.CANCELLED.equals(newState)
        || ActivityState.ABORTED.equals(newState) || ActivityState.SKIPPED.equals(newState)) {
      
      if(isTask()){
        final Querier querier = EnvTool.getJournalQueriers();
        final InternalProcessInstance instance = querier.getProcessInstance(getRootInstanceUUID());
        if(isTaskAssigned()){
          instance.removeActiveUser(getTaskUser());
        } else {
          instance.removeActiveUsers(getTaskCandidates());
        }
      }
    }
    updateLastUpdateDate();
    ActivityState oldState = getState();
    this.state = newState;
    final Date actual = new Date();
    //add a state update
    this.getStateUpdates().add(new StateUpdateImpl(actual, newState, oldState, userId));
    if (ActivityState.READY.equals(newState)) {
      readyDate = actual.getTime();
    } else if (ActivityState.EXECUTING.equals(newState)) {
      startedDate = actual.getTime();
      startedBy = userId;
    } else if (ActivityState.FINISHED.equals(newState) || ActivityState.CANCELLED.equals(newState)
        || ActivityState.ABORTED.equals(newState)) {
      endedDate = actual.getTime();
      endedBy = userId;
    } else if (ActivityState.SKIPPED.equals(newState)) {
    	startedDate = actual.getTime();
    	startedBy = userId;
    	endedDate = actual.getTime();
    	endedBy = userId;
    }
  }

  public Map<String, Object> getVariablesBeforeStarted() {
    if (this.variables != null) {
      return VariableUtil.getVariableValues(this.variables);
    }
    return this.clientVariables;
  }

  public void setVariables(final Map<String, Variable> variables) {
    Misc.badStateIfNotNull(this.variables, "variablesBeforeStarted can not be set twice!");
    this.variables = variables;
    this.clientVariables = null;
  }

}
