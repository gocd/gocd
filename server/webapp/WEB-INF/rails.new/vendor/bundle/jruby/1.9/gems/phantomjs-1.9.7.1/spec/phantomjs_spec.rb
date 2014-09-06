require 'spec_helper'
require 'phantomjs/poltergeist'

HTML_RESPONSE = <<HTML
  <html>
  <head></head>
  <body>
    <h1>Hello</h1>
    <div id="js">NO JS :(</div>
    <script type="text/javascript">
      document.getElementById('js').innerHTML = 'OMG JS!';
    </script>
  </body>
  </html>
HTML
Capybara.app = lambda {|env| [200, {"Content-Type" => "text/html"}, [HTML_RESPONSE]] }
Capybara.default_driver = :poltergeist

describe Phantomjs do
  describe 'A HTTP request using capybara/poltergeist' do
    include Capybara::DSL

    before { visit '/' }
    it "has displayed static html content" do
      within('h1') { page.should have_content('Hello') }
    end

    it "has processed javascript" do
      within "#js" do
        page.should_not have_content('NO JS :(')
        page.should have_content('OMG JS!')
      end
    end
  end

  describe ".run" do
    it "runs phantomjs binary with the correct arguments" do
      script = File.expand_path('./spec/runner.js')
      result = Phantomjs.run(script, 'foo1', 'foo2')
      result.should eq("bar foo1\nbar foo2\n")
    end

    it "accepts a block that will get called for each line of output" do
      lines = []
      script = File.expand_path('./spec/runner.js')
      Phantomjs.run(script, 'foo1', 'foo2') { |line| lines << line }
      lines.should eq(["bar foo1\n", "bar foo2\n"])
    end
  end
end