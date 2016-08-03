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

describe ApiV1::BackupRepresenter do
  it 'renders a ServerBackup' do

    backup_time = java.util.Date.new
    backup      = com.thoughtworks.go.server.domain.ServerBackup.new("/foo/bar", backup_time, "jdoe")

    presenter   = ApiV1::BackupRepresenter.new(backup)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:doc)

    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#backups')

    actual_json.delete(:_links)
    expect(actual_json).to eq({
                                time: backup_time,
                                path: '/foo/bar',
                                user: ApiV1::UserSummaryRepresenter.new(backup.getUsername).to_hash(url_builder: UrlBuilder.new)
                              })
  end
end
