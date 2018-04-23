require "rake"
require 'rake/testtask'

begin
  require 'jeweler'
  Jeweler::Tasks.new do |gem|
    gem.name = "dynamic_form"
    gem.summary = %Q{DynamicForm holds a few helper methods to help you deal with your Rails3 models}
    gem.description = %Q{DynamicForm holds a few helper methods to help you deal with your Rails3 models. It includes the stripped out methods from Rails 2; error_message_on and error_messages_for. It also brings in the functionality of the custom-err-messages plugin, which provides more flexibility over your model error messages.}
    gem.email = "joel@developwithstyle.com"
    gem.homepage = "http://codaset.com/joelmoss/dynamic-form"
    gem.authors = ["Joel Moss"]
    gem.add_development_dependency "mocha", ">= 0"
    # gem is a Gem::Specification... see http://www.rubygems.org/read/chapter/20 for additional settings
  end
  Jeweler::GemcutterTasks.new
rescue LoadError
  puts "Jeweler (or a dependency) not available. Install it with: gem install jeweler"
end

desc 'Default: run unit tests.'
task :default => :test

desc 'Test the dynamic_form plugin.'
Rake::TestTask.new(:test) do |t|
  t.libs << 'test'
  t.pattern = 'test/**/*_test.rb'
end
