module FoundationRailsTestHelpers
  def create_dummy_app
    FileUtils.cd(tmp_path) do
      %x(rails new dummy --skip-active-record --skip-test-unit --skip-spring)
    end
  end

  def remove_dummy_app
    FileUtils.rm_rf(dummy_app_path)
  end

  def install_foundation
    FileUtils.cd(dummy_app_path) do
      %x(rails g foundation:install -f 2>&1)
    end
  end

  def dummy_app_path
    File.join(tmp_path, 'dummy')
  end

  def tmp_path
    @tmp_path ||= File.join(File.dirname(__FILE__), '..')
  end
end
