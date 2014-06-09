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

require File.join(File.dirname(__FILE__), "..", "spec_helper")

describe ComparisonController do
  include StageModelMother

  describe "routes" do

    it "should generate & resolve list" do
      route_for(:controller => "comparison", :action => "list", :pipeline_name => 'foo.bar', :other_pipeline_counter => "10", :format=> "json").should == "/compare/foo.bar/list/compare_with/10"
      params_from(:get, "/compare/foo.bar/list/compare_with/9").should == {:controller => "comparison", :pipeline_name=>"foo.bar", :action => "list", :other_pipeline_counter => "9", :format => "json"}
    end

    it "should generate the route for show action" do
      route_for(:controller => "comparison", :action => "show", :pipeline_name => 'foo.bar', :from_counter => "10", :to_counter => "9").should == "/compare/foo.bar/10/with/9"
    end

    it "should resolve route to show action" do
      params_from(:get, "/compare/foo.bar/10/with/9").should == {:controller => "comparison", :pipeline_name=>"foo.bar", :action => "show", :from_counter => "10", :to_counter => "9"}
    end

    it "should generate the route for page action" do
      route_for(:controller => "comparison", :action => "page", :pipeline_name => 'foo.bar', :page => "1").should == "/compare/foo.bar/page/1"
    end

    it "should resolve route to page action" do
      params_from(:get, "/compare/foo.bar/page/1").should == {:controller => "comparison", :action => "page", :pipeline_name=>"foo.bar", :page => "1"}
    end

    it "should generate the route for browse pipeline timeline" do
      route_for(:controller => "comparison", :action => "timeline", :pipeline_name => "foo.bar", :page => "5").should == "/compare/foo.bar/timeline/5"
    end

    it "should resolve route to browse pipeline timeline" do
      params_from(:get, "/compare/foo.bar/timeline/5").should == {:controller => "comparison", :action => "timeline", :pipeline_name => "foo.bar", :page => "5"}
    end

  end

  describe "comparison_controller" do
    before :each do
      controller.stub!(:current_user).and_return(@loser = Username.new(CaseInsensitiveString.new("loser")))
      controller.stub(:pipeline_history_service).and_return(@phs = mock('PipelineHistoryService'))
    end

    describe "show" do
      before :each do
        controller.should_receive(:mingle_config_service).and_return(@mingle_service = mock('MingleConfigService'))
        @mingle_service.stub(:mingleConfigForPipelineNamed)
        @result = HttpOperationResult.new
        HttpOperationResult.stub(:new).and_return(@result)
      end

      it "should load from and to pipeline instances and cruise_config" do
        to_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 17, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        @phs.should_receive(:findPipelineInstance).with("some_pipeline", 17, @loser, @result).and_return(to_pipeline)

        from_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        @phs.should_receive(:findPipelineInstance).with("some_pipeline", 10, @loser, @result).and_return(from_pipeline)

        pipeline_instances = PipelineInstanceModels.createPipelineInstanceModels()
        (1..10).each do |i|
          pipeline_instances << PipelineInstanceModel.createPipeline("some_pipeline", i, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        end

        get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => "17"

        assigns[:from_pipeline].should == from_pipeline
        assigns[:to_pipeline].should == to_pipeline
        assigns[:cruise_config].should == controller.go_config_service.getCurrentConfig()
      end
    end

    describe "show edge cases" do
      it "should bump up pipeline counter to 1 when counter is 0" do
        get :show, :pipeline_name => "some_pipeline", :from_counter => "0", :to_counter => "42"
        response.should redirect_to(compare_pipelines_path(:pipeline_name => "some_pipeline", :from_counter => "1", :to_counter => "42"))

        get :show, :pipeline_name => "some_pipeline", :from_counter => "42", :to_counter => "0"
        response.should redirect_to(compare_pipelines_path(:pipeline_name => "some_pipeline", :from_counter => "42", :to_counter => "1"))
      end
    end

    describe "list" do
      it "should get list of pipeline instances" do
        result = HttpLocalizedOperationResult.new
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        @phs.should_receive(:findMatchingPipelineInstances).with("some_pipeline", "query", 10, @loser, result).and_return(:from_pipeline)
        get :list, :pipeline_name => "some_pipeline", :other_pipeline_counter => 10, :q => "query", :format => "json"
        assigns[:pipeline_instances].should == :from_pipeline
      end
    end

    describe "timeline" do
      it "should get pipeline history" do
        @phs.should_receive(:findPipelineInstancesByPageNumber).with("pipeline_up", 5, 10, "loser").and_return(:some_pipeline_instances)
        get :timeline, :pipeline_name => "pipeline_up", :page => "5"
        assigns[:pipeline_instances].should == :some_pipeline_instances
      end
    end
  end

end