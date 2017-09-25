# -*- encoding: utf-8 -*-
# stub: bourbon 4.2.4 ruby lib

Gem::Specification.new do |s|
  s.name = "bourbon".freeze
  s.version = "4.2.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Andres Mejia".freeze, "Chad Mazzola".freeze, "Chris Lloyd".freeze, "Gabe Berke-Williams".freeze, "J. Edward Dewyea".freeze, "Jeremy Raines".freeze, "Kyle Fiedler".freeze, "Matt Jankowski".freeze, "Mike Burns".freeze, "Nick Quaranto".freeze, "Phil LaPier".freeze, "Reda Lemeden".freeze, "Travis Haynes".freeze, "Tyson Gach".freeze, "Will McMahan".freeze]
  s.date = "2015-08-21"
  s.description = "Bourbon is a library of pure Sass mixins that are designed to be simple and easy\nto use. No configuration required. The mixins aim to be as vanilla as possible,\nmeaning they should be as close to the original CSS syntax as possible.\n".freeze
  s.email = "design+bourbon@thoughtbot.com".freeze
  s.executables = ["bourbon".freeze]
  s.files = ["bin/bourbon".freeze]
  s.homepage = "http://bourbon.io".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "A simple and lightweight mixin library for Sass".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sass>.freeze, ["~> 3.4"])
      s.add_runtime_dependency(%q<thor>.freeze, ["~> 0.19"])
      s.add_development_dependency(%q<aruba>.freeze, ["~> 0.6.2"])
      s.add_development_dependency(%q<css_parser>.freeze, ["~> 1.3"])
      s.add_development_dependency(%q<rake>.freeze, ["~> 10.4"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 3.3"])
      s.add_development_dependency(%q<scss_lint>.freeze, ["~> 0.40"])
    else
      s.add_dependency(%q<sass>.freeze, ["~> 3.4"])
      s.add_dependency(%q<thor>.freeze, ["~> 0.19"])
      s.add_dependency(%q<aruba>.freeze, ["~> 0.6.2"])
      s.add_dependency(%q<css_parser>.freeze, ["~> 1.3"])
      s.add_dependency(%q<rake>.freeze, ["~> 10.4"])
      s.add_dependency(%q<rspec>.freeze, ["~> 3.3"])
      s.add_dependency(%q<scss_lint>.freeze, ["~> 0.40"])
    end
  else
    s.add_dependency(%q<sass>.freeze, ["~> 3.4"])
    s.add_dependency(%q<thor>.freeze, ["~> 0.19"])
    s.add_dependency(%q<aruba>.freeze, ["~> 0.6.2"])
    s.add_dependency(%q<css_parser>.freeze, ["~> 1.3"])
    s.add_dependency(%q<rake>.freeze, ["~> 10.4"])
    s.add_dependency(%q<rspec>.freeze, ["~> 3.3"])
    s.add_dependency(%q<scss_lint>.freeze, ["~> 0.40"])
  end
end
