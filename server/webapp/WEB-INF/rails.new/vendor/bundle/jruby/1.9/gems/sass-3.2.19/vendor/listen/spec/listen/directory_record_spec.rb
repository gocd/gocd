# encoding: UTF-8
require 'spec_helper'

describe Listen::DirectoryRecord do
  let(:base_directory) { File.dirname(__FILE__) }

  subject { described_class.new(base_directory) }

  describe '.generate_default_ignoring_patterns' do
    it 'creates regexp patterns from the default ignored directories and extensions' do
      described_class.generate_default_ignoring_patterns.should include(
        %r{^(?:\.rbx|\.bundle|\.git|\.svn|log|tmp|vendor)/},
        %r{(?:\.DS_Store)$}
      )
    end

    it 'memoizes the generated results' do
      described_class.generate_default_ignoring_patterns.should equal described_class.generate_default_ignoring_patterns
    end
  end

  describe '#initialize' do
    it 'sets the base directory' do
      subject.directory.should eq base_directory
    end

    it 'sets the default ignoring patterns' do
      subject.ignoring_patterns.should =~ described_class.generate_default_ignoring_patterns
    end

    it 'sets the default filtering patterns' do
      subject.filtering_patterns.should eq []
    end

    it 'raises an error when the passed path does not exist' do
      expect { described_class.new('no way I exist') }.to raise_error(ArgumentError)
    end

    it 'raises an error when the passed path is not a directory' do
      expect { described_class.new(__FILE__) }.to raise_error(ArgumentError)
    end
  end

  describe '#ignore' do
    it 'adds the passed paths to the list of ignored paths in the record' do
      subject.ignore(%r{^\.old/}, %r{\.pid$})
      subject.ignoring_patterns.should include(%r{^\.old/}, %r{\.pid$})
    end
  end

  describe '#ignore!' do
    it 'replace the ignored paths in the record' do
      subject.ignore!(%r{^\.old/}, %r{\.pid$})
      subject.ignoring_patterns.should =~ [%r{^\.old/}, %r{\.pid$}]
    end
  end

  describe '#filter' do
    it 'adds the passed regexps to the list of filters that determine the stored paths' do
      subject.filter(%r{\.(?:jpe?g|gif|png)}, %r{\.(?:mp3|ogg|a3c)})
      subject.filtering_patterns.should include(%r{\.(?:jpe?g|gif|png)}, %r{\.(?:mp3|ogg|a3c)})
    end
  end

  describe '#filter!' do
    it 'replaces the passed regexps in the list of filters that determine the stored paths' do
      subject.filter!(%r{\.(?:jpe?g|gif|png)}, %r{\.(?:mp3|ogg|a3c)})
      subject.filtering_patterns.should =~ [%r{\.(?:mp3|ogg|a3c)}, %r{\.(?:jpe?g|gif|png)}]
    end
  end

  describe '#ignored?' do
    before { subject.stub(:relative_to_base) { |path| path } }

    it 'tests paths relative to the base directory' do
      subject.should_receive(:relative_to_base).with('file.txt')
      subject.ignored?('file.txt')
    end

    it 'returns true when the passed path is a default ignored path' do
      subject.ignored?('tmp/some_process.pid').should be_true
      subject.ignored?('dir/.DS_Store').should be_true
      subject.ignored?('.git/config').should be_true
    end

    it 'returns false when the passed path is not a default ignored path' do
      subject.ignored?('nested/tmp/some_process.pid').should be_false
      subject.ignored?('nested/.git').should be_false
      subject.ignored?('dir/.DS_Store/file').should be_false
      subject.ignored?('file.git').should be_false
    end

    it 'returns true when the passed path is ignored' do
      subject.ignore(%r{\.pid$})
      subject.ignored?('dir/some_process.pid').should be_true
    end

    it 'returns false when the passed path is not ignored' do
      subject.ignore(%r{\.pid$})
      subject.ignored?('dir/some_file.txt').should be_false
    end
  end

  describe '#filtered?' do
    before { subject.stub(:relative_to_base) { |path| path } }

    context 'when no filtering patterns are set' do
      it 'returns true for any path' do
        subject.filtered?('file.txt').should be_true
      end
    end

    context 'when filtering patterns are set' do
      before { subject.filter(%r{\.(?:jpe?g|gif|png)}) }

      it 'tests paths relative to the base directory' do
        subject.should_receive(:relative_to_base).with('file.txt')
        subject.filtered?('file.txt')
      end

      it 'returns true when the passed path is filtered' do
        subject.filter(%r{\.(?:jpe?g|gif|png)})
        subject.filtered?('dir/picture.jpeg').should be_true
      end

      it 'returns false when the passed path is not filtered' do
        subject.filter(%r{\.(?:jpe?g|gif|png)})
        subject.filtered?('dir/song.mp3').should be_false
      end
    end
  end

  describe '#build' do
    it 'stores all files' do
      fixtures do |path|
        touch 'file.rb'
        mkdir 'a_directory'
        touch 'a_directory/file.txt'

        record = described_class.new(path)
        record.build

        record.paths[path]['file.rb'].type.should eq 'File'
        record.paths[path]['a_directory'].type.should eq 'Dir'
        record.paths["#{path}/a_directory"]['file.txt'].type.should eq 'File'
      end
    end

    context 'with ignored path set' do
      it 'does not store ignored directory or its childs' do
        fixtures do |path|
          mkdir 'ignored_directory'
          mkdir 'ignored_directory/child_directory'
          touch 'ignored_directory/file.txt'

          record = described_class.new(path)
          record.ignore %r{^ignored_directory/}
          record.build

          record.paths[path]['/a_ignored_directory'].should be_nil
          record.paths["#{path}/a_ignored_directory"]['child_directory'].should be_nil
          record.paths["#{path}/a_ignored_directory"]['file.txt'].should be_nil
        end
      end

      it 'does not store ignored files' do
        fixtures do |path|
          touch 'ignored_file.rb'

          record = described_class.new(path)
          record.ignore %r{^ignored_file.rb$}
          record.build

          record.paths[path]['ignored_file.rb'].should be_nil
        end
      end
    end

    context 'with filters set' do
      it 'only stores filterd files' do
        fixtures do |path|
          touch 'file.rb'
          touch 'file.zip'
          mkdir 'a_directory'
          touch 'a_directory/file.txt'
          touch 'a_directory/file.rb'

          record = described_class.new(path)
          record.filter(/\.txt$/, /.*\.zip/)
          record.build

          record.paths[path]['file.rb'].should be_nil
          record.paths[path]['file.zip'].type.should eq 'File'
          record.paths[path]['a_directory'].type.should eq 'Dir'
          record.paths["#{path}/a_directory"]['file.txt'].type.should eq 'File'
          record.paths["#{path}/a_directory"]['file.rb'].should be_nil
        end
      end
    end
  end

  describe '#relative_to_base' do
    it 'removes the path of the base-directory from the passed path' do
      path = 'dir/to/app/file.rb'
      subject.relative_to_base(File.join(base_directory, path)).should eq path
    end

    it 'returns nil when the passed path is not inside the base-directory' do
      subject.relative_to_base('/tmp/some_random_path').should be_nil
    end

    it 'works with non UTF-8 paths' do
      path = "tmp/\xE4\xE4"
      subject.relative_to_base(File.join(base_directory, path))
    end

    context 'when containing regexp characters in the base directory' do
      before do
        fixtures do |path|
          mkdir 'a_directory$'
          @dir = described_class.new(path + '/a_directory$')
          @dir.build
        end
      end

      it 'removes the path of the base-directory from the passed path' do
        path = 'dir/to/app/file.rb'
        @dir.relative_to_base(File.join(@dir.directory, path)).should eq path
      end

      it 'returns nil when the passed path is not inside the base-directory' do
        @dir.relative_to_base('/tmp/some_random_path').should be_nil
      end
    end
  end

  describe '#fetch_changes' do
    context 'with single file changes' do
      context 'when a file is created' do
        it 'detects the added file' do
          fixtures do |path|
            modified, added, removed = changes(path) do
              touch 'new_file.rb'
            end

            added.should =~ %w(new_file.rb)
            modified.should be_empty
            removed.should be_empty
          end
        end

        it 'stores the added file in the record' do
          fixtures do |path|
            changes(path) do
              @record.paths.should be_empty

              touch 'new_file.rb'
            end

            @record.paths[path]['new_file.rb'].should_not be_nil
          end
        end

        context 'given a new created directory' do
          it 'detects the added file' do
            fixtures do |path|
              modified, added, removed = changes(path) do
                mkdir 'a_directory'
                touch 'a_directory/new_file.rb'
              end

              added.should =~ %w(a_directory/new_file.rb)
              modified.should be_empty
              removed.should be_empty
            end
          end

          it 'stores the added directory and file in the record' do
            fixtures do |path|
              changes(path) do
                @record.paths.should be_empty

                mkdir 'a_directory'
                touch 'a_directory/new_file.rb'
              end

              @record.paths[path]['a_directory'].should_not be_nil
              @record.paths["#{path}/a_directory"]['new_file.rb'].should_not be_nil
            end
          end
        end

        context 'given an existing directory' do
          context 'with recursive option set to true' do
            it 'detects the added file' do
              fixtures do |path|
                mkdir 'a_directory'

                modified, added, removed = changes(path, :recursive => true) do
                  touch 'a_directory/new_file.rb'
                end

                added.should =~ %w(a_directory/new_file.rb)
                modified.should be_empty
                removed.should be_empty
              end
            end

            context 'with an ignored directory' do
              it "doesn't detect the added file" do
                fixtures do |path|
                  mkdir 'ignored_directory'

                  modified, added, removed = changes(path, :ignore => %r{^ignored_directory/}, :recursive => true) do
                    touch 'ignored_directory/new_file.rb'
                  end

                  added.should be_empty
                  modified.should be_empty
                  removed.should be_empty
                end
              end

              it "doesn't detect the added file when it's asked to fetch the changes of the ignored directory"do
                fixtures do |path|
                  mkdir 'ignored_directory'

                  modified, added, removed = changes(path, :paths => ["#{path}/ignored_directory"], :ignore => %r{^ignored_directory/}, :recursive => true) do
                    touch 'ignored_directory/new_file.rb'
                  end

                  added.should be_empty
                  modified.should be_empty
                  removed.should be_empty
                end
              end
            end
          end

          context 'with recursive option set to false' do
            it "doesn't detect deeply-nested added files" do
              fixtures do |path|
                mkdir 'a_directory'

                modified, added, removed = changes(path, :recursive => false) do
                  touch 'a_directory/new_file.rb'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end

        context 'given a directory with subdirectories' do
          it 'detects the added file' do
            fixtures do |path|
              mkdir_p 'a_directory/subdirectory'

              modified, added, removed = changes(path, :recursive => true) do
                touch 'a_directory/subdirectory/new_file.rb'
              end

              added.should =~ %w(a_directory/subdirectory/new_file.rb)
              modified.should be_empty
              removed.should be_empty
            end
          end

          context 'with an ignored directory' do
            it "doesn't detect added files in neither the directory nor the subdirectory" do
              fixtures do |path|
                mkdir_p 'ignored_directory/subdirectory'

                modified, added, removed = changes(path, :ignore => %r{^ignored_directory/}, :recursive => true) do
                  touch 'ignored_directory/new_file.rb'
                  touch 'ignored_directory/subdirectory/new_file.rb'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end
      end

      context 'when a file is modified' do
        it 'detects the modified file' do
          fixtures do |path|
            touch 'existing_file.txt'

            modified, added, removed = changes(path) do
              sleep 1.5 # make a difference in the mtime of the file
              touch 'existing_file.txt'
            end

            added.should be_empty
            modified.should =~ %w(existing_file.txt)
            removed.should be_empty
          end
        end

        context 'during the same second at which we are checking for changes' do
          before { ensure_same_second }

          # The following test can only be run on systems that report
          # modification times in milliseconds.
          it 'always detects the modified file the first time', :if => described_class::HIGH_PRECISION_SUPPORTED do
            fixtures do |path|
              touch 'existing_file.txt'

              modified, added, removed = changes(path) do
                sleep 0.3 # make sure the mtime is changed a bit
                touch 'existing_file.txt'
              end

              added.should be_empty
              modified.should =~ %w(existing_file.txt)
              removed.should be_empty
            end
          end

          context 'when a file is created and then checked for modifications at the same second - #27' do
            # This issue was the result of checking a file for content changes when
            # the mtime and the checking time are the same. In this case there
            # is no checksum saved, so the file was reported as being changed.
            it ' does not report any changes' do
              fixtures do |path|
                touch 'a_file.rb'

                modified, added, removed = changes(path)

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end

          it "doesn't detects the modified file the second time if the content haven't changed" do
            fixtures do |path|
              touch 'existing_file.txt'

              changes(path) do
                touch 'existing_file.txt'
              end

              modified, added, removed = changes(path, :use_last_record => true) do
                touch 'existing_file.txt'
              end

              added.should be_empty
              modified.should be_empty
              removed.should be_empty
            end
          end

          it 'detects the modified file the second time if the content have changed' do
            fixtures do |path|
              touch 'existing_file.txt'
              # Set sha1 path checksum
              changes(path) do
                touch 'existing_file.txt'
              end
              small_time_difference

              changes(path) do
                touch 'existing_file.txt'
              end

              modified, added, removed = changes(path, :use_last_record => true) do
                open('existing_file.txt', 'w') { |f| f.write('foo') }
              end

              added.should be_empty
              modified.should =~ %w(existing_file.txt)
              removed.should be_empty
            end
          end

          it "doesn't detects the modified file the second time if just touched - #62" do
            fixtures do |path|
              touch 'existing_file.txt'
              # Set sha1 path checksum
              changes(path) do
                touch 'existing_file.txt'
              end
              small_time_difference

              changes(path, :use_last_record => true) do
                open('existing_file.txt', 'w') { |f| f.write('foo') }
              end

              modified, added, removed = changes(path, :use_last_record => true) do
                touch 'existing_file.txt'
              end

              added.should be_empty
              modified.should be_empty
              removed.should be_empty
            end
          end

          it "adds the path in the paths checksums if just touched - #62" do
            fixtures do |path|
              touch 'existing_file.txt'
              small_time_difference

              changes(path) do
                touch 'existing_file.txt'
              end

              @record.sha1_checksums["#{path}/existing_file.txt"].should_not be_nil
            end
          end

        it "deletes the path from the paths checksums" do
          fixtures do |path|
            touch 'unnecessary.txt'

            changes(path) do
              @record.sha1_checksums["#{path}/unnecessary.txt"] = 'foo'

              rm 'unnecessary.txt'
            end

            @record.sha1_checksums["#{path}/unnecessary.txt"].should be_nil
          end
        end


        end

        context 'given a hidden file' do
          it 'detects the modified file' do
            fixtures do |path|
              touch '.hidden'

              modified, added, removed = changes(path) do
                small_time_difference
                touch '.hidden'
              end

              added.should be_empty
              modified.should =~ %w(.hidden)
              removed.should be_empty
            end
          end
        end

        context 'given a file mode change' do
          it 'does not detect the mode change' do
            fixtures do |path|
              touch 'run.rb'
              sleep 1.5 # make a difference in the mtime of the file

              modified, added, removed = changes(path) do
                chmod 0777, 'run.rb'
              end

              added.should be_empty
              modified.should be_empty
              removed.should be_empty
            end
          end
        end

        context 'given an existing directory' do
          context 'with recursive option set to true' do
            it 'detects the modified file' do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/existing_file.txt'

                modified, added, removed = changes(path, :recursive => true) do
                  small_time_difference
                  touch 'a_directory/existing_file.txt'
                end

                added.should be_empty
                modified.should =~ %w(a_directory/existing_file.txt)
                removed.should be_empty
              end
            end
          end

          context 'with recursive option set to false' do
            it "doesn't detects the modified file" do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/existing_file.txt'

                modified, added, removed = changes(path, :recursive => false) do
                  small_time_difference
                  touch 'a_directory/existing_file.txt'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end

        context 'given a directory with subdirectories' do
          it 'detects the modified file' do
            fixtures do |path|
              mkdir_p 'a_directory/subdirectory'
              touch   'a_directory/subdirectory/existing_file.txt'

              modified, added, removed = changes(path, :recursive => true) do
                small_time_difference
                touch 'a_directory/subdirectory/existing_file.txt'
              end

              added.should be_empty
              modified.should =~ %w(a_directory/subdirectory/existing_file.txt)
              removed.should be_empty
            end
          end

          context 'with an ignored subdirectory' do
            it "doesn't detect the modified files in neither the directory nor the subdirectory" do
              fixtures do |path|
                mkdir_p 'ignored_directory/subdirectory'
                touch   'ignored_directory/existing_file.txt'
                touch   'ignored_directory/subdirectory/existing_file.txt'

                modified, added, removed = changes(path, :ignore => %r{^ignored_directory/}, :recursive => true) do
                  touch 'ignored_directory/existing_file.txt'
                  touch 'ignored_directory/subdirectory/existing_file.txt'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end
      end

      context 'when a file is moved' do
        it 'detects the file movement' do
          fixtures do |path|
            touch 'move_me.txt'

            modified, added, removed = changes(path) do
              mv 'move_me.txt', 'new_name.txt'
            end

            added.should =~ %w(new_name.txt)
            modified.should be_empty
            removed.should =~ %w(move_me.txt)
          end
        end

        context 'given an existing directory' do
          context 'with recursive option set to true' do
            it 'detects the file movement into the directory' do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'move_me.txt'

                modified, added, removed = changes(path, :recursive => true) do
                  mv 'move_me.txt', 'a_directory/move_me.txt'
                end

                added.should =~ %w(a_directory/move_me.txt)
                modified.should be_empty
                removed.should =~ %w(move_me.txt)
              end
            end

            it 'detects a file movement out of the directory' do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/move_me.txt'

                modified, added, removed = changes(path, :recursive => true) do
                  mv 'a_directory/move_me.txt', 'i_am_here.txt'
                end

                added.should =~ %w(i_am_here.txt)
                modified.should be_empty
                removed.should =~ %w(a_directory/move_me.txt)
              end
            end

            it 'detects a file movement between two directories' do
              fixtures do |path|
                mkdir 'from_directory'
                touch 'from_directory/move_me.txt'
                mkdir 'to_directory'

                modified, added, removed = changes(path, :recursive => true) do
                  mv 'from_directory/move_me.txt', 'to_directory/move_me.txt'
                end

                added.should =~ %w(to_directory/move_me.txt)
                modified.should be_empty
                removed.should =~ %w(from_directory/move_me.txt)
              end
            end
          end

          context 'with recursive option set to false' do
            it "doesn't detect the file movement into the directory" do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'move_me.txt'

                modified, added, removed = changes(path, :recursive => false) do
                  mv 'move_me.txt', 'a_directory/move_me.txt'
                end

                added.should be_empty
                modified.should be_empty
                removed.should =~ %w(move_me.txt)
              end
            end

            it "doesn't detect a file movement out of the directory" do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/move_me.txt'

                modified, added, removed = changes(path, :recursive => false) do
                  mv 'a_directory/move_me.txt', 'i_am_here.txt'
                end

                added.should =~ %w(i_am_here.txt)
                modified.should be_empty
                removed.should be_empty
              end
            end

            it "doesn't detect a file movement between two directories" do
              fixtures do |path|
                mkdir 'from_directory'
                touch 'from_directory/move_me.txt'
                mkdir 'to_directory'

                modified, added, removed = changes(path, :recursive => false) do
                  mv 'from_directory/move_me.txt', 'to_directory/move_me.txt'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end

            context 'given a directory with subdirectories' do
              it 'detects a file movement between two subdirectories' do
                fixtures do |path|
                  mkdir_p 'a_directory/subdirectory'
                  mkdir_p 'b_directory/subdirectory'
                  touch   'a_directory/subdirectory/move_me.txt'

                  modified, added, removed = changes(path, :recursive => true) do
                    mv 'a_directory/subdirectory/move_me.txt', 'b_directory/subdirectory'
                  end

                  added.should =~ %w(b_directory/subdirectory/move_me.txt)
                  modified.should be_empty
                  removed.should =~ %w(a_directory/subdirectory/move_me.txt)
                end
              end

              context 'with an ignored subdirectory' do
                it "doesn't detect the file movement between subdirectories" do
                  fixtures do |path|
                    mkdir_p 'a_ignored_directory/subdirectory'
                    mkdir_p 'b_ignored_directory/subdirectory'
                    touch   'a_ignored_directory/subdirectory/move_me.txt'

                    modified, added, removed = changes(path, :ignore => %r{^(?:a|b)_ignored_directory/}, :recursive => true) do
                      mv 'a_ignored_directory/subdirectory/move_me.txt', 'b_ignored_directory/subdirectory'
                    end

                    added.should be_empty
                    modified.should be_empty
                    removed.should be_empty
                  end
                end
              end
            end

            context 'with all paths passed as params' do
              it 'detects the file movement into the directory' do
                fixtures do |path|
                  mkdir 'a_directory'
                  touch 'move_me.txt'

                  modified, added, removed = changes(path, :recursive => false, :paths => [path, "#{path}/a_directory"]) do
                    mv 'move_me.txt', 'a_directory/move_me.txt'
                  end

                  added.should =~ %w(a_directory/move_me.txt)
                  modified.should be_empty
                  removed.should =~ %w(move_me.txt)
                end
              end

              it 'detects a file moved outside of a directory' do
                fixtures do |path|
                  mkdir 'a_directory'
                  touch 'a_directory/move_me.txt'

                  modified, added, removed = changes(path, :recursive => false, :paths => [path, "#{path}/a_directory"]) do
                    mv 'a_directory/move_me.txt', 'i_am_here.txt'
                  end

                  added.should =~ %w(i_am_here.txt)
                  modified.should be_empty
                  removed.should =~ %w(a_directory/move_me.txt)
                end
              end

              it 'detects a file movement between two directories' do
                fixtures do |path|
                  mkdir 'from_directory'
                  touch 'from_directory/move_me.txt'
                  mkdir 'to_directory'

                  modified, added, removed = changes(path, :recursive => false, :paths => [path, "#{path}/from_directory", "#{path}/to_directory"]) do
                    mv 'from_directory/move_me.txt', 'to_directory/move_me.txt'
                  end

                  added.should =~ %w(to_directory/move_me.txt)
                  modified.should be_empty
                  removed.should =~ %w(from_directory/move_me.txt)
                end
              end
            end
          end
        end
      end

      context 'when a file is deleted' do
        it 'detects the file removal' do
          fixtures do |path|
            touch 'unnecessary.txt'

            modified, added, removed = changes(path) do
              rm 'unnecessary.txt'
            end

            added.should be_empty
            modified.should be_empty
            removed.should =~ %w(unnecessary.txt)
          end
        end

        it "deletes the file from the record" do
          fixtures do |path|
            touch 'unnecessary.txt'

            changes(path) do
              @record.paths[path]['unnecessary.txt'].should_not be_nil

              rm 'unnecessary.txt'
            end

            @record.paths[path]['unnecessary.txt'].should be_nil
          end
        end

        it "deletes the path from the paths checksums" do
          fixtures do |path|
            touch 'unnecessary.txt'

            changes(path) do
              @record.sha1_checksums["#{path}/unnecessary.txt"] = 'foo'

              rm 'unnecessary.txt'
            end

            @record.sha1_checksums["#{path}/unnecessary.txt"].should be_nil
          end
        end

        context 'given an existing directory' do
          context 'with recursive option set to true' do
            it 'detects the file removal' do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/do_not_use.rb'

                modified, added, removed = changes(path, :recursive => true) do
                  rm 'a_directory/do_not_use.rb'
                end

                added.should be_empty
                modified.should be_empty
                removed.should =~ %w(a_directory/do_not_use.rb)
              end
            end
          end

          context 'with recursive option set to false' do
            it "doesn't detect the file removal" do
              fixtures do |path|
                mkdir 'a_directory'
                touch 'a_directory/do_not_use.rb'

                modified, added, removed = changes(path, :recursive => false) do
                  rm 'a_directory/do_not_use.rb'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end

        context 'given a directory with subdirectories' do
          it 'detects the file removal in subdirectories' do
            fixtures do |path|
              mkdir_p 'a_directory/subdirectory'
              touch   'a_directory/subdirectory/do_not_use.rb'

              modified, added, removed = changes(path, :recursive => true) do
                rm 'a_directory/subdirectory/do_not_use.rb'
              end

              added.should be_empty
              modified.should be_empty
              removed.should =~ %w(a_directory/subdirectory/do_not_use.rb)
            end
          end

          context 'with an ignored subdirectory' do
            it "doesn't detect files removals in neither the directory nor its subdirectories" do
              fixtures do |path|
                mkdir_p 'ignored_directory/subdirectory'
                touch   'ignored_directory/do_not_use.rb'
                touch   'ignored_directory/subdirectory/do_not_use.rb'

                modified, added, removed = changes(path, :ignore => %r{^ignored_directory/}, :recursive => true) do
                  rm 'ignored_directory/do_not_use.rb'
                  rm 'ignored_directory/subdirectory/do_not_use.rb'
                end

                added.should be_empty
                modified.should be_empty
                removed.should be_empty
              end
            end
          end
        end
      end
    end

    context 'multiple file operations' do
      it 'detects the added files' do
        fixtures do |path|
          modified, added, removed = changes(path) do
            touch 'a_file.rb'
            touch 'b_file.rb'
            mkdir 'a_directory'
            touch 'a_directory/a_file.rb'
            touch 'a_directory/b_file.rb'
          end

          added.should =~ %w(a_file.rb b_file.rb a_directory/a_file.rb a_directory/b_file.rb)
          modified.should be_empty
          removed.should be_empty
        end
      end

      it 'detects the modified files' do
        fixtures do |path|
          touch 'a_file.rb'
          touch 'b_file.rb'
          mkdir 'a_directory'
          touch 'a_directory/a_file.rb'
          touch 'a_directory/b_file.rb'

          small_time_difference

          modified, added, removed = changes(path) do
            touch 'b_file.rb'
            touch 'a_directory/a_file.rb'
          end

          added.should be_empty
          modified.should =~ %w(b_file.rb a_directory/a_file.rb)
          removed.should be_empty
        end
      end

      it 'detects the removed files' do
        fixtures do |path|
          touch 'a_file.rb'
          touch 'b_file.rb'
          mkdir 'a_directory'
          touch 'a_directory/a_file.rb'
          touch 'a_directory/b_file.rb'

          modified, added, removed = changes(path) do
            rm 'b_file.rb'
            rm 'a_directory/a_file.rb'
          end

          added.should be_empty
          modified.should be_empty
          removed.should =~ %w(b_file.rb a_directory/a_file.rb)
        end
      end
    end

    context 'single directory operations' do
      it 'detects a moved directory' do
        fixtures do |path|
          mkdir 'a_directory'
          touch 'a_directory/a_file.rb'
          touch 'a_directory/b_file.rb'

          modified, added, removed = changes(path) do
            mv 'a_directory', 'renamed'
          end

          added.should =~ %w(renamed/a_file.rb renamed/b_file.rb)
          modified.should be_empty
          removed.should =~ %w(a_directory/a_file.rb a_directory/b_file.rb)
        end
      end

      it 'detects a removed directory' do
        fixtures do |path|
          mkdir 'a_directory'
          touch 'a_directory/a_file.rb'
          touch 'a_directory/b_file.rb'

          modified, added, removed = changes(path) do
            rm_rf 'a_directory'
          end

          added.should be_empty
          modified.should be_empty
          removed.should =~ %w(a_directory/a_file.rb a_directory/b_file.rb)
        end
      end

      it "deletes the directory from the record" do
        fixtures do |path|
          mkdir 'a_directory'
          touch 'a_directory/file.rb'

          changes(path) do
            @record.paths.should have(2).paths
            @record.paths[path]['a_directory'].should_not be_nil
            @record.paths["#{path}/a_directory"]['file.rb'].should_not be_nil

            rm_rf 'a_directory'
          end

          @record.paths.should have(1).paths
          @record.paths[path]['a_directory'].should be_nil
          @record.paths["#{path}/a_directory"]['file.rb'].should be_nil
        end
      end

      context 'with nested paths' do
        it 'detects removals without crashing - #18' do
          fixtures do |path|
            mkdir_p 'a_directory/subdirectory'
            touch   'a_directory/subdirectory/do_not_use.rb'

            modified, added, removed = changes(path) do
              rm_r 'a_directory'
            end

            added.should be_empty
            modified.should be_empty
            removed.should =~ %w(a_directory/subdirectory/do_not_use.rb)
          end
        end
      end
    end

    context 'with a path outside the directory for which a record is made' do
      it "skips that path and doesn't check for changes" do
          fixtures do |path|
            modified, added, removed = changes(path, :paths => ['some/where/outside']) do
              @record.should_not_receive(:detect_additions)
              @record.should_not_receive(:detect_modifications_and_removals)

              touch 'new_file.rb'
            end

            added.should be_empty
            modified.should be_empty
            removed.should be_empty
          end
      end
    end

    context 'with the relative_paths option set to false' do
      it 'returns full paths in the changes hash' do
        fixtures do |path|
          touch 'a_file.rb'
          touch 'b_file.rb'

          modified, added, removed = changes(path, :relative_paths => false) do
            small_time_difference
            rm    'a_file.rb'
            touch 'b_file.rb'
            touch 'c_file.rb'
            mkdir 'a_directory'
            touch 'a_directory/a_file.rb'
          end

          added.should =~ ["#{path}/c_file.rb", "#{path}/a_directory/a_file.rb"]
          modified.should =~ ["#{path}/b_file.rb"]
          removed.should =~ ["#{path}/a_file.rb"]
        end
      end
    end

    context 'within a directory containing unreadble paths - #32' do
      it 'detects changes more than a second apart' do
        fixtures do |path|
          touch 'unreadable_file.txt'
          chmod 000, 'unreadable_file.txt'

          modified, added, removed = changes(path) do
            sleep 1.1
            touch 'unreadable_file.txt'
          end

          added.should be_empty
          modified.should =~ %w(unreadable_file.txt)
          removed.should be_empty
        end
      end

      context 'with multiple changes within the same second' do
        before { ensure_same_second }

        it 'does not detect changes even if content changes', :unless => described_class::HIGH_PRECISION_SUPPORTED do
          fixtures do |path|
            touch 'unreadable_file.txt'

            modified, added, removed = changes(path) do
              open('unreadable_file.txt', 'w') { |f| f.write('foo') }
              chmod 000, 'unreadable_file.txt'
            end

            added.should be_empty
            modified.should be_empty
            removed.should be_empty
          end
        end
      end
    end

    context 'within a directory containing a removed file - #39' do
      it 'does not raise an exception when hashing a removed file' do

        # simulate a race condition where the file is removed after the
        # change event is tracked, but before the hash is calculated
        Digest::SHA1.should_receive(:file).twice.and_raise(Errno::ENOENT)

        lambda {
          fixtures do |path|
            file = 'removed_file.txt'
            touch file
            changes(path) { touch file }
          end
        }.should_not raise_error(Errno::ENOENT)
      end
    end

    context 'within a directory containing a unix domain socket file' do
      it 'does not raise an exception when hashing a unix domain socket file' do
        fixtures do |path|
          require 'socket'
          UNIXServer.new('unix_domain_socket.sock')
          lambda { changes(path){} }.should_not raise_error(Errno::ENXIO)
        end
      end
    end

    context 'with symlinks', :unless => windows? do
      it 'looks at symlinks not their targets' do
        fixtures do |path|
          touch 'target'
          symlink 'target', 'symlink'

          record = described_class.new(path)
          record.build

          sleep 1
          touch 'target'

          record.fetch_changes([path], :relative_paths => true)[:modified].should == ['target']
        end
      end

      it 'handles broken symlinks' do
        fixtures do |path|
          symlink 'target', 'symlink'

          record = described_class.new(path)
          record.build

          sleep 1
          rm 'symlink'
          symlink 'new-target', 'symlink'
          record.fetch_changes([path], :relative_paths => true)
        end
      end
    end
  end
end
