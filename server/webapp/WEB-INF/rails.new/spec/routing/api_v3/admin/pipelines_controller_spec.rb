##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

describe ApiV3::Admin::PipelinesController do
  include ApiHeaderSetupForRouting

  describe "action" do
    describe "show" do
      describe "with_header" do
        before(:each) do
          setup_header
        end

        it 'should route to show action of pipelines controller for alphanumeric pipeline name' do
          expect(:get => 'api/admin/pipelines/foo123').to route_to(action: 'show', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo123')
        end

        it 'should route to show action of pipelines controller for pipeline name with dots' do
          expect(:get => 'api/admin/pipelines/foo.123').to route_to(action: 'show', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo.123')
        end

        it 'should route to show action of pipelines controller for pipeline name with hyphen' do
          expect(:get => 'api/admin/pipelines/foo-123').to route_to(action: 'show', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo-123')
        end

        it 'should route to show action of pipelines controller for pipeline name with underscore' do
          expect(:get => 'api/admin/pipelines/foo_123').to route_to(action: 'show', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo_123')
        end

        it 'should route to show action of pipelines controller for capitalized pipeline name' do
          expect(:get => 'api/admin/pipelines/FOO').to route_to(action: 'show', controller: 'api_v3/admin/pipelines', pipeline_name: 'FOO')
        end
      end
      describe "without_header" do

        it 'should not route to show action of pipelines controller without header' do
          expect(:get => 'api/admin/pipelines/foo').to_not route_to(action: 'show', controller: 'api_v3/admin/pipelines')
          expect(:get => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
        end
      end
    end

    describe "update" do
      describe "with_header" do
        before(:each) do
          setup_header
        end

        it 'should route to update action of pipelines controller for alphanumeric pipeline name' do
          expect(:put => 'api/admin/pipelines/foo123').to route_to(action: 'update', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo123')
        end

        it 'should route to update action of pipelines controller for pipeline name with dots' do
          expect(:put => 'api/admin/pipelines/foo.123').to route_to(action: 'update', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo.123')
        end

        it 'should route to update action of pipelines controller for pipeline name with hyphen' do
          expect(:put => 'api/admin/pipelines/foo-123').to route_to(action: 'update', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo-123')
        end

        it 'should route to update action of pipelines controller for pipeline name with underscore' do
          expect(:put => 'api/admin/pipelines/foo_123').to route_to(action: 'update', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo_123')
        end

        it 'should route to update action of pipelines controller for capitalized pipeline name' do
          expect(:put => 'api/admin/pipelines/FOO').to route_to(action: 'update', controller: 'api_v3/admin/pipelines', pipeline_name: 'FOO')
        end
      end
      describe "without_header" do


        it 'should not route to update action of pipelines controller without header' do
          expect(:put => 'api/admin/pipelines/foo').to_not route_to(action: 'update', controller: 'api_v3/admin/pipelines')
          expect(:put => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
        end
      end
    end

    describe "create" do
      describe "with_header" do
        before(:each) do
          setup_header
        end

        it 'should route to create action of pipelines controller' do
          expect(:post => 'api/admin/pipelines/').to route_to(action: 'create', controller: 'api_v3/admin/pipelines')
        end
      end
      describe "without_header" do


        it 'should not route to create action of pipelines controller without header' do
          expect(:post => 'api/admin/pipelines').to_not route_to(action: 'create', controller: 'api_v3/admin/pipelines')
          expect(:post => 'api/admin/pipelines').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines')
        end
      end
    end

    describe "destroy" do
      describe "with_header" do
        before(:each) do
          setup_header
        end

        it 'should route to destroy action of pipelines controller for alphanumeric pipeline name' do
          expect(:delete => 'api/admin/pipelines/foo123').to route_to(action: 'destroy', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo123')
        end

        it 'should route to destroy action of pipelines controller for pipeline name with dots' do
          expect(:delete => 'api/admin/pipelines/foo.123').to route_to(action: 'destroy', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo.123')
        end

        it 'should route to destroy action of pipelines controller for pipeline name with hyphen' do
          expect(:delete => 'api/admin/pipelines/foo-123').to route_to(action: 'destroy', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo-123')
        end

        it 'should route to destroy action of pipelines controller for pipeline name with underscore' do
          expect(:delete => 'api/admin/pipelines/foo_123').to route_to(action: 'destroy', controller: 'api_v3/admin/pipelines', pipeline_name: 'foo_123')
        end

        it 'should route to destroy action of pipelines controller for capitalized pipeline name' do
          expect(:delete => 'api/admin/pipelines/FOO').to route_to(action: 'destroy', controller: 'api_v3/admin/pipelines', pipeline_name: 'FOO')
        end
      end

      describe "without_header" do

        it 'should not route to destroy action of pipelines controller without header' do
          expect(:delete => 'api/admin/pipelines/foo').to_not route_to(action: 'destroy', controller: 'api_v3/admin/pipelines')
          expect(:delete => 'api/admin/pipelines/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/pipelines/foo')
        end
      end
    end
  end
end
