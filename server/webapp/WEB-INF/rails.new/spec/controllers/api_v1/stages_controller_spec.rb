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

describe ApiV1::StagesController do

  describe :show do
    describe :security do

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1').with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should allow normal users who have access to pipeline, with security enabled' do
        login_as_user
        allow_current_user_to_access_pipeline('pipeline')
        expect(controller).to allow_action(:get, :show, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1')
      end

      it 'should disallow normal users who do not have access to pipeline, with security enabled' do
        login_as_user
        allow_current_user_to_not_access_pipeline('pipeline')
        expect(controller).to disallow_action(:get, :show, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1')
      end

      describe 'logged in' do
        before(:each) do
          login_as_user
          allow_current_user_to_access_pipeline('pipeline')
          controller.stub(:stage_service).and_return(@stage_service = double('stage_service'))
        end

        it 'should get stage instance json' do
          @stage_model=StageMother.passedStageInstance('stage', 'job', 'pipeline')
          @stage_service.should_receive(:findStageWithIdentifier).with('pipeline', 1, 'stage', '1', @user.getUsername.to_s, anything).and_return(@stage_model)
          get_with_api_header :show, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1'
          expect(response).to be_ok
          expect(actual_response).to eq(expected_response(@stage_model, ApiV1::StageRepresenter))
        end
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        describe :with_pipeline_name_contraint do
          it 'should route to show action of stages controller having dots in pipeline name' do
            expect(:get => 'api/stages/some.thing/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'some.thing', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should route to show action of stages controller having hyphen in pipeline name' do
            expect(:get => 'api/stages/some-thing/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'some-thing', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should route to show action of stages controller having underscore in pipeline name' do
            expect(:get => 'api/stages/some_thing/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'some_thing', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should route to show action of stages controller having alphanumeric pipeline name' do
            expect(:get => 'api/stages/123foo/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: '123foo', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should route to show action of stages controller having capitalized pipeline name' do
            expect(:get => 'api/stages/FOO/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'FOO', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should not route to show action of stages controller for invalid pipeline name' do
            expect(:get => 'api/stages/fo$%#@6/1/bar/2').to_not be_routable
          end
        end

        describe :with_stage_name_constraint do
          it 'should route to show action of stages controller' do
            expect(:get => 'api/stages/foo/1/bar/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'foo', pipeline_counter: '1', stage_name: 'bar', stage_counter: '2')
          end

          it 'should route to show action of stages controller having dots in stage name' do
            expect(:get => 'api/stages/foo/1/some.thing/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'foo', pipeline_counter: '1', stage_name: 'some.thing', stage_counter: '2')
          end

          it 'should route to show action of stages controller having hyphen in stage name' do
            expect(:get => 'api/stages/foo/1/some-thing/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'foo', pipeline_counter: '1', stage_name: 'some-thing', stage_counter: '2')
          end

          it 'should route to show action of stages controller having underscore in stage name' do
            expect(:get => 'api/stages/foo/1/some_thing/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'foo', pipeline_counter: '1', stage_name: 'some_thing', stage_counter: '2')
          end

          it 'should route to show action of stages controller having alphanumeric stage name' do
            expect(:get => 'api/stages/123foo/1/bar123/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: '123foo', pipeline_counter: '1', stage_name: 'bar123', stage_counter: '2')
          end

          it 'should route to show action of stages controller having capitalized stage name' do
            expect(:get => 'api/stages/foo/1/BAR/2').to route_to(controller: 'api_v1/stages', action: 'show', pipeline_name: 'foo', pipeline_counter: '1', stage_name: 'BAR', stage_counter: '2')
          end

          it 'should not route to show action of stages controller for invalid stage name' do
            expect(:get => 'api/stages/foo/1/fo$%#@6/2').to_not be_routable
          end
        end

        describe :with_pipeline_counter_constraint do
          it 'should not route to show action of stages controller for invalid pipeline counter' do
            expect(:get => 'api/stages/some.thing/fo$%#@6/bar/2').to_not be_routable
          end
        end

        describe :with_stage_counter_constraint do
          it 'should not route to show action of stages controller for invalid stage counter' do
            expect(:get => 'api/stages/some.thing/1/bar/fo$%#@6').to_not be_routable
          end
        end
      end

      describe :without_header do
        it 'should not route to show action of stages controller without header' do
          expect(:get => 'api/stages/foo/1/bar/1').to_not route_to(action: 'show', controller: 'api_v1/stages')
          expect(:get => 'api/stages/foo/bar').to route_to(controller: 'application', action: 'unresolved', url: 'api/stages/foo/bar')
        end
      end
    end
  end

  describe :history do
    describe :security do

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :history, pipeline_name: 'pipeline', stage_name: 'stage')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :history, pipeline_name: 'pipeline', stage_name: 'stage').with(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should allow normal users who have access to pipeline, with security enabled' do
        login_as_user
        allow_current_user_to_access_pipeline('pipeline')
        expect(controller).to allow_action(:get, :history, pipeline_name: 'pipeline', stage_name: 'stage')
      end

      it 'should disallow normal users who do not have access to pipeline, with security enabled' do
        login_as_user
        allow_current_user_to_not_access_pipeline('pipeline')
        expect(controller).to disallow_action(:get, :history, pipeline_name: 'pipeline', stage_name: 'stage')
      end

      describe 'logged in' do
        before(:each) do
          login_as_user
          allow_current_user_to_access_pipeline('pipeline')
          controller.stub(:stage_service).and_return(@stage_service = double('stage_service'))
        end

        it 'should get stage instance json' do
          @stage_instance_models = [StageMother.toStageInstanceModel(StageMother.passedStageInstance('stage', 'job', 'pipeline'))]
          @stage_service.should_receive(:getCount).and_return(10)
          @stage_service.should_receive(:findDetailedStageHistoryByOffset).with('pipeline', 'stage', anything, @user.getUsername.to_s, anything).and_return(@stage_instance_models)
          get_with_api_header :history, pipeline_name: 'pipeline', stage_name: 'stage', pipeline_counter: '1', stage_counter: '1'
          expect(response).to be_ok
          expect(actual_response).to eq(expected_response_with_options(@stage_instance_models, {pipeline_name: 'pipeline', stage_name: 'stage'}, ApiV1::StageHistoryRepresenter))
        end
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        describe :with_pipeline_name_contraint do
          it 'should route to history action of stages controller having dots in pipeline name' do
            expect(:get => 'api/stages/some.thing/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'some.thing', stage_name: 'bar')
          end

          it 'should route to history action of stages controller having hyphen in pipeline name' do
            expect(:get => 'api/stages/some-thing/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'some-thing', stage_name: 'bar')
          end

          it 'should route to history action of stages controller having underscore in pipeline name' do
            expect(:get => 'api/stages/some_thing/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'some_thing', stage_name: 'bar')
          end

          it 'should route to history action of stages controller having alphanumeric pipeline name' do
            expect(:get => 'api/stages/123foo/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: '123foo', stage_name: 'bar')
          end

          it 'should route to history action of stages controller having capitalized pipeline name' do
            expect(:get => 'api/stages/FOO/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'FOO', stage_name: 'bar')
          end

          it 'should not route to history action of stages controller for invalid pipeline name' do
            expect(:get => 'api/stages/fo$%#@6/bar').to_not be_routable
          end
        end

        describe :with_stage_name_constraint do
          it 'should route to history action of stages controller' do
            expect(:get => 'api/stages/foo/bar').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'foo', stage_name: 'bar')
          end

          it 'should route to history action of stages controller having dots in stage name' do
            expect(:get => 'api/stages/foo/some.thing').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some.thing')
          end

          it 'should route to history action of stages controller having hyphen in stage name' do
            expect(:get => 'api/stages/foo/some-thing').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some-thing')
          end

          it 'should route to history action of stages controller having underscore in stage name' do
            expect(:get => 'api/stages/foo/some_thing').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'foo', stage_name: 'some_thing')
          end

          it 'should route to history action of stages controller having alphanumeric stage name' do
            expect(:get => 'api/stages/123foo/bar123').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: '123foo', stage_name: 'bar123')
          end

          it 'should route to history action of stages controller having capitalized stage name' do
            expect(:get => 'api/stages/foo/BAR').to route_to(controller: 'api_v1/stages', action: 'history', pipeline_name: 'foo', stage_name: 'BAR')
          end

          it 'should not route to history action of stages controller for invalid stage name' do
            expect(:get => 'api/stages/foo/fo$%#@6').to_not be_routable
          end
        end
      end

      describe :without_header do
        it 'should not route to history of stages controller without header' do
          expect(:get => 'api/stages/foo/bar').to_not route_to(action: 'history', controller: 'api_v1/stages')
          expect(:get => 'api/stages/foo/bar').to route_to(controller: 'application', action: 'unresolved', url: 'api/stages/foo/bar')
        end
      end
    end
  end

end
