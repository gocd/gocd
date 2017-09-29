source 'https://rubygems.org'

# Specify your gem's dependencies in rainbow.gemspec
gemspec

gem 'rake'
gem 'coveralls', require: false
gem 'mime-types', '< 2.0.0', platforms: [:ruby_18]
gem 'rspec'

group :development do
  gem 'mutant-rspec'
end

group :guard do
  gem 'guard'
  gem 'guard-rspec'
end

platform :rbx do
  gem 'json'
  gem 'rubysl'
end
