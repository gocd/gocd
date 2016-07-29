# -*- encoding: utf-8 -*-
# stub: rspec-extra-formatters 1.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-extra-formatters"
  s.version = "1.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Diego Souza", "Flor\u{e9}al TOUMIKIAN"]
  s.date = "2012-04-24"
  s.description = "    rspec-extra-formatters Provides TAP and JUnit formatters for rspec\n"
  s.email = "dsouza+rspec-extra-formatters@bitforest.org"
  s.extra_rdoc_files = ["LICENSE", "README.rst"]
  s.files = ["LICENSE", "README.rst"]
  s.homepage = "https://github.com/dgvncsz0f/rspec_formatters"
  s.rubygems_version = "2.4.8"
  s.summary = "TAP and JUnit formatters for rspec"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rspec>, [">= 0"])
    else
      s.add_dependency(%q<rspec>, [">= 0"])
    end
  else
    s.add_dependency(%q<rspec>, [">= 0"])
  end
end
