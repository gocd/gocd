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

describe PipelinesController do

  before(:each)  do
    @stage_service = double('stage service')
    @material_service = double('material service')
    @user = Username.new(CaseInsensitiveString.new("foo"))
    @user_id = 1
    @status = HttpOperationResult.new
    allow(HttpOperationResult).to receive(:new).and_return(@status)
    @localized_result = HttpLocalizedOperationResult.new
    allow(HttpLocalizedOperationResult).to receive(:new).and_return(@localized_result)
    allow(controller).to receive(:current_user).and_return(@user)
    allow(controller).to receive(:current_user_entity_id).and_return(@user_id)
    allow(controller).to receive(:stage_service).and_return(@stage_service)
    allow(controller).to receive(:material_service).and_return(@material_service)

    @pim = PipelineHistoryMother.singlePipeline("pipline-name", StageInstanceModels.new)
    allow(controller).to receive(:pipeline_history_service).and_return(@pipeline_history_service=double())
    allow(controller).to receive(:pipeline_lock_service).and_return(@pipieline_lock_service=double())
    allow(controller).to receive(:go_config_service).and_return(@go_config_service=double())
    allow(controller).to receive(:security_service).and_return(@security_service=double())
    allow(controller).to receive(:pipeline_config_service).and_return(@pipeline_config_service=double())
    allow(controller).to receive(:pipeline_selections_service).and_return(@pipeline_selections_service=double())
    @pipeline_identifier = PipelineIdentifier.new("blah", 1, "label")
    allow(controller).to receive(:populate_config_validity)
    @pipeline_service = double('pipeline_service')
    allow(controller).to receive(:pipeline_service).and_return(@pipeline_service)
  end

  describe "build_cause" do
    it "should render build cause" do
      expect(@pipeline_history_service).to receive(:findPipelineInstance).with("pipeline_name", 10, @user, @status).and_return(@pim)

      get :build_cause, params:{:pipeline_name => "pipeline_name", :pipeline_counter => "10"}

      expect(assigns[:pipeline_instance]).to eq(@pim)
      assert_template "build_cause"
      assert_template layout: false
    end

    it "should fail to render build cause when not allowed to see pipeline" do
      expect(@pipeline_history_service).to receive(:findPipelineInstance) do |pipeline_name, pipeline_counter, user, result|
        result.error("not allowed", "you are so not allowed", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      expect(controller).to receive(:render).with("build_cause", layout: false).never
      expect(controller).to receive(:render_operation_result_if_failure).with(@status)

      get :build_cause, params:{:pipeline_name => "pipeline_name", :pipeline_counter => "10", :layout => false}
    end

    it "should route to build_cause action" do
      expect({:get => "/pipelines/name_of_pipeline/15/build_cause"}).to route_to(:controller => "pipelines", :action => "build_cause", :pipeline_name => "name_of_pipeline", :pipeline_counter => "15", :no_layout => true)
    end

    it "should route to build_cause action with dots in pipline name" do
      expect({:get => "/pipelines/blah.pipe-line/1/build_cause"}).to route_to(:controller => "pipelines", :action => "build_cause", :pipeline_name => "blah.pipe-line", :pipeline_counter => "1", :no_layout => true)
    end

    it "should have a named route" do
      expect(controller.send(:build_cause_url, :pipeline_name => "foo", :pipeline_counter => 20)).to eq("http://test.host/pipelines/foo/20/build_cause")
    end
  end

  describe "error_handling" do
    before do
      class << @controller
        include ActionRescue
      end
    end

    it "should handle exceptions and log errors" do
      nil.bomb rescue exception = $!
      expect(Rails.logger).to receive(:error).with(%r{#{exception.message}}m)
      expect(@controller).to receive(:render_error_template)
      @controller.rescue_action(exception)
    end
  end
end
