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
let JobsConfigWidget                 = require('views/pipeline_configs/jobs_config_widget');

const Stream          = require('mithril/stream');
const s               = require('string-plus');
const $               = require('jquery');
const tt              = require('helpers/pipeline_configs/tooltips');
const ComponentMixins = require('helpers/mithril_component_mixins');
const Users           = require('models/pipeline_configs/users');
const Roles           = require('models/pipeline_configs/roles');

require('jquery-textcomplete');

const autoComplete = function (values, model, attrName) {
  return function (elem, isInitialized, context) {
    if (!isInitialized) {
      const $inputElem = $($(elem).find(`input[data-prop-name='${  attrName  }']`));

      context.onunload = function () {
        $inputElem.textcomplete('destroy');
      };

      $inputElem.textcomplete([
        {
          words: values, //the list of auto-completes
          match: /(^|,)\s*([^,]+)$/,
          index: 2, // use the second match in the regex to extract the search term
          search (term, callback) {
            term = term.toLowerCase();
            callback($.map(this.words, (word) => {
              return word.toLowerCase().indexOf(term) === 0 ? word : null;
            }));
          },
          replace (word) {
            return `$1 ${  word  }, `;
          }
        }
      ]);

      $inputElem.on('textComplete:select', () => {
        model[attrName]($inputElem.val());
      });
    }
  };
};

let ApprovalWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {
    return (
      <f.accordion accordionTitles={[(<span>Stage Operate Permissions</span>)]}
                   accordionKeys={['stagePermissions']}
                   selectedIndex={vnode.state.vmState('stagePermissionsSelected', Stream(-1))}
                   class='accordion-inner'>
        <div>
          <f.row>
            <f.warning>Permissions will be inherited from the pipeline group if not specified.</f.warning>
          </f.row>
          <f.row>
            <f.inputWithLabel config={autoComplete(Users.list, vnode.attrs.approval().authorization(), 'users')}
                              attrName='users'
                              model={vnode.attrs.approval().authorization()}/>
            <f.inputWithLabel config={autoComplete(Roles.list, vnode.attrs.approval().authorization(), 'roles')}
                              attrName='roles'
                              model={vnode.attrs.approval().authorization()} end/>

          </f.row>
        </div>
      </f.accordion>
    );
  }
};

let SettingsWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {
    const stage           = vnode.attrs.stage;
    const radioName       = `radio-${  stage.uuid()  }-approval-type`;
    const settingSelected = s.isBlank(vnode.attrs.stage.name()) ? 0 : -1;

    return (
      <f.accordion accordionTitles={[(<span>Stage Settings</span>)]}
                   accordionKeys={['stageSettings']}
                   selectedIndex={vnode.state.vmState('stageSettingsSelected', Stream(settingSelected))}
                   class='accordion-inner'>
        <f.row>
          <f.column size={4}>
            <f.row collapse>
              <f.inputWithLabel attrName='name'
                                label='Stage name'
                                model={stage}
                                validate={true}
                                isRequired={true}
                                size={12}
                                largeSize={12}/>
            </f.row>
            <label>Stage Type
              <f.tooltip tooltip={{
                content:   tt.stage.stageType,
                direction: 'bottom',
                size:      'small'
              }}/>
            </label>
            <f.row>
              <f.column size={6} largeSize={6}>
                <input type='radio'
                       name={radioName}
                       id={`${radioName  }-success`}
                       checked={stage.approval().isSuccess()}
                       onchange={stage.approval().makeOnSuccess.bind(stage.approval())}/>
                <label for={`${radioName  }-success`}>On Success</label>
              </f.column>
              <f.column size={6} largeSize={6} end>
                <input type='radio'
                       name={radioName}
                       id={`${radioName  }-manual`}
                       checked={stage.approval().isManual()}
                       onchange={stage.approval().makeManual.bind(stage.approval())}/>
                <label for={`${radioName  }-manual`}>Manual</label>
              </f.column>
            </f.row>
          </f.column>

          <f.column size={4} largeSize={5} end>
            <label></label>
            <f.row>
              <f.checkBox model={stage}
                          attrName='fetchMaterials'
                          size={12}
                          tooltip={{
                            content:   tt.stage.fetchMaterials,
                            direction: 'bottom',
                            size:      'small'
                          }}/>
            </f.row>
            <f.row>
              <f.checkBox model={stage}
                          attrName='neverCleanupArtifacts'
                          size={12}
                          tooltip={{
                            content:   tt.stage.neverCleanupArtifacts,
                            direction: 'bottom',
                            size:      'small'
                          }}/>
            </f.row>
            <f.row>
              <f.checkBox model={stage}
                          attrName='cleanWorkingDirectory'
                          label='Delete working directory on every build'
                          size={12}
                          tooltip={{
                            content:   tt.stage.cleanWorkingDirectory,
                            direction: 'bottom',
                            size:      'small'
                          }}
                          end={true}/>
            </f.row>
          </f.column>
        </f.row>
      </f.accordion>
    );
  }
};

let StageConfigDefinitionWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {
    const stage      = vnode.attrs.stage;
    const stageIndex = vnode.attrs.pipeline().stages().indexOfStage(vnode.attrs.stage);

    return (
      <div class='stage-definition' data-stage-name={stage.name()}>
        <f.removeButton onclick={vnode.attrs.onRemove} class="remove-stage"/>

        <SettingsWidget stage={stage}
                        vm={vnode.state.vmState(`settingsWidgetConfig-${  stageIndex}`)}/>
        <ApprovalWidget approval={stage.approval}
                        vm={vnode.state.vmState(`approvalWidgetConfig-${  stageIndex}`)}/>
        <EnvironmentVariablesConfigWidget title='Stages Environment Variables'
                                          variables={stage.environmentVariables}
                                          key={stage.environmentVariables().uuid()}
                                          vm={vnode.state.vmState(`environmentVariablesConfig-${  stageIndex}`)}/>

        <f.row>
          <f.column size={12} end={true}>
            <JobsConfigWidget jobs={stage.jobs}
                              elasticProfiles={vnode.attrs.elasticProfiles}
                              key={stage.jobs().uuid()}
                              vm={vnode.state.vmState(`jobsWidgetConfig-${  stageIndex}`)}/>
          </f.column>
        </f.row>
      </div>
    );
  }
};

const StagesConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);

    this.removeStage = function (pipeline, stage) {
      const previousStage = pipeline().stages().previousStage(stage);
      pipeline().stages().removeStage(stage);
      const firstStage = pipeline().stages().firstStage();

      vnode.attrs.currentSelection(previousStage || firstStage || pipeline().stages().createStage());
    };
  },

  view (vnode) {
    return (
      <div class='stage-definition-container'>
        <StageConfigDefinitionWidget stage={vnode.attrs.currentSelection()}
                                     elasticProfiles={vnode.attrs.elasticProfiles}
                                     pipeline={vnode.attrs.pipeline}
                                     onRemove={vnode.state.removeStage.bind(vnode.state, vnode.attrs.pipeline, vnode.attrs.currentSelection())}
                                     vm={vnode.state.vmState('stageDefinitionConfigs')}/>
      </div>
    );
  }
};

module.exports = StagesConfigWidget;
