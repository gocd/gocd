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

describe ApiV1::Elastic::ProfilesController do

  include ApiV1::ApiVersionHelper
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of controller' do
        expect(:get => 'api/elastic/profiles').to route_to(action: 'index', controller: 'api_v1/elastic/profiles')
      end
    end
    describe "without_header" do
      it 'should not route to index action of controller without header' do
        expect(:get => 'api/elastic/profiles').to_not route_to(action: 'index', controller: 'api_v1/elastic/profiles')
        expect(:get => 'api/elastic/profiles').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of controller for alphanumeric identifier' do
        expect(:get => 'api/elastic/profiles/foo123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
      end

      it 'should route to show action of controller for identifier with dots' do
        expect(:get => 'api/elastic/profiles/foo.123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
      end

      it 'should route to show action of controller for identifier with hyphen' do
        expect(:get => 'api/elastic/profiles/foo-123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
      end

      it 'should route to show action of controller for identifier with underscore' do
        expect(:get => 'api/elastic/profiles/foo_123').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
      end

      it 'should route to show action of controller for capitalized identifier' do
        expect(:get => 'api/elastic/profiles/FOO').to route_to(action: 'show', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to show action of controller without header' do
        expect(:get => 'api/elastic/profiles/foo').to_not route_to(action: 'show', controller: 'api_v1/elastic/profiles')
        expect(:get => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of controller' do
        expect(:post => 'api/elastic/profiles').to route_to(action: 'create', controller: 'api_v1/elastic/profiles')
      end
    end
    describe "without_header" do
      it 'should not route to create action of controller without header' do
        expect(:post => 'api/elastic/profiles').to_not route_to(action: 'create', controller: 'api_v1/elastic/profiles')
        expect(:post => 'api/elastic/profiles').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to update action of controller for alphanumeric identifier' do
        expect(:put => 'api/elastic/profiles/foo123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
      end

      it 'should route to update action of controller for identifier with dots' do
        expect(:put => 'api/elastic/profiles/foo.123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
      end

      it 'should route to update action of controller for identifier with hyphen' do
        expect(:put => 'api/elastic/profiles/foo-123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
      end

      it 'should route to update action of controller for identifier with underscore' do
        expect(:put => 'api/elastic/profiles/foo_123').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
      end

      it 'should route to update action of controller for capitalized identifier' do
        expect(:put => 'api/elastic/profiles/FOO').to route_to(action: 'update', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to update action of controller without header' do
        expect(:put => 'api/elastic/profiles/foo').to_not route_to(action: 'update', controller: 'api_v1/elastic/profiles')
        expect(:put => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destroy action of controller for alphanumeric identifier' do
        expect(:delete => 'api/elastic/profiles/foo123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo123')
      end

      it 'should route to destroy action of controller for identifier with dots' do
        expect(:delete => 'api/elastic/profiles/foo.123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo.123')
      end

      it 'should route to destroy action of controller for identifier with hyphen' do
        expect(:delete => 'api/elastic/profiles/foo-123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo-123')
      end

      it 'should route to destroy action of controller for identifier with underscore' do
        expect(:delete => 'api/elastic/profiles/foo_123').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'foo_123')
      end

      it 'should route to destroy action of controller for capitalized identifier' do
        expect(:delete => 'api/elastic/profiles/FOO').to route_to(action: 'destroy', controller: 'api_v1/elastic/profiles', profile_id: 'FOO')
      end
    end

    describe "without_header" do
      it 'should not route to destroy action of controller without header' do
        expect(:delete => 'api/elastic/profiles/foo').to_not route_to(action: 'destroy', controller: 'api_v1/elastic/profiles')
        expect(:delete => 'api/elastic/profiles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/elastic/profiles/foo')
      end
    end
  end

end
