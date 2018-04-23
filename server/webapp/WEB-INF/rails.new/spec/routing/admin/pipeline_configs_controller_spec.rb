##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe Admin::PipelineConfigsController do
  it 'should route to edit for alphanumeric pipeline name' do
    expect(:get => 'admin/pipelines/foo123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo123')
  end

  it 'should route to edit for pipeline name with dots' do
    expect(:get => 'admin/pipelines/foo.123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo.123')
  end

  it 'should route to edit for pipeline name with hyphen' do
    expect(:get => 'admin/pipelines/foo-123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo-123')
  end

  it 'should route to edit for pipeline name with underscore' do
    expect(:get => 'admin/pipelines/foo_123/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'foo_123')
  end

  it 'should route to edit for capitalized pipeline name' do
    expect(:get => 'admin/pipelines/FOO/edit').to route_to(action: 'edit', controller: 'admin/pipeline_configs', pipeline_name: 'FOO')
  end
end