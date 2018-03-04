//
var FNS = Java.type('com.bobo.nashorn.Functions');

var API =   FNS.lookup("hipchat.api", "none");
var USER =  FNS.lookup("hipchat.user", "none");
var token = FNS.lookup("hipchat.token", "none").trim();

if (token === "none" || API == "none" || USER == "none") {
  throw "Unable to retrieve hipchat tokens.";
}

var headers = {"Authorization": "Bearer " + token,
               "Content-Type" : "application/json"};

// Convenience

function pprint(obj) {
  var data = obj;
  if (typeof obj === 'string' || obj instanceof String) {
    data = JSON.parse(obj);
  }
  print(JSON.stringify(data, null, 2));
}

function toStr(obj) {
  return JSON.stringify(obj);
}

// API wrappers

function sendMessageToRoom(room, message) {
  let url = API + "room/" + room + "/message";
  let body = toStr({"message" : message});
  print("posting to", url);
  return FNS.httpPost(url, headers, body);
}

function sendMessageToUser(user, message) {
  let url = API + "user/" + user + "/message";
  let body = toStr({"message": message, "message_format" : "text"});
  return FNS.httpPost(url, headers, body);
}

function getRoom(room, path) {
  let url = API + "room/" + room + path;
  return FNS.httpGet(url, headers);
}

let result = sendMessageToUser(USER, "This is from the script manager thing.");
print(result.status);
if (result.body !== "") {
  pprint(result.body);
}
