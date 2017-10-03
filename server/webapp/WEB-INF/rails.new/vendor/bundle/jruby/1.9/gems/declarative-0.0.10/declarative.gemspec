lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'declarative/version'

Gem::Specification.new do |spec|
  spec.name          = "declarative"
  spec.version       = Declarative::VERSION
  spec.authors       = ["Nick Sutterer"]
  spec.email         = ["apotonick@gmail.com"]
  spec.summary       = %q{DSL for nested schemas.}
  spec.description   = %q{DSL for nested generic schemas with inheritance and refining.}
  spec.homepage      = "https://github.com/apotonick/declarative"
  spec.license       = "MIT"

  spec.files         = `git ls-files -z`.split("\x0")
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ["lib"]

  spec.add_development_dependency "rake", "~> 10.0"
  spec.add_development_dependency "minitest"
  spec.add_development_dependency "minitest-line"
end
