begin
  require 'bundler'
rescue LoadError
  puts "although not required, it's recommended that you use bundler during development"
end

require 'rspec'
require 'rspec/autorun'

$VERBOSE = true
require 'multi_json'

def jruby?
  defined?(RUBY_ENGINE) && RUBY_ENGINE == "jruby"
end
