#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "admin/jobs/artifacts.html.erb" do
  include GoUtil
  include FormUI

  plugin_id = 'my.artifact.plugin'
  artifact_plugin_template = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before(:each) do
    pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-name"].to_java(java.lang.String))
    stage = pipeline.get(0)
    @job = stage.getJobs().get(0)
    build_artifact_config = BuildArtifactConfig.new("build-source", "build-dest")
    test_artifact_config = TestArtifactConfig.new("test-source", "test-dest")
    external_artifact_config = PluggableArtifactConfig.new("artifact_id", "store_id", ConfigurationPropertyMother.create('KEY1', false, 'value1'), ConfigurationPropertyMother.create('key2', false, 'value2'))
    @job.artifactTypeConfigs().add(build_artifact_config)
    @job.artifactTypeConfigs().add(test_artifact_config)
    @job.artifactTypeConfigs().add(external_artifact_config)
    assign(:pipeline, pipeline)
    assign(:stage, stage)
    assign(:job, @job)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @artifact_metadata_store = double('artifact_metadata_store')
    allow(@artifact_metadata_store).to receive(:publishTemplate).and_return(artifact_plugin_template)
    assign(:plugin_name_to_id, {"Foo Plugin" => "foo"})
    assign(:store_id_to_plugin_id, {"store_id" => "foo"})
    assign(:artifact_meta_data_store, @artifact_metadata_store)
    @cruise_config.addPipeline("group-1", pipeline)
    @cruise_config.artifactStores().add(ArtifactStore.new("store_id", plugin_id))

    in_params(:stage_parent => "pipelines", :pipeline_name => "foo_bar", :stage_name => "stage-name", :action => "edit", :controller => "admin/stages", :job_name => "foo_bar_baz", :current_tab => "environment_variables")
  end

  it "should include a hidden field used to find out when all the artifacts are deleted" do
    render

    expect(response.body).to have_selector("form input[type='hidden'][name='default_as_empty_list[]'][value='job>artifactTypeConfigs']", visible: :hidden)
  end

  it "should have a heading as Artifacts with a title tooltip" do
    render

    expect(response.body).to have_selector("h3", :text => "Artifacts")
    expect(response.body).to have_selector("span.has_go_tip_right[title='Publish build artifacts to the artifact repository']")
  end

  it 'should have a dropdown for choosing artifact type' do
    render

    expect(response).to have_selector("a[id='add_artifact']", :text => 'Add')

    expect(response).to have_selector("select[id='select_artifact_type']")
    expect(response).to have_selector("select[id='select_artifact_type'] option[value='Build Artifact']", :text => 'Build')
    expect(response).to have_selector("select[id='select_artifact_type'] option[value='Test Artifact']", :text => 'Test')
    expect(response).to have_selector("select[id='select_artifact_type'] option[value='External Artifact']", :text => 'External')
  end

  it "should not prompt user to configure artifact store when it's already configured" do
    render

    artifact_divs = Capybara.string(response.body).all("div.artifact")
    artifact_divs.each {|prompt_message_div|
      expect(prompt_message_div).not_to have_selector("div.information.no_artifact_store")
    }
  end

  it "should render inputs for built in artifacts" do
    render

    expect(Capybara.string(response.body).all('div.artifact .row .columns span.has_go_tip_right')[0]['title']).to eq("There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact.")
    expect(Capybara.string(response.body).all('div.artifact .row .columns span.has_go_tip_right')[1]['title']).to eq("The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).")
    expect(Capybara.string(response.body).all('div.artifact .row .columns span.has_go_tip_right')[2]['title']).to eq("The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory")

    expect(response).to have_selector("div.artifact .row .columns input[type='text'][value='build-source']")
    expect(response).to have_selector("div.artifact .row .columns input[type='text'][value='test-source']")
    expect(response).to have_selector("div.artifact .row .columns input[type='text'][value='build-dest']")
    expect(response).to have_selector("div.artifact .row .columns input[type='text'][value='test-dest']")

  end

  it "should show the delete button for each artifact" do
    render

    expect(Capybara.string(response.body).all('div.artifact .row .columns .delete_artifact').size).to eq(2)
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end

end
