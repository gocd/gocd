# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{archive-tar-minitar}
  s.version = "0.5.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Austin Ziegler, Mauricio Ferna'ndez"]
  s.date = %q{2008-02-26}
  s.default_executable = %q{minitar}
  s.description = %q{Archive::Tar::Minitar is a pure-Ruby library and command-line utility that provides the ability to deal with POSIX tar(1) archive files. The implementation is based heavily on Mauricio Ferna'ndez's implementation in rpa-base, but has been reorganised to promote reuse in other projects.}
  s.email = %q{minitar@halostatue.ca}
  s.executables = ["minitar"]
  s.extra_rdoc_files = ["README", "ChangeLog", "Install"]
  s.files = ["bin", "bin/minitar", "ChangeLog", "Install", "lib", "lib/archive", "lib/archive/tar", "lib/archive/tar/minitar", "lib/archive/tar/minitar/command.rb", "lib/archive/tar/minitar.rb", "Rakefile", "README", "tests", "tests/tc_tar.rb", "tests/testall.rb"]
  s.homepage = %q{http://rubyforge.org/projects/ruwiki/}
  s.rdoc_options = ["--title", "Archive::Tar::MiniTar -- A POSIX tarchive library", "--main", "README", "--line-numbers"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.2")
  s.rubyforge_project = %q{ruwiki}
  s.rubygems_version = %q{1.3.3}
  s.summary = %q{Provides POSIX tarchive management from Ruby programs.}
  s.test_files = ["tests/testall.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
