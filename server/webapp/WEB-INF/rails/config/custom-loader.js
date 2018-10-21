module.exports = function (content) {
  if (/Copyright \d+ ThoughtWorks/.test(content) && content.indexOf('/*!') >= 0) {
    throw new Error(`Files copyright by thoughtworks should not contain loud comments (/*!). The content was ${content}`);
  }
  return content;
};
