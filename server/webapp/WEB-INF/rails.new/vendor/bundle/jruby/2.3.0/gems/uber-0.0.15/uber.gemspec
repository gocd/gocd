# -*- encoding: utf-8 -*-
require File.expand_path('../lib/uber/uber_version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Nick Sutterer"]
  gem.email         = ["apotonick@gmail.com"]
  gem.description   = %q{A gem-authoring framework.}
  gem.summary       = %q{Gem-authoring tools like class method inheritance in modules, dynamic options and more.}
  gem.homepage      = "https://github.com/apotonick/uber"
  gem.license       = "MIT"

  gem.files         = `git ls-files`.split($\)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.name          = "uber"
  gem.require_paths = ["lib"]
  gem.version       = Uber::VERSION

  gem.add_development_dependency "rake",      ">= 0.10.1"
  gem.add_development_dependency "minitest",  ">= 5.0.0"
end
