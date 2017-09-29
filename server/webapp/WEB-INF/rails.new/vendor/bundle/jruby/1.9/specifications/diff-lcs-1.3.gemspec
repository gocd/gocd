# -*- encoding: utf-8 -*-
# stub: diff-lcs 1.3 ruby lib

Gem::Specification.new do |s|
  s.name = "diff-lcs"
  s.version = "1.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Austin Ziegler"]
  s.date = "2017-01-18"
  s.description = "Diff::LCS computes the difference between two Enumerable sequences using the\nMcIlroy-Hunt longest common subsequence (LCS) algorithm. It includes utilities\nto create a simple HTML diff output format and a standard diff-like tool.\n\nThis is release 1.3, providing a tentative fix to a long-standing issue related\nto incorrect detection of a patch direction. Also modernizes the gem\ninfrastructure, testing infrastructure, and provides a warning-free experience\nto Ruby 2.4 users."
  s.email = ["halostatue@gmail.com"]
  s.executables = ["htmldiff", "ldiff"]
  s.extra_rdoc_files = ["Code-of-Conduct.md", "Contributing.md", "History.md", "License.md", "Manifest.txt", "README.rdoc", "docs/COPYING.txt", "docs/artistic.txt"]
  s.files = ["Code-of-Conduct.md", "Contributing.md", "History.md", "License.md", "Manifest.txt", "README.rdoc", "bin/htmldiff", "bin/ldiff", "docs/COPYING.txt", "docs/artistic.txt"]
  s.homepage = "https://github.com/halostatue/diff-lcs"
  s.licenses = ["MIT", "Artistic-2.0", "GPL-2.0+"]
  s.rdoc_options = ["--main", "README.rdoc"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8")
  s.rubygems_version = "2.4.8"
  s.summary = "Diff::LCS computes the difference between two Enumerable sequences using the McIlroy-Hunt longest common subsequence (LCS) algorithm"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<hoe-doofus>, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
      s.add_development_dependency(%q<hoe-git>, ["~> 1.6"])
      s.add_development_dependency(%q<hoe-rubygems>, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-travis>, ["~> 1.2"])
      s.add_development_dependency(%q<rspec>, ["< 4", ">= 2.0"])
      s.add_development_dependency(%q<rake>, ["< 12", ">= 10.0"])
      s.add_development_dependency(%q<rdoc>, [">= 0"])
      s.add_development_dependency(%q<hoe>, ["~> 3.16"])
    else
      s.add_dependency(%q<hoe-doofus>, ["~> 1.0"])
      s.add_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
      s.add_dependency(%q<hoe-git>, ["~> 1.6"])
      s.add_dependency(%q<hoe-rubygems>, ["~> 1.0"])
      s.add_dependency(%q<hoe-travis>, ["~> 1.2"])
      s.add_dependency(%q<rspec>, ["< 4", ">= 2.0"])
      s.add_dependency(%q<rake>, ["< 12", ">= 10.0"])
      s.add_dependency(%q<rdoc>, [">= 0"])
      s.add_dependency(%q<hoe>, ["~> 3.16"])
    end
  else
    s.add_dependency(%q<hoe-doofus>, ["~> 1.0"])
    s.add_dependency(%q<hoe-gemspec2>, ["~> 1.1"])
    s.add_dependency(%q<hoe-git>, ["~> 1.6"])
    s.add_dependency(%q<hoe-rubygems>, ["~> 1.0"])
    s.add_dependency(%q<hoe-travis>, ["~> 1.2"])
    s.add_dependency(%q<rspec>, ["< 4", ">= 2.0"])
    s.add_dependency(%q<rake>, ["< 12", ">= 10.0"])
    s.add_dependency(%q<rdoc>, [">= 0"])
    s.add_dependency(%q<hoe>, ["~> 3.16"])
  end
end
