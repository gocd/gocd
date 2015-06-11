require "spec_helper"

module RSpec::Core
  describe ProjectInitializer, :isolated_directory => true do

    describe "#run" do
      context "with no args" do
        let(:command_line_config) { ProjectInitializer.new }

        before do
          command_line_config.stub(:warn)
          command_line_config.stub(:puts)
          command_line_config.stub(:gets => 'no')
        end

        context "with no .rspec file" do
          it "says it's creating .rspec " do
            command_line_config.should_receive(:puts).with(/create\s+\.rspec/)
            command_line_config.run
          end

          it "generates a .rspec" do
            command_line_config.run
            expect(File.read('.rspec')).to match(/--color\n--format progress/m)
          end
        end

        context "with a .rspec file" do
          it "says .rspec exists" do
            FileUtils.touch('.rspec')
            command_line_config.should_receive(:puts).with(/exist\s+\.rspec/)
            command_line_config.run
          end

          it "doesn't create a new one" do
            File.open('.rspec', 'w') {|f| f << '--color'}
            command_line_config.run
            expect(File.read('.rspec')).to eq('--color')
          end
        end

        context "with no spec/spec_helper.rb file" do
          it "says it's creating spec/spec_helper.rb " do
            command_line_config.should_receive(:puts).with(/create\s+spec\/spec_helper.rb/)
            command_line_config.run
          end

          it "generates a spec/spec_helper.rb" do
            command_line_config.run
            expect(File.read('spec/spec_helper.rb')).to match(/RSpec\.configure do \|config\|/m)
          end
        end

        context "with a spec/spec_helper.rb file" do
          before { FileUtils.mkdir('spec') }

          it "says spec/spec_helper.rb exists" do
            FileUtils.touch('spec/spec_helper.rb')
            command_line_config.should_receive(:puts).with(/exist\s+spec\/spec_helper.rb/)
            command_line_config.run
          end

          it "doesn't create a new one" do
            random_content = "content #{rand}"
            File.open('spec/spec_helper.rb', 'w') {|f| f << random_content}
            command_line_config.run
            expect(File.read('spec/spec_helper.rb')).to eq(random_content)
          end
        end

        context "with autotest/discover.rb" do
          before do
            FileUtils.mkdir('autotest')
            FileUtils.touch 'autotest/discover.rb'
          end

          it "asks whether to delete the file" do
            command_line_config.should_receive(:puts).with(/delete/)
            command_line_config.run
          end

          it "removes it if confirmed" do
            command_line_config.stub(:gets => 'yes')
            command_line_config.run
            expect(File.exist?('autotest/discover.rb')).to be_false
          end

          it "leaves it if not confirmed" do
            command_line_config.stub(:gets => 'no')
            command_line_config.run
            expect(File.exist?('autotest/discover.rb')).to be_true
          end
        end

        context "with lib/tasks/rspec.rake" do
          before do
            FileUtils.mkdir_p('lib/tasks')
            FileUtils.touch 'lib/tasks/rspec.rake'
          end

          it "asks whether to delete the file" do
            command_line_config.should_receive(:puts).with(/delete/)
            command_line_config.run
          end

          it "removes it if confirmed" do
            command_line_config.stub(:gets => 'yes')
            command_line_config.run
            expect(File.exist?('lib/tasks/rspec.rake')).to be_false
          end

          it "leaves it if not confirmed" do
            command_line_config.stub(:gets => 'no')
            command_line_config.run
            expect(File.exist?('lib/tasks/rspec.rake')).to be_true
          end
        end
      end

      context "given an arg" do
        it "warns if arg received (no longer necessary)" do
          config = ProjectInitializer.new("another_arg")
          config.stub(:puts)
          config.stub(:gets => 'no')
          config.run
        end
      end
    end
  end
end
