##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
##########################################################################

require 'spec_helper'

describe ApiV1::MaterialTestController do

  describe :test do
    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :test)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:post, :test).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow normal users, with security enabled' do
        login_as_user
        expect(controller).to allow_action(:post, :test)
      end
    end

    describe 'logged in' do
      before(:each) do
        login_as_user
      end

      it 'renders OK if connection test passed' do
        com.thoughtworks.go.config.materials.git.GitMaterial.
          any_instance.
          should_receive(:checkConnection).with(ApiV1::MaterialTestController.check_connection_execution_context).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        post_with_api_header :test, {
          type:       'git',
          attributes: {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(200, 'Connection OK.')
      end

      it 'renders error if connection test failed' do
        com.thoughtworks.go.config.materials.git.GitMaterial.
          any_instance.
          should_receive(:checkConnection).with(ApiV1::MaterialTestController.check_connection_execution_context).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.notValid('boom!'))

        post_with_api_header :test, {
          type:       'git',
          attributes: {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(422, 'boom!')
      end

      it 'performs parameter expansion if pipeline_name param is specified' do
        com.thoughtworks.go.config.materials.git.GitMaterial.
          any_instance.
          should_receive(:checkConnection).with(ApiV1::MaterialTestController.check_connection_execution_context).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        controller.go_config_service.should_receive(:pipelineConfigNamed).with(CaseInsensitiveString.new('BuildLinux')).and_return(PipelineConfigMother.createPipelineConfigWithJobConfigs('BuildLinux'))

        com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor.any_instance.should_receive(:process) do |pipeline_config|
          expect(pipeline_config.name).to eq(CaseInsensitiveString.new('BuildLinux'))
        end

        post_with_api_header :test, {
          type:          'git',
          pipeline_name: 'BuildLinux',
          attributes:    {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(200, 'Connection OK.')
      end

      it 'does not perform parameter expansion if pipeline_name param is blank' do
        com.thoughtworks.go.config.materials.git.GitMaterial.
          any_instance.
          should_receive(:checkConnection).with(ApiV1::MaterialTestController.check_connection_execution_context).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        post_with_api_header :test, {
          type:          'git',
          pipeline_name: '',
          attributes:    {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(200, 'Connection OK.')
      end
    end
  end
end
