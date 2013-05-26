/**
 * This script handles client-side events and server-side DOM updates. Upon page
 * load, fluInit() must be called. => I got the flu, baby :)
 * 
 * @author uli
 */

var lastCallTime = 0;
var events = "";

//event Type of the current ajax request (initialized with -1)
//when an ajax call gets executed this is the eventType @see FEventType
var currentEventType = -1;
var global = new Object();
var debugEnabled = false;




/*******************************************************************************
 * 
 * General Functions
 * 
 ******************************************************************************/


// Convenience functions used by the backend
if(typeof $ != 'function') // check if not defined by prototype before
	$ = function(id)
	{
		//if first char is . by Class Name is used
		var firstChar = id.charAt(0);
		if(firstChar == ".")
			return getElementsByClassName(id.substring(1));
		return document.getElementById(id); 
	};
/* this is not used at all, is it?
function $$(el, row, col) { return el.rows[row].cells[col]; }
*/

//redirects
function $r(url,newwindow) {
	if( typeof newwindow != 'boolean')
		newwindow = false;
	if(!newwindow)
		document.location.href=url;
	else
		window.open(url);
	return false;
}


// Writes the message into the debug output panel. If the panel is not present, a new one will be added to the current page
function debug( message )
{
	if(!debugEnabled)
		return;

	// Do we have Firebug console logging?
	// If yes, use it.
	if (window.console && console.log)
	{
		console.log(message);
		return;
	}
		
	var debugWnd = $('debugWnd');
	if(debugWnd == null)
	{
		// if the debug output is missing, create a DIV to hold the debug messages
		var debugOut = document.createElement("div");
		debugOut.innerHTML = 'Debug Output:<div id="debugWnd" style="overflow:auto;border:solid lightgrey 1px;height:200px;padding:10px"></div>';
		document.body.appendChild(debugOut);
		debugWnd = $('debugWnd');
	}
	
	// append the new message and scroll to the end of the debug output
	debugWnd.innerHTML += message;
	debugWnd.scrollTop = debugWnd.scrollHeight;
}

function debugln( message )
{
	if(!debugEnabled)
		return;

	if (window.console && console.log)
		console.log( message );
	else
		debug( message + "<br>" );
}


//This function inserts newNode after referenceNode
function insertAfter( referenceNode, newNode )
{
	if(referenceNode)
		referenceNode.parentNode.insertBefore( newNode, referenceNode.nextSibling );
}

/**
 * @param ele
 * @return index of the element from parent's stand point of view, -1 if not found
 */
function indexOfElementInParent(ele)
{
	for(var i=0; i<ele.parentNode.childNodes.length; i++)
	{
		child = ele.parentNode.childNodes[i];
		if(child.id == ele.id)
		{
			return i;
		}
	}
	return -1;
}


function center(element)
{
	element.style.left = "50%";
	element.style.top = "50%";
	
	// TODO For some reason, offsetHeight and offsetWidth are 0
	element.style.marginLeft = -element.offsetWidth / 2 + "px";
	element.style.marginTop = -element.offsetHeight / 2 + "px";
}

// From http://www.howtocreate.co.uk/tutorials/javascript/browserwindow
function getWindowSize()
{
	var myWidth = 0, myHeight = 0;
	if(typeof(window.innerWidth) == 'number')
	{
	    //Non-IE
	    myWidth = window.innerWidth;
	    myHeight = window.innerHeight;
	}
	else if(document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight))
	{
	    //IE 6+ in 'standards compliant mode'
	    myWidth = document.documentElement.clientWidth;
	    myHeight = document.documentElement.clientHeight;
	}
	else if(document.body && (document.body.clientWidth || document.body.clientHeight))
	{
	    //IE 4 compatible
	    myWidth = document.body.clientWidth;
	    myHeight = document.body.clientHeight;
	}

	return [myWidth, myHeight];
}


/****************************************************************************
 * 
 *  Ajax Engine
 *  
 ****************************************************************************/


// Creates an xmlHttp object
function createHttpReq()
{
	var xmlHttp = null;
	
	// Mozilla, Opera, Safari, Internet Explorer 7
	if (typeof XMLHttpRequest != 'undefined')
	    xmlHttp = new XMLHttpRequest();
	
	if (!xmlHttp)
	{
	    // Internet Explorer 6 or older
	    try
	    {
	        xmlHttp  = new ActiveXObject("Msxml2.XMLHTTP");
	    }
	    catch(e)
	    {
	        try
	        {
	            xmlHttp  = new ActiveXObject("Microsoft.XMLHTTP");
	        }
	        catch(e)
	        {
	            xmlHttp  = null;
	        }
	    }
	}
	return xmlHttp;
}

// Updates the time stamp
function updateTime()
{
	lastCallTime = new Date().getTime();
}

// Definitions for async status display
var asyncStatusProgressIcon = getContextPath()+'ajax/icons/busy_anim.gif';
var asyncStatusErrorIcon = getContextPath()+'ajax/icons/warning.png';

/*
 * Initializes the async status display.
 */
function asyncStatusInit()
{
	if ( $('progress')==null )
	{
		debugln("asyncStatusInit");
		var div = document.createElement('div');
		div.id = 'progress';
		div.style.zIndex = 999;
		div.style.display = 'none';
		div.className = 'flProgress';
		div.innerHTML = "<img id='progressImg'></img>";
		document.body.appendChild(div);
		
		// Pre-load progress error icon
		// Otherwise we cannot show the error when the connection disconnects
		var tmp = new Image();
		tmp.src = asyncStatusErrorIcon;		
	}
}

/**
 * Indicate that a async request is in progress.
 * @return
 */
function asyncStatusInProgress()
{
	//if the current event type is set to POLLING, don't show an update icon on the ui
	if(currentEventType !== 13)
	{
		asyncStatusInit();
		debugln("asyncStatusInProgress");
		$('progressImg').src = asyncStatusProgressIcon;
		$('progressImg').alt = '';
		$('progress').style.display = 'block';
		$('progressImg').title = '';
		
		document.body.style.cursor = 'wait';
	}
}

/**
 * Indicate that the async request succeeded.
 * @return
 */
function asyncStatusSuccess()
{
	asyncStatusInit();
	debugln("asyncStatusSuccess");
	$('progress').style.display = 'none';
	$('progressImg').alt = '';
	$('progressImg').title = '';
	
	document.body.style.cursor = '';
}

/**
 * Indicate that the async request had an error.
 * @param error
 * @return
 */
function asyncStatusError( error )
{
	asyncStatusInit();
	debugln("asyncStatusError "+error);
	$('progressImg').src = asyncStatusErrorIcon;
	$('progressImg').width = '32';
	$('progressImg').alt = error;
	$('progressImg').title = error;
	
	// bug 6850: this method seems to be called very often
	// => too many popups appear. it has to be analysed why
	//alert(error);
	
	document.body.style.cursor = '';
}

function getSelection(el) { 
    var start = 0, end = 0;
    if (typeof el.selectionStart == "number") { 
        start = el.selectionStart; 
        end = el.selectionEnd; 
      } else if (document.selection) { 
        el.focus(); 

        var r = document.selection.createRange(); 
        if (r == null) { 
            return {start: 0, end:0}; 
        } 

        var re = el.createTextRange(), 
            rc = re.duplicate(); 
        re.moveToBookmark(r.getBookmark()); 
        rc.setEndPoint('EndToStart', re); 
 
        start = rc.text.length;
        end = start + re.text.length;
      }  
      return {
          start: start,
          end: end
      };
}

function setCursor(el, st, end) {
    if (el.setSelectionRange) {
    	focusWithoutScrolling(el);
        el.setSelectionRange(st, end);
    }
    else {
        if (el.createTextRange) {
            range = el.createTextRange();
            range.collapse(true);
            range.moveEnd('character', end);
            range.moveStart('character', st);
            range.select();
        }
    }
}

function isTextbox(el) {
	return (el && el.tagName && (
			(el.tagName.toLowerCase() == "textarea") ||
			(el.tagName.toLowerCase() == "input" && el.type.toLowerCase() == "text")));
}

/**
 * @param focusEleId the element with this id will be focus
 * @param focusPos in case the element is a textbox, this is the position the caret will be placed at
 */
function setFocusOnElementWithId(focusEleId, focusPos) {
	if (focusEleId) {
		var eleToFocus = document.getElementById(focusEleId);
		if (eleToFocus) {
			if (isTextbox(eleToFocus))
				setCursor(eleToFocus, focusPos.start, focusPos.end);
			else
				focusWithoutScrolling(eleToFocus);
		}
	}
}

/**
 * like el.focus(), but without scrolling to that element
 */
function focusWithoutScrolling(el) {
	if (window.scrollX && window.scrollY) {
		var x = window.scrollX, y = window.scrollY;
	} else {
		// IE
		var x = document.documentElement.scrollLeft;
		var y = document.documentElement.scrollTop;
	}
	// ignore failed focus (can happen when element is hidden, disabled, ...)
	try {
		el.focus();
		window.scrollTo(x, y);
	} catch (e) { }
}

/**
 * Check if an element is in the DOM. E.g. to check if an
 * element was replaced during poll.
 * @param element element to check
 * @returns whether the element is in the DOM
 */
function elementInDocument(element) {
	if (element) {
		while (element = element.parentNode) {
			if (element == document) {
				return true;
			}
		}
	}
	return false;
}

// Process an async update with the backend
function asyncUpdate()
{
	var xmlHttp = createHttpReq();
	if (xmlHttp)
	{
		asyncStatusInProgress();

		//trick Safari and Chrome into not using a cached response (with
		//browser's back button) by adding a highly dynamic parameter:
		var foolWebkit = "";
		if (navigator.userAgent.toLowerCase().indexOf("webkit") != -1)
			foolWebkit = "&fool_webkit=" + new Date().getTime();

		events = (events)?"&evt="+events:'';
		try {
			xmlHttp.open('GET', getContextPath()+'ajax/req/?'
					+ parseQueryString() + events + foolWebkit, true);
        }
        catch (e) {
        	debugln("Exception during asyncUpdate = "+e);
        	debugln("update size="+xmlHttp.responseText.length);
        }
	    //reset current parameters
	    events = "";
	    currentEventType = -1;
	    
	    xmlHttp.onreadystatechange = function ()
	    {
	        if (xmlHttp.readyState == 4)
	        {
	        	if ( xmlHttp.status==200 )
	        	{
		        	try
		        	{
		        		// remember previously focused element + position (in case of textbox), so it
		        		// can be refocused after the poll has been executed
		        		var focusPos;
		        		var focusEleId;
		        		
		        		var focusEle = document.activeElement;
		        		if (focusEle) {
		        			focusEleId = focusEle.id;
		        			if (isTextbox(focusEle))
		        				focusPos = getSelection(focusEle);
		        		}
		        		
		        		eval(xmlHttp.responseText);
		        		
		        		// try to refocus the previously selected element, when it has been
		        		// replaced by the eval(...) statements
		        		if(!elementInDocument(focusEle))
		        			setFocusOnElementWithId(focusEleId, focusPos);
		        		
		        		asyncStatusSuccess();
			        }
			        catch (e)
			        {
			        	debugln("Exception during asyncUpdate = "+e);
			        	debugln("update size="+xmlHttp.responseText.length);
			        	
			        	asyncStatusError( e );
			        }
	        	}
		        else
		        {
		        	asyncStatusError( "Error: " + xmlHttp.statusText + " (HTTP "+xmlHttp.status + ")" );
		        }
	        }
	    };
	    try
	    {
	    	xmlHttp.send(null);
        }
        catch (e)
        {
        	debugln("Exception during asyncUpdate = "+e);
        	debugln("update size="+xmlHttp.responseText.length);

        	asyncStatusError( e );
        }
	}
	else
		alert("Error! No XMLHTTPRequest available");
}

/**
 * executes a post call using ajax
 * @param postReceiverId - component id, e.g. passwordfield1
 * @param parameters e.g. secretpw, was encoded properly upfront
 */
function asyncUpdatePost(postReceiverId, parameters ) {
	
	var xmlHttp = createHttpReq();
	if (xmlHttp)
	{
		asyncStatusInProgress();
		
		xmlHttp.open('POST', getContextPath()+'ajax/req/' + postReceiverId, true);
	    xmlHttp.onreadystatechange = function ()
	    {
	        if (xmlHttp.readyState == 4)
	        {
	        	if ( xmlHttp.status==200 )
	        	{
		        	try
		        	{
			        	eval(xmlHttp.responseText);
			        	
			        	asyncStatusSuccess();
			        }
			        catch (e)
			        {
			        	debugln("Exception during asyncUpdatePost = "+e);
			        	debugln("update size="+xmlHttp.responseText.length);
			        	
			        	asyncStatusError( e );
			        }	
	        	}
	        	else
	        	{
		        	asyncStatusError( "Error: "+xmlHttp.status );
	        	}
	        }
	    };

	    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
		xmlHttp.setRequestHeader("Content-length", parameters.length);
		xmlHttp.setRequestHeader("Connection", "close");

		//parameters e.g.: myvar=secretpw&qry=asd=asd
		xmlHttp.send(parameters);
	}
	else
		alert("Error! No XMLHTTPRequest available");
}


/**
 * gets executed periodically
 * #setTimeout recursion is used instead of #setInterval, because interval gets executed in a regular interval (no update runtime considered).
 * and if the update takes longer than the interval, intervals get stacked and block the browser.
 * Timeout recursion gets only executed, after update is through
 * @see https://developer.mozilla.org/en/window.setInterval
 * @param id - component id
 * @param type - event type (e.g. POLL)
 * @param arg - argument, which gets passed along (e.g. component's value)
 * @param callback
 * @param timeout - timeout in milliseconds (interval)
 */
function asyncUpdatePeriodical(id, type, arg, callback, timeout)
{
	debugln("calling async update with arguments: "+arguments);
	//check if id is present then do the polling with recursion
	if($(id))
	{
		appendEventId( id, type, arg, callback );
		asyncUpdate();
		
		//was used to avoid executing to many ajax calls -> 1 call per second
		//updateTime();
		$(id).timeout = setTimeout(function(){asyncUpdatePeriodical(id, type, arg, callback, timeout);}, timeout);
	}
	else
	{
		debugln("stopping asyncupdate");
	}
}

/**
 * stops periodical update
 * @param id - dom node where timout variable is stored (required to identify the timeout)
 */
function stopAsyncUpdatePeriodical(id)
{
//check if id is there, then stop it
//if id is not there anymore recursion will be stopped automatically @see #asyncUpdatePeriodical
	if($(id))
	{
		var timeoutVar = $(id).timeout;
		debugln('clearing timeout timeoutVar '+timeoutVar);
		clearTimeout(timeoutVar);
	}
	else
		debugln('could not find timeoutSaveId '+id);
}

/**
 * starts the periodical update
 * Calls a function repeatedly, with a fixed time delay between each call to that function.
 * @param id - component id
 * @param type - event type
 * @param arg - argument
 * @param callback
 * @param timeout - timeout interval in ms
 */
function startAsyncUpdatePeriodical(id, type, arg, callback, timeout)
{
	if($(id))
	{
		//first stop already running update
		stopAsyncUpdatePeriodical(id);
		
		debugln("starting polling");
		
		//then start it periodical update (start polling after first polling intervall)
		$(id).timeout = setTimeout(function(){asyncUpdatePeriodical(id, type, arg, callback, timeout);}, timeout);
	}
}


// Catch an event and send to backend
function catchEventId(id, type, arg, callback)
{
	debugln("catch event id: "+id +", type: "+type+", arg: "+arg+", callback: "+callback);
	appendEventId( id, type, arg, callback );
	
	// if ( new Date().getTime() - lastCallTime > 1000 )
	{
		asyncUpdate();
		updateTime();
	}
}

//catch a post event and send to backend
function catchPostEventId(id, type, parameters)
{
	currentType = type;
	parameters = parseQueryString() + ((parameters)?"&"+parameters:"");
	asyncUpdatePost(id, parameters);
	updateTime();
}

/**
 * executes a post event and encodes the parameter
 * builds a parameter string from input and encodes the parameter
 * using encodeURIComponent
 *
 * encodeURIComponent
 * This function encodes special characters. In addition, it encodes the following characters: , / ? : @ & = + $ #
 * The encodeURIComponent() function is supported in all major browsers.
 * all chars but the following: 0-9A-Za-z - _ . ! ~ * ' ( ) get encoded
 * 
 * e.g. postval=!"�$%&/()=?���
 * 
 * @param id - component's id
 * @param type - event type (e.g. 9)
 * @param parameter - value e.g. !"�$%&/()=?���
 * @param paramname e.g. postval, used to get the postparameter back e.g. uri?postval=secretpw
 */
function catchPostEventIdEncode(id, type, parameter, paramname) {
	//default "args", if paramname was not set, can be retrieved via getPostparameter("args")
	paramname = paramname?paramname:"args";
	parameter = paramname+"="+(parameter?encodeURIComponent(parameter):"");
	catchPostEventId(id, type, parameter);
}

/**
 * @usedBy FSubmitButton
 * @param {Object} ids
 * @return json object - consisting of value and id of the component
 * structure:
 * [
 *  {
 *      "id": "myc_comp", 
 *      "val": "this is my text"
 *  }
 * ]
 * @throws Exception if component could not be found
 * 
 */
function getValuesFromComps(ids)
{
	res = [];
    var index;
	var id;
	//iterate through all ids and store them witht he value in a json array
    for (index = 0; index < ids.length; index++)
	{
	   id = ids[index];
	   element = $(id);
	   if(!element)
	   	   throw new Exception("unable to find comp with id "+id);
       res.push( {"id":id, "val":element.value} );
	}
    return res;	
}



// remember event and send it in 100ms if it has not been sent
function delayedCatchEventId(id, type, arg)
{
	appendEventId(id, type, arg);
	setTimeout("sendUnsentEvents();", 500);
}

function sendUnsentEvents()
{
	if ( events != "" )
		asyncUpdate();
}

// remember event and send it next time
function appendEventId(id, type, arg, callback)
{
	//set current eventType to the current call's eventType
	currentEventType = type;
	if ( events!="" )
		events += "/";
	
	callback = (typeof(callback) == 'undefined') ? "" : callback ;

	// If arg is an object, convert it to a json string
	if (typeof(arg)=='object')
	{
		//arg gets quoted
		arg = convertToString(arg);
		//if quoting is detected it has to be undone
		if(arg.length>1 && arg.substring(0,1)=='"' && arg.substring(arg.length-1,arg.length)=='"')
			arg = arg.substring(1,arg.length-1);
	}
	// encode each part and then encode the whole String again -> first decoding will happen with getParameter()
	events = events + encodeURIComponent( id + "/" + type + "/" + encodeURIComponent(arg) + "/" + encodeURIComponent(callback) );
}

/**
 * Input: http://localhost:50080/sap/jsps/pool.jsp?moref=pool-3&name=test#4
 * Output: moref=pool-3&name=test
 * returns the query (specific selection), based on window href
 */
function parseQueryString()
{
     // 'http:%2F%2Flocalhost:50080%2Fajax%2FcontextDemo.htm?param1=myValu%2F%2e&param2=va%2F%2lue2';  
     var context = window.location.href;
     var posQuery = context.indexOf('?');
     //no question mark (question mark indicates parameters)
     if (posQuery == -1)
        return '';
      
     debugln( "plain context: " + context);
     //check if context ends with # and if so remove the hashpart
     var lastIndex = context.lastIndexOf("#");
     if(lastIndex != -1)
       context = context.substring(0, lastIndex);
      
     context = context.substring( posQuery+1 );
     debugln( "cut off context: " + context);
     return context;
}


// encode query string, leave &
function encodeQueryString( s )
{
	n = encodeURI(s);
	return n;
}


function catchEvent(node, type, arg)
{
	catchEventId( node.id, type, arg );
}


function getEventSender( event )
{
	var e = event == null ? window.event : event;
		
	// do we get the sender W3C style or IE style?
	var sender = e.target != null ? e.target : e.srcElement;

	// overcome Safari bug where events get attached to text nodes
	if(sender.nodeType == 3)
		sender = sender.parentNode;
		
	return sender;
}

function evtMouseDown( event )
{
	var target = getEventSender(event);
	if ( target.id!=null && target.id!="" )
	{
		debugln(target.id);
// alert( target.id );
// return false;
	}
}

function evtContextMenu( event )
{
	var target = getEventSender(event);
	if ( target.id!=null && target.id!="" )
	{
//		alert( target.id );
		return false;
	}
}

function monitorInfo( id, handle )
{
	this.id = id;
	this.handle = handle;
	this.lastValue = " ";
}

function monitorCallback( id )
{
	var node = $(id+".input");
	var mInfo = global[ id ];
	if ( mInfo==null )
		alert("monitorInfo: Null "+id);
	
	var val = node.value;
	if ( val!=mInfo.lastValue )
	{
		mInfo.lastValue = val;
		catchEventId(id, 5, val);
	}
}

function enableInputMonitor( id )
{
	var handle = setInterval( "monitorCallback('"+id+"')", 500 );
	var mInfo = new monitorInfo( id, handle );
	global[ id ] = mInfo;
	
	$(id+"_dropdown").style.visibility="";
}



function disableInputMonitor( id )
{
	var mInfo = global[ id ];
	if ( mInfo!=null )
	{
		clearInterval( mInfo.handle );
		global[ id ] = null;
		
		setTimeout( "$('"+id+"_dropdown').style.visibility='hidden';", 1000);		
	}	
}

function flTabPane_show( id, pane )
{
	var i;
	for (i=0; ; i++)
	{
		try
		{
			$(id+'.hidden').appendChild($(id+'.'+i));
		}
		catch (e)
		{
			break;
		}	
	}
	$(id+'.content').appendChild($(id+'.'+pane));
	//looks for the first ul
	var u = $(id).getElementsByTagName('ul')[0];
	//gets only its direct children as this enables nested tabpanes
	var e = new Array();
	for ( var i = 0; i < u.childNodes.length; i++)
		if(u.childNodes[i].nodeName == 'LI')
			e.push(u.childNodes[i]);
	
	for (i=0; i<e.length; i++)
	{
		e[i].className = pane==i ? 'flTabActive tabitem' + i : 'tabitem' + i;
		//TODO fix THIS here
		with ({tid : id, ti: i})
		{
			e[i].onclick = function(){catchEventId(tid, 5, ti);};
		}
	}
}

function flPopup_show( id, x, y)
{
	var node = $(id);
	node.style.display='block';
	if (x>=0)
		node.style.left = x+"px";
	if (y>=0)
		node.style.top = y+"px";
}

function flPopup_hide( id )
{
	$(id).style.display='none';
}

function popup_mouseover( evt, id )
{
//TODO fix THIS here
	with ({x: evt.clientX, y:evt.clientY} )
		global[id]=setTimeout( function() { flPopup_show(id, (x+20), (y+20) ); }, 2000);
}

function popup_mouseout( evt, id )
{
	flPopup_hide(id);
	if (global[id])
		clearTimeout(global[id]);
}

function evt_mouseover( evt, id )
{
	catchEventId(id, 6, id+'_'+evt.clientX+'_'+evt.clientY );
}

function evt_mouseover( evt, id, time )
{
//TODO fix THIS here
	with ({x: evt.clientX, y:evt.clientY} )
		global[id]=setTimeout( function() { catchEventId(id, 6, x+'_'+y  ); }, time);
}

function evt_mouseout( evt, id )
{
	catchEventId(id, 7, evt.clientX+'_'+evt.clientY);
	if (global[id])
		clearTimeout(global[id]);
}

function getDropDownControlOf( obj )
{
	return $(obj.parentNode.id + '_dropdown');
}

// returns an array of dom elements that represent the elements of a drop down list
function getDropDownItems( dropdown )
{
	// first check, if there are children present
	if(dropdown.childNodes == null)
		return null;
		
	// is there only one child present?
	// if yes, check whether the elements are defined as a list element.
	// if more than one child is present, directly return them
	if(dropdown.childNodes.length == 1)
	{
		var child = dropdown.childNodes[0];
		var name = child.nodeName.toLowerCase();
		if(name == 'ul' || name == 'ol')
			return child.childNodes;
	}

	// either the elements are not encapsulated in a list or there
	// is only one element present. So just return the child nodes
	// of the drop down list
	return dropdown.childNodes;
}

function hasActiveDropDown( obj )
{
	// try to find the drop down element that belongs to the current control
	var drop = getDropDownControlOf(obj);
	
	// is the control present and visible? If so, are there
	// child elements defined?
	if(drop != null && drop.style.visibility != 'hidden')
	{
		var dropElements = getDropDownItems(drop);
		return dropElements != null && dropElements.length > 0;
	}
	return false;
}



function ensureItemVisible( control, items, itemid )
{
	if(control == null || items == null || itemid < 0)
		return;
		
	// first determine the scroll position of the selected item
	var scrollPos = 0;
	var itemHeight = 0;
	var i;
	for(i = 0; i < items.length && i <= itemid; i++)
	{
		if(i < itemid)
			scrollPos += items[i].offsetHeight;
		else if(i == itemid)
			itemHeight = items[i].offsetHeight;
	}
	
	var controlHeight = control.clientHeight;
// if(controlHeight == 0)
// controlHeight = control.scrollHeight;
	
	// is the selected item below the visible range?
	if(control.scrollTop + controlHeight < scrollPos + itemHeight)
		control.scrollTop = scrollPos + itemHeight - controlHeight;
	// or is it above the visible range?
	else if( scrollPos < control.scrollTop)
		control.scrollTop = scrollPos;
}

/**
 * executes item's onclick
 * @param items
 * @param itemid
 * @return
 */
function selectItem( items, itemid )
{
	//TODO null check + improbement by directly refering via index --> items[itemid].onclick();
	var i;
	for(i = 0; i < items.length; i++)
	{
		if(i == itemid)
		{
			items[i].onclick();
			return;
		}
	}
}

/**
 * sets the value of the inputid to the item's value
 * @param inputid
 * @param items
 * @param itemid
 * @return
 */
function setItemValue( inputid, items, itemid )
{
	var i;
	for(i = 0; i < items.length; i++)
	{
		if(i == itemid)
		{
			$(inputid).value = items[i].value;
			return;
		}
	}
}



/****************************************************************************
 * 
 *  General purpose helper functions for building advanced components
 *  
 ****************************************************************************/
/**
 * Executes the specified function with all children of the specified HTML node
 * recursively. The specified function will be called once for each child and
 * always receives the child node as the first function argument.
 */
function forAllChildren(node, method)
{
	for (var i = 0; i < node.childNodes.length; i++)
	{
	      method(node.childNodes[i]);
	      
	      forAllChildren(node.childNodes[i], method);
	}
}

/**
 * Cross browser reimplementation of addEventListener(). Also supports old
 * versions of Internet Explorer, which did not implement standard
 * addEventListener(). Note that eventBubbling in old versions of IE is a
 * simulation, which acts similar but not identical to actual event bubbling.
 * 
 * @param element
 *            HTML element to attach event listener
 * 
 * @param eventName
 *            Name of event to listen (just as first argument in standard event
 *            listener)
 * @param method
 *            Event listener method
 * @param eventBubbling
 *            Whether or not to enable event bubbling (has no effect on IE
 *            fallback)
 */
function cbAddEventListener(element, eventName, method, eventBubbling) {
	try {
		element.addEventListener(eventName, method, eventBubbling);
	} catch (e) {
		// and the same again for IE 7 & 8
		try {
			element.attachEvent('on' + eventName, method);
			
			if (eventBubbling)
			{
				var simulatedBubbling = 'simEvtBubbling' + eventName;
				element[simulatedBubbling] = method;
				
				forAllChildren(element, function(node) {
						try {
							node.attachEvent('on' + eventName,
				    		element[simulatedBubbling]);
						}
						catch (e) {}
					});
			}
		}
		catch(e) {}
	}
}

/**
 * Adds or updates a reporter method to a DOM element. Reporter methods delegate
 * events to a collector method at one of their ancestor nodes. This is similar
 * to event bubbling but allows more control in addressing a specific collector
 * method (and has built-in support for old non-standards compliant browsers).
 * 
 * @param id
 *            HTML id of DOM element where the reporting method should be added
 *            or updated.
 * @param eventName
 *            Name of the event (take care of correct use of upper/lower case)
 *            to listen to (e.g. "MouseOver")
 * @param xPathDistance
 *            Distance in XPath up to the ancestor managing the associated
 *            collector method.
 * @param eventBubbling
 *            Should event bubbling be activated in the listener (has no effect
 *            in IE before version 9)
 */
function reportMethodUpdate(id, eventName, xPathDistance, eventBubbling)
{
	if ($(id) == undefined && !debugEnabled)
		return;
	
	cbAddEventListener($(id), eventName.toLowerCase(),
			function() 
			{
				var el = $(id);
				var i;

				for (i = 0; i < xPathDistance; i++)
					el = el.parentNode;

				el['reported' + eventName]();
			}, eventBubbling
		);
}


/**
 * Collects event reports that are being reported by child nodes through an
 * event handler added to the child by reportMethodUpdate(). Collected reports
 * will eventually be sent to the server via catchEventId(). However, a delay
 * can be employed during which reports will be hold back and will only
 * eventually be sent if no concurrent updates did arrive during the delay. The
 * delay can be used to avoid high-frequency updates which basically cancel out
 * each other. It also ensures that outdated events can not easily overtake more
 * recent ones on their way to the server.
 * 
 * @param id
 *            HTML id of DOM element where the collector method should be added
 *            or updated.
 * @param eventName
 *            Name of the event (take care of correct use of upper/lower case)
 *            that is being reported (e.g. "MouseOver")
 * @param serverReportId
 *            Determines what kind of event will be reported to the server when
 *            the report gets through (catchEventId() code, e.g., 1 for 'Click')
 * @param serverReportDelay
 *            Delay (in milli seconds) before the event gets reported to the
 *            server. Reports will be dropped if any concurrent reports come in
 *            on the same shared state during this delay.
 * @param firstWins
 *            (boolean) Determines whether the first or last arriving event wins
 *            in case of concurrent events triggering the same state. If this is
 *            false, concurrent events (e.g., repeated MouseOvers) will start
 *            over on the delay and thus effectively extend the time before a
 *            report is sent to the server. If a concurrent event changes the
 *            value of the shared state it will always win and restart the
 *            delay.
 * @param sharedStateName
 *            A name for the (possibly shared) state that will be changed by
 *            this event (e.g., 'has_focus'). The name will used to build
 *            properties tracking the assumed state and possible concurrent
 *            events occuring during the delay.
 * @param sharedStateVal
 *            A (preferrably numeric) id for the state entered after this event.
 *            You may have several different events reporting on the same shared
 *            state using different state values. Changing states during the
 *            delay cancel each other out (e.g., focus/blur setting state values
 *            1 and 0, respectively).
 * @param exclusivityCancel
 *            (method name, optional) If set, only one DOM element at a time can
 *            assume the specified value of the shared state. If the collector
 *            method lets an element enter this state locally it will call the
 *            specified method on the previous holder of the same state (if
 *            there is one) to let it know that it just lost its state (e.g.,
 *            'has_focus' state value 1 may wish to trigger an additional method
 *            on the previous holder of the focus to notify it that it lost the
 *            focus to a peer sharing on the same state)
 */
function collectorMethodUpdate(id, eventName, serverReportId,
		serverReportDelay, firstWins, sharedStateName, sharedStateVal,
		exclusivityCancel)
{
	if ($(id) == undefined && !debugEnabled)
		return;

	$(id)['reported' + eventName] = function() {
		var el = $(id);

		if (el[sharedStateName + '_state'] == sharedStateVal && firstWins)
			return;

		if (exclusivityCancel)
		{
			var oldEl = document[sharedStateName + '_exclusive_cell_state_'
			                     + sharedStateVal];
			document[sharedStateName + '_exclusive_cell_state_'
			         + sharedStateVal] = el;

			if (oldEl && el[sharedStateName + '_state'] == sharedStateVal)
				oldEl[exclusivityCancel]();
		}

		el[sharedStateName + '_state'] = sharedStateVal;
    
		if (el[sharedStateName + '_event_num'] == undefined)
			el[sharedStateName + '_event_num'] = 0;

		var eventNum = ++el[sharedStateName + '_event_num'];

		setTimeout(function() {
				if (el[sharedStateName + '_state'] == sharedStateVal
					&& el[sharedStateName + '_event_num'] == eventNum)
						catchEventId(id, serverReportId, '');
			}, serverReportDelay);
	};
}


/****************************************************************************
 * 
 *  Necessary functions for FTextInput2
 *  
 ****************************************************************************/


// functions referenced before
// - selectItem(..)
// - getDropDownControlOf( obj)


var _textInputMonitorHandle = new Array();
var _textInputHasUpdates = new Array();
var _showDropDown = new Array();
var _lastInput = new Array();
/**
* retrieves the suggestion list
*/
function catchSuggestionList(id, type, value){
	var dropdown =  id +'_dropdown';
	catchEventId(id, type, value );
}

function textInputFetchSuggestions(inputEl, value) {
	if (_lastInput[inputEl.id] != value) {
		var dropdown = getDropDownControlOf( inputEl );
		// set dropdown to visible in callback
		catchEventId(inputEl.id, 5, inputEl.value);
		
		dropdown.selectedItem=-1;
		_lastInput[inputEl.id] = value;
	}
}

function textInputMonitorCallback( inputEl, value, force) {
	if (!_textInputMonitorHandle[inputEl.id] || !_textInputHasUpdates[inputEl.id])
		return;
	textInputFetchSuggestions(inputEl, inputEl.value);
	setDropdownVisibility( getDropDownControlOf( inputEl ), _showDropDown[inputEl.id], "250" );
	_textInputHasUpdates[inputEl.id] = false;
}

function textInputEnableMonitor( inputEl ) {
//	debugln('textInputEnableMonitor');
//	_textInputMonitorHandle[inputEl.id] = window.setInterval(function() { textInputMonitorCallback( inputEl) }, 500);
}

function textInputDisableMonitor( inputEl ) {
//	debugln("disable input monitor")
//	// do a last update
//	window.clearInterval( _textInputMonitorHandle[inputEl.id] );
//	// _textInputHasUpdates = true;
//	// textInputMonitorCallback( inputEl, inputEl.value, true);
//	textInputFetchSuggestions(inputEl, inputEl.value);
//	_textInputMonitorHandle[inputEl.id] = null;
}

function selectDropDownItem( items, itemid ) {
	if(items == null || items.length == 0)
		return;
		
	var i;
	for( i = 0; i < items.length; i++) {
		items[i].className = (i==itemid)?'selected':'';
	}
}

function textInputValueChanged( inputEl, forceUpdate, showDropDown ) {
	var val = inputEl.value;
	if ( forceUpdate || val!=inputEl.defaultValue)
	{
		inputEl.defaultValue = inputEl.value;
		_showDropDown[inputEl.id] = (showDropDown)?true:false;
		_textInputHasUpdates[inputEl.id] = true;
	}
}

/**
 * checks whether or not dropdown is visible,
 * is used for onchange, as this redraws the dropdown
 * @param textInputAnchorId - e.g. mytextInput
 * @return 
 */
function isDropDownVisible( textInputAnchorId ){
	 var dropdown = $(textInputAnchorId + '_dropdown');
	 var isVisible = dropdown.style.visibility != 'hidden';
	 return isVisible; 
}

/**
 * gets executed, when input gets focus
 * @param textInputAnchorId
 */
function textInputOnFocus( textInputAnchorId )
{
	// keep focus
	input_has_focus = true;
	var input = $(textInputAnchorId + '.input');
	var dropdown = $(textInputAnchorId + '_dropdown');

	// refresh suggestion list by focus on textfield (maybe choices had been added/removed)
	catchSuggestionList(textInputAnchorId,5,input.value);
}

function textInputOnBlur( textInputAnchorId ) {
	debugln(textInputAnchorId+"textInputBlur: li_selected: "+liSelected+ ", keep_focus: "+keep_focus+", input_has_focus: "+input_has_focus);
	//gets set to true again, if "keep_focus" ist true, because focus() calls textInputOnFocus()
	input_has_focus = false;
	// if the scrollbar is clicked, the input loses its focus (chrome & ie bug)
	// use variables input_has_focus and keep_focus to keep focus manualy
	if (keep_focus) {

		// set focus to input again
		var input = $(textInputAnchorId+'.input');
		input.focus();
		keep_focus = false;
		return;
	}
	
	var input = $(textInputAnchorId + '.input');
	var dropdown = $(textInputAnchorId + '_dropdown');
	window.setTimeout( function() {textInputDisableMonitor( input ); }, 500) ;
	setDropdownVisibility( dropdown, false, 250 );
}


var input_has_focus = false;
var keep_focus = false;
/**
 * Keep focus on input 
 */
function suggListOnClick(dropdown){
	// only firefox keeps focus on input field while scrolling
	if(checkBrowserName('chrome') || checkBrowserName('MSIE') || checkBrowserName('safari') || checkBrowserName('opera')){
		
		// if scrollbar is clicked, check if the input field has focus and store this state
		if(!liSelected){
			keep_focus = input_has_focus;
			
		}
		liSelected = false;
	}
}


var liSelected = false;
/**
 * if item from dropdown is selected, dont keep focus on input
 */
function dropdownLiSelected(){
	// only firefox keeps focus on input field while scrolling
	if(checkBrowserName('chrome') || checkBrowserName('MSIE') || checkBrowserName('safari') || checkBrowserName('opera')){
		liSelected = true;
	}
}

/**
*	Set the size of the suggestion list
*/
function setSizeSuggList(dropdown){
	
	dropdown = $(dropdown);
	
	if(dropdown==null){
		return;
	}
	
	// number of elements in suggestion list
	var numChilds = dropdown.childNodes[0].childNodes.length;
	// max elements to display
	var maxElements = ExtractNumber(dropdown.childNodes[0].getAttribute('maxSuggestionsDisplayed'));
	// number of actual suggestions
	var numElements = (numChilds > maxElements) ? maxElements : numChilds;
	if(dropdown.childNodes[0].childNodes[0]!=null){
		
		// set max height of drop down list
		heightPerChild = dropdown.childNodes[0].childNodes[0].offsetHeight;
		var height = heightPerChild * numElements;
		
		// set max-height manualy because ie doesn't support css-property "max-height"
		if(numChilds > maxElements){
			dropdown.style.height = height+'px';
		}else{
			dropdown.style.height = 'auto';
		}
		
		// set width of drop down list to width of input field
		var widthInputField = dropdown.parentNode.childNodes[0].clientWidth;
		dropdown.style.width = widthInputField+'px';
		
	}
	debugln('height: '+height+', num childs: '+numChilds+", num shown Elements: "+numElements);
	
}

function setDropdownVisibilityId( dropdown, visibleFlag, timeout) {
	if($(dropdown)==null){
		return;
	}
	setDropdownVisibility($(dropdown), visibleFlag, timeout);
	
}

/**
* Set visibility of suggestion list
* @param timout - if timeout is set this will be execute, after timeout
*/
function setDropdownVisibility( dropdown, visibleFlag, timeout) {
	
	setSizeSuggList(dropdown.id);
	
	if (!timeout){
		//if dropdown has no children, don't display it
		var ul = dropdown.childNodes[0];
		var siz = 1;
		if(ul && ul.childNodes)
			siz = ul.childNodes.length;
		if(siz <1)
			visibleFlag = false;
		dropdown.style.visibility = (visibleFlag)?'visible':'hidden';
	}else
		window.setTimeout( function() { setDropdownVisibility( dropdown, visibleFlag); }, timeout );
}

function sendSelectionToInput( inputElId, newValue, hideDropdown ) {
	var inputEl  = $(inputElId);
	inputEl.value = newValue;
	textInputValueChanged( inputEl );
	if (hideDropdown)
		setDropdownVisibility( getDropDownControlOf(inputEl), false, "550" );
}


/**
 * Keypress handler for input element:
 * 
 * onEnter: 	submit selection or if nothing selected submit first
 * onTab: 		submit selection or if nothing selected submit first
 * 				go to next input element
 * cursorUp:	select next element in suggestion list
 * cursortDown:	select previous element in suggestion list
 * 
 * note: maybe a different UI handling for tab/enter should be implemented
 * @param event
 * @param id
 * @return
 * TODO TOB --> don't do a callback on each up/down, only when enter was pressed
 */
function keypressAutocompleteHandler1( event, id )
{
	
	var sender = getEventSender(event);
	var dropdown = getDropDownControlOf( sender );
	
	//selected Item Index --> -1 means nothing is selected
	var selItem = dropdown.selectedItem;
	if(selItem == null)
		selItem = -1;
	var items = getDropDownItems( dropdown );

	debugln("found "+items.length+" items");
	
	// was a key of interest pressed? --> navigation key
	switch(event.keyCode) {
	case 13: 	// ENTER
				
				//if nothing is selected --> ENTER goes to the backend
				if (selItem == -1) 
					catchEventId(id,12,$(id+'.input').value);
				//if there is a selection --> autocomplete is triggered
				setDropdownVisibility( dropdown, false, "250" );
				break;
	case  9:	// TAB - text autocompletion with selected element in suggestion list
				//if there is no element in suggestion list --> return
				if (items.length == 0)
					return;
				//if there is no selection, use the first one
				if(selItem  == -1)
					selItem = 0;
				selectItem(items, selItem);
				setDropdownVisibility( dropdown, false, "250" );
				break;
	case 37:	// CURSOR LEFT
				break;
	case 38:	// CURSOR UP - select previous element in suggestion list
				//if there is no element in suggestion list --> return
				if(items.length == 0)
					return;
				//decrease current item index
				selItem--;
				
				//if selectedItemIndex is not within range (smaller than zero and items are there, set it to last element)
				if(selItem < 0)
					selItem = items.length - 1;

				dropdown.selectedItem = selItem;
				selectDropDownItem(items, selItem);
				ensureItemVisible(dropdown, items, selItem);
				catchEventId(id,11,selItem);
				break;
	case 39:	// CURSOR RIGHT
				break;
	case 40:	// CURSOR DOWN - select next element in suggestion list
				//if there is no element in suggestion list --> return
				if(items.length == 0)
					return;
				//increase current item index
				selItem++;
				
				//if selectedItemIndex is smaller than 0 or are big than the itemsList set it to zero
				//TODO use modulo to be in range
				if(selItem >= items.length || selItem < 0)
					selItem = 0;
				
				dropdown.selectedItem = selItem;
				selectDropDownItem(items, selItem);
				ensureItemVisible(dropdown, items, selItem);
				catchEventId(id,11,selItem);
				break;
	}
}

 /**
  * Keyrelease handler for input element:
  * 
  * if no key of interest was pressed, a input changed event gets executed
  * 
  * note: maybe a different UI handling for tab/enter should be implemented
  * @param event
  * @param id
  * @return
  */
 function keyreleaseAutocompleteHandler1( event, id )
 {
	debugln('keyrelease1 '+event.keyCode);
 	var sender = getEventSender(event);
 	var dropdown = getDropDownControlOf( sender );
 	var arg = $(id+'.input').value;
 	// was a key of interest pressed?
 	switch(event.keyCode) {
 	case 13: 	// ENTER
 				break;
 	case  9:	// TAB
 				break;
 	case 37:	// CURSOR LEFT
 				break;
 	case 38:	// CURSOR UP
 				break;
 	case 39:	// CURSOR RIGHT
 				break;
 	case 40:	// CURSOR DOWN
 				break;
 	default:
 				//reset selected item as a non navigation key was pressed
 				//-1 means nothing was selected
 				dropdown.selectedItem = -1;
 				catchEventId(id,5,arg);
 				break;
 	}
}


/**
 * Initializes the F-word UI
 **/
function fluInit(callback)
{
	debugln("FComponents 1.0.0 initialized"); 
	 
	if(typeof callback == 'string')
		catchEventId("__init__",1,"",callback);
	else
		catchEventId("__init__",1,"");
	document.body.onmousedown = evtMouseDown;
	document.body.oncontextmenu = evtContextMenu;
	//dnd is not used yet, and this disables cut n paste in internet explorer
	//draginit();
	initializeTableResize();
}

/**
 * Helper to get the current style for both Mozilla and IE.
 */
function getStyle(obj)
{
	if ( window.getComputedStyle )
		return window.getComputedStyle(obj,"");
		
	if ( obj.currentStyle )
		return obj.currentStyle;
		
	alert("NO getComputedStyle for "+obj+" available!");
	return null;
}

/**
 * Returns the offset for a given DOM element as the object { left: ..., top : ... }
 * Follows offsetParent pointers until it reaches top of the pages and
 * adds up offsetLeft and offsetTop values
 *
 * Width and height can be retrieved via: el.offsetWidth, el.offsetHeight
 *
 * Screen width and height are: window.innerWidth, window.innerHeight
 */
function getLeftTop(obj) 
{
	var res = {};
	res.left = 0;
	res.top = 0;

	while (obj) 
	{
		// If the current obj is a container (position absolute), exit
		var st = getStyle(obj);
		if ( st!=null && st.position == "absolute")
			break;
			
		res.left += obj.offsetLeft;
		res.top += obj.offsetTop;
		obj = obj.offsetParent;
	}
	
	return res;
}


/**
* Alternative Version for getLeftObject 
* offers a fix for certain positioning problems
*/
function getLeftTop1(obj) 
{
	var res = {};
	res.left = 0;
	res.top = 0;

	while (obj) 
	{
		res.left += obj.offsetLeft;
		res.top += obj.offsetTop;
   
		// If the current obj is a container (position absolute), exit
		var st = getStyle(obj);
		if ( st!=null && (st.position == "absolute" || st.position == 'relative' ))
			break;
 
		obj = obj.offsetParent;
	}
	
	return res;
}

/**
 * Uli's Very Own Drag and Drop Stuff (UVODDS).
 */

var initobject = null;
var superid = null;
 
// the currently moving dnd object
var dndobject = null;

// drag position, mouse position
var dragx=0, dragy=0, posx=0, posy=0;

// Flag that tells whether dnd is initialized
var dndinit = false;
var orgOnmousemove = null;
var orgOnmouseup = null;

var dndabsolute = true;
var dndparent = null;
var dndoldposition = null;
var dndsnapinfo = null;

// initialize drag and drop
function draginit()
{
	if (dndinit==false)
	{
		if ( document.onmousemove )
			orgOnmousemove = document.onmousemove;
		if ( document.onmouseup )
			orgOnmouseup = document.onmouseup;

		document.onmousemove = drag;
		document.onmouseup = dragstop;
		dndinit = true;
	}
}

function dragdeinit()
{
	if (dndinit==true)
	{
		document.onmousemove = orgOnmousemove;
		document.onmouseup = orgOnmouseup;
		dndinit = false;
	}
}

function dragstart(element,_superid, _swid)
{
	debugln('dragstart el='+element+' super='+_superid+' id='+_swid);
	
	var abso = false;
	if ( _superid.indexOf("bsolute")>0 )
		abso = true;
	else
	{
		if ( element.id.indexOf("indow")>0 )
			abso = false;
		else
			abso = true;
	}
	
	dragstartEx(element,_superid,_swid,abso);
}

function dragstartEx(element,_superid, _swid, _dndabsolute)
{
	dndobject = element;
	superid = _superid;
	initobject = _swid;
	var id = element.id;

	dndabsolute = _dndabsolute;
	if (dndabsolute==false)
	{
		// element has no "position:absolute"
		dndabsolute = false;
		dndparent = element.parentNode;
	}

	// Different container for DnD defined?
	// If yes, use it
	var dndIx = id.indexOf(".dnd");
	
	if ( dndIx>0 )
	{
		var container = id.substring(0, dndIx);

		dndobject = $( container );
	}

	if (dndabsolute==false)
	{
		dndsnapinfo = null;
		var w = dndobject.offsetWidth;
		var h = dndobject.offsetHeight;
		debugln("oldWidth="+w+" oldHeight="+h);
		dndobject.style.position = "absolute";
		dndobject.style.width = w;
		dndobject.style.height = h;
		
		if (dndobject.className == "flWindow")
			dndobject.className = "flWindowOpaque";
		
		// $("windowplaceholder").style.width = w;
		$("windowplaceholder").style.height = h;
	}
	
	dragx = posx - dndobject.offsetLeft;
	dragy = posy - dndobject.offsetTop;
}

/**
 * Tests whether the given position is within the
 * given element.
 */
function isPosInElement( pos_left, pos_top, el )
{
	var elPos = getLeftTop( el );
	var h = el.offsetHeight;
	var w = el.offsetWidth;
	
	// Hack: For columns, allow appending at bottom
	if ( el.id.indexOf("column")>=0 )
		h += 40;
	
	if ( h<20) h=20;
	if ( w<20) w=20;
	
	if ( pos_left >= elPos.left && pos_left < (elPos.left+w)
		&& pos_top >= elPos.top && pos_top < (elPos.top+h) )
		return true;
		
	return false;
}

/**
 * Calculate snap info for putting the given DnD object into the correct layout position.
 * This is both used during DnD (visualization of the placeholder),
 * and for the final snap of the "real" window.
 * 
 * This function makes certain assumptions about object IDs etc.
 */
function dndsnapin(dndobject)
{
	var res = {};
	res.column = null;
	res.node = null;
	
// debugln("dndsnapin objid="+dndobject.id+" posx="+posx+" posy="+posy+"
// reverting element to position="+dndoldposition);
	for (var i=0; ; i++)
	{
		var col = $("column"+i);
		if ( col==null )
			break;
			
		var pos = getLeftTop(col);
		var isIn = isPosInElement( posx, posy, col );
/*
 * debugln("dndsnapin [column"+i+" left="+ pos.left+" top="+pos.top+" width="+
 * col.clientWidth+" height="+col.clientHeight+ "] isPosInColumn = "+isIn );
 */		
		if ( isIn )
		{
			var iNode = null;
			var kids = col.getElementsByTagName("div");
			for (var i=0; i<kids.length; i++)
			{
				if ( isPosInElement(posx, posy, kids[i]) )
				{
					iNode = kids[i]; break;
				}
			}
			// Fill into the result object
			res.column = col;
			res.node = iNode;
			break;
		}
	}
	return res;
}

/**
 * Puts the DnD object to the location given in snapinfo.
 */
function dndsnapinmove( dndobject, snapinfo )
{
	var col = snapinfo.column;
	var iNode = snapinfo.node;
	
	if ( col==null )
	{
		debugln("dndsnapinmove column==null");
		return;
	}

	if ( iNode!=null )
	{
		debugln("dndsnapinmove before columnId="+col.id+" nodeId="+iNode.id);
		col.insertBefore( dndobject, iNode );
	}
	else
	{
		debugln("dndsnapinmove at end of columnId="+col.id);
		col.appendChild( dndobject );
	}	
}

function dragstop()
{
	if ( dndobject==null )
		return;
		
	// Was it a non-absolute element?
	if ( dndabsolute==false )
	{
		// Snap in the window to the right place
		if ( dndsnapinfo==null )
			dndsnapinfo = dndsnapin( dndobject );
		dndsnapinmove( dndobject, dndsnapinfo );
		
		var pos = -1;
		for ( var i = 0; i < dndsnapinfo.column.childNodes.length; i++) {
			var snapid = dndsnapinfo.column.childNodes[i].id;
			if(snapid=="windowplaceholder"){
				pos = i;
				break;
			}
		}
				
		catchEventId(superid, 1, initobject+','+dndsnapinfo.column.id+','+pos);
		dndsnapinfo = null;

		// Hide the placeholder
		$("dndcontainer").appendChild( $("windowplaceholder") );

		if (dndobject.className == "flWindowOpaque")
			dndobject.className = "flWindow";
									
		dndobject.style.position = "static";
		dndobject.style.width = "";
		dndobject.style.height = "";
		dndobject.style.left = "";
		dndobject.style.top = "";
	}
	else
	{
		catchEventId(superid, 1, initobject+','+dndobject.style.left+','+dndobject.style.top);
	}

	dndobject = null;

}

var dndlasttime = 0;

function drag(ev)
{
	posx = document.all ? window.event.clientX : ev.pageX;
	posy = document.all ? window.event.clientY : ev.pageY;
	if (dndobject != null)
	{
		dndobject.style.left = (posx - dragx) + "px";
		dndobject.style.top = (posy - dragy) + "px";
		//bring to front
		dndobject.style.zIndex = 10000; 
		
		 // cancel out any text selections 
		document.body.focus(); 
		
		if ( dndabsolute==false )
		{
			// Updates the placeholder window.
			// Uses timer as function is expensive (layout/render etc.)
			var t = new Date().getTime();
			if ( t-dndlasttime > 500 )
			{
				dndlasttime = t;
				var plObj = $("windowplaceholder");
				var snapinfo = dndsnapin( plObj );
				if ( snapinfo.column!=null )
				{
					if ( dndsnapinfo==null || dndsnapinfo.column!=snapinfo.column
						|| dndsnapinfo.node!=snapinfo.node )
					{
						dndsnapinfo = snapinfo;
						dndsnapinmove( plObj, dndsnapinfo );
					}	
				}
			}
		}
	}
	return false;
}

/**
 * Makes the DOM object with the given ID draggable.
 * If the object ID contains ".dnd", i.e. "container.dndheader",
 * then the whole parent object "container" will be dragged.
 */
function makeDraggable(id)
{
	draginit();
	$(id).onmousedown = function() { dragstart($(id)); };	
}

 
 /*******************************************************************************
  * 
  * Necessary functions for FSlider
  * 
  ******************************************************************************/
activeSlider=-1; //there can be only one active slider at a time - highlander :)
//because user can only drag one slider at a time

// keep the mouse down state
var mouseDown = 0;

/**
 * @param id - component id (e.g. div id)
 * @param val - actual value rendered (e.g. 50)
 * @param min - min value (e.g. 0)
 * @param max - max value (e.g. 100)
 * @param selectionOptional do you have to choose a value (e.g. NaN)
 * @param granularity - steps which can be selected (e.g. 1 every step, 2 every 2nd step)
 * @param vertbarsGran - which vertical lines will be rendered (e.g. 1 every step's line, 2 every 2nd step's line)
 * @param numberDisplayGranularity - which vertical lines will have numbers below  (e.g. 1 every step's line, 2 every 2nd step's line)
 * @param secondKnob Two slider for range selection
 * @param currentSecond startvalue of second knob
 */
function initSlider(fcompid, id, val, min, max, options)
{
	//TODO: document options field
	//TODO: store everything inside slider.options, to avoid passing everything as parameter
	var slider = document.getElementById(id);
	
	// delete existing bars
	while(getDiv(slider.id,'sliderBarVert')!=null){
		slider.removeChild(getDiv(slider.id,'sliderBarVert'));
	}
	// delete existing values
	while(getDiv(slider.id,'sliderBarVertValue')!=null){
		slider.removeChild(getDiv(slider.id,'sliderBarVertValue'));
	}
	var glowbar = createGlowingBar();
	slider.appendChild(glowbar);
	slider.min = min;
	slider.max = max;
	slider.fcompid = fcompid;
	slider.nrOfDecimals = options.nrOfDecimals;
	
	slider.secondKnob = options.secondKnob;
	//check if selectOptional is not set and startValue is NaN
	if(!options.selectionOptional && isNaN(val)){
		slider.val = min;
	}else{
		slider.val = val;
	}
	
	slider.granularity = Math.abs(max-min)/(options.nrOfSelectableValues-1);
	
	debugln("range ="+Math.abs(max-min)+" nrOfSelectableValues="+options.nrOfSelectableValues);
	
	// set initial values to knob1
	slider.activeKnob = 1;
	setBoxValue(slider.val, slider.granularity, slider.id);
	
	// ititialzie knob2
	if(options.secondKnob){
		slider.currentSecond = options.currentSecond;
		initSecondKnob(slider);
	}
	
	slider.selectionOptional = options.selectionOptional;
	slider.onmousedown = sliderMouseDown;
	slider.onmouseup = sliderMouseUp;
	slider.onmousemove = sliderMouseMove;
	//debugln("init slider "+slider.val);
	drawSliderByVal(slider);
	slider.onchange = setBoxValue;
	slider.onchange(slider.val, slider.granularity, slider.id);
	
	createUnitLabel(slider, options.unitLabel);
	
	// keep mouse down state
	document.body.onmousedown = function() {
	    mouseDown = 1;
	};
	document.body.onmouseup = function() {
	    mouseDown = 0;
	};
	
	//setUp vertical bars
	if(options.nrOfTicks < 1)
		return;
	if(options.selectionOptional)
		addNoValueOption(slider);
	
	//knob to control the slider
	var knob=getKnob(id);
	
	var tickStepWidthInPx = (slider.clientWidth-100)/(options.nrOfTicks-1);
	debugln("slider nrOfVerticalBars="+options.nrOfTicks+" stepWidthInPx="+tickStepWidthInPx);
	for ( var i = 0; i < options.nrOfTicks; i++)
	{
		//vertical lines
		var vert = createNewVertSpot();
		vert.style.left = (i*tickStepWidthInPx+50-1)+"px";
		//debugln(i+" = "+vert.style.left);
		slider.appendChild(vert);
	}
	
	var tickValuesStepWidthInPx = (slider.clientWidth-100)/(options.nrOfTickValues-1);
	debugln("slider nrOfVerticalValues="+options.nrOfTickValues+" valueStepWidthInPx="+tickValuesStepWidthInPx);
	for( var i = 0; i < options.nrOfTickValues; i++ )
	{
		
		var stepoffset = Math.abs(max-min)/(options.nrOfTickValues-1.0);
		
		//debugln("slider stepoffset="+stepoffset);
		
		var valToDisplay = (i*stepoffset)+min;
		
		//round to 15 numbers, after the comma -> js precision loss
		rvalToDisplay = fixRoundingIssues(id, valToDisplay, slider.granularity);
		debugln("value: "+valToDisplay+" rounded: "+rvalToDisplay+" gran: "+slider.granularity);
		
		var vertval = createNewVertDisplayValue(rvalToDisplay);
		
		vertval.style.left = (i*tickValuesStepWidthInPx+50-tickValuesStepWidthInPx/2)+"px";
		vertval.style.width = tickValuesStepWidthInPx+"px";
		slider.appendChild(vertval);
	}
}


/**
 * Initialize the second knob 
 * @param slider
 */
function initSecondKnob(slider)
{
	// draw value on second knob
	slider.activeKnob = 2;
	setBoxValue(slider.val, slider.granularity, slider.id);
	
	// set left side to grey
	var sliderBarLeft = getDiv(slider.id, 'sliderBarLeft');
	sliderBarLeft.style.backgroundImage='url(/ajax/icons/statusbarL_bw.png)';
}
/**
 * Adding the option to select no value
 * @param slider
 */
function addNoValueOption(slider){

	// expand slider bar to the left
	var div = getDiv(slider.id, 'sliderBarLeft');
	div.style.left = 26+'px';
	getDiv(slider.id, 'sliderBarGlow').style.left = 30+'px';
	
	// create new vertical line for "no value selected"
	var vert = createNewVertSpot();
	vert.style.left = 28+'px';
	slider.appendChild(vert);
	var vertval = createNewVertDisplayValue("n/a");
	vertval.style.left = 19+'px';
	slider.appendChild(vertval);
}


/**
 * sets the value of the slider
 * paints the number e.g. 200
 * @param val - value e.g. 200
 * @param granularity - granularity of the value e.g. 100
 * @param sliderid - e.g. sliderKnob
 * @return nop
 */
function setBoxValue(val, granularity, sliderid)
{
	var knob = getKnob(sliderid);
	var slider = $(sliderid);
	
	// only change selected knob
	if(slider.activeKnob==1){
		knob=getKnob(slider.id);
		val = slider.val;
	}
	if(slider.activeKnob==2){
		knob=getKnob2(sliderid);
		val = slider.currentSecond;
	}
	if(!isNaN(val))
	{ // if value is selected
		val = fixRoundingIssues(sliderid, val, granularity);
		knob.innerHTML=val;
		knob.style.color = '#89CA84'; // green
	}
	else
	{ // if no value is selected
		knob.innerHTML='n/a';
		knob.style.color = '#736F6E'; // grey
	}
	
}

/**
 * Add label which displays the unit of the slider values
 * @param slider
 * @param unitLabel label of the unit
 */
function createUnitLabel(slider, unitLabel){
	if(unitLabel=="" || unitLabel==null || unitLabel=="null") return;
	var unitLabelEl = document.createElement("div");
	unitLabelEl.innerHTML = unitLabel;
	unitLabelEl.style.color = '#808080';
	unitLabelEl.style.left = (slider.clientWidth-25)+'px';
	unitLabelEl.style.position = 'absolute';
	slider.appendChild(unitLabelEl);
}


/**
 * Map value to interval and cut needless decimals of
 * @param val
 * @param granularity
 * @param sliderid
 * @return rounded value (based on granularity)
 */
function fixRoundingIssues(sliderid, val, granularity)
{
	if(isNaN(val))
		return val;
	
	var slider = $(sliderid);
	//eliminate rounding errors
	var nrOfDigits = -Math.round((Math.log(granularity)/Math.log(10))-1);
	if(nrOfDigits<1 || !isFinite(nrOfDigits))
		nrOfDigits = 1;
	if(slider.nrOfDecimals != "null")
		nrOfDigits = slider.nrOfDecimals;
	
	val = round(val, nrOfDigits);
	debugln(slider.nrOfDecimals+" round to= "+val+" nrOfDigits="+nrOfDigits);
	val = checkForRanges(slider, val);
	return val;
}

/**
 * get knob from sliderid
 * check for a child of the slider 
 * which is type div and contains classname sliderKnob
 * @param sliderid
 * @return knob div element
 */
function getKnob(sliderid){
	return getDiv(sliderid, 'sliderKnob');
}

/**
 * get second knob from sliderid
 * check for a child of the slider 
 * which is type div and contains classname sliderKnob2
 * @param sliderid
 * @return knob div element
 */
function getKnob2(sliderid){
	return getDiv(sliderid, 'sliderKnob2');
}

 /**
  * get knob from sliderid
  * check for a child of the slider 
  * which is type div and contains classname sliderKnob
  * @param sliderid
  * @return knob div element
  */
function getGlowBar(sliderid){
	 return getDiv(sliderid, 'sliderBarGlow');
}

 /**
  * get specific div from div with id "domid"
  * which is type div and contains classname 'className'
  * @param domid
  * @param className
  * @return div element
  */
function getDiv(domid, className)
{
 	var divs = $(domid).getElementsByTagName('div');
 	for( var i = 0; i < divs.length; i++)
 	{
 		var cName = divs[i].className;
 		if(cName.indexOf(className) != -1)
 			return divs[i];
 	}
 	debugln('element not found');
 	return null;
}

 
function createGlowingBar(){
	var vert = document.createElement("div");
	vert.className = "sliderBarGlow";
	return vert;
}
 
 /**
 * 
 * @return div with vertical bar
 */
function createNewVertDisplayValue(value){
	var vert = document.createElement("div");
	vert.className = "sliderBarVertValue";
	vert.innerHTML = value;
	return vert;
}
 
 /**
  * 
  * @return div with vertical bar
  */
function createNewVertSpot(){
	var vert = document.createElement("div");
	vert.className = "sliderBarVert";
	var vertT = document.createElement("div");
	vertT.className = "sliderBarVertTop";
	var vertC = document.createElement("div");
	vertC.className = "sliderBarVertCenter";
	var vertB = document.createElement("div");
	vertB.className = "sliderBarVertBottom";
	vert.appendChild(vertT);
	vert.appendChild(vertC);
	vert.appendChild(vertB);
	return vert;
}

function sliderMouseDown(e){
	mouseDown = 1;
	sliderClick(e);
	var el=sliderFromEvent(e);
	if (!el) return;
	activeSlider=el.id;
	stopEvent(e);
}

/**
 * mouseUp
 * @param e
 */
function sliderMouseUp(e){
	mouseDown = 0;
	activeSlider=-1;
	stopEvent(e);
	var el = sliderFromEvent(e);
	if (!el) return;
	
	//redraw slider --> moves it to rounded position
	el.val = fixRoundingIssues(el.id, el.val, el.granularity);
	
	//send value to ajax servlet
	catchEventId(el.fcompid, 1, el.val+","+el.currentSecond);
	
	//debugln("slider mouseUp "+el.val);
	drawSliderByVal(el);
}

function sliderClick(e) {
	var el=sliderFromEvent(e);
	if (!el) return;
	if (!e && window.event) e=window.event;
	getActiveKnob(el,e.clientX); // check which knob was clicked
	setSliderByClientX(el, e.clientX);
}

function sliderFromEvent(e) {
	if (!e && window.event) e=window.event;
	if (!e) return false;
	
	var el;
	if (e.target) el=e.target;
	if (e.srcElement) el=e.srcElement;
	
	if (!el.id) el=el.parentNode;
	if (!el.id) el=el.parentNode;
	if (!el) return false;
	
	return el;
}

function stopEvent(e) {
	if (!e && window.event) e=window.event;
	if (e.preventDefault) {
		e.preventDefault();
		e.stopPropagation();
	} else {
		e.returnValue=false;
		e.cancelBubble=true;
	}
}

function sliderMouseMove(e) {
	
	if(mouseDown){
		var el=sliderFromEvent(e);
		if (!el) return;
		if (activeSlider<0) return;
		if (!e && window.event) e=window.event;
		setSliderByClientX(el, e.clientX);
		stopEvent(e);
	}
}

/**
 * Position the slider and the active bar.
 * @param slider
 * @return
 */
function drawSliderByVal(slider) {
	
	var knob=getKnob(slider.id);
	var x1 = drawSliderByVal2(slider, knob, slider.val);
	
	if(slider.secondKnob){
		var knob=getKnob2(slider.id);
		var x2 = drawSliderByVal2(slider, knob, slider.currentSecond);
		// move and scale active bar between the two knobs
		getGlowBar(slider.id).style.left = (x1+knob.clientWidth-10)+"px";
		var width = (x2-x1-knob.clientWidth/2);
		getGlowBar(slider.id).style.width = width>=0 ? width+"px" : 0 + 'px';
	}
}
/**
* positions the slider - sets the left x
*/
function drawSliderByVal2(slider, knob, val){
	 
	// if no value is selected
	if(slider.selectionOptional && isNaN(val) ){
		knob.style.left=4+"px";
		setBoxValue("n/a",slider.granularity, slider.id);
		knob.style.backgroundImage = 'url(/ajax/icons/sliderGreen_sw.png)';
		getGlowBar(slider.id).style.width = 0+"px";
	}else{
		knob.style.backgroundImage = 'url(/ajax/icons/sliderGreen.png)';
		var p=Math.abs((val-slider.min))/Math.abs((slider.max-slider.min)); // 1/4 // 3/100
		if(isNaN(p)) p = 0;
		var x=(slider.clientWidth-100)*p+50-(knob.clientWidth/2); // change to "clientWith" because of bug in chrome
		if(slider.clientWidth-knob.clientWidth<0){
			debugln('slider width was not set properly');
		}
		//debugln("slide left x: "+x+"");
		knob.style.left=x+"px";
		getGlowBar(slider.id).style.width = (x-20)+"px";
		return x;
	}
	
}

/**
* check which knob is clicked
*/
function getActiveKnob(slider, clientX){


	//compare click-position with knob position	
	var knob=getKnob(slider.id);
	var offsetL = getTotalOffsetWidthLeft(slider);
	var sliderPos = offsetL + Number(knob.style.left.replace("px",""));
	if(clientX > sliderPos && clientX < (sliderPos + knob.clientWidth)){	
		slider.activeKnob=1;
	}
	if(slider.secondKnob){ // if second knob is activated check position of second knob
		var knob=getKnob2(slider.id);
		var sliderPos = offsetL + Number(knob.style.left.replace("px",""));
		if(clientX > sliderPos && clientX < (sliderPos + knob.clientWidth)){
			slider.activeKnob=2;
		}
	}
}

/**
 * @param {Object} element - DOM element
 * @return the total offset from left (calculated from all parents' offsets)
 */
function getTotalOffsetWidthLeft(element)
{
	// because mouseclick position is absolute and the element offset is relativ to 
    // his offset-parent, accumulate the offsetLeft-values of all offset-parents
    var offsetL = element.offsetLeft;
    var offsetParent = element.offsetParent;
    if(offsetParent!=null)
	{
        while(offsetParent.offsetLeft!=null)
		{
            offsetL += offsetParent.offsetLeft;
            offsetParent = offsetParent.offsetParent;
            if(offsetParent==null) break;
        }
    }
	return offsetL;
}

function setSliderByClientX(slider, clientX) {
	
	// if user moves the slider to the left out of the range of the values (more than 10px)
	var offsetL = getTotalOffsetWidthLeft(slider);
	if(slider.selectionOptional && (clientX-50+10)<offsetL){
		slider.val = NaN;
		drawSliderByVal(slider);		
		return;
	}
	
    
	
	var p=(clientX-offsetL-50)/(slider.clientWidth-100);
	if(slider.activeKnob==1){
		slider.val=(slider.max-slider.min)*p + slider.min;
		
		// dont move slider over each other
		if(slider.secondKnob && slider.val>=slider.currentSecond)
			slider.val=slider.currentSecond;
		
		// check ranges for positive slider
		slider.val = checkForRanges(slider, slider.val);
		//debugln("setslider client afterx "+slider.val);
		drawSliderByVal(slider);
		//debugln("setslider onchange x "+slider.id);
		slider.onchange(slider.val, slider.granularity, slider.id);
	}
	if(slider.activeKnob==2){
		slider.currentSecond=(slider.max-slider.min)*p + slider.min;
		
		// dont move slider over each other
		if(slider.val>=slider.currentSecond) slider.currentSecond=slider.val;
		
		// check ranges for negative slider
		slider.currentSecond = checkForRanges(slider, slider.currentSecond);
		
		drawSliderByVal(slider);
		slider.onchange(slider.currentSecond, slider.granularity, slider.id);
	}

}
/**
*	Check if given value exceeds the ranges of the slider
*/
function checkForRanges(slider, val){
	var newVal = val;
	if(slider.max > slider.min)
	{
		if (val>slider.max) newVal=slider.max;
		if (val<slider.min) newVal=slider.min;
	}else{
		if (val<slider.max) newVal=slider.max;
		if (val>slider.min) newVal=slider.min;
	}
	debugln(" checkranges "+val+" "+newVal);
	return newVal;
}

/**
 * Functions for FGauge
 * TODO refactoring, renaming (as it was inspired)
 * TODO documentation
 * TODO label or image in the center
 */

/**
 * updates the gauge (moves the pointer)
 * @param gauge - gaugeComponent
 * @param position - actual value
 */
function updateGauge(gaugeId, position)
{
	debugln("updating gauge to "+position);
	var gaugel = $(gaugeId);
	var gauge = gaugel.gauge;
	
	var offset = Math.abs( gauge.position - position ) * gauge.options.updateStepWidthRatio;
	var updateTimer = null;
	debugln("gauge increment "+offset);
	/**
	 * movement of the pointer
	 */
	function movePointer()
	{
		/**
		 * end the movement, by setting the value, and stoping the timeout
		 */
		function endMoved()
		{
			debugln("gauge movement ended");
			gauge.position = round(position, gauge.nrOfDecimals);
			clearInterval( updateTimer );
		}
		
		if ( gauge.position >= position )
		{
			gauge.position -= offset;
			if ( gauge.position - offset <= position )
				endMoved();
		}
		else
		{
			gauge.position += offset;
			if ( gauge.position + offset >= position )
				endMoved();
		}
		debugln("gauge redraw "+gauge.position);
		gauge.drawGauge();
	}

	//start movement
	if ( gauge.position != position )
		updateTimer = setInterval( movePointer, gauge.options.updateSpeedInMs );
}

/**
 * initialization
 * @param canvId - canvas id
 * @param start - start value
 * @param end - end value
 * @param position - current position
 * @param options - complex options object @see FGauge#GaugeOptions
 */
function createGauge(canvId, start, end, position, options )
{
	//gauge specific
	debugln("init gauge "+canvId);
	this.canvId = canvId;
	this.start = start;
	this.end = end;
	
	//maximum 2 decimals, depending on the range
	this.nrOfDecimals = 2-Math.round(Math.log(this.end-this.start)/Math.log(10));
	if(this.nrOfDecimals < 0)
		this.nrOfDecimals = 0;
	if(this.nrOfDecimals > 2)
		this.nrOfDecimals = 2;
	
	debugln("init canvas gauge, decimals "+this.nrOfDecimals);
	this.position = round(position, this.nrOfDecimals);
	this.options = options;
	
	//InternetExplorer 8- canvas, check if ie specific code is defined --> excanvas
	if (typeof(G_vmlCanvasManager) != 'undefined')
		this.canvas = G_vmlCanvasManager.initElement($(this.canvId));
	else
		this.canvas = $(this.canvId);

	debugln("init canvas "+this.canvas.getContext('2d'));
	this.c2d = this.canvas.getContext('2d');
	debugln("c2d="+this.c2d);
	this.width = this.canvas.width;
	this.height = this.canvas.height;
	//calc the minimum radius
	this.radius = Math.min( this.width / 2, this.height / 2 );
	this.innerRadius = this.options.tickInnerRadius * this.radius;
	this.outerRadius = this.options.tickOuterRadius * this.radius;

	this.degVal = Math.PI/180;
	this.spanDeg = this.options.spanPercent*this.degVal; //--> 360
	this.startDeg = (270-this.options.spanPercent/2)*this.degVal;//Math.PI * 5.5 / 8;   180*(Math.PI/180))
	
	debugln("spanDeg="+this.spanDeg+" startDeg="+this.startDeg);
	
	
	this.centerX = this.radius;
	this.centerY = this.radius;
	
	/**
	 * draws a label
	 * uses:	options.labelColor
	 * 			options.labelString
	 */
	//public
	this.drawLabel = function()
	{
		//if options are set then draw the label
		if ( !this.options.labelString || this.options.labelString.length == 0 || !this.options.labelColor)
		{
			debugln("gauge label is not set");
			return;
		}
		this.startPainting();
		
		var fontSize = this.options.labelSize * this.radius;
		this.c2d.font = fontSize.toFixed(0) + 'px sans-serif';
		var textLength = this.c2d.measureText( this.options.labelString ).width;
		this.c2d.fillStyle = this.options.labelColor;
		var yradius = 0.35 * this.radius + fontSize / 2;
		this.c2d.fillText( this.options.labelString, -textLength / 2, yradius );
		
		this.stopPainting();
	};
	
	/**
	 * draws the background
	 * uses: options.bgColorArray
	 */
	//public
	this.drawBackground = function()
	{
		this.startPainting();
		
		var bgCircles = this.options.bgCircles;

		this.c2d.rotate( this.startDeg ); 
		for ( var i = 0; i < bgCircles.length; i++ )
		{
			var bgCircle = bgCircles[i];
			this.c2d.fillStyle = bgCircle.bgColor;
			this.c2d.beginPath();
			//make it a proper circle, which can then also be used in IE
			this.c2d.arc( 0, 0, bgCircle.radius * this.radius, 0, Math.PI * 2, true );
			this.c2d.fill();
		}
		this.stopPainting();
	};
	
	/**
	 * public
	 * draws the ticks
	 * uses:	options.majorTickColor
	 * 			options.numberOfTicksInBetween
	 */
	//public
	this.drawTicks = function()
	{
		this.startPainting();
		this.c2d.rotate( this.startDeg ); 
		this.c2d.lineWidth = this.options.tickLength * this.radius;
		var majorDeg = this.spanDeg / (this.options.numberOfTicks - 1); 
		for ( var i = 0; i < this.options.numberOfTicks; i++ )
		{
			this.c2d.beginPath();
			this.c2d.moveTo( this.innerRadius, 0 );
			this.c2d.lineTo( this.outerRadius, 0 );
			this.c2d.strokeStyle = this.options.tickColor;
			this.c2d.stroke();
			
			// minor ticks
			if ( i + 1 < this.options.numberOfTicks )
			{
				this.c2d.save();
				this.c2d.lineWidth = this.options.tickInBetweenLength * this.radius;
				var minorDeg = majorDeg / (this.options.numberOfTicksInBetween + 1);
				for ( var j = 0; j < this.options.numberOfTicksInBetween; j++ )
				{
					this.c2d.rotate( minorDeg );
					this.c2d.beginPath();
					this.c2d.moveTo( this.innerRadius + ( this.outerRadius - this.innerRadius ) / this.options.tickInBetweenOffset, 0 );
					this.c2d.lineTo( this.outerRadius, 0 );
					this.c2d.strokeStyle = this.options.tickInBetweenColor;
					this.c2d.stroke();
				}
				this.c2d.restore();
			}
			
			this.c2d.rotate( majorDeg );
		}
		this.stopPainting();
	};
	
	/**
	 * draws the current value of the gauge (displayed ont he bottom)
	 */
	//public
	this.drawCurrentValue = function()
	{
		this.startPainting();
		//format the value before displaying it
		var currValueToDraw = ''+round(this.position, this.nrOfDecimals);
		
		// value text
		var fontSize = this.options.currentValueSizeRatio * this.radius;
		this.c2d.font = fontSize.toFixed(0) + 'px sans-serif';
		var renderedTextLength = this.c2d.measureText( currValueToDraw ).width;
		this.c2d.fillStyle = this.options.textColor;
		this.c2d.fillText( currValueToDraw , -renderedTextLength / 2, this.options.currentValuePosY * this.radius );
		
		this.stopPainting();
	};
	
	/**
	 * draw values next to the ticks
	 */
	//public
	this.drawTickValues = function()
	{
		this.startPainting();
		
		var drawTickValRadius = this.options.tickValuesInnerRadius * this.radius;
		// labels
		var currentDeg = (180 + this.options.spanPercent/2 ) * this.degVal;
		
		var fontSize = this.options.tickValuesSizeRatio * this.radius;
		
		var xtranslate = -(fontSize/2);
		var ytranslate = fontSize/2;
		
		for ( var i = 0; i < this.options.numberOfValues; i++)
		{
			this.c2d.save();
			this.c2d.translate(drawTickValRadius * Math.sin( currentDeg ), drawTickValRadius * Math.cos( currentDeg ) );

			//numbersRotateHorizontal
			if(!this.options.numbersRotateHorizontal)
				//rotate so that values get aligned 
				this.c2d.rotate(-currentDeg + Math.PI);

			this.c2d.translate( xtranslate , ytranslate );

			this.c2d.font = fontSize.toFixed(0) + 'px sans-serif';
			debugln("drawTickvalues font: "+this.c2d.font);
			
			var renderedTextLength = this.c2d.measureText( ''+i ).width;
			this.c2d.fillStyle = this.options.textColor;
			var displayVal = ((this.end - this.start)/(this.options.numberOfValues - 1)) * i + this.start;
			displayVal = round(displayVal,this.nrOfDecimals);
			this.c2d.fillText( displayVal , -renderedTextLength / 2, 0 );
			this.stopPainting();
			
			//decrease degree
			currentDeg -= this.spanDeg / (this.options.numberOfValues - 1) ;
		}
		this.stopPainting();
	};
	
	/**
	 * draws the moving pointer
	 */
	//public
	this.drawPointer = function()
	{
		//if there is an image set, draw the image
		if(this.options.pointerImage && this.options.pointerImage != 'null')
		{
//			drawImage(image, x, y)
			var img = new Image();
			var ctx = this.c2d;
			var cenx = this.centerX;
			var ceny = this.centerY;
			var imgheightoffset = this.options.pointerImageHeightOffset;
			var initRot = 90*this.degVal+this.startDeg+ (this.spanDeg * (this.position - this.start)) / (this.end - this.start);
			//47 x 145
			img.onload = function()
			{
				ctx.save();
				ctx.translate( cenx, ceny );
				ctx.rotate( initRot );
				ctx.translate( -img.width / 2, -imgheightoffset );
				ctx.drawImage(img, 0, 0);
				ctx.restore();
			}
			img.src = this.options.pointerImage;
		}			
		else
		{
			this.startPainting();
			this.c2d.rotate( this.startDeg ); 
			this.c2d.rotate( (this.spanDeg * (this.position - this.start)) / (this.end - this.start) );
			function paintPointer( gauge, pointerHeight, pointerWidth )
			{
				gauge.c2d.beginPath();
				gauge.c2d.moveTo( -pointerWidth*4 * gauge.radius, 0 );
				gauge.c2d.lineTo( 0, pointerWidth * gauge.radius );
				gauge.c2d.lineTo( pointerHeight * gauge.radius, 0 );
				gauge.c2d.lineTo( 0, -pointerWidth * gauge.radius );
				gauge.c2d.lineTo( -pointerWidth*4 * gauge.radius, 0 );
			}
			
			this.c2d.lineWidth = this.options.pointerLineWidth * this.radius;
			this.c2d.fillStyle = this.options.pointerBgColor;
			paintPointer( this, this.options.pointerHeight, this.options.pointerWidth );
			this.c2d.fill();
			this.c2d.strokeStyle = this.options.pointerLineColor;
			paintPointer( this, this.options.pointerHeight, this.options.pointerWidth );
			this.c2d.stroke();
			
			function paintPointerCircle( gauge, pointerCircleRadius)
			{
				// center circle
				gauge.c2d.beginPath();
				gauge.c2d.arc( 0, 0, pointerCircleRadius * gauge.radius, 0, 2 * Math.PI, true );
			};
			this.c2d.lineWidth = this.options.pointerCircleLineWidth * this.radius;
			this.c2d.fillStyle = this.options.pointerCircleBgColor;
			paintPointerCircle( this, this.options.pointerCircleRadius );
			this.c2d.fill();
			this.c2d.strokeStyle = this.options.pointerCircleLineColor;
			paintPointerCircle( this, this.options.pointerCircleRadius );
			this.c2d.stroke();
			
			this.stopPainting();
		}
	};
	
	/**
	 * draws a range
	 */
	//public
	this.drawRanges = function()
	{
		for ( var i = 0; i < this.options.rangeArray.length; i++)
		{
			var range = this.options.rangeArray[i];
			var fromRange = range.rangeFrom;
			var toRange = range.rangeTo;
			var rangeColor = range.rangeColor;
			
			var interval = this.end-this.start;
			//if interval is not set, a range cannot be drawn
			if(interval == 0 || fromRange >= toRange)
				continue;
			debugln("drawRange="+interval);
			var span = this.spanDeg * ( toRange - fromRange ) / interval;
		
			this.startPainting();
			
			this.c2d.rotate( this.startDeg ); 
			this.c2d.fillStyle = rangeColor;
			this.c2d.rotate( this.spanDeg * (fromRange - this.start) / interval );    
			this.c2d.beginPath();
			this.c2d.moveTo( this.innerRadius, 0 );
			this.c2d.lineTo( this.outerRadius, 0 );
			this.c2d.arc( 0, 0, this.outerRadius, 0, span, false );
			this.c2d.rotate( span );
			this.c2d.lineTo( this.innerRadius, 0 );
			this.c2d.arc( 0, 0, this.innerRadius, 0, -span, true ); 
			this.c2d.fill();
			
			this.stopPainting();
		}
	};
	
	//public
	/**
	 * 
	 */
	this.clearPainting = function()
	{
		this.c2d.clearRect( 0, 0, this.width, this.height );
	};
	
	/**
	 * 
	 */
	//public
	this.stopPainting = function()
	{
		this.c2d.restore();
	};
	
	/**
	 * 
	 */
	//public
	this.startPainting = function()
	{
		this.c2d.save();
		this.c2d.translate( this.centerX, this.centerY );
	};
	
	/**
	 * 
	 */
	//public
	this.drawGauge = function()
	{
		debugln("gauge render:"+this.c2d);
		this.clearPainting();
		
		//if there is no data ( max is 0) -> display No data
		if(end-start<=0)
		{
			this.options.labelString = "No Data";
			this.drawBackground();
			this.drawLabel();
			this.drawTicks();
		}
		else
		{
			this.drawBackground();
			
			this.drawRanges();
			//caption
			this.drawLabel();
			
			this.drawTicks();
			this.drawPointer();
			this.drawTickValues();
			this.drawCurrentValue();
		}
	};
	
	//initial draw
	this.drawGauge();
	return this;
}

/**
 * Functions for analyzing checkbox forms
 **/

// Returns an array of all check boxes (index numbers).
// Empty array if nothing selected.
function getSelectedCheckbox(buttonGroup)
{
   var retArr = new Array();
   var lastElement = 0;
   if (buttonGroup[0])
   {
      for (var i=0; i<buttonGroup.length; i++)
      {
         if (buttonGroup[i].checked)
         {
            retArr.length = lastElement;
            retArr[lastElement] = i;
            lastElement++;
         }
      }
  	}
  	else
	{
      if (buttonGroup.checked)
      { // if the one check box is checked
         retArr.length = lastElement;
         retArr[lastElement] = 0; // return zero as the only array value
      }
   }
   return retArr;
}

//Returns an array of all check boxes (values).
//Empty array if nothing selected.
function getSelectedCheckboxValue(buttonGroup)
{
 var retArr = new Array();
 var selectedItems = getSelectedCheckbox(buttonGroup);
 if (selectedItems.length != 0)
 { // if there was something selected
    retArr.length = selectedItems.length;
    for (var i=0; i<selectedItems.length; i++)
    {
       if (buttonGroup[selectedItems[i]])
          retArr[i] = buttonGroup[selectedItems[i]].value;
       else
          retArr[i] = buttonGroup.value;
    }
 }
 return retArr;
}


//Returns an array of a limited number of check boxes (values).
//Empty array if nothing selected.
function getSelectedCheckboxValue(buttonGroup, limit)
{
var retArr = new Array();
var selectedItems = getSelectedCheckbox(buttonGroup);
if (selectedItems.length != 0)
{ // if there was something selected
	limit = limit<selectedItems.length?limit:selectedItems.length;
   retArr.length = limit;
	
   for (var i=0; i<limit; i++)
   {
      if (buttonGroup[selectedItems[i]])
         retArr[i] = buttonGroup[selectedItems[i]].value;
      else
         retArr[i] = buttonGroup.value;
   }
}
return retArr;
}





/****************************************************************************
 * 
 *  Necessary functions for client side table resize (FTable2)
 *  
 *   - initialization with 'initializeTableResize()' (called in flu_init())
 *   - mousedown event on splitter activates dragging
 *   - mouseup event stops dragging 
 *   
 *   NOTE: global onmouseup event and onmousemove are overwritten during drag
 * 
 ****************************************************************************/


var _startX = 0;            	// mouse starting position (x)
var _startY = 0;				// mouse starting position (y)
var _splitter = null; 			// splitter element
var _leftThCell = null;			// left header cell
var _rightThCell = null;		// right header cell
var _parentTable = null;		// parent table of header
var _ignoreClick = false;		// flag for ignoring sort click
var _widthPercentageL = 0;      // start_widthPercentage(leftTh)
var _widthPixelsL = 0;          // start_widthPixels(leftTh)
var _widthPercentageR = 0;      // start_widthPercentage(rightTh)
var _widthPixelsR = 0;          // start_widthPixels(rightTh)

var _tabKeyPressedLast = false;

var debugMode = true;			// if true, debug into div with id 'debug-div'
			



function initializeTableResize() {
	initializeSplitterDiv();
    setThWidthIfNotSet();
}

function debugTableResize( message ) {
	if (debugMode) {
		var _debug = $('debug-div');
		if (_debug)
			_debug.innerHTML = '' + message + ''; 
	}
}

function getElementsByClass( searchClass, domNode, tagName) {
    if (domNode == null) domNode = document;
    if (tagName == null) tagName = '*';
    var el = new Array();
    var tags = domNode.getElementsByTagName(tagName);
    var tcl = " "+searchClass+" ";
    for(i=0,j=0; i<tags.length; i++) {
        var test = " " + tags[i].className + " ";
        if (test.indexOf(tcl) != -1)
            el[j++] = tags[i];
    }
    return el;
}

function ExtractNumber(value) {
    var n = parseInt(value);
    return n == null || isNaN(n) ? 0 : n;
}

function ExtractNumberAsFloat(value) {
    var d = parseFloat(value);
    return d == null || isNaN(d) ? 0 : d;
}

function absLeft(el) {
	return (el.offsetParent)? el.offsetLeft+absLeft(el.offsetParent) : el.offsetLeft;
}

function absTop(el) {
	return (el.offsetParent)? el.offsetTop+absTop(el.offsetParent) : el.offsetTop;
}

function getTHLabelWidth( thElement ) {
	return thElement.getElementsByTagName('div')[1].getElementsByTagName('span')[0].offsetWidth;
}

function setThWidthIfNotSet() {
	var tableArr = getElementsByClass( 'resizeable', null, 'table' );
	for (i=0; i<tableArr.length; i++) {
		thArr = tableArr[i].getElementsByTagName( 'thead' )[0].getElementsByTagName('th');

		thWidth = Math.floor( 100 / thArr.length ); 
		sum=0;
		for (j=0; j<thArr.length-1;j++) {
			 if (!thArr[j].style.width)
				thArr[j].style.width = thWidth + "%";
			 thArr[j].style.minWidth = (getTHLabelWidth( thArr[j])+30) + 'px';
			 sum += ExtractNumberAsFloat( thArr[j].style.width );
		}  
		if (!thArr[thArr.length-1].style.width)
			thArr[thArr.length-1].style.width = (100-sum) + '%';
		thArr[j].style.minWidth = (getTHLabelWidth( thArr[thArr.length-1])+20) + 'px';
		sum += ExtractNumberAsFloat( thArr[thArr.length-1].style.width );
	}
}

function initializeSplitterDiv() {
	var splitterDiv = document.createElement("div");
    splitterDiv.id = 'splitterDiv';
    splitterDiv.style.position = 'absolute';
    splitterDiv.style.width = '1px';
    splitterDiv.style.height = '20px';
    splitterDiv.style.borderLeft = '1px dashed red';
    splitterDiv.style.display = 'none';
    document.body.appendChild(splitterDiv);
}

function setWidthOfElement( element, w ) {
	element.style.width = w;
}

function getParentTable( thElement ) {
	tmp = thElement.parentNode;
	while (tmp.nodeName.toLowerCase()!='table')
		tmp = tmp.parentNode;
	return tmp;
}

function getSplitterParent( splitter ) {
    return splitter.parentNode;	
}

function getLeftCell( splitter ) {
    tmp = getRightCell(splitter).previousSibling;
    while (tmp.nodeName.toLowerCase()!='th')
        tmp = tmp.previousSibling;
    return tmp;
}

function getRightCell( splitter ) {
	return getSplitterParent( splitter );
}

function getCellMinWidth( thCell ) {
	return ExtractNumberAsFloat( thCell.style.minWidth );
}

function round(number, digits) {
	return Math.round( number*Math.pow(10,digits) ) / Math.pow(10,digits);
}

function getTableColumnWidthAsArray( tableElement ) {
	thArr = tableElement.getElementsByTagName( 'thead' )[0].getElementsByTagName('th');
    var result = new Array(thArr.length);
    for (i=0; i<thArr.length; i++) {
        result[i] = ExtractNumberAsFloat( thArr[i].style.width );
    }
    return result;
}


function keyPressListener( event ) {
	if (event.keyCode==13) {// enter 
		src = (event.target) ? (event.target) : event.srcElement;
		src.onblur();
	}
	else if (event.keyCode==9) {// tab
		_tabKeyPressedLast = true;
	}
	else
		_tabKeyPressedLast = false;
}

function submitNumberOfPages( invoker )
{
	var el = (invoker.id.indexOf('Input')==-1)?($( invoker.id + 'Input')):invoker;
	var value = ExtractNumber( el.value);
	if (value<=0) {
		invoker.value = $(invoker.id+'Hidden').value;
		debugln("invalid page number specified");
		return true;
	}
	catchEvent( el, 1, value);
}

/**
 * resize the table cells (column width)
 * @param diff - current-starting position
 * @param mousePosX - current position
 * @return
 */
function resizeTableCells( diff, mousePosX ) {
	// previous width in pixels & new width in px
	wl_prev_px = _leftThCell.offsetWidth;
	wr_prev_px = _rightThCell.offsetWidth;
	wl_new_px = (_widthPixelsL + diff);
	wr_new_px = (_widthPixelsR - diff);

	if ( wl_new_px > getCellMinWidth(_leftThCell) && wr_new_px > getCellMinWidth(_rightThCell)) {
		// new percentage values:
		wl_prev_pc = _leftThCell.style.width;
	    wr_prev_pc = _rightThCell.style.width;
		wl_new_p = round( _widthPercentageL / _widthPixelsL * (_widthPixelsL + diff), 2);
	    wr_new_p = (( _widthPercentageL + _widthPercentageR ) - wl_new_p);
	    debugTableResize( "New width: left " + _leftThCell.style.width + " --- right " + _rightThCell.style.width );
	} else {
		// setting not possible
		debugTableResize( "SETTING NOT POSSIBLE, MINWIDTH REACHED" );
	}

	
	_leftThCell.style.width = (wl_new_p)?(wl_new_p + "%"):(wl_prev_pc);
	_rightThCell.style.width = (wr_new_p)?(wr_new_p + "%"):(wr_prev_pc);
	$('splitterDiv').style.left = mousePosX + 'px';
}
/**
 * 
 * @param event
 * @return
 */
function tableResize_OnMouseMove(event) {
    if (event == null) 
        var event = window.event; 
    var diff = event.clientX - _startX;
    resizeTableCells( diff, event.clientX );
}

function tableResize_OnMouseUp(e)
{
    if (_splitter != null)
    {
        // reset the sort catch events (th - onclick)
        window.setTimeout( "_ignoreClick = false;", 100 );
        $('splitterDiv').style.display = 'none';

        // inform the backend about new style size
        catchEvent(_leftThCell, 8, getTableColumnWidthAsArray( _parentTable ) );
       
        // clear the event handlers
    	document.onmouseup = null;
        document.onmousemove = null;
        document.onselectstart = null;
        _splitter.ondragstart = null;
 
        _splitter = null;
        _parentTable = null;
        _leftThCell = null;
        _rightThCell = null;
    }
}

function mouseDownOnSplitter( event, splitter ) {
	if (!event)
		return;

    // set the start variables
	_startX = event.clientX;
    _startY = event.clientY;
    _leftThCell = getLeftCell( splitter );
    _rightThCell = getRightCell( splitter );

    // get the start_width of elements
    _widthPercentageL = ExtractNumberAsFloat( _leftThCell.style.width );
    _widthPixelsL =  _leftThCell.offsetWidth;
    _widthPercentageR = ExtractNumberAsFloat(  _rightThCell.style.width );
    _widthPixelsR =  _rightThCell.offsetWidth;

    // show the splitterDiv
    _parentTable = getParentTable(_leftThCell);
    var splitterDiv = $('splitterDiv');
    splitterDiv.style.top =  absTop(_parentTable) + 'px';
    splitterDiv.style.left = event.clientX + 'px';
    splitterDiv.style.height = _parentTable.offsetHeight + 'px';
    splitterDiv.style.display = 'block';
    
    // set the splitter element
    _splitter = splitter;

    // tell code to not react on click events (sort)
    // reset it onmouseup
    _ignoreClick = true;
    
    // register event handlers and init dragging
	document.onmouseup = tableResize_OnMouseUp;
    document.onmousemove = tableResize_OnMouseMove;
    document.body.focus();
    document.onselectstart = function () { return false; };
    _splitter.ondragstart = function() { return false; };
    return false;
}





/****************************************************************************
 * 
 *  Necessary functions for FCalendar
 *  
 ****************************************************************************/


// script used for calendar component
function var_dump(obj) {
   if(typeof obj == "object") {
      return "Type: "+typeof(obj)+((obj.constructor) ? "\nConstructor: "+obj.constructor : "")+"\nValue: " + obj;
   } else {
      return "Type: "+typeof(obj)+"\nValue: "+obj;
   }
}


/**
 * fires an event on dateclick
 * @param calendar, local calendar
 */
function dateChanged(calendar)
{
	debugln("Calendar Date Changed");
	if(calendar == null)
		debugln("Calendar is null");
	if (calendar.dateClicked) {
		catchEventId(this.calendarId, 1, calendar.date);
	}
};


/*******************************************************************************
 * 
 * Necessary functions for FDropdownMenu2
 * 
 ******************************************************************************/


function initMenu() {
	/*setMenuItemsWidth();*/
}

var _tmpClassName;

function toggleDropdown(el, style) {
	$(el).style.display=style;
}

function toggleBckgrndHover(el, style) {
	el.className=style;
}

function setMenuItemsWidth() {
	var menuArr = getElementsByClass( 'item_l1', null, 'span' );
	for (i=0; i<menuArr.length; i++) {
		labelWidth = menuArr[i].offsetWidth;
		/* menuArr[i].parentNode.style.width=labelWidth + 'px'; */
		menuArr[i].parentNode.style.width='100px';
	}
}

/*****************************************
 *                                       *
 *        functions for ToolTip2         *
 *                                       *
 *****************************************/
function tt2dimension(x,y) {
	if(typeof x == 'number' && typeof y == 'number') {
		this.x = x;
		this.y = y;
	}
	else {
		this.x = 0;
		this.y = 0;
	}
	this.width = 0;
	this.height = 0;
	this.paddingLeft = 0;
	this.paddingRight = 0;
	this.paddingTop = 0;
	this.paddingBottom = 0;
	this.borderLeft = 0;
	this.borderRight = 0;
	this.borderTop = 0;
	this.borderBottom = 0;
}
function tt2getDimensions(el) {
	var d = new tt2dimension();
	
	var dt = tt2getOffset(el);
	d.x = dt.x;
	d.y = dt.y;
	
	d.width = el.offsetWidth;
	d.height = el.offsetHeight;
	
	var pl = parseInt(getStyle(el).paddingLeft);
	var pr = parseInt(getStyle(el).paddingRight);
	var pt = parseInt(getStyle(el).paddingTop);
	var pb = parseInt(getStyle(el).paddingBottom);
	
	if(!isNaN(pl))
		d.paddingLeft = pl;
	if(!isNaN(pr))
		d.paddingRight = pr;
	if(!isNaN(pt))
		d.paddingTop = pt;
	if(!isNaN(pb))
		d.paddingBottom = pb;
	
	var bl = parseInt(getStyle(el).borderLeftWidth);
	var br = parseInt(getStyle(el).borderRightWidth);
	var bt = parseInt(getStyle(el).borderTopWidth);
	var bb = parseInt(getStyle(el).borderBottomWidth);
	
	if(!isNaN(bl))
		d.borderLeft = bl;
	if(!isNaN(br))
		d.borderRight = br;
	if(!isNaN(bt))
		d.borderTop = bt;
	if(!isNaN(bb))
		d.borderBottom = bb;
	
	return d;
}
function tt2getOffset(child) {
	var d = new tt2dimension();
	var current = child;
	var parent = document.getElementsByTagName("body")[0];
	var nextOffset = current;

	while (current != parent) {
		if(nextOffset == current) {
			d.x += current.offsetLeft || 0;
			d.y += current.offsetTop || 0;
			d.x -= current.scrollLeft || 0;
			d.y -= current.scrollTop || 0;
			nextOffset = current.offsetParent;
		}
		current = current.parentNode;
	}
	return d;
}
function tt2show(el,parid,inner,wholewidth,attachtoparent) {
	if(typeof wholewidth != 'boolean')
		wholewidth = false;
	if(typeof attachtoparent == 'boolean')
		if(attachtoparent)
			attachtoparent = 1;
	if(typeof attachtoparent != 'number')
		attachtoparent = 0;

	var tt2 = $('tooltip2');
	
	tt2.innerHTML = inner;
	
	var dtt2 = tt2getDimensions(tt2);
	var dpid = tt2getDimensions($(parid));
	var del = new tt2dimension();
	
	var tempel = $(el);
	
	while(attachtoparent>0) {
		tempel = tempel.parentNode;
		attachtoparent--;
	}
	del = tt2getDimensions(tempel);

	var x = del.x;
	var y = del.y;

	var bodywidth = document.getElementsByTagName("body")[0].offsetWidth || window.innerWidth;
	var bodyheight = document.getElementsByTagName("body")[0].offsetHeight || window.innerHeight;
	var dtt2extrawidth = 0;
	var dtt2extraheight = 0;
	if (navigator.userAgent.indexOf('MSIE') ==-1) {
		dtt2extrawidth = dtt2.paddingLeft+dtt2.paddingRight+dtt2.borderLeft+dtt2.borderRight;
		dtt2extraheight = dtt2.paddingTop+dtt2.paddingBottom+dtt2.borderTop+dtt2.borderBottom;
	}

	tt2.style.maxWidth = bodywidth-dtt2extrawidth+'px';
	dtt2 = tt2getDimensions(tt2);

	if(x+dtt2.width > bodywidth)
		x -= Math.abs(x+dtt2.width - bodywidth)+1;
	
	if(y+dtt2.height+dtt2extraheight > bodyheight)
		y -= dtt2.height+1+del.borderTop;
	else
		y += del.height-del.borderBottom;
	
	if(wholewidth) {
		x = dpid.x;
		tt2.style.width = dpid.width-dtt2extrawidth+'px';
	}

	tt2.style.left = x+1+'px';
	tt2.style.top = y+1+'px';
	tt2.style.visibility = 'visible';
}
function tt2hide() {
	var tt2 = $('tooltip2');
	tt2.style.visibility = 'hidden';
	tt2.style.left = '-1000px';
	tt2.style.top = '-1000px';
	tt2.innerHTML = '';
	tt2.style.width = '';
}
function tt2add() {
	if($('tooltip2') == null) {
		var tt2 = document.createElement('div');
		tt2.id = 'tooltip2';
		document.getElementsByTagName("body")[0].appendChild(tt2);
	}
}

var tt2delay;

/**************************************
 *                                    *
 *        functions for FForm         *
 *                                    *
 **************************************/

function fformSetValid(id,b,fc) {
	var clazz = $(id).className;
	if(fc)
		clazz = $(id).firstChild.className;
	var s = clazz.indexOf("valid");
	if(s==-1 && b != null)
		clazz += (clazz.length>0?' ':'')+(b?'valid':'invalid');
	else {
		clazz = clazz.replace(/\b(in)?valid\b/i,'');
		clazz = clazz.replace(/\s{2,}/ig,' ');
		if(b!= null)
			clazz += (b?'valid':'invalid');
	}
	if(fc)
		$(id).firstChild.className = clazz;
	else
		$(id).className = clazz;
}
 
 function getMultipleComboBox(select){
	var str= new Array();
	select = $(select.id);
	for ( var i = 0; i < select.childNodes.length; i++) {
		var child = select.childNodes[i];
		if(child.selected==true)
			str.push(child.value);
	}
	return str;
}
 
 function selectMultipleValuesInCombobox(id,opts){
	 var select = $(id);
	 var optselected = 0;
	 for ( var i = 0; i < select.childNodes.length; i++) {
		 var option = select.childNodes[i];

		 // removes attribute from option, therefore setting it to false is not required
		 //works with all ie versions, safari, ff (setting it to false didn't work with safari)
		 option.removeAttribute("selected");
		 
		 if(optselected!=opts.length)
			 for( var j = 0; j < opts.length; j++)
			 {
				var txt = option.value;
				if(txt == opts[j])
				{
					optselected++;
					option.setAttribute("selected","selected");
					option.selected = true;
					if(optselected == opts.length)
						break;
				}
			 }
	}
 }


 /****************************************************************************
  * 
  *  Necessary functions for FExtender
  *  
  ****************************************************************************/
 var isCollapsed = 0;
 function toggleExtender(el) {
 	if(isCollapsed == 0) {
 		document.getElementById('extendable').style.display='block';
 		el.innerHTML = '- ';
 		isCollapsed = 1;
 	}
 	else {
 		document.getElementById('extendable').style.display='none';
 		el.innerHTML = '+ ';
 		isCollapsed = 0;
 	}
 }
 

 
 /****************************************************************************
 * 
 *  Necessary functions for FTree (table version)
 *  
 ****************************************************************************/
 
 /**
  * fills the list of removals with child elements of ele which match the attribute superid
  * @param ele - element's parent is used for superid lookup
  * @param index - start
  * @param removals - list which holds the ids of all which match superid
  */
 function removeRec(ele, index, removals){
	if(!index || index == -1)
		index=indexOfElementInParent(ele);
	for(var i=index+1; i<ele.parentNode.childNodes.length; i++){
		child = ele.parentNode.childNodes[i];
		if(!child.attributes || !child.getAttribute('superid'))
			break;
		var superid = child.getAttribute('superid');
		
		if(superid == ele.id)
		{
			removals.push(child);
			if(child.id)
			{
				removeRec(child,i, removals);
			}
		}
	}
}
 
 /****************************************************************************
 * 
 *  Necessary functions for IWB
 *  
 ****************************************************************************/

function toggleContent(contentid,buttonid,buttonClassShow,buttonClassHide){
	 var show;
	 if($(contentid).style.display == 'none'){
		 $(contentid).style.display = 'block';
		 show = true;
	 }else{
		 $(contentid).style.display = 'none';
		 show = false;
	 }
	 
	 if(show){
		 $(buttonid).className = buttonClassHide;
		 $(buttonid).setAttribute("class",buttonClassHide);
	 }else{
		 $(buttonid).className = buttonClassShow;
		 $(buttonid).setAttribute("class",buttonClassShow);
	 }
}

 /**
  * hides the element by setting its display style to 'none'
  * @param id component's id
  */
function hide(id){
	$(id).style.display = 'none';
}

/**
 * show the element by setting its display style to ''
 * this is not set to 'block', as the original display style should be not changed
 * e.g. if it is defined in css as display style 'inline', this will then not be changed to block
 * @param id component's id
 */
function show(id){
	$(id).style.display = '';
}

/****************************************************************************
 * 
 *  Expanding Collapsing slowly (sliding down/up)
 *  
 ****************************************************************************/


 
 var speed = 5;
 
 /**
  * 
  * @param id - id of the component which should be moved
  * @param timeToMove in seconds
  * @param toY - top value, component will be moved to
  * @param initiatorId - id of the initiator used for callback
  * @return
  */
 function moveSlow(id, timeToMove, toY, initiatorId){
	 var comp = $(id);
	 var fromY = comp.offsetTop;
	 if(fromY==toY){
		 debugln("nothing to move ");
		 catchEventId(initiatorId, 14, toY);
		 return;
	 }
	 var down = fromY<toY;
	 var absdistance = down?toY-fromY:fromY-toY;
	 debugln("moving distance "+absdistance);
	 var pxPerLoop = (fromY-toY)/timeToMove;
	 debugln("pxPerloop "+pxPerLoop+" from "+fromY+" to "+toY);
	 move(comp, fromY, toY, pxPerLoop, down, initiatorId);
 }
 
/**
 * 
 * @param comp - component which should be moved
 * @param currentY - variable which holds the current top value
 * @param toY - top value, component will be moved to
 * @param px - for upper movement (constant)
 * @param down - boolean for direction
 * @return
 */
function move(comp, currentY, toY, pxPerLoop, down, initiatorId)
{
	if(down && currentY < toY || !down && currentY > toY)
	{
		comp.style.top = currentY+"px";

		if(down){
			var changerate = Math.ceil((toY-currentY)/speed);
			changerate = changerate<1?1:changerate;
			debugln("changerate "+changerate);
			currentY += changerate;
		}else
			currentY -= pxPerLoop;	
		setTimeout((function(){move(comp, currentY, toY, pxPerLoop, down, initiatorId);}),1);
	}
	else
	{
		comp.style.top = toY+"px";
		debugln("move done");
		catchEventId(initiatorId, 14, toY);
	}
}

 
 /**
  * Slides up or down according to display == none
  * vertical
  * @param id - components id
  * @param timeToSlide - time to slide total in millis
  */
function toggleSlow(id, timeToSlide){
	var comp = $(id);
	var initialOverflow = comp.style.overflow;
	comp.style.overflow = "hidden";
	if(comp.style.display == "none")
	{ // down
		comp.style.display = "";
		var height = comp.offsetHeight;
		comp.style.height="0px";
		var pxPerLoop = height/timeToSlide;
		slide(comp,0,height,pxPerLoop,'down',height,initialOverflow);
	}
	else
	{ //up
		var height = comp.offsetHeight;
		var pxPerLoop = height/timeToSlide;
		slide(comp,height,height,pxPerLoop,'up',height,initialOverflow);
	}
}

/**
 * recursive function for sliding and setting the height
 * @param comp - container to slide
 * @param currentHeight
 * @param totalHeight - the height
 * @param px - number of pixels
 * @param direction - either "up" or "down"
 * @param initialHeight - initial height, used the reset the value
 * @param initialOverflow - initial overflow, used the reset the value
 */
function slide(comp, currentHeight, totalHeight, px, direction, initialHeight, initialOverflow){
	if(direction == 'down')
	{
		if(currentHeight < totalHeight)
		{
			comp.style.height = currentHeight+"px";
			currentHeight += px;
			setTimeout((function(){slide(comp,currentHeight,totalHeight,px,'down',initialHeight,initialOverflow);}),1);
		}
		else
		{
			//resets the values
			comp.style.height = "auto"; //full +px if something goes wrong
			if(initialOverflow){
				comp.style.overflow = initialOverflow;
			}else{
				comp.removeAttribute("overflow");
			}
			
		}
	}
	else if(direction == 'up')
	{
		if(currentHeight > 0)
		{
			comp.style.height = currentHeight+"px";
			currentHeight -= px;
			setTimeout((function(){slide(comp,currentHeight,totalHeight,px,'up',initialHeight,initialOverflow);}),1);
		}
		else
		{
			comp.style.display = "none";
			//set the initial height back
			comp.style.height = initialHeight+"px";
			if(initialOverflow){
				comp.style.overflow = initialOverflow;
			}else{
				comp.removeAttribute("overflow");
			}
		}
	}
}


 /**
  * toggle's the components display, between 'none' and ''
  * hide and show
  * @param id - components id
  */
 function toggleDisplay(id){
	$(id).style.display=($(id).style.display=='none')?'':'none';
	 
 }
 

 /****************************************************************************
  * 
  *  Necessary functions for Navigation of Tabs
  *  
  ****************************************************************************/
  
  
  //moves overflowing tabs to the left
  function flTab_moveLeft(id){
	  tab = $(id);
	  flTab_window = tab.getElementsByTagName("div")[0];
	  flTab = flTab_window.getElementsByTagName("div")[1];
	  flTab = flTab.getElementsByTagName("div")[0];
		
	  margin = getStyle(flTab).marginLeft;
	  margin = ExtractNumber(margin);
	  
	  if(margin<0){
		  if(margin+50> 0){
			  margin =0;
		  }else{
			  margin+=50;
		  }
		  
	  }else{
		  //navigation element
		 leftelement = flTab_window.getElementsByTagName("div")[0];
		 leftelement.setAttribute("style","display:none");
		 leftelement.style.display ="none";
	  }
	  if(margin<=0){
		  //navigation element
		  rightelement = navis[navis.length-1];
		  rightelement.setAttribute("style","");
		  rightelement.style.display ="block";
		 
	  }
	  flTab.setAttribute("style","margin-left: "+margin); 
	  flTab.style.marginLeft = margin+"";
	}

  function flTab_moveRight(id){

	tab = $(id);
	flTab_window = tab.getElementsByTagName("div")[0];
	flTab = flTab_window.getElementsByTagName("div")[1];
	flTab = flTab.getElementsByTagName("div")[0];

	margin = getStyle(flTab).marginLeft;
	margin = ExtractNumber(margin);
	navis = flTab_window.getElementsByTagName("div");

	li = flTab.getElementsByTagName("ul")[0].getElementsByTagName("li");
	tabwith = 0;
	for ( var i = 0; i < li.length; i++) {
		tabwith+=li[i].offsetWidth;
	}

	  if(flTab_window.clientWidth-margin < tabwith){
		  if(flTab_window.clientWidth-margin+50> tabwith){
			  margin -= 0;
			  //navigation element
			  right = navis[navis.length-1];
			  right.setAttribute("style","display:none");
			  right.style.display = "none";
		  }else{
			  margin-=50;
		  }
		  
	  }else{
		  //navigation element
		  right = navis[navis.length-1];
		  right.setAttribute("style","display:none");
		  right.style.display ="none";
	   }

	  
	  if(margin<=0){
		  //navigation element
		  left = navis[0];
		  left.setAttribute("style","");
		  left.style.display = "block";
	  }

	  flTab.setAttribute("style","margin-left: "+margin+"");
	  flTab.style.marginLeft = margin+"";

	}
  
  //initializes tabs (checks for overflow)
  function flTab_init(id){

	tab = $(id);
	flTab_window = tab.getElementsByTagName("div")[0];
	flTab_tabs = flTab_window.getElementsByTagName("div")[1];
	flTab_tabs = flTab_tabs.getElementsByTagName("div")[0];
	
	li = flTab_tabs.getElementsByTagName("ul")[0].getElementsByTagName("li");
	tabwith = 0;
	for ( var i = 0; i < li.length; i++) {
		tabwith+=li[i].offsetWidth;
	}
	maxwidth =flTab_window.clientWidth;
	navis = flTab_window.getElementsByTagName("div");
	leftelement = navis[0];
	rightelement = navis[navis.length-1];

	//hide navigation elements

	
	//show navigation element if an overflow exists
	
	if(maxwidth <= tabwith){

		leftelement.setAttribute("style","display:none");

		leftelement.style.display ='none';

		
		rightelement.setAttribute("style","display:block");
		rightelement.style.display ='block';

		
	}else{
		leftelement.setAttribute("style","display:none");
		leftelement.style.display ="none";
		
		rightelement.setAttribute("style","display:none");
		rightelement.style.display ="none";
		
	}
  }
  /* return IE version if the browser is IE */ 
  function getMsieVersion()
  {
     var ua = window.navigator.userAgent
     var msie = ua.indexOf ( "MSIE " )

     if ( msie > 0 )      // If Internet Explorer, return version number
        return parseInt (ua.substring (msie+5, ua.indexOf (".", msie )))
     else                 // If another browser, return 0
        return -1

  }
  
 /* check for browser */  
function checkBrowserName(name)
{  
  var agent = navigator.userAgent.toLowerCase();  
  return agent.indexOf(name.toLowerCase()) > -1;
} 

//converting a string to json utils

String.prototype.convertToString = 
	Number.prototype.convertToString = 
	Boolean.prototype.convertToString = function (key)
	{
		return this.valueOf();
	};

Date.prototype.convertToString = function (key) {

    return isFinite(this.valueOf()) ?
        this.getUTCFullYear()     + '-' +
        f(this.getUTCMonth() + 1) + '-' +
        f(this.getUTCDate())      + 'T' +
        f(this.getUTCHours())     + ':' +
        f(this.getUTCMinutes())   + ':' +
        f(this.getUTCSeconds())   + 'Z' : null;
};

/**
* quotes the input string
* @param str - input string
*/
function appendQuotes(str)
{
	escapable.lastIndex = 0;
	return escapable.test(str) ? '"'
			+ str.replace(escapable, function(a)
			{
				var c = meta[a];
				return typeof c === 'string' ? c : '\\u'
						+ ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
			}) + '"' : '"' + str + '"';
}

/**
* @param objectKey - key of the current object
* @param transportArray - keeps all the objects
* @return a string representation of the object
*/ 
function createStringFromObjRec(objectKey, transportArray)
{
	var gap = '', _k, res, length, mind = '', partial, jsObj = transportArray[objectKey];

	if (jsObj && typeof jsObj === 'object' && typeof jsObj.convertToString === 'function')
		jsObj = jsObj.convertToString(objectKey);

	switch (typeof jsObj)
	{
	case 'string':
		return appendQuotes(jsObj);

	case 'number':
		return isFinite(jsObj) ? String(jsObj) : 'null';

	case 'boolean':
	case 'null':
		return String(jsObj);

	case 'object':
		if (!jsObj)
			return 'null';

		gap += indent;
		partial = [];

		if (Object.prototype.toString.apply(jsObj) === '[object Array]')
		{

			length = jsObj.length;
			for ( var i = 0; i < length; i++)
			{
				partial[i] = createStringFromObjRec(i, jsObj) || 'null';
			}

			res = partial.length === 0 ? '[]' : gap ? '[\n' + gap
					+ partial.join(',\n' + gap) + '\n' + mind + ']' : '['
					+ partial.join(',') + ']';
			gap = mind;
			return res;
		}

		for (_k in jsObj)
		{
			if (Object.prototype.hasOwnProperty.call(jsObj, _k))
			{
				res = createStringFromObjRec(_k, jsObj);
				if (res)
					partial.push(appendQuotes(_k) + (gap ? ': ' : ':') + res);
			}
		}

		res = partial.length === 0 ? '{}' : gap ? '{\n' + gap
				+ partial.join(',\n' + gap) + '\n' + mind + '}' : '{'
				+ partial.join(',') + '}';
		gap = mind;
		return res;
	}
}

/**
 * @param jsObj -
 *            the javascript object which should be converted to a json string
 * @return a javascript object as string
 */
function convertToString(jsObj)
{
	escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g;
	meta = { // table of character substitutions
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        };
	indent = '';
	return createStringFromObjRec('',
		{
			'' : jsObj
		});
}
/****************************************************************************
 * 
 *  Helper function for the FMap 
 *  
 ****************************************************************************/
function switchContent(div1, div2) 
{
//TODO fix this here
	with(document.getElementById(div1).style)
	{
	display="block";
	}
	with(document.getElementById(div2).style)
	{
	display="none";
	}
}

/**
* Ask for Confirmation before the onClick()-Method ist executed
*/
function showConfirmation(question, id, dialogTitle, okText, abortText){

	var flDialog = getDiv('FPopupWindow', 'flDialog' );
	flDialog.style.width = '40%';
	
	// make popup visible
	$('FPopupWindow').className = 'visible';
	
	// set title
	var flDialogHeader = getDiv('FPopupWindow', 'flDialogHeader' );
	flDialogHeader.innerHTML = dialogTitle;
	
	// add confirmation question and buttons to the popup
	var flDialogContent = getDiv('FPopupWindow', 'flDialogContent' );

	flDialogContent.innerHTML = "<div class=\"confirmationQuestion\">" + question + "</div>" +
		"<div class=\"buttonbar\"><input type='button' value='"+okText+"' onclick=\"catchEventId('" + id + "',1);hidePopup()\">"+
		"<input type='button' value='"+abortText+"' onclick='hidePopup()'></div>";
	//TODO this is wrong here --> should call the FComponent onclick
	centerPopUp();
	return false;

}

/**
* Hide the popup
*/
function hidePopup(){
	
	$('FPopupWindow').className = 'hidden';
	
}


/**
 * Check if an object is contained in the given array
 * @param a Array to search in 
 * @param obj Object to search for
 * @returns {Boolean}
 */
function contains(a, obj){
  for(var i = 0; i < a.length; i++) {
    if(a[i] === obj){
      return true;
    }
  }
  return false;
}


/**
 * Center the popup -> if user has scrolled on the page, the popup has to be moved down
 */
function centerPopUp(){
	
	var elems = $('.flDialog'); // get all elements with class-name 'flDialog' because some java-classes use new FPopupWindow() instead of getPopupWindow()
	
	
	// get the dialog which is visible and has the highest z-index -> only center this popup
	if(elems.length>0){
		var maxZindex = 0;
		var elemOnTop = elems[0];
		for(i=0;i<elems.length;i++){
			
			if(elems[i].parentNode!=null && !contains(elems[i].parentNode.className.split(" "), 'hidden')){
				
				// get z-index
				var elemZindex;
				if(window.getComputedStyle)
					elemZindex = window.getComputedStyle(elems[i], null).zIndex; // the optional pseudoElt must be null for firefox <= 3.6
				else if(elems[i].currentStyle) // zIndex for ie7
					elemZindex = elems[i].currentStyle.zIndex;
					
				if(elemZindex != null && elemZindex != "" && elemZindex >= maxZindex){
					maxZindex = elemZindex;
					elemOnTop = elems[i];
				}
			}
		}
	}
		
	// center horizontal
	var left = (getWindowSize()[0] - elemOnTop.clientWidth) / 2 + getScrollXY()[0];
	elemOnTop.style.left = left+"px";
	
	// center vertical
	var top = (getWindowSize()[1] - elemOnTop.clientHeight) / 4 + getScrollXY()[1];
	elemOnTop.style.top = (top > 0) ? top+"px" : 0+"px";
	
	
	
}


/**
 * Get scroll-position of the browser window
 * tested for ie 5-10, chrome, safari, opera
 */
function getScrollXY() {
  var scrOfX = 0, scrOfY = 0;
  if( typeof( window.pageYOffset ) == 'number' ) {
    //Netscape compliant
    scrOfY = window.pageYOffset;
    scrOfX = window.pageXOffset;
  } else if( document.body && ( document.body.scrollLeft || document.body.scrollTop ) ) {
    //DOM compliant
    scrOfY = document.body.scrollTop;
    scrOfX = document.body.scrollLeft;
  } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
    //IE6 standards compliant mode
    scrOfY = document.documentElement.scrollTop;
    scrOfX = document.documentElement.scrollLeft;
  }
  return [ scrOfX, scrOfY ];
}

/**
 * Get window inner dimensions of the browser window
 * tested for ie 5-10
 */
function getWindowSize() {
  var myWidth = 0, myHeight = 0;
  if( typeof( window.innerWidth ) == 'number' ) {
    //Non-IE
    myWidth = window.innerWidth;
    myHeight = window.innerHeight;
  } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
    //IE 6+ in 'standards compliant mode'
    myWidth = document.documentElement.clientWidth;
    myHeight = document.documentElement.clientHeight;
  } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
    //IE 4 compatible
    myWidth = document.body.clientWidth;
    myHeight = document.body.clientHeight;
  }
  return [ myWidth, myHeight ];
}

/**
 * Return all Elements with given class-name
 */
function getElementsByClassName(name){
	var foundElems = new Array();
	var allElems = document.getElementsByTagName('*');
	for (var i = 0; i < allElems.length; i++) {
		var thisElem = allElems[i];
		if (thisElem.className && thisElem.className == name) {
			foundElems.push(thisElem);
		}
	}
	return foundElems;
}

/**
 * gets called from ajax.js default implementation
 */
function getContextPath()
{
	//if this is not interpreted as JSP return relative path ""
	var res = "<%=request.getContextPath()%>/";
	if(res.indexOf("%=request.getContextPath()%")===1)
		return "";
	return res;
}

// code for drag and drop file upload
function dragEnter(evt) 
{
	evt.currentTarget.style.color = "red";
	evt.stopPropagation();
	evt.preventDefault();
}

function dragExit(evt) 
{
	evt.currentTarget.style.color = "";
	evt.stopPropagation();
	evt.preventDefault();
}

function dragOver(evt) 
{
	evt.currentTarget.style.color = "red";
	evt.stopPropagation();
	evt.preventDefault();
}

function drop(evt) 
{
	evt.currentTarget.style.color = "";
	evt.stopPropagation();
	evt.preventDefault();
	
	var id = evt.currentTarget.id;
	id = id.substring( 0, id.length-5 );
	for (i=0;i<=evt.dataTransfer.files.length;i++)
	{
		var xhr = new XMLHttpRequest();
		if (xhr.upload)
		{
			var formdata = new FormData();
			formdata.append("media", evt.dataTransfer.files[i]);
			xhr.open("POST", "/ajax/req/" + id, true);
			xhr.send( formdata );
		}
	}
}



