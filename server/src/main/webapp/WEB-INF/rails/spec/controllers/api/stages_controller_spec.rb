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

describe Api::StagesController do

  describe "index" do
    before :each do
      allow(controller).to receive(:stage_service).and_return(@stage_service = double('stage_service'))
      # allow(controller).to receive(:set_locale)
      allow(controller).to receive(:populate_config_validity)
    end

    it "should return a 404 HTTP response when id is not a number" do
      get 'index', params:{:id => "does-not-exist", :format => "xml", :no_layout => true}
      expect(response.status).to eq(404)
    end

    it "should return a 404 HTTP response when stage cannot be loaded" do
      expect(@stage_service).to receive(:stageById).with(99).and_throw(Exception.new("foo"))
      get 'index', params:{:id => "99", :format => "xml", :no_layout => true}
      expect(response.status).to eq(404)
    end

    it "should load stage data" do
      updated_date = java.util.Date.new
      stage = StageMother.create_passed_stage("pipeline_name", 100, "blah-stage", 12, "dev", updated_date)
      expect(@stage_service).to receive(:stageById).with(99).and_return(stage)
      get 'index', params:{:id => "99", :format => "xml", :no_layout => true}

      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil)
      expect(assigns[:doc].asXML()).to eq(StageXmlViewModel.new(stage).toXml(context).asXML())
    end
  end

  describe "cancel_stage_using_pipeline_stage_name" do
    before :each do
      @schedule_service = double('schedule_service')
      allow(controller).to receive(:schedule_service).and_return(@schedule_service)
    end

    it "should cancel stage" do
      user = Username.new(CaseInsensitiveString.new("sriki"))
      allow(@controller).to receive(:current_user).and_return(user)
      expect(@schedule_service).to receive(:cancelAndTriggerRelevantStages).with("blah_pipeline", "blah_stage", user, an_instance_of(HttpLocalizedOperationResult))
      post :cancel_stage_using_pipeline_stage_name, params:{:pipeline_name => "blah_pipeline", :stage_name => "blah_stage", :no_layout => true}
    end

    describe "route" do
      describe "with_header" do
        before :each do
          allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
        end
        it "should resolve route to cancel_stage_using_pipeline_stage_name" do
          expect(:post => '/api/stages/blah_pipeline/blah_stage/cancel').to route_to(:controller => "api/stages",
                                                                                     :action => "cancel_stage_using_pipeline_stage_name",
                                                                                     :no_layout => true, :pipeline_name => "blah_pipeline", :stage_name => "blah_stage")
        end
        describe "with_pipeline_name_constraints" do
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

        describe "with_stage_name_constraints" do
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

      describe "without_header" do
        it "should not resolve route to cancel_stage_using_pipeline_stage_name when constraint is not met" do
          allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
          expect(:post => '/api/stages/blah_pipeline/blah_stage/cancel').to_not route_to(:controller => "api/stages",
                                                                                         :action => "cancel_stage_using_pipeline_stage_name",
                                                                                         :no_layout => true, :pipeline_name => "blah_pipeline", :stage_name => "blah_stage")
        end
      end
    end
  end

end
