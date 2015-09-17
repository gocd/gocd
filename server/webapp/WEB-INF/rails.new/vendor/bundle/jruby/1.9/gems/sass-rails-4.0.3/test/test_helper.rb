# Configure Rails Envinronment
ENV["RAILS_ENV"] = "test"

require 'bundler/setup'
require 'rails'
require 'rails/test_help'
require 'sass/rails'

Rails.backtrace_cleaner.remove_silencers!

# If developing against local dependencies, this code will ensure they get picked up
# in the project fixtures that have their own bundle environment
$gem_options = {}
possible_dev_dependencies = %w(sass-rails sass rails actionpack railties sprockets journey sprockets-rails activerecord-deprecated_finders)
Bundler.load.specs.each do |s|
  if possible_dev_dependencies.include?(s.name)
     gem_path = s.full_gem_path
     gem_options = { version: s.version}
     gem_options[:path] = gem_path if File.exists?("#{gem_path}/#{s.name}.gemspec")
     $gem_options[s.name] = gem_options
  end
end

# Load support files
Dir["#{File.dirname(__FILE__)}/support/**/*.rb"].each { |f| require f }
