##########################################################################
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
##########################################################################

require 'rails_helper'

describe ApiV1::Admin::Internal::MaterialTestController do

  include ApiV1::ApiVersionHelper

  describe "test" do
    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :test)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:post, :test).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:post, :test)
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:post, :test)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:post, :test)
      end
    end

    describe 'logged in' do
      before(:each) do
        login_as_group_admin
        @material_config_converter = double(MaterialConfigConverter)
        allow(MaterialConfigConverter).to receive(:new).and_return(@material_config_converter)
      end

      it 'renders OK if connection test passed' do
        @svn_material = double(com.thoughtworks.go.config.materials.svn.SvnMaterial)
        allow(@material_config_converter).to receive(:toMaterial).and_return(@svn_material)
        expect(@svn_material).to receive(:checkConnection).with(an_instance_of(CheckConnectionSubprocessExecutionContext)).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        expect_any_instance_of(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig).
          to receive(:ensureEncrypted)

        post_with_api_header :test, params: {
          type:       'svn',
          attributes: {
            url: 'https://example.com/git/FooBarWidgets.git',
            password: 'password'
          }
        }

        expect(response).to have_api_message_response(200, 'Connection OK.')
      end

      it 'validates material before testing connection' do
        post_with_api_header :test, params: {
          type:       'svn',
          attributes: {
            url: 'https://example.com/svn/FooBarWidgets.git',
            password: 'foo',
            encrypted_password: GoCipher.new.encrypt('bar')
          }
        }

        expect(response).to have_api_message_response(422, 'There was an error with the material configuration!')
      end

      it 'renders error if connection test failed' do
        @git_material = double(com.thoughtworks.go.config.materials.git.GitMaterial)
        allow(@material_config_converter).to receive(:toMaterial).and_return(@git_material)
        expect(@git_material).to receive(:checkConnection).with(an_instance_of(CheckConnectionSubprocessExecutionContext)).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.notValid('boom!'))

        post_with_api_header :test, params: {
          type:       'git',
          attributes: {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(422, 'boom!')
      end

      it 'performs parameter expansion if pipeline_name param is specified' do
        @git_material = double(com.thoughtworks.go.config.materials.git.GitMaterial)
        allow(@material_config_converter).to receive(:toMaterial).and_return(@git_material)
        expect(@git_material).to receive(:checkConnection).with(an_instance_of(CheckConnectionSubprocessExecutionContext)).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        @go_config_service = double(GoConfigService)
        allow(controller).to receive(:go_config_service).and_return(@go_config_service)
        @go_config_validity = double(GoConfigValidity)
        allow(@go_config_validity).to receive(:isValid).and_return(true)
        allow(@go_config_service).to receive(:checkConfigFileValid).and_return(@go_config_validity)
        expect(@go_config_service).to receive(:pipelineConfigNamed).with(CaseInsensitiveString.new('BuildLinux')).and_return(PipelineConfigMother.createPipelineConfigWithJobConfigs('BuildLinux'))

        @config_param_preprocessor = double(ConfigParamPreprocessor)
        allow(ConfigParamPreprocessor).to receive(:new).and_return(@config_param_preprocessor)

        expect(@config_param_preprocessor).to receive(:process).with(an_instance_of(PipelineConfig)) do |pipeline_config|
          expect(pipeline_config.name).to eq(CaseInsensitiveString.new('BuildLinux'))
        end

        post_with_api_header :test, params: {
          type:          'git',
          pipeline_name: 'BuildLinux',
          attributes:    {
            url: 'https://example.com/git/FooBarWidgets.git'
          }
        }

        expect(response).to have_api_message_response(200, 'Connection OK.')
      end

      it 'does not perform parameter expansion if pipeline_name param is blank' do
        @git_material = double(com.thoughtworks.go.config.materials.git.GitMaterial)
        allow(@material_config_converter).to receive(:toMaterial).and_return(@git_material)
        expect(@git_material).to receive(:checkConnection).with(an_instance_of(CheckConnectionSubprocessExecutionContext)).
          and_return(com.thoughtworks.go.domain.materials.ValidationBean.valid)

        post_with_api_header :test, params: {
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
