# -*- encoding: utf-8 -*-
# stub: minitar 0.5.4 ruby lib

Gem::Specification.new do |s|
  s.name = "minitar"
  s.version = "0.5.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Austin Ziegler, Mauricio Fernandez, Antoine Toulme"]
  s.autorequire = "archive/tar/minitar"
  s.date = "2012-11-14"
  s.description = "Archive::Tar::Minitar is a pure-Ruby library and command-line utility that provides the ability to deal with POSIX tar(1) archive files. The implementation is based heavily on Mauricio Ferna'ndez's implementation in rpa-base, but has been reorganised to promote reuse in other projects. Antoine Toulme forked the original project on rubyforge to place it on github, under http://www.github.com/atoulme/minitar"
  s.email = "antoine@lunar-ocean.com"
  s.executables = ["minitar"]
  s.extra_rdoc_files = ["README", "ChangeLog", "Install"]
  s.files = ["ChangeLog", "Install", "README", "bin/minitar"]
  s.homepage = "http://www.github.com/atoulme/minitar"
  s.rdoc_options = ["--title", "Archive::Tar::MiniTar -- A POSIX tarchive library", "--main", "README", "--line-numbers"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.2")
  s.rubygems_version = "2.4.8"
  s.summary = "Provides POSIX tarchive management from Ruby programs."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version
end
