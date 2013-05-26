$!
  HTML code for PDF interface (Print Preview)
  
 
  
  @param contextPath the contextPath of the web application
  @param title the title of the page
  @param pageRef the relative URL of the resource's page, e.g. /resource/Help:Start
  @param footer a common footer with meta information
  
  @author as
!$
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
	<title>$title$</title>                        
	<link type="text/css" href="$contextPath$/stylesheet_fiwb.css" rel="stylesheet" />
	<link type="text/css" href="$contextPath$/css/semwiki.css" rel="stylesheet" />
	<link rel="stylesheet" href="$contextPath$/ajax/stylesheet_fajax.css" type="text/css" />	
	
    <!--[if lte IE 7]><link rel="stylesheet" href="$contextPath$/css/ie7hacks.css" type="text/css" /><![endif]-->
    <!--[if IE 8]><link rel="stylesheet" href="$contextPath$/css/ie8hacks.css" type="text/css" /><![endif]-->	

	<script type="text/javascript" src="$contextPath$/ajax/ajax.js"></script>
    <script type="text/javascript" src="$contextPath$/amchart/js/amcharts.js"></script>
   	<script type="text/javascript" src="$contextPath$/amchart/js/amfallback.js"></script>
   	<script type="text/javascript" src="$contextPath$/svg/canvg.js"></script>
   	<script type="text/javascript" src="$contextPath$/svg/rgbcolor.js"></script>
		
	<script type="text/javascript">	
	
	var timer;
	
	onload=function(){
		if (document.getElementsByClassName == undefined) {
			document.getElementsByClassName = function(className)
			{
				var hasClassName = new RegExp("(?:^|\\s)" + className + "(?:\$|\\s)");
				var allElements = document.getElementsByTagName("*");
				var results = [];

				var element;
				for (var i = 0; (element = allElements[i]) != null; i++) {
					var elementClass = element.className;
					if (elementClass && elementClass.indexOf(className) != -1 && hasClassName.test(elementClass))
						results.push(element);
				}

				return results;
			}
		}
	}
	
	function onFrameLoad(iframe) {
	   
	    timer=setInterval(function(){progressCallback(iframe)},250);
	}
	
	function progressCallback(iframe) {

	    var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
	    
	    var display = iframeDoc.getElementById("progress").style.display;
	    if (display!="none")
	    	return;
		
		clearInterval(timer);
		
	    // copy inner html
	    var target = document.getElementById("target");
	    
		target.innerHTML = iframeDoc.getElementsByClassName("viewContentClazz")[0].innerHTML;

		// hide loading icon
		document.getElementById("progressImg").style.display="none";
		
		// show header & footer
		document.getElementById("print_footer").style.display="block";
		document.getElementById("print_header").style.display="block";
	    
	    timerSvg=setInterval(function(){ 
	    
			 AmCharts.getExport();
				
		    var svgs = document.getElementById("target").getElementsByTagName('svg');
	    	if (svgs==null || svgs.length==0)
	    	{
	    		var success=true;
    			var images = document.getElementsByClassName("ImageForChart");
	    		for (var i=0;i<images.length;i++)
	    		{
	    			success = success && images[i].clientHeight!=null && images[i].clientHeight>0;
	    		}
	    		
	    		if (success)
	    		{
		    		clearInterval(timerSvg);
		    		window.print();
		    	}
	    	}
		}, 1000);
	 }
	 
	</script>
	
</head>

<body>
	<div id="progressIcon">
		<img id="progressImg" src="/ajax/icons/busy_anim.gif" alt="Loading" title="Loading">
	</div>
	<div id="print_header" style="display:none">$header$</div>
	<div id="target"></div>
	<iframe id="iframe" src="$pageRef$" onload="onFrameLoad(this)" style="width:100%; display:block; visibility:hidden"></iframe>
	<div id="print_footer" style="display:none">$footer$</div>
</body>

</html>
