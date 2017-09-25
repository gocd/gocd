describe JsRoutes, "#default_serializer" do
  
  before(:each) do
    evaljs(JsRoutes.generate({}))
  end

  it "should provide this method" do
    expect(evaljs("Routes.default_serializer({a: 1, b: [2,3], c: {d: 4, e: 5}})")).to eq(
      "a=1&b%5B%5D=2&b%5B%5D=3&c%5Bd%5D=4&c%5Be%5D=5"
    )
  end

end
