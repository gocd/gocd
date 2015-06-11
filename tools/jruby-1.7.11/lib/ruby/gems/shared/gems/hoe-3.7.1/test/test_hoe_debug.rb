require 'hoe'
require File.expand_path 'lib/hoe/debug.rb' # ugh. avoid dupe warnings
require 'tmpdir'
require 'tempfile'
require 'minitest/autorun'

class TestHoeDebug < Minitest::Test

  include Hoe::Debug

  # On Rake 0.8.7 verbose_flag is true, causing two tests to fail.
  RakeFileUtils.verbose_flag = nil

  attr_accessor :generated_files

  def setup
    super

    @generated_files = []
  end

  def assert_subprocess_silent
    out, err = capture_subprocess_io do
      yield
    end

    assert_equal "", out
    assert_equal "", err
  end

  def test_check_manifest
    in_tmpdir do
      manifest

      assert_subprocess_silent do
        check_manifest
      end
    end
  end

  def test_check_manifest_generated
    in_tmpdir do
      manifest 'generated.rb'

      open 'generated.rb', 'w' do |io| io.puts 'generated = true' end

      assert_subprocess_silent do
        check_manifest
      end
    end
  end

  def test_check_manifest_missing
    skip "https://github.com/MagLev/maglev/issues/224" if maglev?

    in_tmpdir do
      manifest

      open 'missing.rb', 'w' do |io| io.puts 'missing = true' end

      e = nil

      out, err = capture_subprocess_io do
        e = assert_raises RuntimeError do
          check_manifest
        end
      end

      assert_match %r%^Command failed with status%, e.message
      assert_match %r%^\+missing.rb%, out
      assert_equal "", err
    end
  end

  def in_tmpdir
    old_LOAD_PATH = $LOAD_PATH.dup
    $LOAD_PATH.map! { |path| File.expand_path path }

    Dir.mktmpdir do |path|
      Dir.chdir path do
        yield
      end
    end
  ensure
    $LOAD_PATH.replace old_LOAD_PATH
  end

  def manifest extra = nil
    open 'Manifest.txt', 'w' do |io| # sorted
      io.puts 'History.txt'
      io.puts 'Manifest.txt'
      io.puts 'README.txt'
      io.puts extra if extra
    end

    open 'README.txt',  'w'  do |io| io.puts '= blah' end
    open 'History.txt', 'w'  do |io| io.puts '=== 1.0' end
  end

  def with_config
    yield({ 'exclude' => [] }, '~/.hoerc')
  end
end
