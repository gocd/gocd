# -*- encoding: utf-8 -*-
# stub: jasmine-rails 0.14.7 ruby lib

Gem::Specification.new do |s|
  s.name = "jasmine-rails".freeze
  s.version = "0.14.7"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Justin Searls".freeze, "Mark Van Holstyn".freeze, "Cory Flanigan".freeze]
  s.date = "2017-10-27"
  s.description = "Provides a Jasmine Spec Runner that plays nicely with Rails 3.2 assets and sets up jasmine-headless-webkit".freeze
  s.email = ["searls@gmail.com".freeze, "mvanholstyn@gmail.com".freeze, "seeflanigan@gmail.com".freeze]
  s.homepage = "http://github.com/searls/jasmine-rails".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Makes Jasmine easier on Rails 3.2 & up".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3.2.0"])
      s.add_runtime_dependency(%q<sprockets-rails>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<jasmine-core>.freeze, ["< 3.0", ">= 1.3"])
      s.add_runtime_dependency(%q<phantomjs>.freeze, [">= 1.9"])
      s.add_development_dependency(%q<github_changelog_generator>.freeze, [">= 0"])
    else
      s.add_dependency(%q<railties>.freeze, [">= 3.2.0"])
      s.add_dependency(%q<sprockets-rails>.freeze, [">= 0"])
      s.add_dependency(%q<jasmine-core>.freeze, ["< 3.0", ">= 1.3"])
      s.add_dependency(%q<phantomjs>.freeze, [">= 1.9"])
      s.add_dependency(%q<github_changelog_generator>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<railties>.freeze, [">= 3.2.0"])
    s.add_dependency(%q<sprockets-rails>.freeze, [">= 0"])
    s.add_dependency(%q<jasmine-core>.freeze, ["< 3.0", ">= 1.3"])
    s.add_dependency(%q<phantomjs>.freeze, [">= 1.9"])
    s.add_dependency(%q<github_changelog_generator>.freeze, [">= 0"])
  end
end
