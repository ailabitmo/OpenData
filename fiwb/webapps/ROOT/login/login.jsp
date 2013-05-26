
<%!
   /** 
    * Retrieve the referrer from request
    */
   public String getReferrerFromRequest(javax.servlet.http.HttpServletRequest request){
		String referrer = request.getParameter("j_referrer");
		if (referrer==null)
			referrer = "/";
		return referrer;
   }

	/** 
	 * Retrieve the referrer from request
	 */
	public String getErrorMessageFromRequest(javax.servlet.http.HttpServletRequest request){
			String message = request.getParameter("message");
			return message == null ? "" : message;
	}
%>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<META HTTP-EQUIV="CACHE-CONTROL" CONTENT="NO-CACHE">

		<title>Information Workbench &ndash; Login</title>
		 
		<!-- Feuerwasser Design -->
		<link href="fop_select_login.css" rel="stylesheet" type="text/css" />
		
		
		<!--[if lte IE 7]>
			<link rel="stylesheet" type="text/css" href="/css/ie7hacks.css" />
		<![endif]-->
		
    </head>

	<body>
		
		<!-- As it needs a long time to verify the information against the Authentication Manager we show a verifying screen -->
		<div id="loadingscreen" style="display: none;">
			<div>
				<div>
					<div id="loadingflops"></div>
					Verifying...
				</div>
			</div>
		</div>
		
		<div id="fw_wrapper_login">
		
			<!-- show message box if no JavaScript is available -->
			<noscript>
				<div class="infobox" style="width: 15em; margin:auto;">
					<table class="resizable" rules="groups">
						<thead>
							<tr>
								<th>The Information Workbench is optimized for JavaScript</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>
									<span style="color:red; font-weight:bold;">Please activate JavaScript for this web application.
									<br />You can continue at your own risk, but many features will not be available with the current
									 browser settings.
									</span>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<br />
			</noscript>
			
			<div id="fw_login_top_editioniwb"></div>
			<div style="position: absolute; text-align: right; width: 340px; margin-top: 16px; font-weight: bold; font-size: 16pt;">
			</div>
			<div id="fw_login_bottom">
				<form name="login_form" method="POST">
					<div class="topDescription">
						<span>Please enter your credentials</span>
					</div>
					<div class="bottomDescription">
						<div>
							<span>Domain users can use DOMAIN\\user or user@domain as Username.</span>
						</div>
					</div>
	
					<div align="right">
						<span class='formFieldLabel'>Username&nbsp;&nbsp;</span> 
						<span class='loginfeld'>
							<input type='text' name='j_username' id='username' class='fw_login_field' />
						</span>
						<div style='height: 9px;'></div>
						<span class='formFieldLabel'>Password&nbsp;&nbsp;</span>
						<span class='loginfeld'>
							<input type='password' name='j_password' id='password' class='fw_login_field' />
						</span>
						<div style='height: 9px;'></div>
						<input type="hidden" name="j_session" value="<%= request.getSession(true).getId() %>" />
						<input type="hidden" name="j_redirect" value="<%= getReferrerFromRequest(request)  %>" />
						<input type="submit" name="btnLogin" id="send" value="Login" class="fw_login_button" />
						<div class="fw_login_status"><%= getErrorMessageFromRequest(request) %></div>
					</div>
				</form>
			</div>
			<div id="fw_login_logo"></div>
		</div>
	</body> 
</html>