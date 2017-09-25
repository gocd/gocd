# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'foundation/rails/version'

Gem::Specification.new do |spec|
  spec.name          = "foundation-rails"
  spec.version       = Foundation::Rails::VERSION
  spec.authors       = ["ZURB"]
  spec.email         = ["foundation@zurb.com"]
  spec.description   = %q{ZURB Foundation on Sass/Compass}
  spec.summary       = %q{ZURB Foundation on Sass/Compass}
  spec.homepage      = "http://foundation.zurb.com"
  spec.license       = "MIT"

  spec.files         = `git ls-files`.split($/)
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ["lib"]

  spec.add_dependency "sass", [">= 3.3.0", "< 3.5"]
  spec.add_dependency "railties", [">= 3.1.0"]
  spec.add_dependency "sprockets-es6", [">= 0.9.0"]

  spec.add_development_dependency "bundler", "~> 1.3"
  spec.add_development_dependency "capybara"
  spec.add_development_dependency "rake"
  spec.add_development_dependency "rails"
  spec.add_development_dependency "rspec", "~> 3.2"
end
