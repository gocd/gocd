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

describe Api::Internal::PluggableTaskController do

  describe :route do
    describe :with_header do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).and_return(true)
      end

      it 'should route to validate action of pluggable_task controller' do
        expect(post: 'api/config/internal/pluggable_task/id').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id')
      end

      describe :with_plugin_id_constraints do
        it 'should route to validate action of pluggable task controller with alphanumeric plugin id' do
          expect(post: 'api/config/internal/pluggable_task/id13').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id13')
        end
        it 'should route to validate action of pluggable task controller having dot in plugin id' do
          expect(post: 'api/config/internal/pluggable_task/id.13').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id.13')
        end
        it 'should route to validate action of pluggable task controller having hyphen in plugin id' do
          expect(post: 'api/config/internal/pluggable_task/id-13').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id-13')
        end
        it 'should route to validate action of pluggable task controller having underscore in plugin id' do
          expect(post: 'api/config/internal/pluggable_task/id_13').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id_13')
        end
        it 'should route to validate action of pluggable task controller having capitalized plugin id' do
          expect(post: 'api/config/internal/pluggable_task/ID').to route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'ID')
        end
        it 'should not route to validate action of pluggable task controller having invalid plugin id' do
          expect(post: 'api/config/internal/pluggable_task/fo$%#@6').to_not be_routable
        end
      end
    end
    describe :without_header do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).and_return(false)
      end

      it 'should not route to validate action' do
        expect(post: 'api/config/internal/pluggable_task/id').to_not route_to(controller: 'api/internal/pluggable_task', action: 'validate', plugin_id: 'id')
        expect(post: 'api/config/internal/pluggable_task/id').to route_to(controller: 'application', action: 'unresolved', url: 'api/config/internal/pluggable_task/id')
      end
    end
  end

end
