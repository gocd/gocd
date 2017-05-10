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

let m                 = require('mithril');
let f                 = require('helpers/form_helper');

const Stream          = require('mithril/stream');
const _               = require('lodash');
const ComponentMixins = require('helpers/mithril_component_mixins');
const tt              = require('helpers/pipeline_configs/tooltips');

let EnvironmentVariableWidget = {
  base: {
    view (vnode) {
      const children = vnode.children;
      const variable = vnode.attrs.variable;

      return (
        <f.row class='environment-variable'
               data-variable-type={variable.isSecureValue() ? 'secure' : 'plain'}
               data-variable-name={variable.name()}>
          <f.input model={variable}
                   attrName='name'
                   placeholder='Name'
                   onChange={vnode.attrs.onChange}
                   validate={true}
                   end={true}
                   size={5}
                   largeSize={4}/>
          {children}
          <f.column size={2} largeSize={2} end={true}>
            {vnode.attrs.removeChildPreContent}
            {vnode.attrs.removeWidget}
          </f.column>
        </f.row>
      );
    }
  },

  plain: {
    view (vnode) {
      return (
        <EnvironmentVariableWidget.base {...vnode.attrs}>
          <f.input model={vnode.attrs.variable}
                   attrName='value'
                   placeholder='Value'
                   onChange={vnode.attrs.onChange}
                   validate={'all'}
                   size={5}
                   largeSize={4}/>
        </EnvironmentVariableWidget.base>
      );
    }
  },

  secure: {
    view (vnode) {
      const variable = vnode.attrs.variable;
      let content, removeChildPreContent;

      if (variable.isEditingValue()) {
        content = [
          (<f.input model={variable}
                    attrName='value'
                    placeholder='Value'
                    onChange={vnode.attrs.onChange}
                    type={variable.isSecureValue() ? 'password' : 'text'}
                    size={5}
                    largeSize={4}/>
          )
        ];

        if (variable.isDirtyValue()) {
          removeChildPreContent = (
            <f.resetButton class='position-reset-button' onclick={variable.resetToOriginalValue.bind(variable)}/>
          );
        }
      } else {
        content = [
          (<f.column size={5} largeSize={4}>
            <input type='password' value='password' disabled/>
          </f.column>)
        ];

        removeChildPreContent = (
          <f.editButton onclick={variable.editValue.bind(variable)} class="edit-secure-variable"/>
        );
      }

      return (
        <EnvironmentVariableWidget.base {...vnode.attrs} removeChildPreContent={removeChildPreContent}>
          {content}
        </EnvironmentVariableWidget.base>
      );
    }
  }
};

let VariablesWidget = {
  plain:  {
    oninit (vnode) {
      this.args = vnode.attrs;
      ComponentMixins.ManagesCollection.call(this, {
        as: 'Variable',
        map (callback) {
          return _.map(vnode.attrs.toView(), callback);
        },
        last () {
          return _.last(vnode.attrs.toView());
        },
        add () {
          const variable = vnode.attrs.variables().createVariable({cipherText: ''});
          variable.editValue();
        },
        onInitialize () {
          this.changed();
        }
      });
    },

    view (vnode) {
      return (
        <div>
          {vnode.state.map((variable) => {
            return (
              <EnvironmentVariableWidget.plain variable={variable}
                                               removeWidget={vnode.state.removeLink.call(vnode.state, variable)}
                                               onChange={vnode.state.changed.bind(vnode.state)}
                                               key={variable.uuid()}/>
            );
          })}
        </div>
      );
    }
  },
  secure: {
    oninit (vnode) {
      this.args = vnode.attrs;
      ComponentMixins.ManagesCollection.call(this, {
        as: 'Variable',
        map (callback) {
          return _.map(vnode.attrs.toView(), callback);
        },
        last () {
          return _.last(vnode.attrs.toView());
        },
        add () {
          const variable = vnode.attrs.variables().createVariable();
          variable.becomeSecureValue();
          variable.editValue();
        },
        onInitialize () {
          this.changed();
        }
      });
    },

    view (vnode) {
      return (
        <div>
          {vnode.state.map((variable) => {
            return (
              <EnvironmentVariableWidget.secure variable={variable}
                                                removeWidget={vnode.state.removeLink.call(vnode.state, variable)}
                                                onChange={vnode.state.changed.bind(vnode.state)}
                                                key={variable.uuid()}/>
            );
          })}
        </div>
      );
    }
  }
};


const EnvironmentVariablesConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {
    return (
      <f.accordion accordionTitles={[
        (<span>{vnode.state.args.title}
          <f.tooltip tooltip={{content: tt.environmentVariables.main}} model={vnode.state.args.variables()}/>
        </span>)
      ]}
                   accordionKeys={['environment-variables']}
                   selectedIndex={vnode.state.vmState('environmentVariablesSelected', Stream(-1))}
                   class='environment-variables accordion-inner'>
        <div>
          <VariablesWidget.plain variables={vnode.state.args.variables}
                                 toView={vnode.state.args.variables().plainVariables.bind(vnode.state.args.variables())}/>

          <h5 class="secure-environment-variables-header sub-header">Secure Environment Variables</h5>
          <VariablesWidget.secure variables={vnode.state.args.variables}
                                  toView={vnode.state.args.variables().secureVariables.bind(vnode.state.args.variables())}/>
        </div>
      </f.accordion>
    );
  }
};

module.exports = EnvironmentVariablesConfigWidget;
