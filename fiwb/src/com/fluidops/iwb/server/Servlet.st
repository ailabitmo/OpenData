$!
    Main template for the WikiServlet page.
    
    This template renders the whole screen, top contains infos about viewed
    entity, menu to log in users, plus the "desktop" with Wiki widgets and
    all other optional widgets rendered.
    
    (HTML back ported from the out.println-mess)


    Arguments of this template:
        
    @param term The name of the entity (term) being displayed
    @param refer The name of the referring URL
    @param user The user name (or null if no user logged in)
    @param capitalizedUser The capitalized user name (or null)
    @param uriString The URI of the term
    @param path The root path of the web app
    @param body The body of the content area
    @param loggedIn indicates if IWB-user is logged into the system
	@param user the user currently logged in
	@param tracking google analytics tracking code
	@param toolBarButtons additional toolbar buttons
	@param searchfield the html anchor of the search input field
	@param searchlabel the html anchor of the search input field
	@param licenseNagging the information displayed when license issues occur
	@param userScript the script handles the license visualisation

    @author uli
!$
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
         <title>$term$</title>
        <script type='text/javascript' src='$path$/ajax/ajax.js'></script>
        $userScript$
        
        $if (uriString)$
            <link rel="alternate" title="RDF/XML" type="application/rdf+xml" href="$path$/rdf/?uri=$uriString$"/>
            <link rel='alternate' type='application/rss+xml' title='New info on $term$' href='$path$/rss.jsp?uri=$uriString$'/>
        $endif$
                
        <link rel="shortcut icon" href="$path$/favicon.ico" />

        <link rel='stylesheet' href='$path$/ajax/stylesheet_fajax.css' type='text/css' />
        $head$
        <link rel='stylesheet' href='$path$/stylesheet_fiwb.css' type='text/css' />
        <!--[if lte IE 7]><link rel='stylesheet' href='$path$/css/ie7hacks.css' type='text/css' /><![endif]-->
        
        <!--[if IE 8]><link rel='stylesheet' href='$path$/css/ie8hacks.css' type='text/css' /><![endif]-->

        <link rel='stylesheet' href='$path$/css/semwiki.css' type='text/css' />

  <!--    <script type='text/javascript' src='http://download.skype.com/share/skypebuttons/js/skypeCheck.js'></script> -->
  <!--      <link rel="stylesheet" type="text/css" href="jsgantt.css" /><script language="javascript" src="jsgantt.js"></script>-->
    </head>

    <body onload='javascript:fluInit(); $onLoad$'>
        <div id="pageContainer" class="pageContainer">
            <div id="pageHeader">
                <div class="topmenubar">

					$toolBarLeft$
                	
                    <div align="center" class="topmenubarCenterDIV navButton" style="left:50%;position:absolute" onclick="\$r('$path$/');"></div>
                    <div class="topmenubarRight">
                       <div class="topmenubarSpacerRight1"></div>
                       <div style="display: none;" class="topmenubarRightButtonEvents navButton" onclick="\$r('/jsps/events.jsp');"></div>
                       <div style="display: none;" class="topmenubarRightSpacer"></div>
                       
                       $toolBarButtons$
                                           
                       <div onclick="\$r('http://www.fluidops.com');" class="topmenubarRightLogo navButton"></div>
					   <div class="topmenubarRightSpacer"></div>
                    </div>
                </div>

                <div class="lowermenubar">
                	<div class="lowermenubarGreyLeftTransition"></div>
                    <div class="lowermenubarGreyLeft"></div>
                    <div class="lowermenubarCenter navcenter">
                         <div class="lowermenubarCenterDIV centerDIV" id="pageTitle">
                             <table border="0" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td>
                                        <div class="titleLeft"></div>
                                    </td>
                                    <td>
                                        <div class="titleCenter"><div>$term$</div></div>
                                    </td>
                                    <td>
                                        <div class="titleRight"></div>
                                    </td>
                                 </tr>
                             </table>
                         </div>
                    </div>
                    <div class="lowermenubarGreyRight"></div>
                    <div class="lowermenubarRight">
                        <div class="searchContainer">
                            $searchfield$
                            $searchlabel$
                        </div>
                    </div>
                </div>
                
                <div id="description" onclick="catchEventId('description', 1)"></div>
            </div>

            $! this div contains the desktop content !$
            <div class='pageBody' id='c'>
            $licenseNagging$
            $body$
            </div>
	    	<noscript>Please enable JavaScript to display this page.</noscript>
       
        <div style="display:none;">
        	<div id="hide0"></div>
        	<div id="hide1"></div>
        	<div id="hide2"></div>
        	<div id="hide3"></div>
        	<div id="hide4"></div>
        </div>
    </body>
    $tracking$
</html>
