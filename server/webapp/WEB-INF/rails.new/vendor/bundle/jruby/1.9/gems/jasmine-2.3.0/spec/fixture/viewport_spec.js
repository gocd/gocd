describe('having a viewport', function() {
  it('should have a small viewport', function() {
    expect(window.innerWidth).toBe(200);
    expect(window.innerHeight).toBe(400);
  });
});
