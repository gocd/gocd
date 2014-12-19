# -*- encoding: utf-8 -*-
require File.expand_path('../lib/jasmine-jquery-rails/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Travis Jeffery"]
  gem.email         = ["travisjeffery@gmail.com"]
  gem.summary   = %q{jasmine-jquery via asset pipeline}
  gem.homepage      = "http://github.com/travisjeffery/jasmine-jquery-rails"
  gem.license = 'MIT'
  gem.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  gem.files         = `git ls-files`.split("\n")
  gem.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  gem.name          = "jasmine-jquery-rails"
  gem.require_paths = ["lib"]
  gem.version       = Jasmine::Jquery::Rails::VERSION
end
