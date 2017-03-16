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
  'mithril', 'lodash', 'string-plus',
  'helpers/form_helper', 'helpers/pipeline_configs/tooltips', 'helpers/mithril_component_mixins', 'models/pipeline_configs/materials',
  'views/pipeline_configs/test_connection_widget', 'views/pipeline_configs/pipeline_stage_field_widget', 'models/pipeline_configs/pipelines',
  'models/pipeline_configs/pluggable_scms', 'views/pipeline_configs/pluggable_scm_widget', 'views/pipeline_configs/material_filter_widget',
  'models/pipeline_configs/plugin_infos'
], function (m, _, s, f, tt, ComponentMixins, Materials, TestConnectionWidget, PipelineStageField, Pipelines, PluggableSCMs,
             PluggableSCMWidget, MaterialFilterWidget, PluginInfos) {

  var PasswordField = {
    view: function (_ctrl, args) {
      var model               = args.model;
      var attrName            = args.attrName;
      var capitalizedAttrName = _.upperFirst(attrName);

      if (model['isEditing' + capitalizedAttrName]()) {
        return (
          <div>
            <f.inputWithLabel model={model}
                              attrName={attrName}
                              label={['Password ']}
                              placeholder="Password"
                              type='password'
                              withReset={true}/>
          </div>
        );
      } else {
        return (
          <f.column>
            <label>
              Password{' '}
              <f.link onclick={model['edit' + capitalizedAttrName].bind(model)}>Edit</f.link>
            </label>
            <input type='password'
                   readonly={true}
                   value={s.uuid()}/>
          </f.column>
        );
      }
    }
  };

  var UrlField = {
    view: function(_ctrl, args) {
      return (
        <f.inputWithLabel attrName='url'
                          type='url'
                          validate={true}
                          isRequired={true}
                          model={args.material}/>
      );
    }
  };

  var UsernameField = {
    view: function (_ctrl, args) {
      return (
        <f.inputWithLabel attrName='username'
                          model={args.material}/>
      );
    }
  };

  var DestinationField = {
    view: function (_ctrl, args) {
      return (
        <f.inputWithLabel attrName='destination'
                          label="Destination"
                          model={args.material}
                          validate={true}
                          tooltip={{
                            content: tt.material.destination,
                            direction: 'bottom',
                            size: 'small'
                          }}/>
      );
    }
  };


  var NameField = {
    view: function (_ctrl, args) {
      return (
        <f.inputWithLabel attrName='name'
                          validate={true}
                          model={args.material}
                          tooltip={{
                            content: tt.material.name,
                            direction: 'bottom',
                            size: 'small'
                          }}/>
      );
    }
  };

  var BranchField = {
    view: function (_ctrl, args) {
      return (
        <f.inputWithLabel attrName='branch'
                          model={args.material}/>
      );
    }
  };

  var AutoUpdateField = {
    view: function (_ctrl, args) {
      return (
        <f.checkBox model={args.material}
                    attrName='autoUpdate'
                    addPadding={true}
                    end={true}/>
      );
    }
  };

  var TestConnection = {
    controller: function () {
      ComponentMixins.HasViewModel.call(this);
    },

    view: function (ctrl, args) {
      return (
        <f.row>
          <f.column size={12} largeSize={12}>
            <TestConnectionWidget material={args.material}
                                  pipelineName={args.pipelineName}
                                  vm={ctrl.vmState('testConnection')}/>
          </f.column>
        </f.row>
      );
    }
  };

  var MaterialViews = {
    base: {
      controller: function (args) {
        this.args     = args;
        ComponentMixins.HasViewModel.call(this);
      },

      view: function (ctrl, args, children) {
        var title = function () {
          if(Materials.isBuiltInType(args.material.type())) {
            return [args.material.type(), '-', args.material.name()].join(' ');
          }
          if(args.material.type() === 'plugin') {
            return [args.material.type(), '-', args.material.scm() ? args.material.scm().name() : ''].join(' ');
          } else {
            return args.material.type();
          }
        };

        return (
          <f.accordion class="material-definitions accordion-inner"
                       accordionTitles={[title()]}
                       accordionKeys={[args.material.uuid()]}
                       selectedIndex={ctrl.vmState('selectedMaterialIndex', m.prop(-1))}>

            <div class="material-definition">
              <f.removeButton onclick={args.onRemove.bind(this, args.material)} class="remove-material"/>
              {children}
            </div>
          </f.accordion>
        );
      }
    },

    svn: {
      view: function (_ctrl, args) {
        var material = args.material;

        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <UrlField material={material}/>
              <AutoUpdateField material={material}/>
            </f.row>
            <f.row>
              <UsernameField material={material}/>
              <PasswordField model={material}
                             attrName='passwordValue'/>
              <f.checkBox type="checkbox"
                          model={material}
                          attrName='checkExternals'
                          addPadding={true}
                          end={true}/>
            </f.row>
            <f.row>
              <DestinationField material={material}/>
            </f.row>
            <TestConnection material={material} pipelineName={args.pipelineName}/>
            <MaterialFilterWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    git: {
      view: function (_ctrl, args) {
        var material = args.material;

        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <UrlField material={material}/>
              <AutoUpdateField material={material}/>
            </f.row>
            <f.row>
              <BranchField material={material}/>
              <f.checkBox model={args.material}
                          attrName='shallowClone'
                          label="Shallow clone (recommended for large repositories)"
                          addPadding={true}
                          end={true}
                          size={4}
                          largeSize={4}/>
            </f.row>
            <f.row>
              <DestinationField material={material}/>
            </f.row>
            <TestConnection material={material}  pipelineName={args.pipelineName}/>
            <MaterialFilterWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    hg: {
      view: function (_ctrl, args) {
        var material = args.material;
        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <UrlField material={material}/>
              <AutoUpdateField material={material}/>
            </f.row>
            <f.row>
              <DestinationField material={material}/>
            </f.row>
            <TestConnection material={material}  pipelineName={args.pipelineName}/>
            <MaterialFilterWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    p4: {
      view: function (_ctrl, args) {
        var material = args.material;
        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <f.inputWithLabel attrName='port'
                                model={material}
                                validate={true}
                                isRequired={true}
                                label="Server and Port"
                                onChange={m.withAttr('value', material.port)}/>
              <AutoUpdateField material={material}/>
            </f.row>
            <f.row>
              <UsernameField material={material}/>
              <PasswordField model={material}
                             attrName='passwordValue'/>
            </f.row>
            <f.row>
              <f.textareaWithLabel attrName='view'
                                   validate={true}
                                   isRequired={true}
                                   model={material}
                                   size={4}/>
              <f.checkBox name="material[use_tickets]"
                          type="checkbox"
                          model={material}
                          attrName='useTickets'
                          addPadding={true}
                          end={true}/>
            </f.row>
            <f.row>
              <DestinationField material={material}/>
            </f.row>
            <TestConnection material={material}  pipelineName={args.pipelineName}/>
            <MaterialFilterWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    tfs: {
      view: function (_ctrl, args) {
        var material = args.material;
        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <UrlField material={material}/>
              <AutoUpdateField material={material}/>
            </f.row>
            <f.row>
              <f.inputWithLabel attrName='domain'
                                model={material}/>
              <f.inputWithLabel attrName='projectPath'
                                model={material}
                                validate={true}
                                isRequired={true}
                                end={true}/>
            </f.row>
            <f.row>
              <f.inputWithLabel attrName='username'
                                model={material}
                                isRequired={true}
                                validate={true}/>
              <PasswordField model={material}
                             attrName='passwordValue'
                             end={true}/>
            </f.row>
            <f.row>
              <DestinationField material={material}/>
            </f.row>
            <TestConnection material={material}  pipelineName={args.pipelineName}/>
            <MaterialFilterWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    dependency: {
      view: function (_ctrl, args) {
        var material = args.material;

        return (
          <MaterialViews.base {...args}>
            <f.row>
              <NameField material={material}/>
              <f.column size={4} end='true'>
                <PipelineStageField material={material} pipelines={args.pipelines}/>
              </f.column>
            </f.row>
          </MaterialViews.base>
        );
      }
    },

    plugin: {
      view: function (_ctrl, args) {
        var material = args.material;

        return (
          <MaterialViews.base {...args}>
            <PluggableSCMWidget material={material}/>
          </MaterialViews.base>
        );
      }
    },

    package: {
      view: function (_ctrl, args) {
        return (
          <MaterialViews.base {...args}>
            <f.info>Package materials edit is not yet supported, click <f.link href={["/go/admin/pipelines/",  args.pipelineName(), "/materials?current_tab=materials"].join('')}>here</f.link> to edit.</f.info>
          </MaterialViews.base>
        );
      }
    }

  };

  var MaterialTypeSelector = {
    controller: function () {
      this.selected       = m.prop('git');
    },

    view: function (ctrl, args) {
      var items = _.reduce(_.merge({}, Materials.Types, PluggableSCMs.Types), function (accumulator, value, key) {
        accumulator.push({id: key, text: value.description});
        return accumulator;
      }, []);

      return (
        <f.row class='material-selector'>
          <f.select
            model={ctrl}
            attrName='selected'
            class='inline'
            label='Add a new material of type'
            items={items}
            size={3}
            largeSize={3}/>
          <f.column size={2} end={true}>
            <f.link class='add-button button' onclick={args.createMaterial.bind(ctrl, ctrl.selected)}>Add</f.link>
          </f.column>
        </f.row>
      );
    }
  };

  var MaterialsConfigWidget = {
    controller: function (args) {
      var self       = this;
      self.args      = args;
      self.pipelines = Pipelines.init(args.pipelineName());
      ComponentMixins.HasViewModel.call(self);

      self.removeMaterial = function (materials) {
        return function (material) {
          materials().removeMaterial(material);
        };
      };

      self.createMaterial = function (materials) {
        return function (type) {
          var newMaterial = Materials.isBuiltInType(type()) ? materials().createMaterial({type: type()})
                                                            : materials().createMaterial({type: type(), pluginInfo: PluginInfos.findById(type())});

          var indexOfMaterial = materials().indexOfMaterial(newMaterial);
          self.vmState('material-' + indexOfMaterial, {selectedMaterialIndex: m.prop(0)});
        };
      };
    },

    view: function (ctrl, args) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>Materials</span>
                        )
        ]}
                     accordionKeys={['materials']}
                     class='materials'
                     selectedIndex={ctrl.vmState('materialsSelected', m.prop(-1))}>
          <div>
            {args.materials().mapMaterials(function (material, index) {
              var materialView = MaterialViews[material.type()];
              return (m.component(materialView, {
                material: material,
                onRemove: ctrl.removeMaterial(args.materials),
                key: material.uuid(),
                pipelineName: args.pipelineName,
                pipelines: ctrl.pipelines,
                vm: ctrl.vmState('material-' + index)
              }));
            })}
            <MaterialTypeSelector createMaterial={ctrl.createMaterial(args.materials)}
                                  key='material-type-selector'/>
          </div>
        </f.accordion>
      );
    }
  };

  return MaterialsConfigWidget;
});
