describe('failing afterAll', function() {
  afterAll(function() {
    throw 'afterAll go boom';
  });

  it('is fine otherwise', function() {
  });
});
