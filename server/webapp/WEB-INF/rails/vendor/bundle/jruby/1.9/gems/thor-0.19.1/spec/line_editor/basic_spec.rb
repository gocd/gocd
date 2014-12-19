require "helper"

describe Thor::LineEditor::Basic do
  describe ".available?" do
    it "returns true" do
      expect(Thor::LineEditor::Basic).to be_available
    end
  end

  describe "#readline" do
    it "uses $stdin and $stdout to get input from the user" do
      expect($stdout).to receive(:print).with("Enter your name ")
      expect($stdin).to receive(:gets).and_return("George")
      expect($stdin).not_to receive(:noecho)
      editor = Thor::LineEditor::Basic.new("Enter your name ", {})
      expect(editor.readline).to eq("George")
    end

    it "disables echo when asked to" do
      expect($stdout).to receive(:print).with("Password: ")
      noecho_stdin = double("noecho_stdin")
      expect(noecho_stdin).to receive(:gets).and_return("secret")
      expect($stdin).to receive(:noecho).and_yield(noecho_stdin)
      editor = Thor::LineEditor::Basic.new("Password: ", :echo => false)
      expect(editor.readline).to eq("secret")
    end
  end
end
