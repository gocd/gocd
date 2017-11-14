require "spec_helper"

describe JsRoutes, "compatibility with Rails"  do

  before(:each) do
    evaljs(JsRoutes.generate({}))
  end

  it "should generate collection routing" do
    expect(evaljs("Routes.inboxes_path()")).to eq(test_routes.inboxes_path())
  end

  it "should generate member routing" do
    expect(evaljs("Routes.inbox_path(1)")).to eq(test_routes.inbox_path(1))
  end

  it "should raise error if required argument is not passed" do
    expect { evaljs("Routes.thing_path()") }
      .to raise_error('Route parameter missing: id')
    expect { evaljs("Routes.search_path()") }
      .to raise_error('Route parameter missing: q')
    expect { evaljs("Routes.book_path()") }
      .to raise_error('Route parameter missing: title')
    expect { evaljs("Routes.book_title_path()") }
      .to raise_error('Route parameter missing: title')
  end

  it "should support 0 as a member parameter" do
    expect(evaljs("Routes.inbox_path(0)")).to eq(test_routes.inbox_path(0))
  end

  it "should generate nested routing with one parameter" do
    expect(evaljs("Routes.inbox_messages_path(1)")).to eq(test_routes.inbox_messages_path(1))
  end

  it "should generate nested routing" do
    expect(evaljs("Routes.inbox_message_path(1,2)")).to eq(test_routes.inbox_message_path(1, 2))
  end

  it "should generate routing with format" do
    expect(evaljs("Routes.inbox_path(1, {format: 'json'})")).to eq(test_routes.inbox_path(1, :format => "json"))
  end

  it "should support routes with reserved javascript words as parameters" do
    expect(evaljs("Routes.object_path(1, 2)")).to eq(test_routes.object_path(1,2))
  end

  it "should support routes with trailing_slash" do
    expect(evaljs("Routes.inbox_path(1, {trailing_slash: true})")).to eq(test_routes.inbox_path(1, trailing_slash: true))
  end

  it "should support url anchor given as parameter" do
    expect(evaljs("Routes.inbox_path(1, {anchor: 'hello'})")).to eq(test_routes.inbox_path(1, :anchor => "hello"))
  end

  it "should support url anchor and get parameters" do
    expect(evaljs("Routes.inbox_path(1, {expanded: true, anchor: 'hello'})")).to eq(test_routes.inbox_path(1, :expanded => true, :anchor => "hello"))
  end

  it "should use irregular ActiveSupport pluralizations" do
    expect(evaljs("Routes.budgies_path()")).to eq(test_routes.budgies_path)
    expect(evaljs("Routes.budgie_path(1)")).to eq(test_routes.budgie_path(1))
    expect(evaljs("Routes.budgy_path")).to eq(nil)
    expect(evaljs("Routes.budgie_descendents_path(1)")).to eq(test_routes.budgie_descendents_path(1))
  end

  describe "when route has defaults" do
    it "should support route default format" do
      expect(evaljs("Routes.api_purchases_path()")).to eq(test_routes.api_purchases_path)
    end

    it 'should support route default subdomain' do
      expect(evaljs("Routes.backend_root_path()")).to eq(test_routes.backend_root_path)
    end

    it "should support default format override" do
      expect(evaljs("Routes.api_purchases_path({format: 'xml'})")).to eq(test_routes.api_purchases_path(format: 'xml'))
    end

    it "should support default format override by passing it in args" do
      expect(evaljs("Routes.api_purchases_path('xml')")).to eq(test_routes.api_purchases_path('xml'))
    end

    it "doesn't apply defaults to path" do
      expect(evaljs("Routes.with_defaults_path()")).to eq(test_routes.with_defaults_path)
    end
  end

  context "with rails engines" do
    it "should support simple route" do
      expect(evaljs("Routes.blog_app_posts_path()")).to eq(blog_routes.posts_path())
    end

    it "should support root route" do
      expect(evaljs("Routes.blog_app_path()")).to eq(test_routes.blog_app_path())
    end

    it "should support route with parameters" do
      expect(evaljs("Routes.blog_app_post_path(1)")).to eq(blog_routes.post_path(1))
    end
    it "should support root path" do
      expect(evaljs("Routes.blog_app_root_path()")).to eq(blog_routes.root_path)
    end
    it "should support single route mapping" do
      expect(evaljs("Routes.support_path({page: 3})")).to eq(test_routes.support_path(:page => 3))
    end
  end

  it "shouldn't require the format" do
    expect(evaljs("Routes.json_only_path({format: 'json'})")).to eq(test_routes.json_only_path(:format => 'json'))
  end

  it "should serialize object with empty string value" do
    expect(evaljs("Routes.inboxes_path({a: '', b: 1})")).to eq(test_routes.inboxes_path(:a => '', :b => 1))
  end

  it "should support utf-8 route" do
    expect(evaljs("Routes.hello_path()")).to eq(test_routes.hello_path)
  end

  it "should support root_path" do
    expect(evaljs("Routes.root_path()")).to eq(test_routes.root_path)
  end

  describe "get paramters" do
    it "should support simple get parameters" do
      expect(evaljs("Routes.inbox_path(1, {format: 'json', lang: 'ua', q: 'hello'})")).to eq(test_routes.inbox_path(1, :lang => "ua", :q => "hello", :format => "json"))
    end

    it "should support array get parameters" do
      expect(evaljs("Routes.inbox_path(1, {hello: ['world', 'mars']})")).to eq(test_routes.inbox_path(1, :hello => [:world, :mars]))
    end

    it "should support nested get parameters" do
      expect(evaljs("Routes.inbox_path(1, {format: 'json', env: 'test', search: { category_ids: [2,5], q: 'hello'}})")).to eq(
        test_routes.inbox_path(1, :env => 'test', :search => {:category_ids => [2,5], :q => "hello"}, :format => "json")
      )
    end

    it "should support null and undefined parameters" do
      expect(evaljs("Routes.inboxes_path({uri: null, key: undefined})")).to eq(test_routes.inboxes_path(:uri => nil, :key => nil))
    end

    it "should escape get parameters" do
      expect(evaljs("Routes.inboxes_path({uri: 'http://example.com'})")).to eq(test_routes.inboxes_path(:uri => 'http://example.com'))
    end

  end


  context "routes globbing" do
    it "should be supported as parameters" do
      expect(evaljs("Routes.book_path('thrillers', 1)")).to eq(test_routes.book_path('thrillers', 1))
    end

    it "should support routes globbing as array" do
      expect(evaljs("Routes.book_path(['thrillers'], 1)")).to eq(test_routes.book_path(['thrillers'], 1))
    end

    it "should bee support routes globbing as array" do
      expect(evaljs("Routes.book_path([1, 2, 3], 1)")).to eq(test_routes.book_path([1, 2, 3], 1))
    end

    it "should bee support routes globbing as hash" do
      expect(evaljs("Routes.book_path('a_test/b_test/c_test', 1)")).to eq(test_routes.book_path('a_test/b_test/c_test', 1))
    end

    it "should support routes globbing as array with optional params" do
      expect(evaljs("Routes.book_path([1, 2, 3, 5], 1, {c: '1'})")).to eq(test_routes.book_path([1, 2, 3, 5], 1, { :c => "1" }))
    end

    it "should support routes globbing in book_title route as array" do
      expect(evaljs("Routes.book_title_path('john', ['thrillers', 'comedian'])")).to eq(test_routes.book_title_path('john', ['thrillers', 'comedian']))
    end

    it "should support routes globbing in book_title route as array with optional params" do
      expect(evaljs("Routes.book_title_path('john', ['thrillers', 'comedian'], {some_key: 'some_value'})")).to eq(test_routes.book_title_path('john', ['thrillers', 'comedian'], {:some_key => 'some_value'}))
    end

    it "should support required paramters given as options hash" do
      expect(evaljs("Routes.search_path({q: 'hello'})")).to eq(test_routes.search_path(:q => 'hello'))
    end

    it "should support nested object null parameters" do
      expect(evaljs("Routes.inboxes_path({hello: {world: null}})")).to eq(test_routes.inboxes_path(:hello => {:world => nil}))
    end
  end

  context "using optional path fragments" do
    context "including not optional parts" do
      it "should include everything that is not optional" do
        expect(evaljs("Routes.foo_path()")).to eq(test_routes.foo_path)
      end
    end

    context "but not including them" do
      it "should not include the optional parts" do
        expect(evaljs("Routes.things_path()")).to eq(test_routes.things_path)
        expect(evaljs("Routes.things_path({ q: 'hello' })")).to eq(test_routes.things_path(q: 'hello'))
      end

      it "should not require the optional parts as arguments" do
        expect(evaljs("Routes.thing_path(null, 5)")).to eq(test_routes.thing_path(nil, 5))
      end

      it "should treat undefined as non-given optional part" do
        expect(evaljs("Routes.thing_path(5, {optional_id: undefined})")).to eq(test_routes.thing_path(5, :optional_id => nil))
      end

      it "should raise error when passing non-full list of arguments and some query params" do
        expect { evaljs("Routes.thing_path(5, {q: 'hello'})") }
          .to raise_error('Route parameter missing: id')
      end

      it "should treat null as non-given optional part" do
        expect(evaljs("Routes.thing_path(5, {optional_id: null})")).to eq(test_routes.thing_path(5, :optional_id => nil))
      end

      it "should work when passing required params in options" do
        expect(evaljs("Routes.thing_deep_path({second_required: 1, third_required: 2})")).to eq(test_routes.thing_deep_path(second_required: 1, third_required: 2))
      end

      it "should skip leading and trailing optional parts" do
        skip if Rails.version < '4'
        expect(evaljs("Routes.thing_deep_path(1, 2)")).to eq(test_routes.thing_deep_path(1, 2))
      end
    end

    context "and including them" do
      if Rails.version < '4'
        it "should fail when insufficient arguments are given" do
          expect { evaljs("Routes.thing_deep_path(1)")}
            .to raise_error('Route parameter missing: second_required')
          expect { evaljs("Routes.thing_deep_path(1,2)")}
            .to raise_error('Route parameter missing: third_required')
        end
      end

      it "should include the optional parts" do
        expect(evaljs("Routes.things_path({optional_id: 5})")).to eq(test_routes.things_path(:optional_id => 5))
        expect(evaljs("Routes.things_path(5)")).to eq(test_routes.things_path(5))
        expect(evaljs("Routes.thing_deep_path(1, { third_required: 3, second_required: 2 })")).to eq(test_routes.thing_deep_path(1, third_required: 3, second_required: 2))
        expect(evaljs("Routes.thing_deep_path(1, { third_required: 3, second_required: 2, forth_optional: 4 })")).to eq(test_routes.thing_deep_path(1, third_required: 3, second_required: 2, forth_optional: 4))
        expect(evaljs("Routes.thing_deep_path(2, { third_required: 3, first_optional: 1 })")).to eq(test_routes.thing_deep_path(2, third_required: 3, first_optional: 1))
        expect(evaljs("Routes.thing_deep_path(3, { first_optional: 1, second_required: 2 })")).to eq(test_routes.thing_deep_path(3, first_optional: 1, second_required: 2))
        expect(evaljs("Routes.thing_deep_path(3, { first_optional: 1, second_required: 2, forth_optional: 4 })")).to eq(test_routes.thing_deep_path(3, first_optional: 1, second_required: 2, forth_optional: 4))
        expect(evaljs("Routes.thing_deep_path(4, { first_optional: 1, second_required: 2, third_required: 3 })")).to eq(test_routes.thing_deep_path(4, first_optional: 1, second_required: 2, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(1, 2, { third_required: 3 })")).to eq(test_routes.thing_deep_path(1, 2, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(1,2, {third_required: 3, q: 'bogdan'})")).to eq(test_routes.thing_deep_path(1,2, {third_required: 3, q: 'bogdan'}))
        expect(evaljs("Routes.thing_deep_path(1, 2, { forth_optional: 4, third_required: 3 })")).to eq(test_routes.thing_deep_path(1, 2, forth_optional: 4, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(1, 3, { second_required: 2 })")).to eq(test_routes.thing_deep_path(1, 3, second_required: 2))
        expect(evaljs("Routes.thing_deep_path(1, 4, { second_required: 2, third_required: 3 })")).to eq(test_routes.thing_deep_path(1, 4, second_required: 2, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(2, 3, { first_optional: 1 })")).to eq(test_routes.thing_deep_path(2, 3, first_optional: 1))
        expect(evaljs("Routes.thing_deep_path(2, 3, { first_optional: 1, forth_optional: 4 })")).to eq(test_routes.thing_deep_path(2, 3, first_optional: 1, forth_optional: 4))
        expect(evaljs("Routes.thing_deep_path(2, 4, { first_optional: 1, third_required: 3 })")).to eq(test_routes.thing_deep_path(2, 4, first_optional: 1, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(3, 4, { first_optional: 1, second_required: 2 })")).to eq(test_routes.thing_deep_path(3, 4, first_optional: 1, second_required: 2))
        expect(evaljs("Routes.thing_deep_path(1, 2, 3)")).to eq(test_routes.thing_deep_path(1, 2, 3))
        expect(evaljs("Routes.thing_deep_path(1, 2, 3, { forth_optional: 4 })")).to eq(test_routes.thing_deep_path(1, 2, 3, forth_optional: 4))
        expect(evaljs("Routes.thing_deep_path(1, 2, 4, { third_required: 3 })")).to eq(test_routes.thing_deep_path(1, 2, 4, third_required: 3))
        expect(evaljs("Routes.thing_deep_path(1, 3, 4, { second_required: 2 })")).to eq(test_routes.thing_deep_path(1, 3, 4, second_required: 2))
        expect(evaljs("Routes.thing_deep_path(2, 3, 4, { first_optional: 1 })")).to eq(test_routes.thing_deep_path(2, 3, 4, first_optional: 1))
        expect(evaljs("Routes.thing_deep_path(1, 2, 3, 4)")).to eq(test_routes.thing_deep_path(1, 2, 3, 4))

      end

      context "on nested optional parts" do
        if Rails.version <= "5.0.0"
          # this type of routing is deprecated
          it "should include everything that is not optional" do
            expect(evaljs("Routes.classic_path({controller: 'classic', action: 'edit'})")).to eq(test_routes.classic_path(controller: :classic, action: :edit))
          end
        end
      end
    end
  end

  context "when wrong parameters given" do

    it "should throw Exception if not enough parameters" do
      expect {
        evaljs("Routes.inbox_path()")
      }.to raise_error(js_error_class)
    end
    it "should throw Exception if required parameter is not defined" do
      expect {
        evaljs("Routes.inbox_path(null)")
      }.to raise_error(js_error_class)
    end

    it "should throw Exceptions if when there is too many parameters" do
      expect {
        evaljs("Routes.inbox_path(1,2,3)")
      }.to raise_error(js_error_class)
    end

    it "should throw Exceptions if when pass id with null" do
      expect {
        evaljs("Routes.inbox_path({id: null})")
      }.to raise_error(js_error_class)
    end

    it "should throw Exceptions if when pass to_param with null" do
      expect {
        evaljs("Routes.inbox_path({to_param: null})")
      }.to raise_error(js_error_class)
    end
  end

  context "when javascript engine without Array#indexOf is used" do
    before(:each) do
      evaljs("Array.prototype.indexOf = null")
    end
    it "should still work correctly" do
      expect(evaljs("Routes.inboxes_path()")).to eq(test_routes.inboxes_path())
    end
  end

  context "when arguments are objects" do

    let(:klass) { Struct.new(:id, :to_param) }
    let(:inbox) { klass.new(1,"my") }

    it "should support 0 as a to_param option" do
      expect(evaljs("Routes.inbox_path({to_param: 0})")).to eq(test_routes.inbox_path(0))
    end

    it "should check for options special key" do
      expect(evaljs("Routes.inbox_path({id: 7, q: 'hello', _options: true})")).to eq(test_routes.inbox_path(id: 7, q: 'hello'))
      expect {
        evaljs("Routes.inbox_path({to_param: 7, _options: true})")
      }.to raise_error(js_error_class)
      expect(evaljs("Routes.inbox_message_path(5, {id: 7, q: 'hello', _options: true})")).to eq(test_routes.inbox_message_path(5, id: 7, q: 'hello'))
    end

    it "should check for options special key" do
    end

    it "should support 0 as an id option" do
      expect(evaljs("Routes.inbox_path({id: 0})")).to eq(test_routes.inbox_path(0))
    end

    it "should use id property of the object in path" do
      expect(evaljs("Routes.inbox_path({id: 1})")).to eq(test_routes.inbox_path(1))
    end

    it "should prefer to_param property over id property" do
      expect(evaljs("Routes.inbox_path({id: 1, to_param: 'my'})")).to eq(test_routes.inbox_path(inbox))
    end

    it "should call to_param if it is a function" do
      expect(evaljs("Routes.inbox_path({id: 1, to_param: function(){ return 'my';}})")).to eq(test_routes.inbox_path(inbox))
    end

    it "should call id if it is a function" do
      expect(evaljs("Routes.inbox_path({id: function() { return 1;}})")).to eq(test_routes.inbox_path(1))
    end

    it "should support options argument" do
      expect(evaljs(
        "Routes.inbox_message_path({id:1, to_param: 'my'}, {id:2}, {custom: true, format: 'json'})"
      )).to eq(test_routes.inbox_message_path(inbox, 2, :custom => true, :format => "json"))
    end

    context "when globbing" do
      it "should prefer to_param property over id property" do
        expect(evaljs("Routes.book_path({id: 1, to_param: 'my'}, 1)")).to eq(test_routes.book_path(inbox, 1))
      end

      it "should call to_param if it is a function" do
        expect(evaljs("Routes.book_path({id: 1, to_param: function(){ return 'my';}}, 1)")).to eq(test_routes.book_path(inbox, 1))
      end

      it "should call id if it is a function" do
        expect(evaljs("Routes.book_path({id: function() { return 'technical';}}, 1)")).to eq(test_routes.book_path('technical', 1))
      end

      it "should support options argument" do
        expect(evaljs(
          "Routes.book_path({id:1, to_param: 'my'}, {id:2}, {custom: true, format: 'json'})"
        )).to eq(test_routes.book_path(inbox, 2, :custom => true, :format => "json"))
      end
    end
  end

  context "when specs" do
    it "should show inbox spec" do
      expect(evaljs("Routes.inbox_path.toString()")).to eq('/inboxes/:id(.:format)')
    end

    it "should show inbox spec convert to string" do
      expect(evaljs("'' + Routes.inbox_path")).to eq('/inboxes/:id(.:format)')
    end

    it "should show inbox message spec" do
      expect(evaljs("Routes.inbox_message_path.toString()")).to eq('/inboxes/:inbox_id/messages/:id(.:format)')
    end

    it "should show inbox message spec convert to string" do
      expect(evaljs("'' + Routes.inbox_message_path")).to eq('/inboxes/:inbox_id/messages/:id(.:format)')
    end
  end

  describe "required_params" do
    it "should show inbox spec" do
      expect(evaljs("Routes.inbox_path.required_params").to_a).to eq(["id"])
    end

    it "should show inbox message spec" do
      expect(evaljs("Routes.inbox_message_path.required_params").to_a).to eq(["inbox_id", "id"])
    end
  end


end
