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

describe ApiV1::PipelineOperationController do
  include ApiHeaderSetupTeardown
  include ApiV2::ApiVersionHelper

  before(:each) do
    @pipeline_pause_service = double('pipeline_pause_service')
    allow(controller).to receive('pipeline_pause_service').and_return(@pipeline_pause_service)

    @pipeline_name = 'up42'
    @pause_cause = 'under construction'
    @user = Username.new(CaseInsensitiveString.new(SecureRandom.hex))
  end

  describe 'pause' do
    describe 'security' do
      it 'should allow anyone to pause a pipeline' do
        disable_security
        login_as_anonymous

        expect(controller).to allow_action(:post, :pause, {:pipeline_name => @pipeline_name, pause_cause: @pause_cause})
      end

      it 'should not allow normal users to pause a pipeline' do
        enable_security
        login_as_pipeline_group_Non_Admin_user

        expect(controller).to disallow_action(:post, :pause, {:pipeline_name => @pipeline_name, pause_cause: @pause_cause}).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admins to pause a pipeline' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :pause, {:pipeline_name => @pipeline_name, pause_cause: @pause_cause})
      end

      it 'should allow pipeline group admins to pause a pipeline' do
        enable_security
        login_as_group_admin

        expect(controller).to allow_action(:post, :pause, {:pipeline_name => @pipeline_name, pause_cause: @pause_cause})
      end
    end

    it 'should allow admins to pause a pipeline' do
      login_as_admin

      allow(@pipeline_pause_service).to receive(:pause).with(@pipeline_name, @pause_cause, @user, anything) do |pipeline_name, pause_cause, user, result|
        result.setMessage(LocalizedMessage.string('PIPELINE_PAUSE_SUCCESSFUL', pipeline_name));
      end

      post_with_api_header :pause, pipeline_name: @pipeline_name, pause_cause: @pause_cause
      expect(response).to be_ok
    end

    it 'should show errors occurred while pausing the pipeline' do
      login_as_admin

      allow(@pipeline_pause_service).to receive(:pause).with(@pipeline_name, @pause_cause, @user, anything) do |pipeline_name, pause_cause, user, result|
        result.preconditionFailed(LocalizedMessage.string('PIPELINE_ALREADY_PAUSED', pipeline_name));
      end

      post_with_api_header :pause, pipeline_name: @pipeline_name, pause_cause: @pause_cause
      expect(response).to have_api_message_response(412, 'Failed to pause pipeline \'up42\'. Pipeline \'up42\' is already paused.')
    end
  end

  describe 'unpause' do
    describe 'security' do
      it 'should allow anyone to pause a pipeline' do
        disable_security
        login_as_anonymous

        expect(controller).to allow_action(:post, :unpause, {:pipeline_name => @pipeline_name})
      end

      it 'should not allow normal users to pause a pipeline' do
        enable_security
        login_as_pipeline_group_Non_Admin_user

        expect(controller).to disallow_action(:post, :unpause, {:pipeline_name => @pipeline_name}).with(401, "You are not authorized to perform this action.")
      end

      it 'should allow admins to pause a pipeline' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :unpause, {:pipeline_name => @pipeline_name})
      end

      it 'should allow pipeline group admins to pause a pipeline' do
        enable_security
        login_as_group_admin

        expect(controller).to allow_action(:post, :unpause, {:pipeline_name => @pipeline_name})
      end
    end

    it 'should allow admins to unpause a pipeline' do
      login_as_admin

      allow(@pipeline_pause_service).to receive(:unpause).with(@pipeline_name, @user, anything) do |pipeline_name, user, result|
        result.setMessage(LocalizedMessage.string('PIPELINE_UNPAUSE_SUCCESSFUL', pipeline_name));
      end

      post_with_api_header :unpause, pipeline_name: @pipeline_name
      expect(response).to be_ok
    end

    it 'should show errors occurred while unpausing the pipeline' do
      login_as_admin

      allow(@pipeline_pause_service).to receive(:unpause).with(@pipeline_name, @user, anything) do |pipeline_name, user, result|
        result.preconditionFailed(LocalizedMessage.string('PIPELINE_ALREADY_UNPAUSED', pipeline_name));
      end

      post_with_api_header :unpause, pipeline_name: @pipeline_name, pause_cause: @pause_cause
      expect(response).to have_api_message_response(412, 'Failed to unpause pipeline \'up42\'. Pipeline \'up42\' is already unpaused.')
    end
  end
end
