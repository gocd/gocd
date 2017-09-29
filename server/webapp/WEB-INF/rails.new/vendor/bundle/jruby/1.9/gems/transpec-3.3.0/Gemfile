source 'https://rubygems.org'

gemspec

group :development, :test do
  rspec_version = ENV['RSPEC_VERSION'] || '2.14'

  case rspec_version
  when /^\d+\.\d+$/
    gem 'rspec', "~> #{rspec_version}.0"
  else
    %w(rspec rspec-core rspec-expectations rspec-mocks rspec-support).each do |lib|
      gem lib, git: "https://github.com/rspec/#{lib}.git", branch: rspec_version
    end
  end

  # We cannot update rake to 12.x since it breaks compatibility with RSpec 2.x.
  # https://github.com/rspec/rspec-core/pull/2197
  gem 'rake',      '~> 11.0'
  gem 'simplecov', '~> 0.7'
  gem 'rubocop',   '~> 0.39'
end

group :development do
  gem 'fuubar',        '>= 1.3', '< 3.0'
  gem 'guard-rspec',   '>= 4.2.3', '< 5.0'
  gem 'guard-rubocop', '~> 1.0'
  gem 'guard-shell',   '~> 0.5'
  gem 'ruby_gntp',     '~> 0.3'
end

group :test do
  gem 'coveralls',      '~> 0.6'
end
