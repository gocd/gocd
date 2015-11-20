# -*- encoding: utf-8 -*-
$:.push File.expand_path('../lib', __FILE__)
require 'listen/version'

Gem::Specification.new do |s|
  s.name        = 'listen'
  s.version     = Listen::VERSION
  s.platform    = Gem::Platform::RUBY
  s.authors     = ['Thibaud Guillaume-Gentil', 'Maher Sallam']
  s.email       = ['thibaud@thibaud.me', 'maher@sallam.me']
  s.homepage    = 'https://github.com/guard/listen'
  s.license     = 'MIT'
  s.summary     = 'Listen to file modifications'
  s.description = 'The Listen gem listens to file modifications and notifies you about the changes. Works everywhere!'

  s.required_rubygems_version = '>= 1.3.6'
  s.rubyforge_project = 'listen'

  s.add_dependency 'rb-fsevent', '>= 0.9.3'
  s.add_dependency 'rb-inotify', '>= 0.9'
  s.add_dependency 'rb-kqueue',  '>= 0.2'

  s.add_development_dependency 'bundler'
  s.add_development_dependency 'rspec'

  s.files        = Dir.glob('{lib}/**/*') + %w[CHANGELOG.md LICENSE README.md]
  s.require_path = 'lib'
end
