require 'spec_helper'

describe 'command line', :ui do
  let(:stderr) { StringIO.new }
  let(:stdout) { StringIO.new }

  before :all do
    write_file 'spec/simple_spec.rb', """
      describe 'group 1' do
        specify('group 1 example 1') {}
        specify('group 1 example 2') {}
        specify('group 1 example 3') {}
        describe 'group 1-1' do
          specify('group 1-1 example 1') {}
          specify('group 1-1 example 2') {}
          specify('group 1-1 example 3') {}
        end
      end
    """

    write_file 'spec/simple_spec2.rb', """
      describe 'group 2' do
        specify('group 2 example 1') {}
        specify('group 2 example 2') {}
        specify('group 2 example 3') {}
        describe 'group 2-1' do
          specify('group 2-1 example 1') {}
          specify('group 2-1 example 2') {}
          specify('group 2-1 example 3') {}
        end
      end
    """

    write_file 'spec/order_spec.rb', """
      describe 'group 1' do
        specify('group 1 example 1')  {}
        specify('group 1 example 2')  {}
        specify('group 1 example 3')  {}
        specify('group 1 example 4')  {}
        specify('group 1 example 5')  {}
        specify('group 1 example 6')  {}
        specify('group 1 example 5')  {}
        specify('group 1 example 7')  {}
        specify('group 1 example 8')  {}
        specify('group 1 example 9')  {}
        specify('group 1 example 10') {}

        describe 'group 1-1' do
          specify('group 1-1 example 1')  {}
          specify('group 1-1 example 2')  {}
          specify('group 1-1 example 3')  {}
          specify('group 1-1 example 4')  {}
          specify('group 1-1 example 5')  {}
          specify('group 1-1 example 6')  {}
          specify('group 1-1 example 7')  {}
          specify('group 1-1 example 8')  {}
          specify('group 1-1 example 9')  {}
          specify('group 1-1 example 10') {}
        end

        describe('group 1-2')  { specify('example') {} }
        describe('group 1-3')  { specify('example') {} }
        describe('group 1-4')  { specify('example') {} }
        describe('group 1-5')  { specify('example') {} }
        describe('group 1-6')  { specify('example') {} }
        describe('group 1-7')  { specify('example') {} }
        describe('group 1-8')  { specify('example') {} }
        describe('group 1-9')  { specify('example') {} }
        describe('group 1-10') { specify('example') {} }
      end

      describe('group 2')  { specify('example') {} }
      describe('group 3')  { specify('example') {} }
      describe('group 4')  { specify('example') {} }
      describe('group 5')  { specify('example') {} }
      describe('group 6')  { specify('example') {} }
      describe('group 7')  { specify('example') {} }
      describe('group 8')  { specify('example') {} }
      describe('group 9')  { specify('example') {} }
      describe('group 10') { specify('example') {} }
    """
  end

  describe '--order rand' do
    it 'runs the examples and groups in a different order each time' do
      run_command 'tmp/aruba/spec/order_spec.rb --order rand -f doc'
      RSpec.configuration.seed = srand && srand # reset seed in same process
      run_command 'tmp/aruba/spec/order_spec.rb --order rand -f doc'

      expect(stdout.string).to match(/Randomized with seed \d+/)

      top_level_groups      {|first_run, second_run| expect(first_run).to_not eq(second_run)}
      nested_groups         {|first_run, second_run| expect(first_run).to_not eq(second_run)}
      examples('group 1')   {|first_run, second_run| expect(first_run).to_not eq(second_run)}
      examples('group 1-1') {|first_run, second_run| expect(first_run).to_not eq(second_run)}
    end
  end

  describe '--order rand:SEED' do
    it 'runs the examples and groups in the same order each time' do
      2.times { run_command 'tmp/aruba/spec/order_spec.rb --order rand:123 -f doc' }

      expect(stdout.string).to match(/Randomized with seed 123/)

      top_level_groups      {|first_run, second_run| expect(first_run).to eq(second_run)}
      nested_groups         {|first_run, second_run| expect(first_run).to eq(second_run)}
      examples('group 1')   {|first_run, second_run| expect(first_run).to eq(second_run)}
      examples('group 1-1') {|first_run, second_run| expect(first_run).to eq(second_run)}
    end
  end

  describe '--seed SEED' do
    it "forces '--order rand' and runs the examples and groups in the same order each time" do
      2.times { run_command 'tmp/aruba/spec/order_spec.rb --seed 123 -f doc' }

      expect(stdout.string).to match(/Randomized with seed \d+/)

      top_level_groups      {|first_run, second_run| expect(first_run).to eq(second_run)}
      nested_groups         {|first_run, second_run| expect(first_run).to eq(second_run)}
      examples('group 1')   {|first_run, second_run| expect(first_run).to eq(second_run)}
      examples('group 1-1') {|first_run, second_run| expect(first_run).to eq(second_run)}
    end

    it "runs examples in the same order, regardless of the order in which files are given" do
      run_command 'tmp/aruba/spec/simple_spec.rb tmp/aruba/spec/simple_spec2.rb --seed 1337 -f doc'
      run_command 'tmp/aruba/spec/simple_spec2.rb tmp/aruba/spec/simple_spec.rb --seed 1337 -f doc'

      top_level_groups      {|first_run, second_run| expect(first_run).to eq(second_run)}
      nested_groups         {|first_run, second_run| expect(first_run).to eq(second_run)}
    end
  end

  describe '--order default on CLI with --order rand in .rspec' do
    it "overrides --order rand with --order default" do
      write_file '.rspec', '--order rand'

      run_command 'tmp/aruba/spec/order_spec.rb --order default -f doc'

      expect(stdout.string).not_to match(/Randomized/)

      expect(stdout.string).to match(
        /group 1.*group 1 example 1.*group 1 example 2.*group 1-1.*group 1-2.*group 2.*/m
      )
    end
  end

  context 'when a custom order is configured' do
    before do
      write_file 'spec/custom_order_spec.rb', """
        RSpec.configure do |config|
          config.order_groups_and_examples do |list|
            list.sort_by { |item| item.description }
          end
        end

        describe 'group B' do
          specify('group B example D')  {}
          specify('group B example B')  {}
          specify('group B example A')  {}
          specify('group B example C')  {}
        end

        describe 'group A' do
          specify('group A example 1')  {}
        end
      """
    end

    it 'orders the groups and examples by the provided strategy' do
      run_command 'tmp/aruba/spec/custom_order_spec.rb -f doc'

      top_level_groups    { |groups| expect(groups.flatten).to eq(['group A', 'group B']) }
      examples('group B') do |examples|
        letters = examples.flatten.map { |e| e[/(.)\z/, 1] }
        expect(letters).to eq(['A', 'B', 'C', 'D'])
      end
    end
  end

  def examples(group)
    yield split_in_half(stdout.string.scan(/^\s+#{group} example.*$/))
  end

  def top_level_groups
    yield example_groups_at_level(0)
  end

  def nested_groups
    yield example_groups_at_level(2)
  end

  def example_groups_at_level(level)
    split_in_half(stdout.string.scan(/^\s{#{level*2}}group.*$/))
  end

  def split_in_half(array)
    length, midpoint = array.length, array.length / 2
    return array.slice(0, midpoint), array.slice(midpoint, length)
  end

  def run_command(cmd)
    RSpec::Core::Runner.run(cmd.split, stderr, stdout)
  end
end
