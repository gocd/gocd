# -*- encoding: utf-8 -*-
# stub: sass-rails 4.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "sass-rails"
  s.version = "4.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["wycats", "chriseppstein"]
  s.date = "2013-10-15"
  s.description = "Sass adapter for the Rails asset pipeline."
  s.email = ["wycats@gmail.com", "chris@eppsteins.net"]
  s.homepage = "https://github.com/rails/sass-rails"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "sass-rails"
  s.rubygems_version = "2.1.9"
  s.summary = "Sass adapter for the Rails asset pipeline."

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sass>, [">= 3.1.10"])
      s.add_runtime_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
      s.add_runtime_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
    else
      s.add_dependency(%q<sass>, [">= 3.1.10"])
      s.add_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
      s.add_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
    end
  else
    s.add_dependency(%q<sass>, [">= 3.1.10"])
    s.add_dependency(%q<railties>, ["< 5.0", ">= 4.0.0"])
    s.add_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
  end
end
