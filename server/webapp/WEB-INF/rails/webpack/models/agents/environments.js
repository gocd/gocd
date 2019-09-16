/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import $ from "jquery";
import _ from "lodash";
import {mrequest} from "helpers/mrequest";
import {TriStateCheckbox} from "models/agents/tri_state_checkbox";
import {SparkRoutes} from "helpers/spark_routes";

const shouldBeDisabled = (environmentName, selectedAgents) => {
  if (!selectedAgents) {
    return false;
  }
  return selectedAgents
    .flatMap((agent) => agent.environments()) //all environments of selected agents
    .filter((env) => env.name() === environmentName) //which have the right name
    .map((env) => env.associatedFromConfigRepo()) //are they associated from configRepo
    .reduce(((accumulator, currentValue) => currentValue || accumulator), false); //for at least one of the selected agents
};

const isUnknown = (environmentName, selectedAgents) => {
  if (!selectedAgents) {
    return false;
  }
  return selectedAgents
    .flatMap((agent) => agent.environments()) //all environments of selected agents
    .some((env) => (env.name() === environmentName) && env.isUnknown()); //which have the right name
};

const getToolTip = (disabled, unknown) => {
  if(disabled) {
    return "Cannot edit Environment associated from Config Repo";
  } else if(unknown){
    return "Environment is not defined in config XML";
  }
  return  "";
};

const getSortedEnvironments = (environments, selectedAgents) => {
  const selectedAgentsEnvironmentNames = _.map(selectedAgents, (agent) => agent.environmentNames());

  const allAgentEnvs = _.flatMap(selectedAgentsEnvironmentNames);
  const environmentList = _.uniq(environments.concat(allAgentEnvs));

  return _.map(environmentList.sort(), (environment) => {
    const disabled = shouldBeDisabled(environment, selectedAgents);
    const isUnknownEnv = isUnknownAndAssociatedWithOneAgent(environment, selectedAgents);
    const tooltip = getToolTip(disabled, isUnknown(environment, selectedAgents));
    return new TriStateCheckbox(environment, selectedAgentsEnvironmentNames, (disabled || isUnknownEnv), tooltip);
  });
};


function isUnknownAndAssociatedWithOneAgent(environment, selectedAgents) {
  return isUnknown(environment, selectedAgents) && selectedAgents.length !== 1;
}

export const Environments = {};

Environments.all = (selectedAgents) => $.Deferred(function () {
  const deferred = this;

  const jqXHR = $.ajax({
    method:      'GET',
    url:         SparkRoutes.apiAdminInternalEnvironmentsPath(),
    timeout:     mrequest.timeout,
    beforeSend:  mrequest.xhrConfig.forVersion('v1'),
    contentType: false
  });

  const didFulfill = (data) => {
    deferred.resolve(getSortedEnvironments(data, selectedAgents));
  };

  const didReject = (jqXHR) => {
    deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
  };

  jqXHR.then(didFulfill, didReject);
}).promise();

