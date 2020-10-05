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
import {ApiResult, ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroups} from "models/internal_pipeline_structure/pipeline_structure";
import {NotificationFilter, NotificationFilterJSON} from "models/new_preferences/notification_filters";
import {NotificationFiltersCRUD} from "models/new_preferences/notification_filters_crud";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {FlashMessage, FlashMessageModel} from "views/components/flash_message";
import {FormBody} from "views/components/forms/form";
import {CheckboxField, Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "views/pages/page_operations";

class FilterPipelineAndStage {
  pipelines: Array<(string | Option)>     = [NotificationFilter.DEFAULT_PIPELINE];
  protected stages: Map<string, string[]> = new Map<string, string[]>();

  constructor(pipelineGroups: PipelineGroups) {
    pipelineGroups.forEach((group) => {
      group.pipelines().forEach((pipeline) => {
        this.pipelines.push(pipeline.name());
        this.stages.set(pipeline.name(), pipeline.stages().map(s => s.name()));
      });
    });
  }

  getStages(pipeline: string): string[] {
    let stages = [NotificationFilter.DEFAULT_STAGE];
    if (this.stages.has(pipeline)) {
      stages = stages.concat(this.stages.get(pipeline)!);
    }
    return stages;
  }
}

abstract class BaseModal extends Modal {
  protected filter: Stream<NotificationFilter>;
  protected operationState       = Stream(OperationState.DONE);
  protected ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  protected readonly onSuccessfulSave: (msg: m.Children) => void;
  protected readonly flashMessage: FlashMessageModel;

  protected pipelinesAndStages: FilterPipelineAndStage;

  protected selectedPipeline: Stream<string>;
  protected stagesOptions: Stream<string[]>;

  protected constructor(filter: NotificationFilter,
                        pipelineGroups: Stream<PipelineGroups>,
                        onSuccessfulSave: (msg: m.Children) => void) {
    super(Size.small);
    this.filter             = Stream(filter);
    this.pipelinesAndStages = new FilterPipelineAndStage(pipelineGroups());
    this.onSuccessfulSave   = onSuccessfulSave;
    this.flashMessage       = new FlashMessageModel();

    this.selectedPipeline = Stream(filter.pipeline());
    this.stagesOptions    = Stream(this.pipelinesAndStages.getStages(this.selectedPipeline()));
  }

  public readonly POSSIBLE_EVENTS = ['All', 'Passes', 'Fails', 'Breaks', 'Fixed', 'Cancelled'];

  body(): m.Children {
    let errorMsg;
    if (this.flashMessage.hasMessage()) {
      errorMsg = <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>;
    }
    return <div>
      {errorMsg}
      <FormBody>
        <SelectField property={this.pipelineProxy.bind(this)} label={"Pipeline"}
                     errorText={this.filter().errors().errorsForDisplay('pipeline_name')}>
          <SelectFieldOptions selected={this.filter().pipeline()} items={this.pipelinesAndStages.pipelines}/>
        </SelectField>

        <SelectField property={this.filter().stage} label={"Stage"}
                     errorText={this.filter().errors().errorsForDisplay('stage_name')}>
          <SelectFieldOptions selected={this.filter().stage()} items={this.stagesOptions()}/>
        </SelectField>

        <SelectField property={this.filter().event} label={"Event"}
                     errorText={this.filter().errors().errorsForDisplay('event')}>
          <SelectFieldOptions selected={this.filter().event()} items={this.POSSIBLE_EVENTS}/>
        </SelectField>

        <CheckboxField label="Only if it contains my check-ins" property={this.filter().matchCommits}/>
      </FormBody>
    </div>;
  }

  buttons() {
    return [
      <ButtonGroup>
        <Cancel data-test-id="button-cancel" onclick={() => this.close()}
                ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Cancel>
        <Primary data-test-id="button-save"
                 disabled={this.isLoading()}
                 ajaxOperationMonitor={this.ajaxOperationMonitor}
                 ajaxOperation={this.performOperation.bind(this)}>Save</Primary>
      </ButtonGroup>
    ];
  }

  protected performOperation(e: MouseEvent) {
    e.stopPropagation();
    if (!this.filter().isValid()) {
      this.flashMessage.alert("Validation Errors!");
      return Promise.reject();
    }
    return this.operationPromise()
               .then(this.onOperationResult.bind(this));
  }

  protected abstract operationPromise(): Promise<any>;

  protected abstract successMessage(): m.Children;

  private onOperationResult(result: ApiResult<ObjectWithEtag<NotificationFilter>>) {
    result.do(this.onSuccess.bind(this), (e) => this.onError(e, result.getStatusCode()));
  }

  private onError(errorResponse: ErrorResponse, statusCode: number) {
    if (errorResponse.data) {
      this.filter(NotificationFilter.fromJSON(errorResponse.data as NotificationFilterJSON));
    } else if (errorResponse.body) {
      const parse = JSON.parse(errorResponse.body!);
      if (parse.id) {
        this.filter(NotificationFilter.fromJSON(parse));
      }
      this.flashMessage.alert(parse.message);
    } else {
      this.flashMessage.alert(errorResponse.message);
    }
  }

  private onSuccess(successResponse: SuccessResponse<ObjectWithEtag<NotificationFilter>>) {
    this.onSuccessfulSave(this.successMessage());
    this.filter(successResponse.body.object);
    this.close();
  }

  private pipelineProxy(newValue?: string) {
    if (!newValue) {
      return this.selectedPipeline();
    }
    this.selectedPipeline(newValue);
    this.filter().pipeline(newValue);
    this.stagesOptions(this.pipelinesAndStages.getStages(newValue));
    return newValue;
  }
}

export class CreateNotificationFilterModal extends BaseModal {
  constructor(pipelineGroups: Stream<PipelineGroups>, onSuccessfulSave: (msg: m.Children) => void) {
    super(NotificationFilter.default(), pipelineGroups, onSuccessfulSave);
  }

  title(): string {
    return "Add Notification Filter";
  }

  protected operationPromise(): Promise<any> {
    return NotificationFiltersCRUD.create(this.filter());
  }

  protected successMessage(): m.Children {
    return <span>Notification filter created successfully!</span>;
  }
}

export class EditNotificationFilterModal extends BaseModal {
  constructor(filter: NotificationFilter, pipelineGroups: Stream<PipelineGroups>, onSuccessfulSave: (msg: m.Children) => void) {
    super(filter.clone(), pipelineGroups, onSuccessfulSave);
  }

  title(): string {
    return "Edit Notification Filter";
  }

  protected operationPromise(): Promise<any> {
    return NotificationFiltersCRUD.update(this.filter());
  }

  protected successMessage(): m.Children {
    return <span>Notification filter updated successfully!</span>;
  }
}
