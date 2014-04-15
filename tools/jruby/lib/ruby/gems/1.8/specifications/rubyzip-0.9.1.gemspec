# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{rubyzip}
  s.version = "0.9.1"

  s.required_rubygems_version = nil if s.respond_to? :required_rubygems_version=
  s.authors = ["Thomas Sondergaard"]
  s.cert_chain = nil
  s.date = %q{2006-07-29}
  s.email = %q{thomas(at)sondergaard.cc}
  s.files = ["README", "NEWS", "TODO", "ChangeLog", "install.rb", "Rakefile", "samples/example.rb", "samples/example_filesystem.rb", "samples/gtkRubyzip.rb", "samples/qtzip.rb", "samples/write_simple.rb", "samples/zipfind.rb", "test/alltests.rb", "test/gentestfiles.rb", "test/ioextrastest.rb", "test/stdrubyexttest.rb", "test/zipfilesystemtest.rb", "test/ziprequiretest.rb", "test/ziptest.rb", "test/data/file1.txt", "test/data/file1.txt.deflatedData", "test/data/file2.txt", "test/data/notzippedruby.rb", "test/data/rubycode.zip", "test/data/rubycode2.zip", "test/data/testDirectory.bin", "test/data/zipWithDirs.zip", "lib/download_quizzes.rb", "lib/zip/ioextras.rb", "lib/zip/stdrubyext.rb", "lib/zip/tempfile_bugfixed.rb", "lib/zip/zip.rb", "lib/zip/zipfilesystem.rb", "lib/zip/ziprequire.rb", "lib/quiz1/t/solutions/Bill Guindon/solitaire.rb", "lib/quiz1/t/solutions/Carlos/solitaire.rb", "lib/quiz1/t/solutions/Dennis Ranke/solitaire.rb", "lib/quiz1/t/solutions/Florian Gross/solitaire.rb", "lib/quiz1/t/solutions/Glen M. Lewis/solitaire.rb", "lib/quiz1/t/solutions/James Edward Gray II/solitaire.rb", "lib/quiz1/t/solutions/Jamis Buck/bin/main.rb", "lib/quiz1/t/solutions/Jamis Buck/lib/cipher.rb", "lib/quiz1/t/solutions/Jamis Buck/lib/cli.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tc_deck.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tc_key-stream.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tc_keying-algorithms.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tc_solitaire-cipher.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tc_unkeyed-algorithm.rb", "lib/quiz1/t/solutions/Jamis Buck/test/tests.rb", "lib/quiz1/t/solutions/Jim Menard/solitaire_cypher.rb", "lib/quiz1/t/solutions/Jim Menard/test.rb", "lib/quiz1/t/solutions/Moses Hohman/cipher.rb", "lib/quiz1/t/solutions/Moses Hohman/deck.rb", "lib/quiz1/t/solutions/Moses Hohman/solitaire.rb", "lib/quiz1/t/solutions/Moses Hohman/test_cipher.rb", "lib/quiz1/t/solutions/Moses Hohman/test_deck.rb", "lib/quiz1/t/solutions/Moses Hohman/test_util.rb", "lib/quiz1/t/solutions/Moses Hohman/testsuite.rb", "lib/quiz1/t/solutions/Moses Hohman/util.rb", "lib/quiz1/t/solutions/Niklas Frykholm/solitaire.rb", "lib/quiz1/t/solutions/Thomas Leitner/solitaire.rb"]
  s.homepage = %q{http://rubyzip.sourceforge.net/}
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new("> 0.0.0")
  s.rubygems_version = %q{1.3.3}
  s.summary = %q{rubyzip is a ruby module for reading and writing zip files}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 1

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
