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

describe ApiV1::UsersRepresenter do

  it 'renders a list of users' do
    user = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

    presenter   = ApiV1::UsersRepresenter.new([user])
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/users')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#users')

    actual_json.fetch(:_embedded).should == { :users => [ApiV1::UserRepresenter.new(user).to_hash(url_builder: UrlBuilder.new)] }
  end

end
