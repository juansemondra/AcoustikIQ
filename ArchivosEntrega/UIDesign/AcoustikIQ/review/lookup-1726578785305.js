(function(window, undefined) {
  var dictionary = {
    "239506bf-8455-4088-83ce-a4ebc3af200c": "Venue",
    "a7d6cc25-4b71-4547-835f-1839c6fe2f95": "MagnitudeActivity",
    "742a35eb-4e66-4d43-9eb0-76c255d7754e": "EventActivity",
    "d4fc9f35-8708-449e-bb53-88f484282665": "LevelMeterActivity",
    "1d36ede7-a2ca-4301-bfd1-665e887acde5": "EQRecommendationActivity",
    "4698c810-010e-4173-a8fc-8371a8792180": "DeleteEvent",
    "8e4a471d-8fde-4788-b953-f4433942304a": "LineArray",
    "b467b9bb-b053-4333-b26d-6457b8e2abc9": "PhaseAnalyzerActivity",
    "5b507129-05d2-48e3-9e78-7bfc4d052fc1": "CreateEvent",
    "0d6b82de-1fd3-4def-b401-c0da3479083a": "NewPlace",
    "d12245cc-1680-458d-89dd-4f0d7fb22724": "Screen 1",
    "f4f9b812-e04b-4f98-894b-ab7fb514ad9b": "SpectrumActivity",
    "e7310499-e5e3-4bca-bd65-18356b69534f": "EditEvent",
    "f39803f7-df02-4169-93eb-7547fb8c961a": "Template 1",
    "bb8abf58-f55e-472d-af05-a7d1bb0cc014": "Board 1"
  };

  var uriRE = /^(\/#)?(screens|templates|masters|scenarios)\/(.*)(\.html)?/;
  window.lookUpURL = function(fragment) {
    var matches = uriRE.exec(fragment || "") || [],
        folder = matches[2] || "",
        canvas = matches[3] || "",
        name, url;
    if(dictionary.hasOwnProperty(canvas)) { /* search by name */
      url = folder + "/" + canvas;
    }
    return url;
  };

  window.lookUpName = function(fragment) {
    var matches = uriRE.exec(fragment || "") || [],
        folder = matches[2] || "",
        canvas = matches[3] || "",
        name, canvasName;
    if(dictionary.hasOwnProperty(canvas)) { /* search by name */
      canvasName = dictionary[canvas];
    }
    return canvasName;
  };
})(window);