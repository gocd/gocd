# -*- encoding: utf-8 -*-
require File.expand_path('../lib/phantomjs/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Christoph Olszowka"]
  gem.email         = ["christoph at olszowka.de"]
  gem.description   = %q{Auto-install phantomjs on demand for current platform. Comes with poltergeist integration.}
  gem.summary       = %q{Auto-install phantomjs on demand for current platform. Comes with poltergeist integration.}
  gem.homepage      = "https://github.com/colszowka/phantomjs-gem"
  gem.license       = 'MIT'

  gem.add_development_dependency 'poltergeist'
  gem.add_development_dependency 'capybara', '~> 2.0.0'
  gem.add_development_dependency 'rspec', ">= 2.11.0"
  gem.add_development_dependency 'simplecov'
  gem.add_development_dependency 'rake'

  gem.files         = `git ls-files`.split($\)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.name          = "phantomjs"
  gem.require_paths = ["lib"]
  gem.version       = Phantomjs::VERSION
end
