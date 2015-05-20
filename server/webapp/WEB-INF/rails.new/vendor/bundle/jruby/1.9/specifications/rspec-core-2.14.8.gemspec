# -*- encoding: utf-8 -*-
# stub: rspec-core 2.14.8 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-core"
  s.version = "2.14.8"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Steven Baker", "David Chelimsky", "Chad Humphries"]
  s.bindir = "exe"
  s.date = "2014-02-28"
  s.description = "BDD for Ruby. RSpec runner and example groups."
  s.email = "rspec-users@rubyforge.org"
  s.executables = ["autospec", "rspec"]
  s.files = ["exe/autospec", "exe/rspec"]
  s.homepage = "http://github.com/rspec/rspec-core"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--charset=UTF-8"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "rspec"
  s.rubygems_version = "2.1.9"
  s.summary = "rspec-core-2.14.8"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, ["~> 10.0.0"])
      s.add_development_dependency(%q<cucumber>, ["~> 1.1.9"])
      s.add_development_dependency(%q<aruba>, ["~> 0.5"])
      s.add_development_dependency(%q<ZenTest>, ["~> 4.6"])
      s.add_development_dependency(%q<nokogiri>, ["= 1.5.2"])
      s.add_development_dependency(%q<syntax>, ["= 1.0.0"])
      s.add_development_dependency(%q<mocha>, ["~> 0.13.0"])
      s.add_development_dependency(%q<rr>, ["~> 1.0.4"])
      s.add_development_dependency(%q<flexmock>, ["~> 0.9.0"])
    else
      s.add_dependency(%q<rake>, ["~> 10.0.0"])
      s.add_dependency(%q<cucumber>, ["~> 1.1.9"])
      s.add_dependency(%q<aruba>, ["~> 0.5"])
      s.add_dependency(%q<ZenTest>, ["~> 4.6"])
      s.add_dependency(%q<nokogiri>, ["= 1.5.2"])
      s.add_dependency(%q<syntax>, ["= 1.0.0"])
      s.add_dependency(%q<mocha>, ["~> 0.13.0"])
      s.add_dependency(%q<rr>, ["~> 1.0.4"])
      s.add_dependency(%q<flexmock>, ["~> 0.9.0"])
    end
  else
    s.add_dependency(%q<rake>, ["~> 10.0.0"])
    s.add_dependency(%q<cucumber>, ["~> 1.1.9"])
    s.add_dependency(%q<aruba>, ["~> 0.5"])
    s.add_dependency(%q<ZenTest>, ["~> 4.6"])
    s.add_dependency(%q<nokogiri>, ["= 1.5.2"])
    s.add_dependency(%q<syntax>, ["= 1.0.0"])
    s.add_dependency(%q<mocha>, ["~> 0.13.0"])
    s.add_dependency(%q<rr>, ["~> 1.0.4"])
    s.add_dependency(%q<flexmock>, ["~> 0.9.0"])
  end
end
