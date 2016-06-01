module MiscSpecExtensions
  def java_date_utc(year, month, day, hour, minute, second)
    org.joda.time.DateTime.new(year, month, day, hour, minute, second, 0, org.joda.time.DateTimeZone::UTC).toDate()
  end

  def current_user
    @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
    @controller.stub(:current_user).and_return(@user)
    @user
  end

  def setup_base_urls
    config_service = Spring.bean("goConfigService")
    if (config_service.currentCruiseConfig().server().getSiteUrl().getUrl().nil?)
      config_service.updateConfig(Class.new do
        def update config
          server = config.server()
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "siteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("http://test.host"))
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "secureSiteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("https://ssl.host:443"))
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

  def stub_service(service_getter)
    service = double(service_getter.to_s.camelize)
    controller.stub(service_getter).and_return(service)
    ServiceCacheStrategy.instance.replace_service(service_getter.to_s, service)
    service
  end

  def stub_localized_result
    result = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.new
    com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.stub(:new).and_return(result)
    result
  end
end
