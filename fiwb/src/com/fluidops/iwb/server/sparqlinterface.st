$!
  HTML code four the SPARQL interface
  
  This HTML page is rendered by the SPARQLServlet if no query is passed
  as parameter.
  
  @param contextPath the contextpath of the web application
  
  @author as
!$

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html version="-//W3C//DTD XHTML 1.1//EN"
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<title>SPARQL Query Interface</title>
<meta name="Copyright"
	content="Copyright &copy; 2011 fluid Operations AG" />
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />

<script type="text/javascript" src="$contextPath$/codemirror/codemirror.js"></script>
<script type="text/javascript" src="$contextPath$/codemirror/sparql.js"></script>
<script type="text/javascript" src="$contextPath$/codemirror/searchcursor.js"></script>
<script type="text/javascript" src="$contextPath$/codemirror/match-highlighter.js"></script>
<link rel="stylesheet" href="$contextPath$/codemirror/codemirror.css"/>

<script type="text/javascript">

	$! values supported in graph query !$
	var supportedGraphQuery = new Array("auto","application/rdf+xml","text/plain",
			  "text/turtle","text/rdf+n3","application/x-trig","application/trix");
	var supportedTupleQuery = new Array("auto","application/sparql-results+xml",
			  "application/sparql-results+json","text/csv");
	
	$! function that is executed on submit: checks format validity !$
	function onFormSubmit(form)
	{
		var query = form.query.value; 
		var format = form.format.value;
		var infer = document.getElementById("inferCB").checked;
		
        $! set inference of hidden field according to checkbox selection !$
        document.getElementById("infer").value = (infer ? 'true' : 'false');

		<!-- check supported types for construct queries
		if (query.match(/\bconstruct\b/i) || query.match(/\bdescribe\b/i)) {
			if (!arrayContains(supportedGraphQuery,format)) {
				alert('Format ' + format + ' not supported for graphqueries.')
				return false;
			}
		}
		else if (query.match(/\bselect\b/i)) {
			if (!arrayContains(supportedTupleQuery,format)) {
				alert('Format ' + format + ' not supported for tuple queries.')
				return false;
			}
		}

		return true;	
	}
	
	$! test if the specified array contains the object !$
	function arrayContains(arr,obj) { 
	    return (arr.indexOf(obj) != -1); 
	} 
	
	function onInit() {
		
		$! create a sparql editor with syntax highlighting !$
	    var editor = CodeMirror.fromTextArea(document.getElementById("query"), {
	        			lineNumbers: true,
	         			onCursorActivity: function() {editor.matchHighlight("CodeMirror-matchhighlight"); }
	      				});
      		
		$! define the protoype Array.prototype.indexOf if it is not present maybe needed for ie6 !$
		if (!Array.prototype.indexOf) { 
		  Array.prototype.indexOf = function (obj) { 
		    for (var i = 0, j = this.length; i < j; i++) { 
		        if (this[i] === obj) 
		            return i; 
		    } 
		    return -1; 
		  }; 
		} 
		
	}

</script>
	
</head>

<body onload="onInit()">

	<h2>SPARQL Query Interface</h2>
	<form action="" method="post" onsubmit="return onFormSubmit(this)">
		<label for="query">Enter your query in the text field below.</label>
		<br />
		<textarea name="query" id="query" onchange=";" cols="80" rows="15">select distinct ?type where { ?x rdf:type ?type }</textarea>

		<br /><br />
		<label for="format">Results format:</label>
		<select name="format" id="format" size="1">
			<option value="auto" title="auto" selected="selected">auto</option>
			<option value="application/sparql-results+xml" title="SPARQL/XML">SPARQL/XML</option>
			<option value="application/sparql-results+json" title="SPARQL/JSON">SPARQL/JSON</option>
			<option value="text/csv" title="SPARQL/CSV">SPARQL/CSV</option>
			<option value="application/rdf+xml" title="RDF/XML">RDF/XML</option>
			<option value="text/plain" title="N-Triples">N-Triples</option>
			<option value="text/turtle" title="Turtle">Turtle</option>
			<option value="text/rdf+n3" title="N3">N3</option>
			<option value="application/x-trig" title="TriG">TriG</option>
			<option value="application/trix" title="TriX">TriX</option>

		</select>
		
		$! checkbox and hidden field for inferencing, hidden field is set in onsubmit js code !$
		&nbsp;&nbsp;&nbsp;&nbsp;
		<input type="checkbox" id="inferCB" /> Inferencing (requires repository support)<br>
		<input type="hidden" id="infer" name="infer" value="false" />
		<input type="hidden" id="forceDownload" name="forceDownload" value="true" />
		
		<br/><br/>
		<input type="submit" value="Submit" />
		<input id="reset" type="reset" value="Reset" />
	</form>


</body>
</html>