# -*- encoding: utf-8 -*-
# stub: uber 0.0.13 ruby lib

Gem::Specification.new do |s|
  s.name = "uber"
  s.version = "0.0.13"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sutterer"]
  s.date = "2015-01-14"
  s.description = "A gem-authoring framework."
  s.email = ["apotonick@gmail.com"]
  s.homepage = "https://github.com/apotonick/uber"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Gem-authoring tools like class method inheritance in modules, dynamic options and more."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0.10.1"])
      s.add_development_dependency(%q<minitest>, [">= 5.0.0"])
    else
      s.add_dependency(%q<rake>, [">= 0.10.1"])
      s.add_dependency(%q<minitest>, [">= 5.0.0"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0.10.1"])
    s.add_dependency(%q<minitest>, [">= 5.0.0"])
  end
end
