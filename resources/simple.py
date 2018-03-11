# For dev testing to see if Jython works at all
from com.bobo.nashorn import Functions as FNS

def searchDuckDuckGo(terms):
    url = FNS.format("http://api.duckduckgo.com/?q=%s&format=json", terms)
    return FNS.httpGet(url, {"Accept": "text/html"})

result = searchDuckDuckGo("python")
data = FNS.fromJSON(result)

print "Search:", data.getString("Heading")

related = data.getJSONArray("RelatedTopics")

for r in related:
    if r.has("Text"):
        print " - ", r.getString("Text")
    if r.has("Name"):
        print " - Topic: ", r.getString("Name")
        topics = r.getJSONArray("Topics")
        for t in topics:
            if t.has("Text"):
                print " -- ", t.getString("Text")
