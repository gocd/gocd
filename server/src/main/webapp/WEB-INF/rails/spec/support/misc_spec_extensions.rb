#
# Copyright 2024 Thoughtworks, Inc.
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

module MiscSpecExtensions
  def current_user
    @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
    allow(@controller).to receive(:current_user).and_return(@user)
    @user
  end

  def stub_service(service_getter, thing=controller)
    service = double(service_getter.to_s.camelize)
    allow(thing).to receive(service_getter).and_return(service)
    ServiceCacheStrategy.instance.replace_service(service_getter.to_s, service)
    service
  end
end