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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe Api::StagesController do

  describe "index" do
    before :each do
      controller.stub(:stage_service).and_return(@stage_service = mock())
      controller.stub(:set_locale)
      controller.stub(:licensed_agent_limit)
      controller.stub(:populate_config_validity)
    end

    it "should return a 404 HTTP response when id is not a number" do
      get 'index', :id => "does-not-exist", :format => "xml", :no_layout => true
      expect(response.status).to eq(404)
    end

    it "should return a 404 HTTP response when stage cannot be loaded" do
      @stage_service.should_receive(:stageById).with(99).and_throw(Exception.new("foo"))
      get 'index', :id => "99", :format => "xml", :no_layout => true
      expect(response.status).to eq(404)
    end

    it "should load stage data" do
      updated_date = java.util.Date.new
      stage = StageMother.create_passed_stage("pipeline_name", 100, "blah-stage", 12, "dev", updated_date)
      @stage_service.should_receive(:stageById).with(99).and_return(stage)
      get 'index', :id => "99", :format => "xml", :no_layout => true

      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, nil)
      assigns[:doc].asXML().should == StageXmlViewModel.new(stage).toXml(context).asXML()
    end
  end

  describe "cancel" do
    before :each do
      @schedule_service = double('schedule_service')
      controller.stub(:schedule_service).and_return(@schedule_service)
    end

    it "should resolve route to cancel" do
      expect(:post => '/api/stages/424242/cancel').to route_to(:controller => "api/stages", :action => "cancel", :no_layout=>true, :id => "424242")
    end

    it "should cancel stage" do
      user = Username.new(CaseInsensitiveString.new("sriki"))
      @controller.stub(:current_user).and_return(user)
      @schedule_service.should_receive(:cancelAndTriggerRelevantStages).with(42, user, an_instance_of(HttpLocalizedOperationResult))
      post :cancel, {:id => "42", :no_layout => true}
    end
  end

  describe "cancel_stage_using_pipeline_stage_name" do
    before :each do
      @schedule_service = double('schedule_service')
      controller.stub(:schedule_service).and_return(@schedule_service)
    end

    it "should resolve route to cancel_stage_using_pipeline_stage_name" do
      expect(:post => '/api/stages/blah_pipeline/blah_stage/cancel').to route_to(:controller => "api/stages",
                                                                                   :action => "cancel_stage_using_pipeline_stage_name",
                                                                                   :no_layout=>true, :pipeline_name => "blah_pipeline", :stage_name => "blah_stage")
    end

    it "should cancel stage" do
      user = Username.new(CaseInsensitiveString.new("sriki"))
      @controller.stub(:current_user).and_return(user)
      @schedule_service.should_receive(:cancelAndTriggerRelevantStages).with("blah_pipeline", "blah_stage", user, an_instance_of(HttpLocalizedOperationResult))
      post :cancel_stage_using_pipeline_stage_name, {:pipeline_name => "blah_pipeline", :stage_name => "blah_stage", :no_layout => true}
    end
  end
end
