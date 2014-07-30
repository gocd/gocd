source "https://rubygems.org"

gemspec

platforms :rbx do
  gem 'racc'
  gem 'rubysl', '~> 2.0'
  gem 'psych'
end

if RUBY_VERSION >= '1.9'
  gem 'rubocop', '~> 0.24', :group => [:development]
else
  gem 'execjs', '~> 2.0.2'
end

