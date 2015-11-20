# coding: utf-8
lib = File.expand_path('../lib/', __FILE__)
$:.unshift lib unless $:.include?(lib)
require 'bundler/version'

Gem::Specification.new do |s|
  s.name        = 'bundler'
  s.version     = Bundler::VERSION
  s.licenses    = ['MIT']
  s.authors     = ["André Arko", "Terence Lee", "Carl Lerche", "Yehuda Katz"]
  s.email       = ["andre.arko+terence.lee@gmail.com"]
  s.homepage    = "http://bundler.io"
  s.summary     = %q{The best way to manage your application's dependencies}
  s.description = %q{Bundler manages an application's dependencies through its entire life, across many machines, systematically and repeatably}

  s.required_ruby_version     = '>= 1.8.7'
  s.required_rubygems_version = '>= 1.3.6'

  s.add_development_dependency 'mustache',  '0.99.6'
  s.add_development_dependency 'rake',      '~> 10.0'
  s.add_development_dependency 'rdiscount', '~> 1.6'
  s.add_development_dependency 'ronn',      '~> 0.7.3'
  s.add_development_dependency 'rspec',     '~> 3.0'

  s.files       = `git ls-files -z`.split("\x0").reject { |f| f.match(%r{^(test|spec|features)/}) }
  # we don't check in man pages, but we need to ship them because
  # we use them to generate the long-form help for each command.
  s.files      += Dir.glob('lib/bundler/man/**/*')

  s.executables   = %w(bundle bundler)
  s.require_paths = ["lib"]
end
