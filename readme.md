# nashorn-app

The idea is that you provide some basic primitives and limit
JavaScripts to those primitives:

``` javascript
var FNS = Java.type('com.fooko.sandboxed.Functions');

FNS.httpGet(...);
FNS.httpPost(...);
FNS.sprintf(...);
FNS.lookup(...);
FNS.store(...);
FNS.delete(...);

```


This lets you access web-services (perhaps the host's own), store some data.

For configuration, you could mandate an "init" function that returns
an object with specific values:

```javascript
function init() {
  return {
    "crontab": "* * 8 * * *",
    "name": "integration thing",
    "description": "whitelist thingy",
    "version": "1.2.42"
  }
}
```

And maybe a function that must take an identifier supplied by the
host:

``` javascript
var HOST_ID = null;

funtion setId(id) {
  HOST_ID = id;
}
```

Maybe this isn't necessary, but I was thinking it acts as a namespace
for saving local data. The only way around this (that I can think of)
is to use thread-local ID that's accessed by the stuff in `FNS` to
properly sandbox data. Not sure that'll work.

Given the exposed functions are static, you should probably get a
context object back, and pass that to every functions.

``` javascript
var FNS = Java.type('com.bobo.sandboxed.Functions');

var ctx = FNS.getContext({"init": "stuff"});

var apiKey = FNS.lookup(ctx, "api_key");
```

Something like that seems reasonable.

Maybe the name of where the script is stored is the id of the script
such that when it's replaced it can use the old data.
