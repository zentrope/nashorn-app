//
// Get a reference to the only package scripts have access
// to. These supply convenience functions not available in
// JavaScript.
//
var FNS = Java.type('com.bobo.nashorn.Functions');

// ECMAScript 5.1 -- for JDK 8

function pprint(obj) {
  var data = obj;
  if (typeof obj === 'string' || obj instanceof String) {
    data = JSON.parse(obj);
  }
  print(JSON.stringify(data, null, 2));
}

function findIssues(owner, name, last) {
  var q = "query " +
      "{ repository(owner:\"%s\", name: \"%s\") {" +
      "   owner { login url } name primaryLanguage { name } "+
      "   issues(first: %s, orderBy:{field: CREATED_AT, direction: DESC} , states: [OPEN]) " +
      "     { edges { node { author { login } title url }}}}}";
  return FNS.sprintf(q, owner, name, last);
}

function pollIssues(owner, name, last) {
  var token = FNS.lookup("github_token");
  var github = 'https://api.github.com/graphql';

  var query = JSON.stringify(
    {"query" : findIssues(owner, name, last)}
  );

  var headers = {"Authorization": "bearer " + token};
  var result = FNS.httpPost(github, headers, query);

  if (result.status != 200) {
    print("ERROR: ", result.status);
    return;
  }

  print("QUERY RESULT");
  pprint(result.body);
}

pollIssues("clojure-emacs", "cider", 13);
