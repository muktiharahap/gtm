<html>

<head>
	<title><g:message code="default.welcome.title" args="[meta(name:'app.name')]"/> </title>
	<meta name="layout" content="kickstart" />
</head>

<body>
	<section>
		<div class="row">
			<div class="col-lg-3 col-xs-6">
				<!-- small box -->
				<div class="small-box bg-aqua">
					<div class="inner">
						<h3>150</h3>
						<p>New Gallon</p>
					</div>
					<div class="icon">
						<span class="glyphicon glyphicon-list"></span>
					</div>
					<a href="${createLink(controller: 'productionInHeader', action: 'production')}" class="small-box-footer">Production In</a>
				</div>
			</div>
		</div>
	</section>
</body>

</html>
