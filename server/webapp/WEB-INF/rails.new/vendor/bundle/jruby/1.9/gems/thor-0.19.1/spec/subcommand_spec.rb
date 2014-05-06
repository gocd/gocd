require "helper"

describe Thor do

  describe "#subcommand" do

    it "maps a given subcommand to another Thor subclass" do
      barn_help = capture(:stdout) { Scripts::MyDefaults.start(%w[barn]) }
      expect(barn_help).to include("barn help [COMMAND]  # Describe subcommands or one specific subcommand")
    end

    it "passes commands to subcommand classes" do
      expect(capture(:stdout) { Scripts::MyDefaults.start(%w[barn open]) }.strip).to eq("Open sesame!")
    end

    it "passes arguments to subcommand classes" do
      expect(capture(:stdout) { Scripts::MyDefaults.start(%w[barn open shotgun]) }.strip).to eq("That's going to leave a mark.")
    end

    it "ignores unknown options (the subcommand class will handle them)" do
      expect(capture(:stdout) { Scripts::MyDefaults.start(%w[barn paint blue --coats 4]) }.strip).to eq("4 coats of blue paint")
    end

    it "passes parsed options to subcommands" do
      output = capture(:stdout) { TestSubcommands::Parent.start(%w[sub print_opt --opt output]) }
      expect(output).to eq("output")
    end

    it "accepts the help switch and calls the help command on the subcommand" do
      output = capture(:stdout) { TestSubcommands::Parent.start(%w[sub print_opt --help]) }
      sub_help = capture(:stdout) { TestSubcommands::Parent.start(%w[sub help print_opt]) }
      expect(output).to eq(sub_help)
    end

    it "accepts the help short switch and calls the help command on the subcommand" do
      output = capture(:stdout) { TestSubcommands::Parent.start(%w[sub print_opt -h]) }
      sub_help = capture(:stdout) { TestSubcommands::Parent.start(%w[sub help print_opt]) }
      expect(output).to eq(sub_help)
    end

    it "the help command on the subcommand and after it should result in the same output" do
      output = capture(:stdout) { TestSubcommands::Parent.start(%w[sub help])}
      sub_help = capture(:stdout) { TestSubcommands::Parent.start(%w[help sub])}
      expect(output).to eq(sub_help)
    end
  end

end
