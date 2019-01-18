const express = require('express')
const opn = require('opn')
var https = require('https');
var http = require('http');
var fs = require('fs');

const app = express()
const port = 8080

function allowCrossDomain(req, res, next) {
  res.header('Access-Control-Allow-Origin', '*')
  res.header('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
  res.header('Access-Control-Allow-Headers', 'Content-Type')
  next()
}

app.use(allowCrossDomain)

app.get('/redirect', function(req, res) {
  res.redirect('coolappauth:' + req.query.authResponse);
})

app.use('/', express.static(__dirname + '/public'))
/*app.listen(port, (err) => {
  if (err) {
    return console.log('something bad happened', err)
  }
  console.log(`server is listening on ${port}`)
  opn('http://localhost:8080')
})*/

var options = {
  key: fs.readFileSync('/etc/letsencrypt/live/innermatrix.co/privkey.pem'),
  cert: fs.readFileSync('/etc/letsencrypt/live/innermatrix.co/fullchain.pem')
};

http.createServer(app).listen(8080);
https.createServer(options, app).listen(443);

console.log(`server is listening on ${port}`)
