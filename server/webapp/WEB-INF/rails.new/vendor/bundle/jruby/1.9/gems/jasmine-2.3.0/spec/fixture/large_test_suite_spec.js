function nestedBlock(nestingDepth, index) {
  if (nestingDepth == 0) {
    return;
  } else {
    describe('Suite ' + index + ' at depth ' + nestingDepth, function() {
      it('should be a failing test', function() {
        expect(true).toBe(false);
      });
      it('should be a passing test', function() {
        expect(true).toBe(true);
      });
      nestedBlock(nestingDepth - 1, index);
    });
  }
}

for (i=0; i<2000; i++) {
  nestedBlock(10, i);
}

