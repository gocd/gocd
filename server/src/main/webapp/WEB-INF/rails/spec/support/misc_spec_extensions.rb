#
# Copyright 2020 ThoughtWorks, Inc.
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
  def java_date_utc(year, month, day, hour, minute, second)
    org.joda.time.DateTime.new(year, month, day, hour, minute, second, 0, org.joda.time.DateTimeZone::UTC).toDate()
  end

  def current_user
    @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
    allow(@controller).to receive(:current_user).and_return(@user)
    @user
  end

  def setup_base_urls
    config_service = Spring.bean("goConfigService")
    if (config_service.currentCruiseConfig().server().getSiteUrl().getUrl().nil?)
      config_service.updateConfig(Class.new do
        def update config
          server = config.server().siteUrls()
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "siteUrl", com.thoughtworks.go.domain.SiteUrl.new("http://test.host"))
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "secureSiteUrl", com.thoughtworks.go.domain.SecureSiteUrl.new("https://ssl.host:443"))
          return config
        end
      end.new)
    end
  end

  def cdata_wraped_regexp_for(value)
    /<!\[CDATA\[#{value}\]\]>/
  end

  def fake_template_presence file_path, content
    controller.prepend_view_path(ActionView::FixtureResolver.new(file_path => content))
  end

  def stub_service(service_getter, thing=controller)
    service = double(service_getter.to_s.camelize)
    allow(thing).to receive(service_getter).and_return(service)
    ServiceCacheStrategy.instance.replace_service(service_getter.to_s, service)
    service
  end

  def stub_localized_result
    result = HttpLocalizedOperationResult.new
    allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
    result
  end

  def uuid_pattern
    hex = "[a-f0-9]"
    "#{hex}{8}-#{hex}{4}-#{hex}{4}-#{hex}{4}-#{hex}{12}"
  end

  def with_caching(perform_caching)
    old_perform_caching = ActionController::Base.perform_caching
    begin
      ActionController::Base.perform_caching = perform_caching
      yield
    ensure
      ActionController::Base.perform_caching = old_perform_caching
    end
  end

end