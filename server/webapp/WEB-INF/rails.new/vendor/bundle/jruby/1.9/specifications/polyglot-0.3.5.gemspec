# -*- encoding: utf-8 -*-
# stub: polyglot 0.3.5 ruby lib

Gem::Specification.new do |s|
  s.name = "polyglot"
  s.version = "0.3.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Clifford Heath"]
  s.date = "2014-05-30"
  s.description = "\nThe Polyglot library allows a Ruby module to register a loader\nfor the file type associated with a filename extension, and it\naugments 'require' to find and load matching files."
  s.email = ["clifford.heath@gmail.com"]
  s.extra_rdoc_files = ["README.txt"]
  s.files = ["README.txt"]
  s.homepage = "http://github.com/cjheath/polyglot"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Augment 'require' to load non-Ruby file types"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version
end
