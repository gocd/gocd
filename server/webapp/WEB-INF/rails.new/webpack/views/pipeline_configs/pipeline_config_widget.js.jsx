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

let m                      = require('mithril');
let f                      = require('helpers/form_helper');
let PipelineSettingsWidget = require('views/pipeline_configs/pipeline_settings_widget');
let MaterialsConfigWidget  = require('views/pipeline_configs/materials_config_widget');
let PipelineFlowWidget     = require('views/pipeline_configs/pipeline_flow_widget');

const Stream          = require('mithril/stream');
const s               = require('string-plus');
const ComponentMixins = require('helpers/mithril_component_mixins');
const Pipeline        = require('models/pipeline_configs/pipeline');
const Modal           = require('views/shared/modal');

const ToggleViewWarning = {
  view (vnode) {
    return (
      <div class='modal-content'>
        <p> 'Proceed' will discard any unsaved data.</p>
        <div class='actions'>
          <a class='button secondary' href="javascript:void(0)"
             onclick={vnode.attrs.parentView.close.bind(vnode.attrs.parentView)}>Cancel</a>
          <a class='button primary'
             href={['/go/admin/pipelines/', vnode.attrs.pipeline().name(), '/general'].join('')}>Proceed</a>
        </div>
      </div>
    );
  }
};

const PipelineConfigWidget = function (options) {
  return {
    oninit (vnode) {
      const ctrl            = vnode.state;
      const self            = vnode.state;
      self.pipeline         = Stream();
      self.currentSelection = Stream();
      self.etag             = Stream();
      self.url              = options.url;
      self.callback         = options.callback;
      self.elasticProfiles  = options.elasticProfiles;
      self.vm               = new Pipeline.vm();

      ComponentMixins.HasViewModel.call(this);

      self.modal = new Modal({
        subView: {component: ToggleViewWarning, args: {pipeline: self.pipeline}},
        title:   'Unsaved Changes'
      });

      window.pipelineConfigWidget = Stream(this);

      self.setPipelineAndPreserveSelection = function (newPipeline) {
        const oldPipeline = self.pipeline();
        let newSelection;

        if (oldPipeline) {
          newSelection = newPipeline.stages().findStage((stage) => {
            return stage.name() === ctrl.currentSelection().name();
          });
        }

        newSelection = newSelection || newPipeline.stages().firstStage();

        self.pipeline(newPipeline);
        self.currentSelection(newSelection);
      };

      const extractEtag = function (xhr) {
        if (xhr.status === 200) {
          self.etag = Stream(xhr.getResponseHeader('ETag'));
        }
        return xhr.responseText;
      };

      Pipeline.find(self.url(), (data, _textStatus, xhr) => {
        extractEtag(xhr);
        self.setPipelineAndPreserveSelection(Pipeline.fromJSON(data));
        if (self.callback) {
          self.callback(ctrl);
        }
      });

      self.onSavePipeline = function () {
        self.vm.clearErrors();

        if (!self.pipeline().isValid()) {
          self.vm.markClientSideErrors();
          self.vm.defaultState();
          return;
        }

        self.vm.updating();
        m.redraw();

        const onReject  = function (data) {
          self.vm.saveFailed(data);
          if (data.data) {
            ctrl.setPipelineAndPreserveSelection(Pipeline.fromJSON(data.data));
          }
        };
        const onFulfill = function (data) {
          ctrl.setPipelineAndPreserveSelection(Pipeline.fromJSON(JSON.parse(data)));
          self.vm.saveSuccess();
        };

        self.pipeline().update(self.etag(), extractEtag).then(onFulfill, onReject);
      };
    },

    view (vnode) {
      const pipeline = vnode.state.pipeline();
      if (!pipeline) {
        return (<form class='page-spinner'/>);
      }

      const errors = vnode.state.vm.hasErrors() ? (<f.alert>{vnode.state.vm.errors()}</f.alert>) : undefined;

      const header = function () {
        return (
          <header class="pipeline-page-header">
            <f.row class="heading">
              <f.column size={10} largeSize={10}>
                <h1>
                  Pipeline configuation for pipeline
                  {' '}
                  {pipeline.name()}
                </h1>
                <a class='toggle-old-view' href="javascript:void(0)"
                   onclick={vnode.state.modal.open.bind(vnode.state.modal)}>Normal Edit</a>
              </f.column>
              <f.column size={1}>
                <f.button onclick={vnode.state.onSavePipeline.bind(vnode.state)}
                          class={`save-pipeline ${  vnode.state.vm.saveState()}`}>
                  <span class={`save-state ${  vnode.state.vm.saveState()}`}/>
                  Save
                </f.button>
              </f.column>
            </f.row>
          </header>
        );
      };

      const pipelineFlowWidget = function () {
        if (!s.isBlank(vnode.state.pipeline().template())) {
          return (
            <f.info>{['Pipeline :', "'", vnode.state.pipeline().name(), "'", 'uses template :', "'", vnode.state.pipeline().template(), "'",
              '. Template editing is not yet supported. '].join(' ')} Click
              <f.link href={["/go/admin/templates/", vnode.state.pipeline().template(), "/general"].join('')}>here
              </f.link>
              to edit template.</f.info>
          );
        }

        return (
          <PipelineFlowWidget pipeline={vnode.state.pipeline}
                              elasticProfiles={vnode.state.elasticProfiles}
                              currentSelection={vnode.state.currentSelection}
                              vm={vnode.state.vmState('pipelineFlowConfig')}/>
        );
      };

      return (
        <form class='pipeline'>
          {header()}
          <f.row class={`pipeline-body ${vnode.state.vm.pageSaveState()}`}>
            {errors}
            <f.column end={true} size={12}>
              <PipelineSettingsWidget pipeline={vnode.state.pipeline}
                                      vm={vnode.state.vmState('pipelineSettingsConfig')}/>
              <MaterialsConfigWidget materials={pipeline.materials}
                                     key={pipeline.materials().uuid}
                                     pipelineName={pipeline.name}
                                     vm={vnode.state.vmState('materialsConfig')}/>
              {pipelineFlowWidget()}
            </f.column>
          </f.row>
          <f.row class={vnode.state.vm.pageSaveSpinner()}/>
          {vnode.state.modal.view()}
        </form>
      );
    }
  };
};

module.exports = PipelineConfigWidget;
