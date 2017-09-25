require File.dirname(__FILE__) + '/spec_helper'

class StringSerializer < YARD::Serializers::Base
  attr_accessor :files, :string
  def initialize(files, string)
    @files = files
    @string = string
  end

  def serialize(object, data)
    files << object
    string << data
  end
end

describe YARD::Templates::Engine.template(:default, :onefile) do
  before do
    Registry.clear
    if defined?(::Encoding)
      @eenc, Encoding.default_external = Encoding.default_external, 'ascii-8bit'
      @ienc, Encoding.default_internal = Encoding.default_internal, 'ascii-8bit'
    end
  end

  after do
    if defined?(::Encoding)
      Encoding.default_internal = @ienc
      Encoding.default_external = @eenc
    end
  end

  def render
    @files = []
    @output = ''
    YARD.parse_string <<-eof
      class A
        # Foo method
        # @return [String]
        def foo; end

        # Bar method
        # @return [Numeric]
        def bar; end
      end
    eof
    readme = CodeObjects::ExtraFileObject.new('README',
      "# This is a code comment\n\n# Top of file\n\n\nclass C; end")
    Templates::Engine.generate Registry.all(:class),
      :serializer => StringSerializer.new(@files, @output),
      :onefile => true, :format => :html, :readme => readme, :files => [readme,
        CodeObjects::ExtraFileObject.new('LICENSE', 'This is a license!')
      ]
  end

  it "should render html" do
    render
    @files.should == ['index.html']
    @output.should include("This is a code comment")
    @output.should include("This is a license!")
    @output.should include("Class: A")
    @output.should include("Foo method")
    @output.should include("Bar method")
  end
end
