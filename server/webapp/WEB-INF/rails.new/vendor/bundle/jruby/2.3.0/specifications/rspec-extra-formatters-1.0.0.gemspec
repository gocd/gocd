# -*- encoding: utf-8 -*-
# stub: rspec-extra-formatters 1.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-extra-formatters".freeze
  s.version = "1.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Diego Souza".freeze, "Flor\u{e9}al TOUMIKIAN".freeze]
  s.date = "2012-04-24"
  s.description = "    rspec-extra-formatters Provides TAP and JUnit formatters for rspec\n".freeze
  s.email = "dsouza+rspec-extra-formatters@bitforest.org".freeze
  s.extra_rdoc_files = ["LICENSE".freeze, "README.rst".freeze]
  s.files = ["LICENSE".freeze, "README.rst".freeze]
  s.homepage = "https://github.com/dgvncsz0f/rspec_formatters".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "TAP and JUnit formatters for rspec".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rspec>.freeze, [">= 0"])
    else
      s.add_dependency(%q<rspec>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<rspec>.freeze, [">= 0"])
  end
end
