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

require 'spec_helper'

describe ApiV2::BulkDeletionFailureResultRepresenter do

  it 'renders a bulk delete result' do
    non_existent_users = java.util.ArrayList.new
    non_existent_users.add('jane')
    non_existent_users.add('joe')

    enabled_users = java.util.ArrayList.new
    enabled_users.add('john')

    presenter   = ApiV2::BulkDeletionFailureResultRepresenter.new(BulkDeletionFailureResult.new(non_existent_users, enabled_users))

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq({
                                non_existent_users: ['jane','joe'],
                                enabled_users: ['john']
                              })
  end

  it 'renders a bulk delete result skipping fields that are empty' do
    non_existent_users = java.util.ArrayList.new
    non_existent_users.add('jane')
    non_existent_users.add('joe')

    enabled_users = java.util.ArrayList.new

    presenter   = ApiV2::BulkDeletionFailureResultRepresenter.new(BulkDeletionFailureResult.new(non_existent_users, enabled_users))

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq({
                                non_existent_users: ['jane','joe'],
                              })
  end

end
