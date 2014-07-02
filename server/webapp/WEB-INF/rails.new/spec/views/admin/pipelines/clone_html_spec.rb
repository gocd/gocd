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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/pipelines/clone.html.erb" do

  include ReflectiveUtil

  it "should have a text box for pipeline name and group name" do
    pipeline = PipelineConfigMother.pipelineConfig("some-pipeline")
    assigns[:pipeline] = pipeline
    pipeline_group = PipelineConfigs.new
    pipeline_group.add(pipeline)
    assigns[:pipeline_group] = pipeline_group
    assigns[:group_name] = ""
    assigns[:groups_list] = ["foo.bar", "some_other_group"]
    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    set(@cruise_config, "md5", "abc")
    template.stub(:is_user_a_group_admin?).and_return(false)

    render "/admin/pipelines/clone.html"

    response.body.should have_tag("form[method='post']") do
      with_tag("label[for='pipeline_group_pipeline_name']", "New Pipeline Name*")
      with_tag("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]']")
      with_tag("label[for='pipeline_group_group']", "Pipeline Group Name")
      with_tag("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='']")
    end
  end

end