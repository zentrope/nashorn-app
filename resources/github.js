//
// Example script using Github API
//
// You must set up a github.token property to make this work
//

const FNS = Java.type('com.bobo.nashorn.Functions');
const token = FNS.lookup("github.token");
const github = 'https://api.github.com/graphql';
const org = FNS.lookup("github.org", "clojure-emacs");
const project = FNS.lookup("github.project", "cider");


// ECMAScript 5.1 -- for JDK 8 with a few extras (let, const)

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
  return FNS.format(q, owner, name, last);
}

function pollIssues(owner, name, last) {

  var query = JSON.stringify(
    {"query" : findIssues(owner, name, last)}
  );

  var headers = {"Authorization": "bearer " + token};
  var result = FNS.httpPost(github, headers, query);

  if (result.status != 200) {
    print("ERROR: ", result.status);
    return;
  }

  pprint(result.body);

  // The prints can be captured by the evaluator. The result of the
  // last expression is returned as the result of the eval.
  // return "done";
}


pollIssues(org, project, 3);
