#! /usr/bin/env python3
"""
A minimalistic web server for the Droshed project.
@author chilowi at u-pem.fr

How to use it?
> ./droshserver.py <port of the server> <model directory> <data directory>
Prior running, you must have python3 and flask installed.

The model directory is where all the sheet models are stored as XML files.
It contains also a ".auth" file that contain "login:password" tuples with passwords in plain text (very bad but this server is only designed for testing).

The data directory is where all the data are saved. For exemple when the user "foo" sends the incremental delta for the sheet "project" for the version 42, the content is stored in :
datadir/project/foo/42

For all the requests, you must authenticate using one of the "login:password" tuple of the ".auth" file.

First we retrieve the model for the sheet "project":
GET http://example.com/project/model

Then we retrieve the last known version for the data for our own user:
GET http://example.com/project/data/lastversion

If the last version is 42 and we don't have any data changeset stored locally, we must retrieve all the changes up to version 42:
GET http://example.com/project/data/0
GET http://example.com/project/data/1
...
GET http://example.com/project/data/42

We edit the content of the sheet and produce a new version 43: we can upload this version to the server (the XML content is supplied in the body of the request):
PUT http://example.com/project/data/43

Now "http://example.com/project/data/lastversion" should return 43
"""

from functools import wraps
from flask import Flask, request, make_response, Response, jsonify

import os, sys, base64, glob, json

app = Flask(__name__)

class Item(object):
    name = ""
    version = 0

    # The class "constructor" - It's actually an initializer 
    def __init__(self, name, version):
        self.name = name
        self.version = version

def make_item(name, version):
    item = Item(name, version)
    return item

def item_tostring(item):
    str = "{name:"
    str+= item.name
    str+= "}"
    return str

def authenticated(f):

    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        print(auth)
        if not auth or app.config['users'].get(auth.username) != auth.password:
            return Response('Authentication error', 401,{app.config['users'].get(auth.username): auth.password})
        return f(*args, **kwargs)
    return decorated

@app.route("/login",methods=['GET'])
@authenticated
def checkAuthentifation():
	return Response('Authentication Ok', 200, {'WWW-Authenticate': 'Basic realm="Login Required accepted"'})

#@app.route('/', methods=['PUT'], defaults={'path': ''})
#@app.route('/<path:path>', methods=['PUT'])
#@authenticated
#def getListOfFolder(path):
#    return getResponseWithJson(path)

@app.route('/', methods=['GET'], defaults={'path': ''})
@app.route('/<path:path>', methods=['GET'])
@authenticated
def getFileContent(path):
	command = request.args.get('info')
	if(command == 'version'):
		with open(path) as json_data:
    			d = json.load(json_data)
			data = {}
			data['version'] = d['version']
			return app.response_class(
	        	response=json.dumps(data),
        		status=200,
        		mimetype='application/json')



	if(os.path.isfile(path)):
		#jsonData = {'name': os.path.basename(path)}
		with open(path, "r") as file:
    			lines = file.read().split("\n")	
        	#jsonData['content'] = ''.join(lines)
		#print(jsonData['content'])
		return app.response_class(
        	response=lines,
        	status=200,
        	mimetype='application/json')
	else:
		return Response('Not found', 404, {'WWW-Authenticate': 'Bad Path."'})

def getVersionFile(path):
	json_data=open(path).read()
	data = json.loads(json_data)
	

@app.route("/model",methods=['GET'])
@authenticated
def getlistmodel():
	return getResponseWithJson("model")

@app.route("/data",methods=['GET'])
@authenticated
def getlistdata():
	return getResponseWithJson("datasheet")

def getResponseWithJson(nameFolder):
	return app.response_class(
        response=json.dumps(path_to_dict(nameFolder), indent=2),
        status=200,
        mimetype='application/json')

def path_to_dict(path):
    d = {'name': os.path.basename(path)}
    d['path'] = path
    if os.path.isdir(path):
        d['type'] = "directory"
        d['children'] = [path_to_dict(os.path.join(path,x)) for x in os.listdir(path)]
    else:
        d['type'] = "file"
    return d

@app.route("/<sheetname>/model")
@authenticated
def getmodel(sheetname):
	if sheetname.startswith("."): # to protect the password file
		return "not found", 404
	try:
		with open(os.path.join(app.config["modeldir"], sheetname)) as f:
			return make_response(f.read())
	except FileNotFoundError:
		return "not found", 404
		
def _getlastversion(sheetname, username):
	try:
		return int(sorted(os.listdir(os.path.join(app.config["datadir"], sheetname, username)), key=int)[-1])
	except:
		return -1
		
@app.route("/<sheetname>/data/lastversion")
@authenticated
def getlastversion(sheetname):
	lv = _getlastversion(sheetname, request.authorization.username)
	if lv >= 0:
		return str(lv)
	else:
		return "no version available", 404
		
@app.route("/<sheetname>/data/<int:version>")
@authenticated
def getversion(sheetname, version):
	try:
		with open(os.path.join(app.config["datadir"], sheetname, request.authorization.username, str(version))) as f:
			return f.read()
	except:
		return "unavailable version", 404
		
@app.route("/<sheetname>/data/<int:version>", methods=["PUT"])
@authenticated
def putversion(sheetname, version):
	lv = _getlastversion(sheetname, request.authorization.username)
	filepath = os.path.join(app.config["datadir"], sheetname, request.authorization.username, str(version))
	if version < 0 or version not in (lv, lv+1):
		return "invalid version number", 500
	else:
		dirname = os.path.dirname(filepath)
		if not os.path.exists(dirname):
			os.makedirs(dirname)
		with open(filepath, "wb") as f:
			f.write(request.data)
		return "registered", 200

@app.route('/', methods=['PUT'], defaults={'path': ''})
@app.route('/<path:path>', methods=['PUT'])
@authenticated
def getUploadFile(path):
	j = json.loads(request.data)
	fileName = j['path']
	if fileName.startswith('data') or fileName.startswith('model'):
		writeJsonStringToFile(fileName,j)
		return "upload OK", 200
	else:
		return "upload problem", 403

def writeStringToFile(fileName,content):
	file = open(fileName,"w")	 
	file.write(content)
	file.close()

def writeJsonStringToFile(fileName,jj):
	file = open(fileName,"w")	 
	file.write(json.dumps(jj, indent=4, sort_keys=True))
	file.close()

if __name__ == "__main__":
	loginDir = "login"
	try:
		(port, modeldir, datadir) = (int(sys.argv[1]), sys.argv[2], sys.argv[3])
	except:
		print("Usage: {} port modeldir datadir".format(sys.argv[0]))
		sys.exit(-1)
	else:
		app.config.update(modeldir=modeldir, datadir=datadir)
		# load the users
		users = {}
		with open(os.path.join(loginDir, ".auth")) as f:
			for line in filter(lambda x: x.strip() and x.find(":") >= 0, f):
				(login, password) = line.split(":", 1)
				users[login.strip()] = password.strip()
		app.config["users"] = users
		print(app.config["users"])
		print(password.strip())
		# run the server
		app.run(host="0.0.0.0",port=port)
