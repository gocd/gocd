# -*- encoding: utf-8 -*-
# stub: sprockets-rails 2.3.2 ruby lib

Gem::Specification.new do |s|
  s.name = "sprockets-rails".freeze
  s.version = "2.3.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Joshua Peek".freeze]
  s.date = "2015-06-23"
  s.email = "josh@joshpeek.com".freeze
  s.homepage = "https://github.com/rails/sprockets-rails".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Sprockets Rails integration".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sprockets>.freeze, ["< 4.0", ">= 2.8"])
      s.add_runtime_dependency(%q<actionpack>.freeze, [">= 3.0"])
      s.add_runtime_dependency(%q<activesupport>.freeze, [">= 3.0"])
      s.add_development_dependency(%q<railties>.freeze, [">= 3.0"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<sass>.freeze, [">= 0"])
      s.add_development_dependency(%q<uglifier>.freeze, [">= 0"])
    else
      s.add_dependency(%q<sprockets>.freeze, ["< 4.0", ">= 2.8"])
      s.add_dependency(%q<actionpack>.freeze, [">= 3.0"])
      s.add_dependency(%q<activesupport>.freeze, [">= 3.0"])
      s.add_dependency(%q<railties>.freeze, [">= 3.0"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<sass>.freeze, [">= 0"])
      s.add_dependency(%q<uglifier>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<sprockets>.freeze, ["< 4.0", ">= 2.8"])
    s.add_dependency(%q<actionpack>.freeze, [">= 3.0"])
    s.add_dependency(%q<activesupport>.freeze, [">= 3.0"])
    s.add_dependency(%q<railties>.freeze, [">= 3.0"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<sass>.freeze, [">= 0"])
    s.add_dependency(%q<uglifier>.freeze, [">= 0"])
  end
end
