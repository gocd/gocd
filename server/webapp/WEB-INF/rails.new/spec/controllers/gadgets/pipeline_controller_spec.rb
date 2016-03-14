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

describe Gadgets::PipelineController do
  before :each do
    controller.stub(:populate_health_messages)
    controller.stub(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new("user")))
    UserHelper.stub(:getUserId).and_return(1)
    controller.stub(:populate_config_validity)
  end


  describe "index" do
    it "should have return the pipeline gadget" do
      expect(pipeline_status_gadget_url).to eq("http://test.host/gadgets/pipeline.xml")
    end

    it "should resolve" do
      expect(:get => "/gadgets/pipeline.xml").to route_to(:controller => "gadgets/pipeline", :action => "index", :format => "xml")
    end

    it "should set the page expiration to 1 day" do
      get :index, :format => 'xml'
      expect(response.headers["Cache-Control"]).to eq("max-age=86400, public")
      assert_template layout: false
    end
  end


  describe "content" do
    it "should return params for content" do
      expect(:get => "/gadgets/pipeline/content").to route_to(:controller => "gadgets/pipeline", :action => "content", :no_layout => true)
    end

    it "should return the pipeline gadget content" do
      expect(pipeline_status_gadget_content_url).to eq("http://test.host/gadgets/pipeline/content")
    end

    it "should return the pipeline model by pipeline name" do
      controller.stub(:go_config_service).and_return(@go_config_service=double())
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new("first")).and_return(true)

      controller.stub(:security_service).and_return(@security_service=double())
      @security_service.should_receive(:hasViewPermissionForPipeline).with(@user, "first").and_return(true)

      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
      pipeline_group_models = java.util.ArrayList.new

      group1 = PipelineGroupModel.new("firstGroup")
      group1.add(pipeline = PipelineModel.new("first", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused))
      group1.add(PipelineModel.new("second", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused))

      group2 = PipelineGroupModel.new("secondGroup")
      group2.add(PipelineModel.new("cruise", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused))
      group2.add(PipelineModel.new("mingle", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused))

      pipeline_group_models.add(group1)
      pipeline_group_models.add(group2)

      @pipeline_history_service.should_receive(:getActivePipelineInstance).with(@user, "first").and_return(pipeline_group_models)

      get :content, :pipeline_name => "first", :no_layout => true

      expect(assigns[:pipeline]).to eq(pipeline)
    end

    it "should respond with not found if the pipeline name is not found" do
      controller.stub(:go_config_service).and_return(@go_config_service=double())
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new("pipeline1")).and_return(false)

      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
      @pipeline_history_service.stub(:getActivePipelineInstance).with(@user, "pipeline1").and_return(:foo)

      get :content, :pipeline_name => "pipeline1", :no_layout => true

      expect(response.code).to eq("404")
      expect(response.body).to eq("Pipeline 'pipeline1' not found.\n")
      expect(assigns[:pipeline]).to be_nil
    end

    it "should respond with error if the pipeline name is nil" do
      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
      @pipeline_history_service.stub(:getActivePipelineInstance).with(@user, "pipeline1").and_return(:foo)

      get :content, :no_layout => true

      expect(response.code).to eq("400")
      expect(response.body).to eq("Request does not have parameter 'pipeline_name' set. Please specify the name of the pipeline.\n")
      expect(assigns[:pipeline]).to be_nil
    end

    it "should respond with error if the pipeline name is empty" do
      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
      @pipeline_history_service.stub(:getActivePipelineInstance).with(@user, "pipeline1").and_return(:foo)

      get :content, :pipeline_name => "", :no_layout => true

      expect(response.code).to eq("400")
      expect(response.body).to eq("Parameter 'pipeline_name' is empty. Please specify the name of the pipeline.\n")
      expect(assigns[:pipeline]).to be_nil
      assert_template layout: false
    end

    it "should respond with unauthorised if the user does not view permission on the pipeline" do
      controller.stub(:go_config_service).and_return(@go_config_service=double())
      @go_config_service.should_receive(:hasPipelineNamed).with(CaseInsensitiveString.new("pipeline1")).and_return(true)

      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
      @pipeline_history_service.stub(:getActivePipelineInstance).with(@user, "pipeline1").and_return(:foo)

      controller.stub(:security_service).and_return(@security_service=double())
      @security_service.should_receive(:hasViewPermissionForPipeline).with(@user, "pipeline1").and_return(false)

      get :content, :pipeline_name => "pipeline1", :no_layout => true

      expect(response.code).to eq("403")
      expect(response.body).to eq("User 'user' does not have view permission on pipeline 'pipeline1'\n")
      expect(assigns[:pipeline]).to be_nil
      assert_template layout: false
    end
  end
end
