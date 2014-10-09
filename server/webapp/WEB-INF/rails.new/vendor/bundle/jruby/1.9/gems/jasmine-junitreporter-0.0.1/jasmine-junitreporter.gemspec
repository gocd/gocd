# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'jasmine/junitreporter/version'

Gem::Specification.new do |spec|
  spec.name          = "jasmine-junitreporter"
  spec.version       = Jasmine::JUnitReporter::VERSION
  spec.authors       = ["Jake Goulding"]
  spec.email         = ["jake.goulding@gmail.com"]
  spec.summary       = %q{Provides a JUnit reporter suitable for use with jasmine-rails}
  spec.homepage      = "http://github.com/shepmaster/jasmine-junitreporter-gem"
  spec.license       = "MIT"

  spec.files         = `git ls-files -z`.split("\x0")
  spec.require_paths = ["lib"]

  spec.add_development_dependency "bundler", "~> 1.5"
  spec.add_development_dependency "rake"
end
