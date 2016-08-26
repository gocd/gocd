##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe Api::StagesController do

  describe "index" do
    before :each do
      controller.stub(:stage_service).and_return(@stage_service = double('stage_service'))
      controller.stub(:set_locale)
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
      allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect(:post => '/api/stages/424242/cancel').to route_to(:controller => "api/stages", :action => "cancel", :no_layout => true, :id => "424242")
    end

    it "should not resolve route to cancel when constraint is not met" do
      allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
      expect(:post => '/api/stages/424242/cancel').to_not route_to(:controller => "api/stages", :action => "cancel", :no_layout => true, :id => "424242")
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

    it "should cancel stage" do
      user = Username.new(CaseInsensitiveString.new("sriki"))
      @controller.stub(:current_user).and_return(user)
      @schedule_service.should_receive(:cancelAndTriggerRelevantStages).with("blah_pipeline", "blah_stage", user, an_instance_of(HttpLocalizedOperationResult))
      post :cancel_stage_using_pipeline_stage_name, {:pipeline_name => "blah_pipeline", :stage_name => "blah_stage", :no_layout => true}
    end

    describe :route do
      describe :with_header do
        before :each do
          allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
        end
        it "should resolve route to cancel_stage_using_pipeline_stage_name" do
          expect(:post => '/api/stages/blah_pipeline/blah_stage/cancel').to route_to(:controller => "api/stages",
                                                                                     :action => "cancel_stage_using_pipeline_stage_name",
                                                                                     :no_layout => true, :pipeline_name => "blah_pipeline", :stage_name => "blah_stage")
        end
        describe :with_pipeline_name_constraints do
          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having dots in pipeline name' do
            expect(:post => '/api/stages/some.thing/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'some.thing', stage_name: 'bar')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having hyphen in pipeline name' do
            expect(:post => '/api/stages/some-thing/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'some-thing', stage_name: 'bar')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having underscore in pipeline name' do
            expect(:post => '/api/stages/some_thing/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'some_thing', stage_name: 'bar')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having alphanumeric pipeline name' do
            expect(:post => '/api/stages/123foo/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: '123foo', stage_name: 'bar')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having capitalized pipeline name' do
            expect(:post => '/api/stages/FOO/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'FOO', stage_name: 'bar')
          end

          it 'should not route to cancel_stage_using_pipeline_stage_name action of stages controller for invalid pipeline name' do
            expect(:post => '/api/stages/fo$%#@6/bar/cancel').to_not be_routable
          end
        end

        describe :with_stage_name_constraints do
          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller' do
            expect(:post => '/api/stages/foo/bar/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'foo', stage_name: 'bar')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having dots in stage name' do
            expect(:post => '/api/stages/foo/some.thing/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'foo', stage_name: 'some.thing')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having hyphen in stage name' do
            expect(:post => '/api/stages/foo/some-thing/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'foo', stage_name: 'some-thing')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having underscore in stage name' do
            expect(:post => '/api/stages/foo/some_thing/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'foo', stage_name: 'some_thing')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having alphanumeric stage name' do
            expect(:post => '/api/stages/123foo/bar123/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: '123foo', stage_name: 'bar123')
          end

          it 'should route to cancel_stage_using_pipeline_stage_name action of stages controller having capitalized stage name' do
            expect(:post => '/api/stages/foo/BAR/cancel').to route_to(:no_layout => true, controller: 'api/stages', action: 'cancel_stage_using_pipeline_stage_name', pipeline_name: 'foo', stage_name: 'BAR')
          end

          it 'should not route to cancel_stage_using_pipeline_stage_name action of stages controller for invalid stage name' do
            expect(:post => '/api/stages/foo/fo$%#@6/cancel').to_not be_routable
          end
        end
      end

      describe :without_header do
        it "should not resolve route to cancel_stage_using_pipeline_stage_name when constraint is not met" do
          allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
          expect(:post => '/api/stages/blah_pipeline/blah_stage/cancel').to_not route_to(:controller => "api/stages",
                                                                                         :action => "cancel_stage_using_pipeline_stage_name",
                                                                                         :no_layout => true, :pipeline_name => "blah_pipeline", :stage_name => "blah_stage")
        end
      end
    end
  end

  describe :history do
    include APIModelMother

    before :each do
      controller.stub(:stage_service).and_return(@stage_service = double('stage_service'))
    end

    it "should render history json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @stage_service.should_receive(:getCount).and_return(10)
      @stage_service.should_receive(:findDetailedStageHistoryByOffset).with('pipeline', 'stage', anything, "loser", anything).and_return([create_stage_model])

      get :history, :pipeline_name => 'pipeline', :stage_name => 'stage', :offset => '5', :no_layout => true

      expect(response.body).to eq(StageHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), [create_stage_model]).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @stage_service.should_receive(:getCount).and_return(10)
      @stage_service.should_receive(:findDetailedStageHistoryByOffset).with('pipeline', 'stage', anything, "loser", anything) do |pipeline_name, stage_name, pagination, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :history, :pipeline_name => 'pipeline', :stage_name => 'stage', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe :route do
      it "should route to history" do
        expect(:get => "/api/stages/pipeline/stage/history").to route_to(:controller => 'api/stages', :action => "history", :pipeline_name => "pipeline", :stage_name => "stage", :offset => "0", :no_layout => true)
        expect(:get => "/api/stages/pipeline/stage/history/1").to route_to(:controller => 'api/stages', :action => "history", :pipeline_name => "pipeline", :stage_name => "stage", :offset => "1", :no_layout => true)
      end

      describe :with_pipeline_name_contraint do
        it 'should route to history action of stages controller having dots in pipeline name' do
          expect(:get => 'api/stages/some.thing/bar/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'some.thing', stage_name: 'bar', offset: '0')
        end

        it 'should route to history action of stages controller having hyphen in pipeline name' do
          expect(:get => 'api/stages/some-thing/bar/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'some-thing', stage_name: 'bar', offset: '0')
        end

        it 'should route to history action of stages controller having underscore in pipeline name' do
          expect(:get => 'api/stages/some_thing/bar/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'some_thing', stage_name: 'bar', offset: '0')
        end

        it 'should route to history action of stages controller having alphanumeric pipeline name' do
          expect(:get => 'api/stages/123foo/bar/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: '123foo', stage_name: 'bar', offset: '0')
        end

        it 'should route to history action of stages controller having capitalized pipeline name' do
          expect(:get => 'api/stages/FOO/bar/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'FOO', stage_name: 'bar', offset: '0')
        end

        it 'should not route to history action of stages controller for invalid pipeline name' do
          expect(:get => 'api/stages/fo$%#@6/bar/history').to_not be_routable
        end
      end

      describe :with_stage_name_constraint do
        it 'should route to history action of stages controller having dots in stage name' do
          expect(:get => 'api/stages/foo/some.thing/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some.thing', offset: '0')
        end

        it 'should route to history action of stages controller having hyphen in stage name' do
          expect(:get => 'api/stages/foo/some-thing/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some-thing', offset: '0')
        end

        it 'should route to history action of stages controller having underscore in stage name' do
          expect(:get => 'api/stages/foo/some_thing/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some_thing', offset: '0')
        end

        it 'should route to history action of stages controller having alphanumeric stage name' do
          expect(:get => 'api/stages/123foo/bar123/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: '123foo', stage_name: 'bar123', offset: '0')
        end

        it 'should route to history action of stages controller having capitalized stage name' do
          expect(:get => 'api/stages/foo/BAR/history').to route_to(no_layout: true, controller: 'api/stages', action: 'history', pipeline_name: 'foo', stage_name: 'BAR', offset: '0')
        end

        it 'should not route to history action of stages controller for invalid stage name' do
          expect(:get => 'api/stages/some_thing/fo$%#@6/history').to_not be_routable
        end
      end

    end
  end

  describe :instance_by_counter do
    include APIModelMother

    before :each do
      controller.stub(:stage_service).and_return(@stage_service = double('stage_service'))
    end

    it "should route to history" do
      expect(:get => "/api/stages/pipeline/stage/instance/1/1").to route_to(:controller => 'api/stages', :action => "instance_by_counter", :pipeline_name => "pipeline", :stage_name => "stage", :pipeline_counter => "1", :stage_counter => "1", :no_layout => true)
    end

    it "should render instance json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @stage_service.should_receive(:findStageWithIdentifier).with('pipeline', 1, 'stage', '1', "loser", anything).and_return(create_stage_model_for_instance)

      get :instance_by_counter, :pipeline_name => 'pipeline', :stage_name => 'stage', :pipeline_counter => '1', :stage_counter => '1', :no_layout => true

      expect(response.body).to eq(StageAPIModel.new(create_stage_model_for_instance).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @stage_service.should_receive(:findStageWithIdentifier).with('pipeline', 1, 'stage', '1', "loser", anything) do |pipeline_name, pipeline_counter, stage_name, stage_counter, username, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :instance_by_counter, :pipeline_name => 'pipeline', :stage_name => 'stage', :pipeline_counter => '1', :stage_counter => '1', :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end

    describe :route do
      describe :with_pipeline_name_contraint do
        it 'should route to instance_by_counter action of stages controller having dots in pipeline name' do
          expect(:get => 'api/stages/some.thing/bar/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'some.thing', stage_name: 'bar', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having hyphen in pipeline name' do
          expect(:get => 'api/stages/some-thing/bar/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'some-thing', stage_name: 'bar', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having underscore in pipeline name' do
          expect(:get => 'api/stages/some_thing/bar/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'some_thing', stage_name: 'bar', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having alphanumeric pipeline name' do
          expect(:get => 'api/stages/123foo/bar/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: '123foo', stage_name: 'bar', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having capitalized pipeline name' do
          expect(:get => 'api/stages/FOO/bar/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'FOO', stage_name: 'bar', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should not route to instance_by_counter action of stages controller for invalid pipeline name' do
          expect(:get => 'api/stages/fo$%#@6/bar/instance/1/2').to_not be_routable
        end
      end

      describe :with_stage_name_constraint do
        it 'should route to instance_by_counter action of stages controller having dots in stage name' do
          expect(:get => 'api/stages/foo/some.thing/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'foo', stage_name: 'some.thing', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having hyphen in stage name' do
          expect(:get => 'api/stages/foo/some-thing/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'foo', stage_name: 'some-thing', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having underscore in stage name' do
          expect(:get => 'api/stages/foo/some_thing/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'foo', stage_name: 'some_thing', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having alphanumeric stage name' do
          expect(:get => 'api/stages/123foo/bar123/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: '123foo', stage_name: 'bar123', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should route to instance_by_counter action of stages controller having capitalized stage name' do
          expect(:get => 'api/stages/foo/BAR/instance/1/2').to route_to(no_layout: true, controller: 'api/stages', action: 'instance_by_counter', pipeline_name: 'foo', stage_name: 'BAR', pipeline_counter: '1', stage_counter: '2')
        end

        it 'should not route to instance_by_counter action of stages controller for invalid stage name' do
          expect(:get => 'api/stages/some_thing/fo$%#@6/instance/1/2').to_not be_routable
        end
      end

      describe :with_pipeline_counter_constraint do
        it 'should not route to instance_by_counter action of stages controller for invalid pipeline counter' do
          expect(:get => 'api/stages/some.thing/bar/instance/fo$%#@6/2').to_not be_routable
        end
      end

      describe :with_stage_counter_constraint do
        it 'should not route to instance_by_counter action of stages controller for invalid stage counter' do
          expect(:get => 'api/stages/some.thing/bar/instance/1/fo$%#@6').to_not be_routable
        end
      end
    end
  end
end
