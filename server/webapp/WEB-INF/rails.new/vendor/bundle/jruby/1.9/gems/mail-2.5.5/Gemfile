source 'https://rubygems.org'

gemspec

if RUBY_VERSION < '1.9.3'
  gem 'activesupport', '< 4'
elsif RUBY_VERSION < '2.2.2'
  gem 'activesupport', '< 5'
end
gem 'i18n', '< 0.7.0' if RUBY_VERSION < '1.9.3'
gem "tlsmail" if RUBY_VERSION <= '1.8.6'
gem "mime-types", "~> 1.16"
gem "treetop", "~> 1.4.10"

gem 'jruby-openssl', :platform => :jruby

# For gems not required to run tests
group :local_development, :test do
  gem 'rake', '> 0.8.7', '< 11.0.1'
  gem 'rdoc', '< 4.3' if RUBY_VERSION < '1.9.3'
  gem "rspec",      "~> 2.8.0"
  case
  when defined?(RUBY_ENGINE) && RUBY_ENGINE == 'rbx'
    # Skip it
  when RUBY_PLATFORM == 'java'
    # Skip it
  when RUBY_VERSION < '1.9'
    gem "ruby-debug"
  else
    # Skip it
  end
end
