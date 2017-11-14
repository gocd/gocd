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

describe ApiV3::Admin::TemplatesController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of templates controller' do
        expect(:get => 'api/admin/templates').to route_to(action: 'index', controller: 'api_v3/admin/templates')
      end
    end
    describe "without_header" do
      it 'should not route to index action of templates controller without header' do
        expect(:get => 'api/admin/templates').to_not route_to(action: 'index', controller: 'api_v3/admin/templates')
        expect(:get => 'api/admin/templates').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/templates')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of templates controller for alphanumeric template name' do
        expect(:get => 'api/admin/templates/foo123').to route_to(action: 'show', controller: 'api_v3/admin/templates', template_name: 'foo123')
      end

      it 'should route to show action of templates controller for template name with dots' do
        expect(:get => 'api/admin/templates/foo.123').to route_to(action: 'show', controller: 'api_v3/admin/templates', template_name: 'foo.123')
      end

      it 'should route to show action of templates controller for template name with hyphen' do
        expect(:get => 'api/admin/templates/foo-123').to route_to(action: 'show', controller: 'api_v3/admin/templates', template_name: 'foo-123')
      end

      it 'should route to show action of templates controller for template name with underscore' do
        expect(:get => 'api/admin/templates/foo_123').to route_to(action: 'show', controller: 'api_v3/admin/templates', template_name: 'foo_123')
      end

      it 'should route to show action of templates controller for capitalized template name' do
        expect(:get => 'api/admin/templates/FOO').to route_to(action: 'show', controller: 'api_v3/admin/templates', template_name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to show action of templates controller without header' do
        expect(:get => 'api/admin/templates/foo').to_not route_to(action: 'show', controller: 'api_v3/admin/templates')
        expect(:get => 'api/admin/templates/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/templates/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of templates controller' do
        expect(:post => 'api/admin/templates').to route_to(action: 'create', controller: 'api_v3/admin/templates')
      end
    end
    describe "without_header" do
      it 'should not route to create action of templates controller without header' do
        expect(:post => 'api/admin/templates').to_not route_to(action: 'create', controller: 'api_v3/admin/templates')
        expect(:post => 'api/admin/templates').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/templates')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to update action of templates controller for alphanumeric template name' do
        expect(:put => 'api/admin/templates/foo123').to route_to(action: 'update', controller: 'api_v3/admin/templates', template_name: 'foo123')
      end

      it 'should route to update action of templates controller for template name with dots' do
        expect(:put => 'api/admin/templates/foo.123').to route_to(action: 'update', controller: 'api_v3/admin/templates', template_name: 'foo.123')
      end

      it 'should route to update action of templates controller for template name with hyphen' do
        expect(:put => 'api/admin/templates/foo-123').to route_to(action: 'update', controller: 'api_v3/admin/templates', template_name: 'foo-123')
      end

      it 'should route to update action of templates controller for template name with underscore' do
        expect(:put => 'api/admin/templates/foo_123').to route_to(action: 'update', controller: 'api_v3/admin/templates', template_name: 'foo_123')
      end

      it 'should route to update action of templates controller for capitalized template name' do
        expect(:put => 'api/admin/templates/FOO').to route_to(action: 'update', controller: 'api_v3/admin/templates', template_name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to update action of templates controller without header' do
        expect(:put => 'api/admin/templates/foo').to_not route_to(action: 'update', controller: 'api_v3/admin/templates')
        expect(:put => 'api/admin/templates/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/templates/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destroy action of templates controller for alphanumeric template name' do
        expect(:delete => 'api/admin/templates/foo123').to route_to(action: 'destroy', controller: 'api_v3/admin/templates', template_name: 'foo123')
      end

      it 'should route to destroy action of templates controller for template name with dots' do
        expect(:delete => 'api/admin/templates/foo.123').to route_to(action: 'destroy', controller: 'api_v3/admin/templates', template_name: 'foo.123')
      end

      it 'should route to destroy action of templates controller for template name with hyphen' do
        expect(:delete => 'api/admin/templates/foo-123').to route_to(action: 'destroy', controller: 'api_v3/admin/templates', template_name: 'foo-123')
      end

      it 'should route to destroy action of templates controller for template name with underscore' do
        expect(:delete => 'api/admin/templates/foo_123').to route_to(action: 'destroy', controller: 'api_v3/admin/templates', template_name: 'foo_123')
      end

      it 'should route to destroy action of templates controller for capitalized template name' do
        expect(:delete => 'api/admin/templates/FOO').to route_to(action: 'destroy', controller: 'api_v3/admin/templates', template_name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of templates controller without header' do
        expect(:delete => 'api/admin/templates/foo').to_not route_to(action: 'destroy', controller: 'api_v3/admin/templates')
        expect(:delete => 'api/admin/templates/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/templates/foo')
      end
    end
  end
end
