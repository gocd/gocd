source 'https://rubygems.org'

gem 'yard', '~> 0.8.6'
gem 'minitest', '~> 5.0'

gem 'rake'

group :engines do
  gem 'asciidoctor', '>= 0.1.0'
  gem 'builder'
  gem 'coffee-script'
  gem 'contest'
  gem 'creole'
  gem 'erubis'
  gem 'haml', '>= 2.2.11', '< 4'
  gem 'kramdown'
  gem 'less'
  gem 'liquid'
  gem 'markaby'
  gem 'maruku'
  gem 'nokogiri' if RUBY_VERSION > '1.9.2'
  gem 'radius'
  gem 'sass'
  gem 'rdoc', (ENV['RDOC_VERSION'] || '> 0')

  platform :ruby do
    gem 'wikicloth'
    gem 'yajl-ruby'
    gem 'redcarpet' if RUBY_VERSION > '1.8.7'
    gem 'rdiscount', '>= 2.1.6' if RUBY_VERSION != '1.9.2'
    gem 'RedCloth'
  end

  platform :mri do
    gem 'therubyracer'
    gem 'bluecloth' if ENV['BLUECLOTH']
  end
end

## WHY do I have to do this?!?
platform :rbx do
  gem 'rubysl'
end

