RSpec.configure do |config|
# Use color not only in STDOUT but also in pagers and files
  config.tty = true
  config.color = true
  config.default_formatter = "doc"

  config.add_formatter :documentation
  config.add_formatter RspecJunitFormatter, File.join(ENV['REPORTS_DIR'] || Rails.root.join('tmp/reports'), 'spec_full_report.xml')
  config.include ApiSpecHelper
  config.include MiscSpecExtensions
  config.include CacheTestHelpers

# clear flash messages for every spec
  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
    setup_base_urls
  end

  config.after(:each) do
    ServiceCacheStrategy.instance.clear_services
  end

  config.example_status_persistence_file_path = Rails.root.join('tmp/rspec_failures.txt')
end

include JavaSpecImports
include JavaImports
