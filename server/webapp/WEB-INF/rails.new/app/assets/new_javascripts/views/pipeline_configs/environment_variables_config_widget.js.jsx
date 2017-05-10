/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  'mithril', 'lodash', 'helpers/form_helper', 'helpers/mithril_component_mixins', 'helpers/pipeline_configs/tooltips'
], function (m, _, f, ComponentMixins, tt) {

  var EnvironmentVariableWidget = {
    base: {
      view: function (_ctrl, args, children) {
        var variable = args.variable;

        return (
          <f.row class='environment-variable'
                 data-variable-type={variable.isSecureValue() ? 'secure' : 'plain'}
                 data-variable-name={variable.name()}>
              <f.input model={variable}
                       attrName='name'
                       placeholder='Name'
                       onChange={args.onChange}
                       validate={true}
                       end={true}
                       size={5}
                       largeSize={4}/>
            {children}
            <f.column size={2} largeSize={2} end={true}>
                {args.removeChildPreContent}
                {args.removeWidget}
            </f.column>
          </f.row>
        );
      }
    },

    plain: {
      view: function (_ctrl, args) {
        return (
          <EnvironmentVariableWidget.base {...args}>
            <f.input model={args.variable}
                     attrName='value'
                     placeholder='Value'
                     onChange={args.onChange}
                     validate={'all'}
                     size={5}
                     largeSize={4}/>
          </EnvironmentVariableWidget.base>
        );
      }
    },

    secure: {
      view: function (_ctrl, args) {
        var variable = args.variable,
          content,
          removeChildPreContent;

        if (variable.isEditingValue()) {
          content = [
            (<f.input model={variable}
                      attrName='value'
                      placeholder='Value'
                      onChange={args.onChange}
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
          <EnvironmentVariableWidget.base {...args} removeChildPreContent={removeChildPreContent}>
            {content}
          </EnvironmentVariableWidget.base>
        );
      }
    }
  };

  var VariablesWidget = {
    plain:  {
      controller: function (args) {
        this.args = args;
        ComponentMixins.ManagesCollection.call(this, {
          as:           'Variable',
          map:          function (callback) {
            return _.map(args.toView(), callback);
          },
          last:         function () {
            return _.last(args.toView());
          },
          add:          function () {
            var variable = args.variables().createVariable({cipherText: ''});
            variable.editValue();
          },
          onInitialize: function () {
            this.changed();
          }
        });
      },

      view: function (ctrl) {
        return (
          <div>
            {ctrl.map(function (variable) {
              return (
                <EnvironmentVariableWidget.plain variable={variable}
                                                 removeWidget={ctrl.removeLink.call(ctrl, variable)}
                                                 onChange={ctrl.changed.bind(ctrl)}
                                                 key={variable.uuid()}/>
              );
            })}
          </div>
        );
      }
    },
    secure: {
      controller: function (args) {
        this.args = args;
        ComponentMixins.ManagesCollection.call(this, {
          as:           'Variable',
          map:          function (callback) {
            return _.map(args.toView(), callback);
          },
          last:         function () {
            return _.last(args.toView());
          },
          add:          function () {
            var variable = args.variables().createVariable();
            variable.becomeSecureValue();
            variable.editValue();
          },
          onInitialize: function () {
            this.changed();
          }
        });
      },

      view: function (ctrl) {
        return (
          <div>
            {ctrl.map(function (variable) {
              return (
                <EnvironmentVariableWidget.secure variable={variable}
                                                  removeWidget={ctrl.removeLink.call(ctrl, variable)}
                                                  onChange={ctrl.changed.bind(ctrl)}
                                                  key={variable.uuid()}/>
              );
            })}
          </div>
        );
      }
    }
  };


  var EnvironmentVariablesConfigWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
    },

    view: function (ctrl) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>{ctrl.args.title}<f.tooltip tooltip={{content: tt.environmentVariables.main}} model={ctrl.args.variables()}/></span>
                        )
        ]}
                     accordionKeys={['environment-variables']}
                     selectedIndex={ctrl.vmState('environmentVariablesSelected', m.prop(-1))}
                     class='environment-variables accordion-inner'>
          <div>
            <VariablesWidget.plain variables={ctrl.args.variables}
                                   toView={ctrl.args.variables().plainVariables.bind(ctrl.args.variables())}/>

            <h5 class="secure-environment-variables-header sub-header">Secure Environment Variables</h5>
            <VariablesWidget.secure variables={ctrl.args.variables}
                                    toView={ctrl.args.variables().secureVariables.bind(ctrl.args.variables())}/>
          </div>
        </f.accordion>
      );
    }
  };

  return EnvironmentVariablesConfigWidget;
});
