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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {FetchTaskAttributes} from "models/pipeline_configs/task";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {UpstreamJobToFetchArtifactFromWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/upstream_job_info_to_fetch_artifact_from_widget";

interface BuiltInArtifactViewAttrs {
  attributes: FetchTaskAttributes;
  autoSuggestions: Stream<any>;
}

export class BuiltInFetchArtifactView extends MithrilViewComponent<BuiltInArtifactViewAttrs> {
  view(vnode: m.Vnode<BuiltInArtifactViewAttrs>) {
    const attributes = vnode.attrs.attributes;

    const sourceHelpText      = "The path of the artifact directory or file of a specific job, relative to the sandbox directory. If the directory or file does not exist, the job is failed.";
    const destinationHelpText = "The path of the directory where the artifact is fetched to. The directory is overwritten if it already exists. The directory path is relative to the pipeline working directory.";

    return <div data-test-id="built-in-fetch-artifact-view">
      <UpstreamJobToFetchArtifactFromWidget {...vnode.attrs}/>
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
