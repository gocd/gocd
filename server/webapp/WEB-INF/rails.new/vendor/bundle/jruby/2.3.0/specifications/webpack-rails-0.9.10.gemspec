# -*- encoding: utf-8 -*-
# stub: webpack-rails 0.9.10 ruby lib

Gem::Specification.new do |s|
  s.name = "webpack-rails".freeze
  s.version = "0.9.10"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Michael Pearson".freeze]
  s.date = "2017-01-22"
  s.description = "Production-tested, JavaScript-first tooling to use webpack within your Rails application".freeze
  s.email = ["mipearson@gmail.com".freeze]
  s.homepage = "http://github.com/mipearson/webpack-rails".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 2.0.0".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Webpack & Rails integration made easier".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3.2.0"])
      s.add_development_dependency(%q<rails>.freeze, [">= 3.2.0"])
    else
      s.add_dependency(%q<railties>.freeze, [">= 3.2.0"])
      s.add_dependency(%q<rails>.freeze, [">= 3.2.0"])
    end
  else
    s.add_dependency(%q<railties>.freeze, [">= 3.2.0"])
    s.add_dependency(%q<rails>.freeze, [">= 3.2.0"])
  end
end
