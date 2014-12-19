begin
  require 'bundler/setup'
rescue LoadError
  puts 'You must `gem install bundler` and `bundle install` to run rake tasks'
end

require 'rdoc/task'

RDoc::Task.new(:rdoc) do |rdoc|
  rdoc.rdoc_dir = 'rdoc'
  rdoc.title    = 'Oauth2Provider'
  rdoc.options << '--line-numbers'
  rdoc.rdoc_files.include('README.rdoc')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

Bundler::GemHelper.install_tasks
APP_RAKEFILE = File.expand_path("../spec/dummy/Rakefile", __FILE__)
load 'rails/tasks/engine.rake'

require 'rspec/core'
require 'rspec/core/rake_task'
desc "Run all specs in spec directory (excluding plugin specs)"
RSpec::Core::RakeTask.new
task :default => :spec

namespace :db do
  task :nuke => [:drop, :create, :migrate]
  
  task 'test:prepare'
end
