# coding: utf-8

lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'transpec/version'

Gem::Specification.new do |spec|
  spec.name          = 'transpec'
  spec.version       = Transpec::Version.to_s
  spec.authors       = ['Yuji Nakayama']
  spec.email         = ['nkymyj@gmail.com']
  spec.summary       = 'The RSpec syntax converter'
  spec.description   = 'Transpec converts your specs to the latest RSpec syntax ' \
                       'with static and dynamic code analysis.'
  spec.homepage      = 'http://yujinakayama.me/transpec/'
  spec.license       = 'MIT'

  spec.files         = `git ls-files -z`.split("\x0").reject do |path|
    path.match(%r{^(test|spec|features)/})
  end
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.require_paths = ['lib']

  spec.required_ruby_version = '>= 1.9.3'

  spec.add_runtime_dependency 'parser',        '>= 2.3.0.7'
  spec.add_runtime_dependency 'astrolabe',     '~> 1.2'
  spec.add_runtime_dependency 'bundler',       '~> 1.3'
  spec.add_runtime_dependency 'rainbow',       '>= 1.99.1', '< 3.0'
  spec.add_runtime_dependency 'json',          '>= 1.8', '< 3.0'
  spec.add_runtime_dependency 'activesupport', '>= 3.0', '< 6.0'
end
