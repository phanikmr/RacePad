define(['lodash'], function (_) {
  'use strict';

  if (typeof window !== 'object') {
    return {
      load: function (name, parentRequire, onLoad) {
        onLoad(false);
      },
      isOpen: _.constant(false)
    };
  }

  var runningExperiments = _.mapKeys((window.rendererModel || window.editorModel || {}).runningExperiments, function(value, key) {
    return key.toLowerCase();
  });

  function isOpen(name) {
    return runningExperiments[name.toLowerCase()] === 'new';
  }

  return {
    load: function (name, parentRequire, onload) {
      onload(isOpen(name));
    },
    isOpen: isOpen
  };
});
