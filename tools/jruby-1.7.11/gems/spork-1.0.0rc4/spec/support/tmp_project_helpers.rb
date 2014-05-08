module TmpProjectHelpers
  def clear_tmp_dir
    FileUtils.rm_rf(SPEC_TMP_DIR) if File.directory?(SPEC_TMP_DIR)
  end

  def create_file(filename, contents)
    FileUtils.mkdir_p(SPEC_TMP_DIR) unless File.directory?(SPEC_TMP_DIR)

    in_current_dir do
      FileUtils.mkdir_p(File.dirname(filename))
      File.open(filename, 'wb') { |f| f << contents }
    end
  end

  def create_helper_file(test_framework = FakeFramework)
    create_file(test_framework.helper_file, "# stub spec helper file")
  end

  def in_current_dir(&block)
    Dir.chdir(current_dir, &block)
  end

  def current_dir
    @current_dir ||= SPEC_TMP_DIR
  end

  def change_current_dir(sub_path)
    @current_dir = File.expand_path(sub_path, SPEC_TMP_DIR)
  end
end
