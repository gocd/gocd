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
  end

end
