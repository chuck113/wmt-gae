<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml" style="height:100%;margin:0">
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
  <title>WheresMyTube.com</title>
  <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=<%=getServletConfig().getServletContext().getInitParameter("GoogleMapsKey") %>" type="text/javascript"></script>
  <script src="javascripts/prototype.js" type="text/javascript"></script>
  <script src="javascripts/scriptaculous.js" type="text/javascript"></script>
  <link type="text/css" rel="stylesheet" href="/stylesheets/main.css" />    
</head>
<body onunload="GUnload()" onload="loadMap()" style="height:100%;margin:0">

<div id="navigation"  style="width: 300px; height: 100%;">
  <h1>Where's My Tube?</h1>
    <p><b>A realtime view of the London Underground.</b></p>

    <br/>
    <div>
    <p>This site is a mash-up between Google Maps and the TFL Live Arriaval Boards site.</p>
        <table>
            <tr><td><a href="http://maps.google.uk">Google Maps</a></td></tr>
            <tr><td><a href="http://www.tfl.gov.uk/tfl/livetravelnews/departureboards/">TFL Departure Boards</a></td></tr>
        </table>
    <br/><br/>
        <p> <img src="/images/up4.png" alt="up" /></br>
            Red markers represent Underground trains and the arrow inside the marker indicates the train's direction.</p>
    </div>
    <div id="liveInfo" style="display:none; width:240px; height:60px; background:#00CC00; border:1px solid #333;">loading trains..</div>

    <div id="footer">
        <p style="font-size:7">For educational purposes only; this site is in no way connected to TFL. 
        <a href="mailto:tube@charleskubicek.com">contact</a></p>
        <img src="http://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine" />
    </div>
</div>
<div id="content" style="height: 100%;">
</div>

<script type="text/javascript">

var branchesToGet=["victoria", "jubilee"]

function state() {
  this.trainsOnMap = {}
  this.trainsOnMapToggle = {}

  for (var i = 0; i < branchesToGet.length; i++) {
    this.trainsOnMap[branchesToGet[i]] = []
  }
}

var imagesFolder = <%=getServletConfig().getServletContext().getInitParameter("com.web.imagesFolder")%>
var appContext = <%=getServletConfig().getServletContext().getInitParameter("com.web.appContext")%>

var myState = new state()
var map = null

var directonImageDict = {};
directonImageDict["Southbound"] = imagesFolder+"/down4.png";
directonImageDict["Northbound"] = imagesFolder+"/up4.png";
directonImageDict["Westbound"] = imagesFolder+"/left5.png";
directonImageDict["Eastbound"] = imagesFolder+"/right5.png";

lineColourDict = {}
lineColourDict["northern"] = '#000000'
lineColourDict["victoria"] = '#009FE0'
lineColourDict["jubilee"] = '#8F989E'
lineColourDict["bakerloo"] = '#AE6118'
lineColourDict["metropolitan"] = '#893267'

var useLocalServerData = getURLParam("local")

function infoViewerState(){
    this.branchesWaitingFor={}
     this.branchesWaitingFor["victoria"]=false
    this.branchesWaitingFor["jubilee"]=false
}

var myInfoViewerState = new infoViewerState()

function makeBranchesWaitingString(){
   var st = "";
   for (var i in myInfoViewerState.branchesWaitingFor){
       if(myInfoViewerState.branchesWaitingFor[i]){
           st += " "+i +"<br/>\n";
       }
   }

   if(st == "")return "";
   else return "getting data for:<br/>\n"+st; 
}

// would be synchronized!
function addBranchWaitingFor(branch){
   myInfoViewerState.branchesWaitingFor[branch] = true;
   updateInfoBox();
}

function removeBranchWaitingFor(branch){
    myInfoViewerState.branchesWaitingFor[branch] = false;
    updateInfoBox();
}

function updateInfoBox(){
   var infoString = makeBranchesWaitingString();
    if(infoString.length > 0){
        document.getElementById('liveInfo').innerHTML = infoString;
        $('liveInfo').appear();
    }else {
       $('liveInfo').hide();
    }
}

function stationIcon(){
      var icon = new GIcon();
      icon.image = imagesFolder+"/station.png";
      icon.iconSize = new GSize(14, 14);
      icon.shadow = "";
      icon.iconAnchor = new GPoint(7, 7);
      icon.infoWindowAnchor = new GPoint(6, 10);

      return icon;
}

/**
 * TODO optimize by downloading muliple/all lines
 * @param line
 */
function drawStations(line) {
    var url = appContext+"/rest/stations/" + line
    var icon = stationIcon();
    
    GDownloadUrl(url, function(data, responseCode) {
        var stationsObj = eval('(' + data + ')');
        lines = [];
        for (var i = 0; i < stationsObj.stations.stationsArray.length; i++) {
            stationObj = stationsObj.stations.stationsArray[i];
            var point = new GLatLng(stationObj.lat, stationObj.lng);
            lines.push(new GLatLng(stationObj.lat, stationObj.lng));

            map.addOverlay(makeStationMarker(point, stationObj, line, icon));
        }

        var polyLine = new GPolyline(lines, lineColourDict[line], 4, 1)
        map.addOverlay(polyLine);
    })
}


//* uses googles download url to load json into map
function loadTrains(branch) {
    var url = appContext+"/rest/branches/" + branch
    //url = test ? url + "?testMode=1" : url
    url = useLocalServerData ? url + "?local=true" : url
    addBranchWaitingFor(branch)

    GDownloadUrl(url, function(data, responseCode) {
        removeBranchPointsFromMap(branch)
        var pointsObj = eval('(' + data + ')');
        var trainMarkers = []

        for (var i = 0; i < pointsObj.points.pointsArray.length; i++) {
            pointObj = pointsObj.points.pointsArray[i]
            var point = new GLatLng(pointObj.lat, pointObj.lng);

            var marker = new createMarker(point, pointObj.description, pointObj.direction, "false");
            trainMarkers.push(marker)
        }

        myState.trainsOnMap[branch] = trainMarkers
        myState.trainsOnMapToggle[branch] = true        
        pauseBeforeAddingTrainsBackToMap(branch)        
    })

}

function pauseBeforeAddingTrainsBackToMap(branch){
    setTimeout(function() {
        addBranchPointsToMap(branch)
        removeBranchWaitingFor(branch)
     }, 1000);
}

//* gets the XMLHttpRequest browser neutrally
function getHTTPObject() {
  if (typeof XMLHttpRequest != 'undefined') {
    return new XMLHttpRequest();
  }
  try {
    return new ActiveXObject("Msxml2.XMLHTTP");
  } catch (e) {
    try {
      return new ActiveXObject("Microsoft.XMLHTTP");
    } catch (e) {
    }
  }
  return false;
}

function createTrainMarker(direction, multiple){
  var icon = new GIcon();
  icon.image = directonImageDict[direction];
  icon.iconSize = new GSize(20, 34);
  icon.shadow = "";
  //if (multiple == "true")
  //  icon.iconAnchor = new GPoint(5, 34);
  //else
    icon.iconAnchor = new GPoint(10, 34);

  icon.infoWindowAnchor = new GPoint(6, 10);

  return icon;
}


function stationIcon() {
  var icon = new GIcon();
  icon.image = imagesFolder+"/station.png";
  icon.iconSize = new GSize(14, 14);
  icon.shadow = "";
  icon.iconAnchor = new GPoint(7, 7);
  icon.infoWindowAnchor = new GPoint(6, 10);

  return icon;
}

function makeStationMarker(point, stationObj, line, icon) {
  var marker = new GMarker(point, icon)

  GEvent.addListener(marker, "click", function() {
    marker.openInfoWindowHtml(stationObj.name+"<br/><p><a href='http://www.tfl.gov.uk/tfl/livetravelnews/departureboards/tube/default.asp?LineCode="+line+"&StationCode="+stationObj.code+"'>Go to live departure board</a></p>");
  });

  return marker
}


function createMarker(point, text, direction, multiple) {
  var marker = new GMarker(point, createTrainMarker(direction, multiple));

  GEvent.addListener(marker, "click", function() {
    marker.openInfoWindowHtml(text);
  });
  return marker;
}


function loadMap() {
  if (GBrowserIsCompatible()) {
      map = new GMap2(document.getElementById("content"));
      map.addControl(new GLargeMapControl());
      map.addControl(new GMapTypeControl());

      map.setCenter(new GLatLng(51.5183, -0.1246), 12);
      //branch = 'victoria'
      for (var i=0; i<branchesToGet.length; i++){
        drawStations(branchesToGet[i])
        loadTrains(branchesToGet[i])

        /**
        * Get all trains to start with, then start polling every 60 secs for new trains,
        * but stagger the polls by 10 seconds each (assuming 2 branches)
        */
        startPolling(((10 * (i+1)) * 1000), branchesToGet[i])  
      }
  }
}

function startPolling(waitTime, branch){
     setTimeout(function() {
        loadTrains(branch)
        reloadBranchAfterTimeout(branch)
        return;
     }, waitTime);
}

function reloadBranchAfterTimeout(branch){
    setTimeout(function() {
        loadTrains(branch)
        reloadBranchAfterTimeout(branch)
        return;
    }, (60 * 1000));
}

function addBranchPointsToMap(key) {
  myState.trainsOnMap[key].each(function(item, index){
     map.addOverlay(item);
  });
}

function removeBranchPointsFromMap(key) {
  myState.trainsOnMap[key].each(function(item, index){
    map.removeOverlay(item);
  });
}

// taken from http://mattwhite.me/11tmr.nsf/D6Plinks/MWHE-695L9Z
function getURLParam(strParamName){
  var strReturn = "";
  var strHref = window.location.href;
  if ( strHref.indexOf("?") > -1 ){
    var strQueryString = strHref.substr(strHref.indexOf("?")).toLowerCase();
    var aQueryString = strQueryString.split("&");
    for ( var iParam = 0; iParam < aQueryString.length; iParam++ ){
      if (aQueryString[iParam].indexOf(strParamName.toLowerCase() + "=") > -1 ){
        var aParam = aQueryString[iParam].split("=");
        strReturn = aParam[1];
        break;
      }
    }
  }
  return unescape(strReturn);
}


//]]>
</script>
</body>
</html>
