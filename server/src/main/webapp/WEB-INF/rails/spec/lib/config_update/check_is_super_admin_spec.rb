#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ConfigUpdate::CheckIsSuperAdmin do
  include ::ConfigUpdate::CheckIsSuperAdmin

  def params
    @params = {}
  end

  before do
    @security_service = double("security_service") #Instance variable because the module expects this to be defined
    @user = "loser" #Instance variable because the module expects this to be defined
  end

  it "should return unsuccessful result if user is not an admin" do
    cruise_config = GoConfigMother.configWithPipelines(["pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    expect(@security_service).to receive(:isUserAdmin).with("loser").and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to edit.")
  end

  it "should return successful if user is an admin" do
    cruise_config = GoConfigMother.configWithPipelines(["pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    expect(@security_service).to receive(:isUserAdmin).with("loser").and_return(true)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_truthy
    expect(result.httpCode()).to eq(200)
  end

end
