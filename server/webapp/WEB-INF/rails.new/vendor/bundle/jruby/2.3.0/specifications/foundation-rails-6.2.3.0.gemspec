# -*- encoding: utf-8 -*-
# stub: foundation-rails 6.2.3.0 ruby lib

Gem::Specification.new do |s|
  s.name = "foundation-rails".freeze
  s.version = "6.2.3.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["ZURB".freeze]
  s.date = "2016-05-31"
  s.description = "ZURB Foundation on Sass/Compass".freeze
  s.email = ["foundation@zurb.com".freeze]
  s.homepage = "http://foundation.zurb.com".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "ZURB Foundation on Sass/Compass".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sass>.freeze, ["< 3.5", ">= 3.3.0"])
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3.1.0"])
      s.add_runtime_dependency(%q<sprockets-es6>.freeze, [">= 0.9.0"])
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.3"])
      s.add_development_dependency(%q<capybara>.freeze, [">= 0"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<rails>.freeze, [">= 0"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 3.2"])
    else
      s.add_dependency(%q<sass>.freeze, ["< 3.5", ">= 3.3.0"])
      s.add_dependency(%q<railties>.freeze, [">= 3.1.0"])
      s.add_dependency(%q<sprockets-es6>.freeze, [">= 0.9.0"])
      s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
      s.add_dependency(%q<capybara>.freeze, [">= 0"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<rails>.freeze, [">= 0"])
      s.add_dependency(%q<rspec>.freeze, ["~> 3.2"])
    end
  else
    s.add_dependency(%q<sass>.freeze, ["< 3.5", ">= 3.3.0"])
    s.add_dependency(%q<railties>.freeze, [">= 3.1.0"])
    s.add_dependency(%q<sprockets-es6>.freeze, [">= 0.9.0"])
    s.add_dependency(%q<bundler>.freeze, ["~> 1.3"])
    s.add_dependency(%q<capybara>.freeze, [">= 0"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<rails>.freeze, [">= 0"])
    s.add_dependency(%q<rspec>.freeze, ["~> 3.2"])
  end
end
