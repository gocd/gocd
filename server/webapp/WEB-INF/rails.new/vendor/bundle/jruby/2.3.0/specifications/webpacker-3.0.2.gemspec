# -*- encoding: utf-8 -*-
# stub: webpacker 3.0.2 ruby lib

Gem::Specification.new do |s|
  s.name = "webpacker".freeze
  s.version = "3.0.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["David Heinemeier Hansson".freeze, "Gaurav Tiwari".freeze]
  s.bindir = "exe".freeze
  s.date = "2017-10-04"
  s.email = ["david@basecamp.com".freeze, "gaurav@gauravtiwari.co.uk".freeze]
  s.executables = ["webpack".freeze, "webpack-dev-server".freeze]
  s.files = ["exe/webpack".freeze, "exe/webpack-dev-server".freeze]
  s.homepage = "https://github.com/rails/webpacker".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 2.2.0".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Use Webpack to manage app-like JavaScript modules in Rails".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>.freeze, [">= 4.2"])
      s.add_runtime_dependency(%q<railties>.freeze, [">= 4.2"])
      s.add_runtime_dependency(%q<rack-proxy>.freeze, [">= 0.6.1"])
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.12"])
    else
      s.add_dependency(%q<activesupport>.freeze, [">= 4.2"])
      s.add_dependency(%q<railties>.freeze, [">= 4.2"])
      s.add_dependency(%q<rack-proxy>.freeze, [">= 0.6.1"])
      s.add_dependency(%q<bundler>.freeze, ["~> 1.12"])
    end
  else
    s.add_dependency(%q<activesupport>.freeze, [">= 4.2"])
    s.add_dependency(%q<railties>.freeze, [">= 4.2"])
    s.add_dependency(%q<rack-proxy>.freeze, [">= 0.6.1"])
    s.add_dependency(%q<bundler>.freeze, ["~> 1.12"])
  end
end
