##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe "comparison/list.json.erb" do
  include StageModelMother
  it "should return JSON object with 'html' key and array of markup" do
    pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 17, "label-17", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
    pipeline_instances = PipelineInstanceModels.createPipelineInstanceModels()
    pipeline_instances.add(pipeline)
    assign(:pipeline_instances, pipeline_instances)
    view.should_receive(:render_json).with(:partial=>'pipeline_autocomplete_list_entry.html.erb', :locals => {:scope => {:pipeline => pipeline}}).and_return("\"abc\"")
    render :template => "comparison/list.json.erb"

    json = JSON.parse(response.body)
    expect(json["html"]).to eq([{"data" => "abc", "value" => "17", "result" => "label-17"}])
  end
end
