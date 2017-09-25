# -*- encoding: utf-8 -*-
# stub: treetop 1.4.15 ruby lib

Gem::Specification.new do |s|
  s.name = "treetop".freeze
  s.version = "1.4.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Nathan Sobo".freeze, "Clifford Heath".freeze]
  s.autorequire = "treetop".freeze
  s.date = "2013-08-17"
  s.email = "cliffordheath@gmail.com".freeze
  s.executables = ["tt".freeze]
  s.extra_rdoc_files = ["LICENSE".freeze, "README.md".freeze]
  s.files = ["LICENSE".freeze, "README.md".freeze, "bin/tt".freeze]
  s.homepage = "https://github.com/cjheath/treetop".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "A Ruby-based text parsing and interpretation DSL".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<polyglot>.freeze, [">= 0"])
      s.add_development_dependency(%q<jeweler>.freeze, [">= 0"])
      s.add_development_dependency(%q<activesupport>.freeze, [">= 0"])
      s.add_development_dependency(%q<i18n>.freeze, ["~> 0.5.0"])
      s.add_development_dependency(%q<rr>.freeze, ["~> 1.0"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 2.0.0"])
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<polyglot>.freeze, [">= 0.3.1"])
    else
      s.add_dependency(%q<polyglot>.freeze, [">= 0"])
      s.add_dependency(%q<jeweler>.freeze, [">= 0"])
      s.add_dependency(%q<activesupport>.freeze, [">= 0"])
      s.add_dependency(%q<i18n>.freeze, ["~> 0.5.0"])
      s.add_dependency(%q<rr>.freeze, ["~> 1.0"])
      s.add_dependency(%q<rspec>.freeze, [">= 2.0.0"])
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<polyglot>.freeze, [">= 0.3.1"])
    end
  else
    s.add_dependency(%q<polyglot>.freeze, [">= 0"])
    s.add_dependency(%q<jeweler>.freeze, [">= 0"])
    s.add_dependency(%q<activesupport>.freeze, [">= 0"])
    s.add_dependency(%q<i18n>.freeze, ["~> 0.5.0"])
    s.add_dependency(%q<rr>.freeze, ["~> 1.0"])
    s.add_dependency(%q<rspec>.freeze, [">= 2.0.0"])
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<polyglot>.freeze, [">= 0.3.1"])
  end
end
