# -*- encoding: utf-8 -*-
# stub: sass-rails 5.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "sass-rails"
  s.version = "5.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["wycats", "chriseppstein"]
  s.date = "2015-09-03"
  s.description = "Sass adapter for the Rails asset pipeline."
  s.email = ["wycats@gmail.com", "chris@eppsteins.net"]
  s.homepage = "https://github.com/rails/sass-rails"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Sass adapter for the Rails asset pipeline."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
      s.add_runtime_dependency(%q<sass>, ["~> 3.1"])
      s.add_runtime_dependency(%q<sprockets-rails>, ["< 4.0", ">= 2.0"])
      s.add_runtime_dependency(%q<sprockets>, ["< 4.0", ">= 2.8"])
      s.add_runtime_dependency(%q<tilt>, ["< 3", ">= 1.1"])
      s.add_development_dependency(%q<sqlite3>, [">= 0"])
    else
      s.add_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
      s.add_dependency(%q<sass>, ["~> 3.1"])
      s.add_dependency(%q<sprockets-rails>, ["< 4.0", ">= 2.0"])
      s.add_dependency(%q<sprockets>, ["< 4.0", ">= 2.8"])
      s.add_dependency(%q<tilt>, ["< 3", ">= 1.1"])
      s.add_dependency(%q<sqlite3>, [">= 0"])
    end
  else
    s.add_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
    s.add_dependency(%q<sass>, ["~> 3.1"])
    s.add_dependency(%q<sprockets-rails>, ["< 4.0", ">= 2.0"])
    s.add_dependency(%q<sprockets>, ["< 4.0", ">= 2.8"])
    s.add_dependency(%q<tilt>, ["< 3", ">= 1.1"])
    s.add_dependency(%q<sqlite3>, [">= 0"])
  end
end
