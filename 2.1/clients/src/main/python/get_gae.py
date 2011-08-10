import urllib2
import sys
import ConfigParser

try: import json #python 2.6 included simplejson as json
except ImportError: import simplejson as json

def doGet(configFile, uri):
    config = ConfigParser.RawConfigParser()
    config.read(configFile)

    HOST = config.get("Connection", "host")
    APIKEY = config.get("Connection", "apikey")

    print("GET https://" + HOST + uri)

    request = urllib2.Request("https://" + HOST + uri)
    request.add_header("x-addama-apikey", APIKEY)
    resp = urllib2.urlopen(request)
    output = resp.read()

    try:
        print json.dumps(json.JSONDecoder().decode(output), sort_keys=True, indent=4)
    except:
        print "generating file for " + uri
        
        filestart = uri.rfind("/") + 1
        fileend = len(uri)
        filename = uri[filestart:fileend]
        fsock = open(filename, 'w')
        sys.stdout = fsock
        print output
        fsock.close()

if __name__ == "__main__":
    doGet(sys.argv[1], sys.argv[2])
