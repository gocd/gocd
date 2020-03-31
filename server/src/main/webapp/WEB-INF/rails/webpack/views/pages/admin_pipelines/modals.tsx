/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiVersion, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Pipeline} from "models/environments/types";
import {PipelineGroup, PipelineGroups, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {ModelWithNameIdentifierValidator} from "models/shared/name_validation";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormBody} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "../page_operations";

export class MoveConfirmModal extends Modal {
  private readonly pipeline: PipelineWithOrigin;
  private pipelineGroups: PipelineGroups;
  private readonly sourceGroup: PipelineGroup;
  private readonly callback: (targetGroup: string) => void;
  private readonly selection: Stream<string>;

  constructor(allPipelineGroups: PipelineGroups,
              sourceGroup: PipelineGroup,
              pipeline: PipelineWithOrigin,
              callback: (targetGroup: string) => void) {
    super();
    this.pipelineGroups = allPipelineGroups;
    this.sourceGroup    = sourceGroup;
    this.pipeline       = pipeline;
    this.callback       = callback;
    this.selection      = Stream<string>();
  }

  body() {
    const items = _(this.pipelineGroups)
      .filter((eachGroup) => eachGroup.name() !== this.sourceGroup.name())
      .sortBy((eachGroup) => eachGroup.name().toLowerCase())
      .map((eachGroup) => ({id: eachGroup.name(), text: eachGroup.name()}))
      .value();

    return (
      <div>
        <SelectField dataTestId="move-pipeline-group-selection"
                     property={this.selection}
                     label={<span>Select the pipeline group where the pipeline <em>{this.pipeline.name()}</em> should be moved to:</span>}>
          <SelectFieldOptions items={items} selected={this.selection()}/>
        </SelectField>
      </div>
    );
  }

  title() {
    return `Move pipeline ${this.pipeline.name()}`;
  }

  buttons() {
    return [<Buttons.Primary data-test-id="button-move"
                             disabled={_.isEmpty(this.selection())}
                             onclick={this.doMove.bind(this)}>Move</Buttons.Primary>];
  }

  private doMove() {
    this.callback(this.selection());
    this.close();
  }
}

export class CreatePipelineGroupModal extends Modal {
  private readonly group: ModelWithNameIdentifierValidator;
  private readonly callback: (groupName: string) => void;

  constructor(callback: (groupName: string) => void) {
    super();
    this.group    = new ModelWithNameIdentifierValidator();
    this.callback = callback;
  }

  body() {
    return <TextField property={this.group.name}
                      errorText={this.group.errors().errorsForDisplay("name")}
                      onchange={() => this.group.validate("name")}
                      required={true}
                      label={"Pipeline group name"}/>;
  }

  title() {
    return "Create new pipeline group";
  }

  buttons() {
    return [<Buttons.Primary
      data-test-id="button-create"
      disabled={_.isEmpty(this.group.name()) || this.group.errors().hasErrors()}
      onclick={this.create.bind(this)}>Create</Buttons.Primary>];
  }

  private create() {
    this.callback(this.group.name());
    this.close();
  }
}

export class ClonePipelineConfigModal extends Modal {
  errorMessage?: string;
  private readonly sourcePipeline: Pipeline;
  private readonly newPipelineName: ModelWithNameIdentifierValidator;
  private readonly newPipelineGroupName: ModelWithNameIdentifierValidator;
  private readonly callback: (newPipelineName: string, newPipelineGroup: string) => void;

  constructor(sourcePipeline: Pipeline,
              callback: (newPipelineName: string, newPipelineGroup: string) => void) {
    super();
    this.sourcePipeline       = sourcePipeline;
    this.callback             = callback;
    this.newPipelineName      = new ModelWithNameIdentifierValidator();
    this.newPipelineGroupName = new ModelWithNameIdentifierValidator();
  }

  body() {
    if (this.isLoading()) {
      return;
    }
    if (!_.isEmpty(this.errorMessage)) {
      return <FlashMessage type={MessageType.alert} message={this.errorMessage}/>;
    }
    return (
      <FormBody>
        <Form last={true} compactForm={true}>
          <TextField
            property={this.newPipelineName.name}
            errorText={this.newPipelineName.errors().errorsForDisplay("name")}
            onchange={() => this.newPipelineName.validate("name")}
            required={true}
            label="New pipeline name"/>
          <TextField
            property={this.newPipelineGroupName.name}
            errorText={this.newPipelineGroupName.errors().errorsForDisplay("name")}
            onchange={() => this.newPipelineGroupName.validate("name")}
            required={true}
            label="Pipeline group name"
            helpText={"A new pipeline group will be created, if it does not already exist."}/>
        </Form>
      </FormBody>
    );
  }

  title(): string {
    return `Clone pipeline - ${this.sourcePipeline.name()}`;
  }

  buttons(): m.ChildArray {
    const disabled =
            (_.isEmpty(this.newPipelineName.name()) || this.newPipelineName.errors().hasErrors())
            || (_.isEmpty(this.newPipelineGroupName.name()) || this.newPipelineGroupName.errors().hasErrors());
    return [<Buttons.Primary
      data-test-id="button-clone"
      disabled={disabled}
      onclick={this.save.bind(this)}>Clone</Buttons.Primary>];
  }

  private save() {
    this.callback(this.newPipelineName.name(), this.newPipelineGroupName.name());
    this.close();
  }

}

export class DownloadPipelineModal extends Modal {
  private readonly pipeline: PipelineWithOrigin;
  private readonly pluginInfos: PluginInfos;
  private readonly callback: (pluginId: string) => void;

  constructor(pipeline: PipelineWithOrigin, pluginInfos: PluginInfos, callback: (pluginId: string) => void) {
    super();
    this.pipeline    = pipeline;
    this.callback    = callback;
    this.pluginInfos = pluginInfos.getConfigRepoPluginInfosWithExportPipelineCapabilities();
  }

  body() {
    return (
      <div>
        Choose the download format for the pipeline <em>{this.pipeline.name()}</em>:
        <ul>
          {this.pluginInfos.map((eachPluginInfo) => {
            return (
              <li>
                <Link href="javascript:void(0)"
                      onclick={(e: MouseEvent) => {
                        this.callback(eachPluginInfo.id);
                      }}>{eachPluginInfo.about.name}</Link>
              </li>);
          })}
        </ul>
      </div>
    );
  }

  title() {
    return `Download pipeline ${this.pipeline.name()}`;
  }

  buttons(): m.ChildArray {
    return [<Buttons.Primary data-test-id="button-close" onclick={this.close.bind(this)}>Close</Buttons.Primary>];
  }
}

export class ExtractTemplateModal extends Modal {
  private readonly sourcePipelineName: string;
  private readonly callback: (templateName: string) => void;
  private readonly templateName: ModelWithNameIdentifierValidator;

  constructor(sourcePipelineName: string, callback: (templateName: string) => void) {
    super();
    this.sourcePipelineName = sourcePipelineName;
    this.callback           = callback;
    this.templateName       = new ModelWithNameIdentifierValidator();
  }

  body() {
    return (
      <div>
        <TextField
          property={this.templateName.name}
          errorText={this.templateName.errors().errorsForDisplay("name")}
          onchange={() => this.templateName.validate("name")}
          required={true}
          label={"New template name"}/>
        <div>
          The pipeline <em>{this.sourcePipelineName}</em> will begin to use this new template.
        </div>
      </div>
    );
  }

  buttons() {
    return [<Buttons.Primary data-test-id="button-extract-template"
                             disabled={_.isEmpty(this.templateName.name()) || this.templateName.errors()
                                                                                  .hasErrors("name")}
                             onclick={this.extractTemplate.bind(this)}>Extract template</Buttons.Primary>];
  }

  title(): string {
    return `Extract template from pipeline ${this.sourcePipelineName}`;
  }

  private extractTemplate() {
    this.callback(this.templateName.name());
    this.close();
  }

}

export class DeletePipelineGroupModal extends Modal {
  private pipelineGrpName: string;
  private successCallback: (msg: m.Children) => void;
  private readonly message: m.Children;
  private operationState: Stream<OperationState> = Stream<OperationState>(OperationState.UNKNOWN);
  private errorMessage?: string;
  private apiService: ApiService<string>;

  constructor(pipelineGrpName: string, successCallback: (msg: m.Children) => void, apiService?: ApiService<string>) {
    super(Size.small);
    this.pipelineGrpName = pipelineGrpName;
    this.successCallback = successCallback;
    this.apiService      = apiService ? apiService : new DeletePipelineGroupService(pipelineGrpName);
    this.message         = <span>Are you sure you want to delete the pipeline group <em>{pipelineGrpName}</em>?</span>;
  }

  body(): m.Children {
    if (this.errorMessage !== undefined) {
      return <FlashMessage type={MessageType.alert} message={this.errorMessage}/>;
    }
    return <div>{this.message}</div>;
  }

  title(): string {
    return "Are you sure?";
  }

  buttons(): m.ChildArray {
    if (this.errorMessage !== undefined) {
      return [
        <Buttons.Primary ajaxOperationMonitor={this.operationState} data-test-id='button-no-delete'
                         onclick={this.close.bind(this)}>OK</Buttons.Primary>
      ];
    }
    return [
      <Buttons.Danger data-test-id='button-delete'
                      ajaxOperationMonitor={this.operationState}
                      ajaxOperation={this.delete.bind(this)}>Yes Delete</Buttons.Danger>,
      <Buttons.Cancel ajaxOperationMonitor={this.operationState}
                      data-test-id='button-no-delete' onclick={this.close.bind(this)}
      >No</Buttons.Cancel>
    ];
  }

  private delete() {
    return this.apiService.performOperation(
      () => {
        this.successCallback(
          <span>The pipeline group <em>{this.pipelineGrpName}</em> was deleted successfully!</span>
        );
        this.close();
      },
      (errorResponse: ErrorResponse) => {
        if (errorResponse.body) {
          this.errorMessage = JSON.parse(errorResponse.body).message;
        }
      }
    );
  }
}

export interface ApiService<T> {
  performOperation(onSuccess: (data: SuccessResponse<T>) => void,
                   onError: (message: ErrorResponse) => void): Promise<void>;
}

class DeletePipelineGroupService implements ApiService<string> {
  private pipelineGrpName: string;

  constructor(pipelineGrpName: string) {
    this.pipelineGrpName = pipelineGrpName;
  }

  performOperation(onSuccess: (data: SuccessResponse<string>) => void,
                   onError: (message: ErrorResponse) => void): Promise<void> {
    return ApiRequestBuilder.DELETE(SparkRoutes.pipelineGroupsPath(this.pipelineGrpName), ApiVersion.latest)
                            .then((result) => result.do(onSuccess, onError));

  }
}
