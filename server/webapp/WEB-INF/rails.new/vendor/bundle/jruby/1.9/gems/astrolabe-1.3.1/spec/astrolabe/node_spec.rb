# coding: utf-8

require 'astrolabe/node'

module Astrolabe
  describe Node, :ast do
    describe '#parent' do
      let(:source) { <<-END }
        def some_method(arg_a, arg_b)
          do_something
        end
      END

      # (def :some_method
      #   (args
      #     (arg :arg_a)
      #     (arg :arg_b))
      #   (send nil :do_something))

      context 'with a non-root node' do
        let(:target_node) { root_node.each_node.find(&:args_type?) }

        it 'returns the parent node' do
          expect(target_node.parent).to be_def_type
        end
      end

      context 'with a root node' do
        it 'returns nil' do
          expect(root_node.parent).to be_nil
        end
      end
    end

    describe '#root?' do
      let(:source) { <<-END }
        def some_method
          do_something
        end
      END

      subject { target_node.root? }

      context 'with root node' do
        let(:target_node) { root_node }
        it { is_expected.to be true }
      end

      context 'with non-root node' do
        let(:target_node) { root_node.each_child_node.to_a.first }
        it { is_expected.to be false }
      end
    end

    describe '#sibling_index' do
      let(:source) { <<-END }
        2.times do |index|
          puts index
        end
      END

      # (block
      #   (send
      #     (int 2) :times)
      #   (args
      #     (arg :index))
      #   (send nil :puts
      #     (lvar :index)))

      let(:target_node) { root_node.each_node.find(&:args_type?) }

      it 'returns the index of the receiver node in its siblings' do
        expect(target_node.sibling_index).to eq(1)
      end

      context 'when there are same structure nodes in the siblings' do
        let(:source) { <<-END }
          foo = 1
          puts foo
          puts foo
        END

        # (begin
        #   (lvasgn :foo
        #     (int 1))
        #   (send nil :puts
        #     (lvar :foo))
        #   (send nil :puts
        #     (lvar :foo)))

        let(:target_node) { root_node.each_node(:send).to_a.last }

        it 'does not confuse them' do
          expect(target_node.sibling_index).to eq(2)
        end
      end
    end

    shared_examples 'node enumerator' do |method_name|
      context 'when no block is given' do
        it 'returns an enumerator' do
          expect(target_node.send(method_name)).to be_a Enumerator
        end

        describe 'the returned enumerator' do
          subject(:enumerator) { target_node.send(method_name) }

          it 'enumerates ancestor nodes' do
            expected_types.each do |expected_type|
              expect(enumerator.next.type).to eq(expected_type)
            end

            expect { enumerator.next }.to raise_error(StopIteration)
          end

          context 'when a node type symbol is passed' do
            subject(:enumerator) { target_node.send(method_name, :send) }

            it 'enumerates only nodes matching the type' do
              count = 0

              begin
                loop do
                  expect(enumerator.next.type).to eq(:send)
                  count += 1
                end
              rescue StopIteration
                expect(count).to be > 0
              end
            end
          end
        end
      end
    end

    describe '#each_ancestor' do
      let(:source) { <<-END }
        class SomeClass
          attr_reader :some_attr

          def some_method(arg_a, arg_b)
            do_something
          end
        end
      END

      # (class
      #   (const nil :SomeClass) nil
      #   (begin
      #     (send nil :attr_reader
      #       (sym :some_attr))
      #     (def :some_method
      #       (args
      #         (arg :arg_a)
      #         (arg :arg_b))
      #       (send nil :do_something))))

      let(:target_node) { root_node.each_node.find(&:args_type?) }
      let(:expected_types) { [:def, :begin, :class] }

      context 'when a block is given' do
        it 'yields each ancestor node in order from parent to root' do
          yielded_types = []

          target_node.each_ancestor do |node|
            yielded_types << node.type
          end

          expect(yielded_types).to eq(expected_types)
        end

        it 'returns itself' do
          returned_value = target_node.each_ancestor {}
          expect(returned_value).to equal(target_node)
        end

        context 'and a node type symbol is passed' do
          it 'scans all the ancestor nodes but yields only nodes matching the type' do
            yielded_types = []

            target_node.each_ancestor(:begin) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:begin])
          end
        end

        context 'and multiple node type symbols are passed' do
          it 'scans all the ancestor nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_ancestor(:begin, :def) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:def, :begin])
          end
        end

        context 'and an array including type symbols are passed' do
          it 'scans all the ancestor nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_ancestor([:begin, :def]) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:def, :begin])
          end
        end
      end

      include_examples 'node enumerator', :each_ancestor
    end

    describe '#each_child_node' do
      let(:source) { <<-END }
        def some_method(arg_a, arg_b)
          do_something
        end
      END

      # (def :some_method
      #   (args
      #     (arg :arg_a)
      #     (arg :arg_b))
      #   (send nil :do_something))

      let(:target_node) { root_node.each_node.find(&:def_type?) }
      let(:expected_types) { [:args, :send] }

      context 'when a block is given' do
        it 'yields each child node' do
          yielded_types = []

          target_node.each_child_node do |node|
            yielded_types << node.type
          end

          expect(yielded_types).to eq(expected_types)
        end

        it 'returns itself' do
          returned_value = target_node.each_child_node {}
          expect(returned_value).to equal(target_node)
        end

        context 'and a node type symbol is passed' do
          it 'scans all the child nodes but yields only nodes matching the type' do
            yielded_types = []

            target_node.each_child_node(:send) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send])
          end
        end

        context 'and multiple node type symbols are passed' do
          it 'scans all the child nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_child_node(:send, :args) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:args, :send])
          end
        end

        context 'and an array including type symbols are passed' do
          it 'scans all the child nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_child_node([:send, :args]) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:args, :send])
          end
        end
      end

      include_examples 'node enumerator', :each_child_node
    end

    describe '#each_descendant' do
      let(:source) { <<-END }
        class SomeClass
          attr_reader :some_attr

          def some_method(arg_a, arg_b)
            do_something
          end
        end
      END

      # (class
      #   (const nil :SomeClass) nil
      #   (begin
      #     (send nil :attr_reader
      #       (sym :some_attr))
      #     (def :some_method
      #       (args
      #         (arg :arg_a)
      #         (arg :arg_b))
      #       (send nil :do_something))))

      let(:target_node) { root_node }
      let(:expected_types) { [:const, :begin, :send, :sym, :def, :args, :arg, :arg, :send] }

      context 'when a block is given' do
        it 'yields each descendant node with depth first order' do
          yielded_types = []

          target_node.each_descendant do |node|
            yielded_types << node.type
          end

          expect(yielded_types).to eq(expected_types)
        end

        it 'returns itself' do
          returned_value = target_node.each_descendant {}
          expect(returned_value).to equal(target_node)
        end

        context 'and a node type symbol is passed' do
          it 'scans all the descendant nodes but yields only nodes matching the type' do
            yielded_types = []

            target_node.each_descendant(:send) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :send])
          end
        end

        context 'and multiple node type symbols are passed' do
          it 'scans all the descendant nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_descendant(:send, :def) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :def, :send])
          end
        end

        context 'and an array including type symbols are passed' do
          it 'scans all the descendant nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_descendant([:send, :def]) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :def, :send])
          end
        end
      end

      include_examples 'node enumerator', :each_descendant
    end

    describe '#each_node' do
      let(:source) { <<-END }
        class SomeClass
          attr_reader :some_attr

          def some_method(arg_a, arg_b)
            do_something
          end
        end
      END

      # (class
      #   (const nil :SomeClass) nil
      #   (begin
      #     (send nil :attr_reader
      #       (sym :some_attr))
      #     (def :some_method
      #       (args
      #         (arg :arg_a)
      #         (arg :arg_b))
      #       (send nil :do_something))))

      let(:target_node) { root_node }
      let(:expected_types) { [:class, :const, :begin, :send, :sym, :def, :args, :arg, :arg, :send] }

      context 'when a block is given' do
        it 'yields the node itself and each descendant node with depth first order' do
          yielded_types = []

          target_node.each_node do |node|
            yielded_types << node.type
          end

          expect(yielded_types).to eq(expected_types)
        end

        it 'returns itself' do
          returned_value = target_node.each_node {}
          expect(returned_value).to equal(target_node)
        end

        context 'and a node type symbol is passed' do
          it 'scans all the nodes but yields only nodes matching the type' do
            yielded_types = []

            target_node.each_node(:send) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :send])
          end
        end

        context 'and multiple node type symbols are passed' do
          it 'scans all the nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_node(:send, :def) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :def, :send])
          end
        end

        context 'and an array including type symbols are passed' do
          it 'scans all the nodes but yields only nodes matching any of the types' do
            yielded_types = []

            target_node.each_node([:send, :def]) do |node|
              yielded_types << node.type
            end

            expect(yielded_types).to eq([:send, :def, :send])
          end
        end
      end

      include_examples 'node enumerator', :each_node
    end

    describe '#send_type?' do
      subject { root_node.send_type? }

      context 'with send type node' do
        let(:source) { 'do_something' }
        it { is_expected.to be true }
      end

      context 'with non-send type node' do
        let(:source) { 'foo = 1' }
        it { is_expected.to be false }
      end
    end

    describe '#defined_type?' do
      subject { root_node.defined_type? }

      context 'with defined? type node' do
        let(:source) { 'defined?(Foo)' }
        it { is_expected.to be true }
      end

      context 'non-defined? type node' do
        let(:source) { 'foo = 1' }
        it { is_expected.to be false }
      end
    end
  end
end
