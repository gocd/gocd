source 'https://rubygems.org'

gemspec

gem 'rake'

require 'rbconfig'
gem 'wdm', '>= 0.1.0' if RbConfig::CONFIG['target_os'] =~ /mswin|mingw/i

group :development do
  gem 'guard-rspec'
  gem 'yard'
  gem 'redcarpet'
  gem 'pimpmychangelog'
end

group :test do
  gem 'rspec'
  gem 'coveralls', :require => false
end
