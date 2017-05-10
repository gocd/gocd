/*
 * Copyright 2017 ThoughtWorks, Inc.
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

let m                                = require('mithril');
let f                                = require('helpers/form_helper');
let EnvironmentVariablesConfigWidget = require('views/pipeline_configs/environment_variables_config_widget');
let TasksConfigWidget                = require('views/pipeline_configs/tasks_config_widget');
let ArtifactsConfigWidget            = require('views/pipeline_configs/artifacts_config_widget');
let PropertiesConfigWidget           = require('views/pipeline_configs/properties_config_widget');
let TabsConfigWidget                 = require('views/pipeline_configs/tabs_config_widget');

const Stream          = require('mithril/stream');
const _               = require('lodash');
const s               = require('string-plus');
const $               = require('jquery');
const tt              = require('helpers/pipeline_configs/tooltips');
const ComponentMixins = require('helpers/mithril_component_mixins');
const Resources       = require('models/pipeline_configs/resources');

require('jquery-textcomplete');


const resourceReplaceFunc = function (word) {
  return `$1${  word  }, `;
};

const profileIdReplaceFunc = function (word) {
  return word;
};

const autoComplete = function (values, model, attrName, match, matchIndex, replaceFunc) {
  return function (elem, isInitialized, context) {
    if (!isInitialized) {
      const $inputElem = $($(elem).find(`input[data-prop-name='${  attrName  }']`));

      context.onunload = function () {
        $inputElem.textcomplete('destroy');
      };

      $inputElem.textcomplete([
        {
          match,
          index:   matchIndex, // use the second match in the regex to extract the search term
          search (term, callback) {
            term = term.toLowerCase();
            callback($.map(values, (word) => {
              return word.toLowerCase().indexOf(term) === 0 ? word : null;
            }));
          },
          replace: replaceFunc
        }
      ]);

      $inputElem.on('textComplete:select', () => {
        model[attrName]($inputElem.val());
      });
    }
  };
};

let JobTimeout = {
  view (vnode) {
    const job       = vnode.attrs.job;
    const radioName = `radio-${  job.uuid()  }-timeout`;
    const errors    = job.validate();
    let timeoutError;

    if (errors.errors('timeout')) {
      timeoutError = (<small class='form-error is-visible'>{errors.errorsForDisplay('timeout')}</small>);
    }
    return (
      <div>
        <f.row>
          <f.column size={12}>
            <label>
              Timeout
              <f.tooltip tooltip={{
                content:   tt.job.timeout,
                size:      'small',
                direction: 'bottom'
              }}
                         model={job}
                         attrName='timeout'/>
            </label>
          </f.column>
        </f.row>

        <f.row>
          <f.column size={12}>
            <input type='radio'
                   name={radioName}
                   id={`${radioName  }-never`}
                   checked={job.isTimeoutNever()}
                   onchange={job.timeout.bind(job, 'never')}/>
            <label for={`${radioName  }-never`}>Never</label>
            <input type='radio'
                   name={radioName}
                   id={`${radioName  }-default`}
                   checked={job.isTimeoutDefault()}
                   onchange={job.timeout.bind(job, null)}/>
            <label for={`${radioName  }-default`}>Default</label>

            <input type='radio'
                   name={radioName}
                   id={`${radioName  }-custom`}
                   checked={!(job.isTimeoutNever() || job.isTimeoutDefault())}
                   onchange={job.timeout.bind(job, '')}/>
            <label for={`${radioName  }-custom`}>
              Cancel after
              <span style={{display: 'inline-table'}}>
              {m.trust('&nbsp;')}
                <input type='number'
                       min="1"
                       style={{display: 'inline', width: '60px'}} size={3}
                       oninput={m.withAttr('value', job.timeout)}
                       class={timeoutError ? 'error' : ''}
                       value={(!(job.isTimeoutNever() || job.isTimeoutDefault())) ? job.timeout() : null}/>
                {m.trust('&nbsp;')}
                minute(s) of inactivity
                {timeoutError}
                </span>
            </label>
          </f.column>
        </f.row>
      </div>
    );
  }
};


let RunOnAgent = {
  view (vnode) {
    const job                  = vnode.attrs.job;
    const radioName            = `radio-${  job.uuid()  }-runInstanceCount`;
    const errors               = job.validate();
    const requiresElasticAgent = job.requiresElasticAgent();

    let runOnAllAgentsMessage;
    let runInstanceCounteError;


    if (errors.errors('runInstanceCount')) {
      runInstanceCounteError = (
        <small class='form-error is-visible'>{errors.errorsForDisplay('runInstanceCount')}</small>);
    }

    if (requiresElasticAgent) {
      runOnAllAgentsMessage = (<f.tooltip tooltip={{
        type:    'info',
        content: 'Run on all agents not applicable for job assigned to elastic agents. Please clear "Elastic Profile Id" input box.'
      }}/>);
    }

    return (
      <div>
        <f.row>
          <f.column size={12}>
            <label>
              Number of jobs
              <f.tooltip tooltip={{
                content:   tt.job.runInstanceCount.main,
                size:      'small',
                direction: 'bottom'
              }}
                         model={job}
                         attrName='runInstanceCount'/>
            </label>
          </f.column>
        </f.row>

        <f.row>
          <f.column size={12}>
            <input type='radio'
                   name={radioName}
                   id={`${radioName  }-one`}
                   checked={job.isRunOnOneAgent()}
                   onchange={job.runInstanceCount.bind(job, null)}/>
            <label for={`${radioName  }-one`}>Run on one agent</label>

            <input type='radio'
                   name={radioName}
                   class={requiresElasticAgent ? 'disabled-radio' : ''}
                   id={`${radioName  }-all`}
                   disabled={requiresElasticAgent}
                   checked={job.isRunOnAllAgents()}
                   onchange={job.runInstanceCount.bind(job, 'all')}/>
            <label for={`${radioName  }-all`}>
              Run on all agents
              {runOnAllAgentsMessage}
            </label>

            <input type='radio'
                   name={radioName}
                   id={`${radioName  }-custom`}
                   checked={!(job.isRunOnOneAgent() || job.isRunOnAllAgents())}
                   onchange={job.runInstanceCount.bind(job, '')}/>
            <label for={`${radioName  }-custom`}>
              Run
              <span style={{display: 'inline-table'}}>
                {m.trust('&nbsp;')}
                <input type='number'
                       min="1"
                       style={{display: 'inline', width: '60px'}} size={3}
                       value={!(job.isRunOnOneAgent() || job.isRunOnAllAgents()) ? job.runInstanceCount() : null}
                       class={runInstanceCounteError ? 'error' : ''}
                       oninput={m.withAttr('value', job.runInstanceCount)}/>
                {m.trust('&nbsp;')}
                instances of job
                {runInstanceCounteError}
              </span>

            </label>
          </f.column>
        </f.row>
      </div>
    );
  }
};

const JobsConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
    ComponentMixins.ManagesCollection.call(this, {as: 'Job'});
    this.removeJob = function (job) {
      const previousJobIndex = vnode.attrs.jobs().indexOfJob(job) - 1;
      this.vmState('selectedJobIndex')(previousJobIndex !== -1 ? previousJobIndex : 0);
      this.remove(job);
    };
  },

  view (vnode) {
    const allJobNames = vnode.state.map((job) => {
      return job.name();
    });

    const allJobUUIDs = vnode.state.map((job) => {
      return job.uuid();
    });

    const tabTitles = _(allJobNames).concat((
      <f.link onclick={vnode.state.add.bind(vnode.state)} class='add-job'>Add Job</f.link>)
    ).value();

    const tabUUIDs = _(allJobUUIDs).concat('add-job');

    return (
      <f.tabs class="job-definitions"
              tabTitles={tabTitles}
              tabKeys={tabUUIDs}
              isVertical={true}
              selectedIndex={vnode.state.vmState('selectedJobIndex', Stream(0))}>
        {vnode.state.map((job) => {
          const jobIndex                       = vnode.attrs.jobs().indexOfJob(job);
          const disableElasticProfileSelection = job.isRunOnAllAgents() || !s.isBlank(job.resources());
          let elasticProfileIdDisabledMessage;
          if (disableElasticProfileSelection) {
            elasticProfileIdDisabledMessage = (
              <div>
                Elastic profile id cannot be set for a job if:
                <ul>
                  <li>"Run on all agents" option is selected</li>
                  <li>job uses "Resources"</li>
                </ul>
              </div>
            );
          }

          const disableResourceTextbox = job.requiresElasticAgent();
          let resourcesDisabledMessage;
          if (disableResourceTextbox) {
            resourcesDisabledMessage = (<div>Resources cannot be set on a job if "Elastic Profile Id" is set.</div>);
          }

          return (
            <f.row class='job-definition' key={job.uuid()}>
              <f.column size={12}>
                <f.removeButton onclick={vnode.state.removeJob.bind(vnode.state, job)} class='remove-job'/>
                <f.accordion accordionTitles={[(<span>Job Settings</span>)]}
                             accordionKeys={['job-settings']}
                             selectedIndex={vnode.state.vmState(`jobSettingsSelected${  jobIndex}`, Stream(s.isBlank(job.name()) ? 0 : -1))}
                             class="accordion-inner">
                  <div>
                    <f.row>
                      <f.inputWithLabel
                        attrName='name'
                        model={job}
                        validate={true}
                        isRequired={true}
                        size={4}
                        largeSize={4}/>
                    </f.row>
                    <f.row>
                      <f.inputWithLabel
                        config={autoComplete(Resources.list, job, 'resources', /(^|,)\s*([^,]+)$/, 2, resourceReplaceFunc)}
                        attrName='resources'
                        model={job}
                        disabled={disableResourceTextbox}
                        message={resourcesDisabledMessage}
                        tooltip={{
                          content:   tt.job.resources,
                          direction: 'bottom',
                          size:      'small'
                        }}
                        size={4}
                        largeSize={4}/>
                      <f.inputWithLabel
                        config={autoComplete(vnode.attrs.elasticProfiles().collectProfileProperty('id'), job, 'elasticProfileId', /(.*)$/, 1, profileIdReplaceFunc)}
                        model={job}
                        attrName='elasticProfileId'
                        label='Elastic Profile Id'
                        disabled={disableElasticProfileSelection}
                        message={elasticProfileIdDisabledMessage}
                        tooltip={{
                          content:   tt.job.elasticProfile,
                          direction: 'bottom',
                          size:      'small'
                        }}
                        size={4}
                        largeSize={4}
                        end={true}/>
                    </f.row>
                    <f.row>
                      <JobTimeout job={job}/>
                    </f.row>
                    <f.row>
                      <RunOnAgent job={job}/>
                    </f.row>
                  </div>
                </f.accordion>
                <f.row>
                  <EnvironmentVariablesConfigWidget title='Jobs Environment Variables'
                                                    variables={job.environmentVariables}
                                                    key={job.environmentVariables().uuid()}
                                                    vm={vnode.state.vmState(`environmentVariablesConfig-${  jobIndex}`)}/>
                </f.row>
                <f.row>
                  <TasksConfigWidget tasks={job.tasks}
                                     vm={vnode.state.vmState(`tasksConfig-${  jobIndex}`)}/>
                </f.row>

                <f.row>
                  <ArtifactsConfigWidget artifacts={job.artifacts}
                                         keys={job.artifacts().uuid()}
                                         vm={vnode.state.vmState(`artifactsConfig-${  jobIndex}`)}/>
                  <TabsConfigWidget tabs={job.tabs}
                                    keys={job.tabs().uuid()}
                                    vm={vnode.state.vmState(`tabsConfig-${  jobIndex}`)}/>
                  <PropertiesConfigWidget properties={job.properties}
                                          keys={job.properties().uuid()}
                                          vm={vnode.state.vmState(`propertiesConfig-${  jobIndex}`)}/>
                </f.row>
              </f.column>
            </f.row>
          );
        })}
      </f.tabs>
    );
  }
};

module.exports = JobsConfigWidget;
