require 'rubygems'

# try to activate jruby-openssl gem, and only require from the gem if it's there
begin
  gem 'jruby-openssl'
  gem_success = true
rescue Gem::LoadError => e
  gem_success = false
end
require 'openssl.rb' if gem_success
