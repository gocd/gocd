# coding: utf-8
lib = File.expand_path('../lib/', __FILE__)
$:.unshift lib unless $:.include?(lib)
require 'bundler/version'

Gem::Specification.new do |spec|
  spec.name        = 'bundler'
  spec.version     = Bundler::VERSION
  spec.licenses    = ['MIT']
  spec.authors     = ["André Arko", "Terence Lee", "Carl Lerche", "Yehuda Katz"]
  spec.email       = ["andre@arko.net"]
  spec.homepage    = "http://bundler.io"
  spec.summary     = %q{The best way to manage your application's dependencies}
  spec.description = %q{Bundler manages an application's dependencies through its entire life, across many machines, systematically and repeatably}

  spec.required_ruby_version     = '>= 1.8.7'
  spec.required_rubygems_version = '>= 1.3.6'

  spec.add_development_dependency 'ronn', '~> 0.7.3'
  spec.add_development_dependency 'rspec', '~> 2.99.0.beta1'

  spec.files       = `git ls-files -z`.split("\x0")
  spec.files      += Dir.glob('lib/bundler/man/**/*') # man/ is ignored by git
  spec.test_files  = spec.files.grep(%r{^spec/})

  spec.executables   = %w(bundle bundler)
  spec.require_paths = ["lib"]
end
