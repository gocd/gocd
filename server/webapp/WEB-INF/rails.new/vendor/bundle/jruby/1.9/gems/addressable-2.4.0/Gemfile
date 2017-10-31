source 'https://rubygems.org'

gemspec

gem 'rake', '~> 10.4', '>= 10.4.2'

group :test do
  gem 'rspec', '~> 3.0'
  gem 'rspec-its', '~> 1.1'
end

group :development do
  gem 'launchy', '~> 2.4', '>= 2.4.3'
  gem 'redcarpet', :platform => :mri_19
  gem 'yard'
end

group :test, :development do
  gem 'simplecov', :require => false
  gem 'coveralls', :require => false, :platforms => [
    :ruby_19, :ruby_20, :ruby_21, :rbx, :jruby
  ]
  # Used to test compatibility.
  gem 'rack-mount', git: 'https://github.com/sporkmonger/rack-mount.git', require: 'rack/mount'
end

gem 'idn-ruby', :platform => [:mri_19, :mri_20, :mri_21, :mri_22]
