# This file is copied to spec/ when you run 'rails generate rspec:install'
ENV['RAILS_ENV'] ||= 'test'
require File.expand_path('../../config/environment', __FILE__)
# Prevent database truncation if the environment is production
abort("The Rails environment is running in production mode!") if Rails.env.production?
require 'spec_helper'
require 'rspec/rails'
# Add additional requires below this line. Rails is not loaded until this point!

# Requires supporting ruby files with custom matchers and macros, etc, in
# spec/support/ and its subdirectories. Files matching `spec/**/*_spec.rb` are
# run as spec files by default. This means that files in spec/support that end
# in _spec.rb will both be required and run as specs, causing the specs to be
# run twice. It is recommended that you do not name files matching this glob to
# end with _spec.rb. You can configure this pattern with the --pattern
# option on the command line or in ~/.rspec, .rspec or `.rspec-local`.
#
# The following line is provided for convenience purposes. It has the downside
# of increasing the boot-up time by auto-requiring all files in the support
# directory. Alternatively, in the individual `*_spec.rb` files, manually
# require only the support files necessary.
#
Dir[Rails.root.join('spec/support/**/*.rb')].each { |f| require f }
Dir[Rails.root.join('spec/util/*.rb')].each { |f| load f }

$rack_default_headers = Rack::MockRequest::DEFAULT_ENV.dup

RSpec.configure do |config|
  # Use color not only in STDOUT but also in pagers and files
  config.tty = true
  config.color = true

  config.default_formatter = 'documentation'
  config.add_formatter 'documentation'

  reports_dir = ENV['REPORTS_DIR'] || Rails.root.join('tmp/reports')
  FileUtils.mkdir_p(reports_dir)
  config.add_formatter 'html', File.join(reports_dir, 'rspec.html')
  config.add_formatter 'RspecJunitFormatter', File.join(reports_dir, 'spec_full_report.xml')

  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
    setup_base_urls
  end

  config.after(:each) do
    ServiceCacheStrategy.instance.clear_services
  end

  config.include ApiSpecHelper
  config.include CacheTestHelpers
  config.include MiscSpecExtensions
  config.include ExtraSpecAssertions


  # RSpec Rails can automatically mix in different behaviours to your tests
  # based on their file location, for example enabling you to call `get` and
  # `post` in specs under `spec/controllers`.
  #
  # You can disable this behaviour by removing the line below, and instead
  # explicitly tag your specs with their type, e.g.:
  #
  #     RSpec.describe UsersController, :type => :controller do
  #       # ...
  #     end
  #
  # The different available types are documented in the features, such as in
  # https://relishapp.com/rspec/rspec-rails/docs
  config.infer_spec_type_from_file_location!

  # Turns deprecation warnings into errors, in order to surface
  # the full backtrace of the call site. This can be useful when
  # you need more context to address a deprecation than the
  # single-line call site normally provided.
  config.raise_errors_for_deprecations!

  # Filter lines from Rails gems in backtraces.
  config.filter_rails_from_backtrace!
  # arbitrary gems may also be filtered via:
  # config.filter_gems_from_backtrace("gem name")
end

include JavaSpecImports
