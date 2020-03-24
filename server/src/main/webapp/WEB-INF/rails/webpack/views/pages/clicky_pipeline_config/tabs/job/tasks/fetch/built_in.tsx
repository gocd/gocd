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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {FetchTaskAttributes} from "models/pipeline_configs/task";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {CheckboxField, TextField} from "views/components/forms/input_fields";

interface BuiltInArtifactViewState {
  pipelineSuggestions: PipelinesAutocompletionProvider;
  stageSuggestions: StagesAutocompletionProvider;
  jobSuggestions: JobsAutocompletionProvider;
}

interface BuiltInArtifactViewAttrs {
  attributes: FetchTaskAttributes;
  autoSuggestions: Stream<any>;
}

export class BuiltInArtifactView extends MithrilComponent<BuiltInArtifactViewAttrs, BuiltInArtifactViewState> {
  oninit(vnode: m.Vnode<BuiltInArtifactViewAttrs, BuiltInArtifactViewState>) {
    vnode.state.pipelineSuggestions = new PipelinesAutocompletionProvider(vnode.attrs.autoSuggestions);
    vnode.state.stageSuggestions    = new StagesAutocompletionProvider(vnode.attrs.attributes.pipeline,
                                                                       vnode.attrs.autoSuggestions);
    vnode.state.jobSuggestions      = new JobsAutocompletionProvider(vnode.attrs.attributes.pipeline,
                                                                     vnode.attrs.attributes.stage,
                                                                     vnode.attrs.autoSuggestions);
  }

  view(vnode: m.Vnode<BuiltInArtifactViewAttrs, BuiltInArtifactViewState>) {
    const attributes = vnode.attrs.attributes;

    const pipelineHelpText    = "The name of direct upstream pipeline or ancestor pipeline of one of the upstream pipelines on which the pipeline of the job depends on. The pipeline should be a dependency material or should be reachable as an ancestor(of the form fetch-from-pipeline/path/to/upstream-pipeline) of at-least one dependency material. Defaults to current pipeline if not specified.";
    const stageHelpText       = "The name of the stage to fetch artifacts from.";
    const jobHelpText         = "The name of the job to fetch artifacts from.";
    const sourceHelpText      = "The path of the artifact directory or file of a specific job, relative to the sandbox directory. If the directory or file does not exist, the job is failed.";
    const destinationHelpText = "The path of the directory where the artifact is fetched to. The directory is overwritten if it already exists. The directory path is relative to the pipeline working directory.";

    return <div data-test-id="built-in-artifact-view">
      <AutocompleteField helpText={pipelineHelpText}
                         required={false}
                         errorText={attributes.errors().errorsForDisplay("pipeline")}
                         provider={vnode.state.pipelineSuggestions}
                         onchange={vnode.state.stageSuggestions.update.bind(vnode.state.stageSuggestions)}
                         label="Pipeline"
                         property={attributes.pipeline}/>
      <AutocompleteField helpText={stageHelpText}
                         errorText={attributes.errors().errorsForDisplay("stage")}
                         provider={vnode.state.stageSuggestions}
                         onchange={vnode.state.jobSuggestions.update.bind(vnode.state.jobSuggestions)}
                         required={true}
                         label="Stage"
                         property={attributes.stage}/>
      <AutocompleteField helpText={jobHelpText}
                         errorText={attributes.errors().errorsForDisplay("job")}
                         provider={vnode.state.jobSuggestions}
                         required={true}
                         label="Job"
                         property={attributes.job}/>
      <TextField helpText={sourceHelpText}
                 required={true}
                 label="Source"
                 errorText={attributes.errors().errorsForDisplay("source")}
                 property={attributes.source}/>
      <CheckboxField label="Source is a file(Not a directory)"
                     property={attributes.isSourceAFile}/>
      <TextField helpText={destinationHelpText}
                 label="Destination"
                 errorText={attributes.errors().errorsForDisplay("destination")}
                 property={attributes.destination}/>

    </div>;
  }
}

class PipelinesAutocompletionProvider extends SuggestionProvider {
  private readonly suggestions: Stream<any>;

  constructor(suggestions: Stream<any>) {
    super();
    this.suggestions = suggestions;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve(Object.keys(this.suggestions()));
    });
  }
}

class StagesAutocompletionProvider extends SuggestionProvider {
  private readonly suggestions: Stream<any>;
  private readonly pipeline: Stream<string | undefined>;

  constructor(pipeline: Stream<string | undefined>, suggestions: Stream<any>) {
    super();
    this.pipeline    = pipeline;
    this.suggestions = suggestions;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      const stages = this.suggestions()[this.pipeline()!];
      (stages) ? resolve(Object.keys(stages)) : resolve([]);
    });
  }
}

class JobsAutocompletionProvider extends SuggestionProvider {
  private readonly suggestions: Stream<any>;
  private readonly pipeline: Stream<string | undefined>;
  private readonly stage: Stream<string>;

  constructor(pipeline: Stream<string | undefined>, stage: Stream<string>, suggestions: Stream<any>) {
    super();
    this.pipeline    = pipeline;
    this.stage       = stage;
    this.suggestions = suggestions;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      const stages = this.suggestions()[this.pipeline()!];
      if (!stages) {
        resolve([]);
      }

      const jobs = stages[this.stage()];
      (jobs) ? resolve(Object.keys(jobs)) : resolve([]);
    });
  }
}
