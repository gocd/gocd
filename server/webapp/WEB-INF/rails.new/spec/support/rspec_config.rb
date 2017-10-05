RSpec.configure do |config|
# Use color not only in STDOUT but also in pagers and files
  config.tty = true
  config.color = true
  config.default_formatter = "doc"

  config.add_formatter :documentation
  # config.formatter :documentation
  config.include ApiSpecHelper
  config.include MiscSpecExtensions

# clear flash messages for every spec
  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
    setup_base_urls
  end

  config.after(:each) do
    ServiceCacheStrategy.instance.clear_services
  end
end

include JavaSpecImports
include JavaImports
