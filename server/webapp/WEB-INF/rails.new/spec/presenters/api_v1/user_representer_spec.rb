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

describe ApiV1::UserRepresenter do

  it 'renders a user' do
    presenter   = ApiV1::UserRepresenter.new(User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/users/jdoe')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/users/:login_name')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#users')

    actual_json.delete(:_links)
    expect(actual_json).to eq({
                                login_name:      'jdoe',
                                checkin_aliases: ['jdoe', 'jdoe@example.com'],
                                display_name:    'Jon Doe',
                                email:           'jdoe@example.com',
                                enabled:         true,
                                email_me:        true
                              })
  end

end
