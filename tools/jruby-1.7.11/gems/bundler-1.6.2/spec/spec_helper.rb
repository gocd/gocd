$:.unshift File.expand_path('..', __FILE__)
$:.unshift File.expand_path('../../lib', __FILE__)
require 'rspec'
require 'bundler/psyched_yaml'
require 'fileutils'
require 'rubygems'
require 'bundler'
require 'uri'
require 'digest/sha1'

# Require the correct version of popen for the current platform
if RbConfig::CONFIG['host_os'] =~ /mingw|mswin/
  begin
    require 'win32/open3'
  rescue LoadError
    abort "Run `gem install win32-open3` to be able to run specs"
  end
else
  require 'open3'
end

Dir["#{File.expand_path('../support', __FILE__)}/*.rb"].each do |file|
  require file unless file =~ /fakeweb\/.*\.rb/
end

$debug    = false
$show_err = true

Spec::Rubygems.setup
FileUtils.rm_rf(Spec::Path.gem_repo1)
ENV['RUBYOPT'] = "#{ENV['RUBYOPT']} -r#{Spec::Path.root}/spec/support/hax.rb"
ENV['BUNDLE_SPEC_RUN'] = "true"

# Don't wrap output in tests
ENV['THOR_COLUMNS'] = '10000'

RSpec.configure do |config|
  config.include Spec::Builders
  config.include Spec::Helpers
  config.include Spec::Indexes
  config.include Spec::Matchers
  config.include Spec::Path
  config.include Spec::Rubygems
  config.include Spec::Platforms
  config.include Spec::Sudo
  config.include Spec::Permissions

  if ENV['BUNDLER_SUDO_TESTS'] && Spec::Sudo.present?
    config.filter_run :sudo => true
  else
    config.filter_run_excluding :sudo => true
  end

  if ENV['BUNDLER_REALWORLD_TESTS']
    config.filter_run :realworld => true
  else
    config.filter_run_excluding :realworld => true
  end

  if RUBY_VERSION >= "1.9"
    config.filter_run_excluding :ruby => "1.8"
  else
    config.filter_run_excluding :ruby => "1.9"
  end

  if RUBY_VERSION >= "2.0"
    config.filter_run_excluding :ruby => "1.8"
    config.filter_run_excluding :ruby => "1.9"
  else
    config.filter_run_excluding :ruby => "2.0"
    config.filter_run_excluding :ruby => "2.1"
  end

  if Gem::VERSION < "2.2"
    config.filter_run_excluding :rubygems => "2.2"
  end

  config.filter_run :focused => true unless ENV['CI']
  config.run_all_when_everything_filtered = true
  config.alias_example_to :fit, :focused => true

  original_wd       = Dir.pwd
  original_path     = ENV['PATH']
  original_gem_home = ENV['GEM_HOME']

  def pending_jruby_shebang_fix
    pending "JRuby executables do not have a proper shebang" if RUBY_PLATFORM == "java"
  end

  config.expect_with :rspec do |c|
    c.syntax = :expect
  end

  config.before :all do
    build_repo1
  end

  config.before :each do
    reset!
    system_gems []
    in_app_root
  end

  config.after :each do |example|
    puts @out if example.exception

    Dir.chdir(original_wd)
    # Reset ENV
    ENV['PATH']           = original_path
    ENV['GEM_HOME']       = original_gem_home
    ENV['GEM_PATH']       = original_gem_home
    ENV['BUNDLE_PATH']    = nil
    ENV['BUNDLE_GEMFILE'] = nil
    ENV['BUNDLER_TEST']   = nil
    ENV['BUNDLE_FROZEN']  = nil
    ENV['BUNDLER_SPEC_PLATFORM'] = nil
    ENV['BUNDLER_SPEC_VERSION']  = nil
    ENV['BUNDLE_APP_CONFIG']     = nil
  end
end
