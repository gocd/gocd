require 'test_helper'

class FilePermissionsTest < MiniTest::Test

  FILENAME = File.join(File.dirname(__FILE__), "umask.zip")

  def teardown
    ::File.unlink(FILENAME)
  end

  if ::Zip::RUNNING_ON_WINDOWS
    # Windows tests

    DEFAULT_PERMS = 0644

    def test_windows_perms
      create_file

      assert_equal DEFAULT_PERMS, ::File.stat(FILENAME).mode
    end

  else
    # Unix tests

    DEFAULT_PERMS = 0100666

    def test_current_umask
      umask = DEFAULT_PERMS - ::File.umask
      create_file

      assert_equal umask, ::File.stat(FILENAME).mode
    end

    def test_umask_000
      set_umask(0000) do
        create_file
      end

      assert_equal DEFAULT_PERMS, ::File.stat(FILENAME).mode
    end

    def test_umask_066
      umask = 0066
      set_umask(umask) do
        create_file
      end

      assert_equal((DEFAULT_PERMS - umask), ::File.stat(FILENAME).mode)
    end

  end

  def create_file
    ::Zip::File.open(FILENAME, ::Zip::File::CREATE) do |zip|
      zip.comment = "test"
    end
  end

  # If anything goes wrong, make sure the umask is restored.
  def set_umask(umask, &block)
    begin
      saved_umask = ::File.umask(umask)
      yield
    ensure
      ::File.umask(saved_umask)
    end
  end

end
