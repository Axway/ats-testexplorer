/*Root element and its width and height*/
var svg;
var svgWidth;
var svgHeight;

/*Rectangle that wraps all of the svg content (text, rectangle,circle etc.)*/
var svgRect;
var svgRectWidth = 0;
var svgRectHeight = 0;
var svgRectX = 10;
var svgRectY = 10;
/*how round edges the svgRect will have*/
var svgRectRX = 20;
var svgRectRY = 20;

var runsData;
/*product name, version name, etc.*/
var chartData;
/*status of the last run (PASS/FAIL)*/
var statusData;

var chartDataDrawn = false;
var statusIndicatorDrawn = false;

var maxProductAndVersionChartDataLength = 0;

var COLOR_RED = "#FF0000";
var COLOR_YELLOW = "#FFC000";
var COLOR_GREEN = "#9DBB61";

var SVG_RECT_COLOR = "#F2F2F2";
var colors = [COLOR_GREEN, COLOR_RED, COLOR_YELLOW];

var rectColors = ['#FDECDD','#DBEEF3','#EEEBF2'];
var rectColorIdx = 0;

var fontFace = 'Open Sans';
var fontSize = 18;

d3.select(window).on('resize', resize);

function setChartData(data){
	chartData = data;
}

function setRunsData(data){
	runsData = data;
}

function setStatusData(data){
	statusData = data;
}

function resize(){
	svgWidth = $(window).innerWidth() * 0.9;
	svgHeight = $(window).innerHeight() * 0.75;
	init();
}

function init(){
	
	/*reset values on page load event*/ 
	rectColorIdx = 0;
	maxProductAndVersionChartDataLength = 0;
	
	/*remove the svg, because its data is already incorrect*/
	removeSvg();
	
	/* find largest number of build types for single product and version name pair*/
	for (datum of chartData){
		maxProductAndVersionChartDataLength = (maxProductAndVersionChartDataLength < datum.length) ? datum.length : maxProductAndVersionChartDataLength;
	}
	
	svgHeight = (320 * maxProductAndVersionChartDataLength);
	svgWidth = (300 * chartData.length);
	if(svgWidth < 300){
		svgWidth = 300;	
	}
	if(svgHeight < 320){
		svgHeight = 320;	
	}
	
	initSvg();
	initSvgRect();
	
	/*if there is no data, draw the appropriate UI (SVG) elements*/
	if(chartData.length == 0){
		drawNoData();
	}

	/*create chart (titles for product, version, build, total executions and passing rate for each entry in chartData*/
	var productGroup = svg.selectAll('.productGroup')
	  .data(chartData)
	  .enter()
	  .append('g') /*create the svg group element*/
	  .attr('class', 'productGroup')
	  .attr('id', function(d,i){return 'productGroup'+i;})
	  .each(function(d,i){
		  
		  chartDataDrawn = false;
		  statusIndicatorDrawn = false;
		  
		  var productAndVersionIdx = i;
		  var productAndVersionChartData = d;
		  
		  /*create group (g) for each product and version pair*/
		  drawProductAndVersionGroup(d3.select(this), d, i);
		  
		  /*get the rectangle that wraps the current product and version pair content*/
		  var productAndVersionGroupBoundingRect = d3.select(this).select('rect');
		  
		  /*create group for each build type of the current product and version pair*/
		  var buildTypeGroup = d3.select(this).selectAll('.buildTypeGroup')
						.data(d)
						.enter()
						.append('g')
						.attr('class', 'buildTypeGroup')
						.attr('id', function(d,i){return 'buildTypeGroup'+productAndVersionIdx+d.type.replace(' ','');})
						.each(function(d,i){
								/*draw the circle, runs dots and status indicator for this build type*/
								drawBuildTypeGroup(d3.select(this), productAndVersionGroupBoundingRect, d, productAndVersionChartData, productAndVersionIdx, i);
						});
						
	  });
	
}

function drawNoData(){
	
	/*padding for the noSuiteDataText text element*/
	var paddingY = parseFloat(svgRect[0][0].attributes['height'].value)/2;
	var paddingX = parseFloat(svgRect[0][0].attributes['width'].value)/2;
	
	var noSuiteDataText = svg.append('text')
	                            .attr('id','noSuiteDataText')
	                            .attr('x',parseFloat(svgRect[0][0].attributes['x'].value) + paddingX)
	                            .attr('y',parseFloat(svgRect[0][0].attributes['y'].value) + paddingY)
	                            .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
								.attr("alignment-baseline","middle")
								.attr("text-anchor","middle")
	                            .text('No Data found.');
}

function initSvg(){

	svg = d3.select("#"+'container')
			.append('svg')
			.attr("width", svgWidth)
			.attr("height", svgHeight);

}

function removeSvg(){
	d3.select('svg').remove();
}

function initSvgRect(){
	
	svgRectWidth = (svgWidth) - (svgRectX*2);
	svgRectHeight = (svgHeight) - (svgRectY*2);
	
	svgRect = initRect(undefined,[svgRectX,svgRectY,svgRectWidth,svgRectHeight],
				'svgRect','fill:'+ SVG_RECT_COLOR +';stroke:black;stroke-width:1.0'
	);
	svgRect.attr('rx',svgRectRX).attr('ry',svgRectRY);
	
}

function drawProductAndVersionGroup(group, data, idx){
	
	var productAndVersionChartData = data;
	
	/*X and Y position offset for the productAndVersionGroupRect*/	
	var offsets = [0,10];
	
	// space between different productAndVersionGroupRects
	var rectToRectPadding = 20;
	
	var productAndVersionGroupBoundingRectW = ((svgRectWidth-(offsets[0]*2))/chartData.length)-rectToRectPadding;
		
	var productAndVersionGroupBoundingRectH = svgRectHeight-(offsets[1]*2);
		
	var productAndVersionGroupBoundingRectX = (svgRectX + offsets[0]) + (+ rectToRectPadding/2) + (idx*(productAndVersionGroupBoundingRectW)) + rectToRectPadding*idx;
	
	var productAndVersionGroupBoundingRectY = svgRectY+(offsets[1]);
		
			
	var bounds = [
						productAndVersionGroupBoundingRectX,
						productAndVersionGroupBoundingRectY,
						productAndVersionGroupBoundingRectW,
						productAndVersionGroupBoundingRectH
	];
	
	/*get the color for the current group rectangle*/
	var rectColor = rectColors[rectColorIdx++];
	
	/*if we run out of colors, zero the rectColorIdx and start cycling them again*/
	if(rectColorIdx >= rectColors.length){
		rectColorIdx = 0;
	}
			
	/*create (initialize) the group rect for this product and version group*/
	var productAndVersionGroupBoundingRect = initRect(group, bounds, 
					'productAndVersionGroupBoundingRect'+idx,
					'fill:'+ rectColor +';stroke:black;stroke-width:1.0');
}

function drawBuildTypeGroup(buildTypeGroup, productAndVersionGroupBoundingRect, data, productAndVersionChartData, productAndVersionIdx, buildTypeIdx){

	/*link which is used/visited when user click on the circle, that corresponds for this product, version and build type*/
	var linkTarget = window.location.href.substring(0,window.location.href.lastIndexOf('/'))+"/dashboardrun?&productName="+data.product+"&versionName="+data.version+"&type="+data.type;
	
	var buildTypeChartData = data;
	var buildTypeRunsData = runsData[productAndVersionIdx][buildTypeIdx];

	// position at the center of the parent rect
	var offsets = [0,0];
	
	var buildTypeGroupBoundingRectW = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['width'].value);
		
	var buildTypeGroupBoundingRectH = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['height'].value)/maxProductAndVersionChartDataLength + offsets[1]*2;
		
	var buildTypeGroupBoundingRectX = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['x'].value) + offsets[0];
		
	var buildTypeGroupBoundingRectY = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['y'].value) + offsets[1] + (buildTypeIdx)*buildTypeGroupBoundingRectH;
	
	
	if(buildTypeGroupBoundingRectH < 320){
		buildTypeGroupBoundingRectH = 320;	
	}
	
	
	var buildTypeGroupBoundingRectBounds = [
						buildTypeGroupBoundingRectX,
						buildTypeGroupBoundingRectY,
						buildTypeGroupBoundingRectW,
						buildTypeGroupBoundingRectH
			];
	/* rectangle for this build type*/
	var buildTypeGroupBoundingRect = initRect(buildTypeGroup, buildTypeGroupBoundingRectBounds, 
					'buildTypeGroupBoundingRect'+productAndVersionIdx+data.type.replace(' ',''),
					'fill-opacity:'+ 0 +';stroke:black;stroke-width:0.0');
					
					
	var circleBounds = [
				buildTypeGroupBoundingRectBounds[0] + buildTypeGroupBoundingRectBounds[2]/2 -parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['width'].value)/6.5,
				buildTypeGroupBoundingRectBounds[1] + buildTypeGroupBoundingRectBounds[3]/2,
				(buildTypeGroupBoundingRectBounds[2] >= buildTypeGroupBoundingRectBounds[3]) ? buildTypeGroupBoundingRectBounds[3] * 0.25 : buildTypeGroupBoundingRectBounds[2] * 0.25
	];
	/*circle that wraps the run dots for this build type*/						
	var buildTypeGroupCircle = initCicle(buildTypeGroup, circleBounds, 'buildTypeGroupCircle'+productAndVersionIdx+data.type.replace(' ',''),
										'fill:'+ '#E8EEF8' +';stroke:black;stroke-width:1.0');
										
	
	var buildTypeGroupCircleRectOverlayBounds = [
				circleBounds[0]-circleBounds[2],
				circleBounds[1]-circleBounds[2],
				circleBounds[2]*2,
				circleBounds[2]*2
	];
	/*rectangle that overlays (wraps) the circle, containing the dots, and handles the click event*/
	var buildTypeGroupCircleRectOverlay = initRect(buildTypeGroup, buildTypeGroupCircleRectOverlayBounds, 
					'buildTypeGroupCircleRectOverlayBounds'+productAndVersionIdx+data.type.replace(' ',''),
					'fill-opacity:0;fill:white;stroke:black;stroke-width:0.0;cursor:pointer');
					
					
	buildTypeGroupCircleRectOverlay.on('mouseover', function() { 
										buildTypeGroupCircle.style({'stroke':  '#89C4F4', 'stroke-width':  '2'});
										
							         })
								     .on('mouseout', function() { 
										buildTypeGroupCircle.style({'fill':'#E8EEF8', 'stroke':'black', 'stroke-width':'1.0'});
								     })
								     .on('click', function(){
										 window.location.href = linkTarget;
									});
	/*change the font size, according to the buildTypeRect size*/								
	var size = (buildTypeGroupBoundingRectBounds[2] >= buildTypeGroupBoundingRectBounds[3]) ? buildTypeGroupBoundingRectBounds[3] : buildTypeGroupBoundingRectBounds[2];								
	fontSize = Math.ceil(size * 0.055);
	if(fontSize > 18){
		fontSize = 18;
	}
	if(fontSize < 12){
		fontSize = 12;
	}
									
	/* draw titles (append text element to this buildType group) for product, version, build etc*/
	drawChartDatum(buildTypeGroup, buildTypeGroupBoundingRect, data, productAndVersionIdx+data.type.replace(' ',''), buildTypeGroupCircle);

	/* we want to see runs dots only when their position is somethere inside the circle,
	 * so we are using the clipPath element which has the same x, y and radius values as the circle, containing the runs dots
	 * */
	var clipPath = buildTypeGroup.append("clipPath")
		              .attr("id", "clipPath"+productAndVersionIdx+data.type.replace(' ',''))
	                  .append("circle")
	                  .attr("cx", parseFloat(buildTypeGroupCircle[0][0].attributes['cx'].value))
	                  .attr("cy", parseFloat(buildTypeGroupCircle[0][0].attributes['cy'].value))
                      .attr("r", parseFloat(buildTypeGroupCircle[0][0].attributes['r'].value));
    
    /* create group for the runs dots */
    var dotsGroup = buildTypeGroup.append("g").attr('id','dotsGroup'+productAndVersionIdx+data.type.replace(' ',''))
						   .attr("clip-path", "url(#clipPath"+productAndVersionIdx+data.type.replace(' ','')+")");
						   
	//runs data for this product, version and build type
	var runsDatum = runsData[productAndVersionIdx][buildTypeIdx];
	
	if(runsDatum.length > 0){
		/*draw runs dots*/
		drawRunsDots(runsDatum, dotsGroup, buildTypeGroupCircle, productAndVersionIdx+data.type.replace(' ',''), linkTarget);
		/*we want to draw status indicator only once for each product and version pair*/
		if(!statusIndicatorDrawn){
			drawStatusIndicator(buildTypeGroup, buildTypeGroupCircleRectOverlay, buildTypeGroupCircle, 
			productAndVersionIdx+data.type.replace(' ',''), statusData[productAndVersionIdx][buildTypeIdx]);
			statusIndicatorDrawn = false;
		}
		
	}
	else{
		drawNoRunsDatum(buildTypeGroup, buildTypeGroupCircle);
	}
}

function drawChartDatum(group, boundingRect, datum, idSubtype, circle){
	
	// initial position (y) for the productText
	var paddingY = parseFloat(boundingRect[0][0].attributes['height'].value)*0.1;
	// every next title (besides product and totalIterations) is with this offset by Y
	var titleOffsetY = fontSize*1.2;
	
	var productText = group.append("text") // append text svg element
					   .attr('id','productText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize*1.1+'px '+fontFace, 'font-weight': 'bold'})
					   .attr("x",parseFloat(circle[0][0].attributes['cx'].value))
					   .attr("y",parseFloat(boundingRect[0][0].attributes['y'].value)+paddingY)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.product);
		               
	var versionText = group.append("text")
					   .attr('id','versionText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					   .attr("x",parseFloat(productText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(productText[0][0].attributes['y'].value) + titleOffsetY)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.version);
		               
	/*make a little more space (vertical) between version and type text elements*/
	var separatorText = group.append("text")
					   .attr('id','separatorText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					   .attr("x",parseFloat(versionText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(versionText[0][0].attributes['y'].value) + titleOffsetY/4)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text("");
		               
	var typeText = group.append("text")
					   .attr('id','typeText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					   .attr("x",parseFloat(separatorText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(separatorText[0][0].attributes['y'].value) + titleOffsetY)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.type);
		               
	var totalIterationsText = group.append("text")
					   .attr('id','totalIterationsText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					   .attr("x",parseFloat(productText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(circle[0][0].attributes['cy'].value) + parseFloat(circle[0][0].attributes['r'].value) + titleOffsetY*0.75)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.totalIterations);
		               
	var passingRateText = group.append("text")
					   .attr('id','passingRateText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					   .attr("x",parseFloat(totalIterationsText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(totalIterationsText[0][0].attributes['y'].value) + titleOffsetY)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.passingRate);
		               
	/*product and version text elements must be visible only once for each product and version pair*/
	if(chartDataDrawn){
		productText.text("");
		versionText.text("");
	}
	else{
		chartDataDrawn = true;
	}
	
}

function drawRunsDots(runsDatum, dotsGroup, circle, idSubtype, linkTarget){
	var clusters, nodeMap, nodeNest, nodePass, nodeFail, nodeWarn, nodes=[], node, force, quadtree,
					
		//Graph margins in pixels
		margin = { top: 0, right: 0, bottom: 0, left: 0}, 
		
		//Padding between clustered nodes.
		padding = 2,
				  
		width = parseFloat(circle[0][0].attributes['cx'].value) * 2,
		
		height = parseFloat(circle[0][0].attributes['cy'].value) * 2,
				
		//Padding between clusters.
		clusterPadding = 2,
				
		//Radius of individual nodes.
		maxRadius = parseFloat(circle[0][0].attributes['r'].value) * 0.055,
				
		//Number of distinct clusters.
		m = 2,
				
		//Switch to denote whether the force layout is moving.
		moving = 0,
				
		//Switch to denote whether the force layout movement
		//is slight enough to begin counting down settleTicks.
		settle = 0,    

		//Number of ticks to allow time for force layout to settle into place.
		//500 ticks is roughly 5 seconds.
		settleTicks = 500;
		
		// entirely not needed for now
		runsDatum.forEach(function(d) {
							if(!d.Status){d.Status = d.status;}
							if(!d.Result && d.Status == "OK"){d.Result = "PASS";}
							if(!d.Result && d.Status == "KO"){d.Result = "FAIL";}
							if(!d.Result && d.Status == "passed"){d.Result = "PASS";}
							if(!d.Result && d.Status == "failed"){d.Result = "FAIL";}
							if(!d.Result && d.Status == "Passed"){d.Result = "PASS";}
							if(!d.Result && d.Status == "Failed"){d.Result = "FAIL";}     

						}
		);
		
		//Establish a node for each cluster.
		clusters = new Array(m);
			 
		//Map test data to node template with properties for force layout simulaiton.
		nodeMap = runsDatum.map(function(q) {
			var i = q.Result == "PASS" ? 0 : q.Result == "FAIL" ? 1 : q.Result == "WARN"? 2 : null,
			r = Math.sqrt((1) / m ) * maxRadius,
			d = {cluster: i, radius: r, result: q.Result, test: q._id};
			if (!clusters[i] || (r > clusters[i].radius)) {clusters[i] = d;}
			return d;
		});

		nodeNest = d3.nest().key(function(d) { return d.cluster; }).entries(nodeMap);
				
		nodePass = new genPack(nodeNest["0"].values);

		nodeFail = nodeNest["1"] ? new genPack(nodeNest["1"].values):null;
				
		nodeWarn = nodeNest["2"] ? new genPack(nodeNest["2"].values):null;     
				
		nodePass[0].forEach(function(d){nodes.push(d);});
				
		if(nodeFail){nodeFail[0].forEach(function(d){nodes.push(d);});}
				
		if(nodeWarn){nodeWarn[0].forEach(function(d){nodes.push(d);});}
		
		//Initialize force layout simulation
		force = d3.layout.force()
				   .nodes(nodes)
				   .size([width, height])
				   .gravity(0.059)
				   .charge(0)
				   .on("tick", tick)
				   .start();

		//Set emitter point for each node based on its cluster.
		nodes.forEach(function(node) {
			var i = 1;
			if (node.cluster===0){node.x=0;node.y=0;} //PASS
			if (node.cluster===1){
				node.x=parseFloat(circle[0][0].attributes['cx'].value)+parseFloat(circle[0][0].attributes['r'].value);
				node.y=parseFloat(circle[0][0].attributes['cy'].value)+parseFloat(circle[0][0].attributes['r'].value);
			} //FAIL
			if (node.cluster===2){node.x=0;node.y=0;} //WARN
			i++;
		});
		
		//Append result nodes.
		node = dotsGroup.selectAll(".resCircle"+idSubtype)
						.data(nodes)
						.enter()
						.append("circle")
						.attr('class','resCircle'+idSubtype)
						.style({'cursor':'pointer', 'fill' : function(d) { return colors[d.cluster]; }})
						.attr("r", function(d){return d.radius;})
						.on('mouseover', function(d) {
								circle.style({'stroke':  '#89C4F4', 'stroke-width':  '2'});
						})
						.on('click', function(){window.location.href = linkTarget;});

		//Function to generate pack layout for nodes. 
		function genPack(root){
				var escape = [],
				result = d3.layout.pack()
						   .padding(1.2)
						   .size([180, 180])
						   .radius(4)
						   .children(function(d) { return d.values; })
						   .nodes({values: root});
				escape.push(result);
				return(escape);
		}

		function tick() {  
			node.each(collide(0.3))
				.attr("cx", function(d) { return d.x; })
				.attr("cy", function(d) { return d.y; });
					if (force.alpha()<0.0051 && moving==1){force.alpha(0.007);}
					if (settle==1 && moving==1){settleTicks--;} 
					nodes.forEach(function(d){
						moving = 0;
						settle = 0;
						if(!d.children){if(d.fx){ if(Math.abs(d.fx - d.x)>0.005){moving=1;}}}
						if(!d.children){if(d.fy){ if(Math.abs(d.fy - d.y)>0.005){moving=1;}}}
						if(!d.children){if(d.fx){ if(Math.abs(d.fx - d.x)<0.005 && Math.abs(d.fx - d.x)>0.0017){moving=1;settle=1;}}}
						if(!d.children){if(d.fy){ if(Math.abs(d.fy - d.y)<0.005 && Math.abs(d.fy - d.y)>0.0017){moving=1;settle=1;}}}
						if(settleTicks<1){moving=0;settle=0;}
						d.fx=d.x;
						d.fy=d.y;});
		}

		// Resolves collisions between d and all other circles.
		function collide(alpha) {
			quadtree = d3.geom.quadtree(nodes);
			return function(d) {
				var r = d.radius + maxRadius + Math.max(padding, clusterPadding),
				nx1 = d.x - r,
				nx2 = d.x + r,
				ny1 = d.y - r,
				ny2 = d.y + r;
				quadtree.visit(function(quad, x1, y1, x2, y2) {
					if (quad.point && (quad.point !== d)) {
						var x = d.x - quad.point.x,
						y = d.y - quad.point.y,
						l = Math.sqrt(x * x + y * y),
						r = d.radius + quad.point.radius + (d.cluster === quad.point.cluster ? padding : clusterPadding);
						if (l < r) {
							l = (l - r) / l * alpha;
							d.x -= x *= l;
							d.y -= y *= l;
							quad.point.x += x;
							quad.point.y += y;
						}
					}
					return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1;
				});
			};
		}
}

function drawNoRunsDatum(group, circle){
	
	var paddingY = parseFloat(circle[0][0].attributes['r'].value) * 0.1;
	
	group.append('text')
	     .attr('x', parseFloat(circle[0][0].attributes['cx'].value))
	     .attr('y', parseFloat(circle[0][0].attributes['cy'].value) + paddingY)
	     .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
	     .attr("alignment-baseline","middle")
	     .attr("text-anchor","middle")
	     .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+' '+fontFace})
	     .text("No Data found");
}

function initRect(group, bounds, id, style){
	if(group === undefined){
		return rect = svg.append('rect')
							    .attr('id',id)
							    .attr('x',bounds[0])
							    .attr('y',bounds[1])
							    .attr('width',bounds[2])
							    .attr('height',bounds[3])
							    .attr('style', style);
	}
	else{
		return rect = group.append('rect')
							    .attr('id',id)
							    .attr('x',bounds[0])
							    .attr('y',bounds[1])
							    .attr('width',bounds[2])
							    .attr('height',bounds[3])
							    .attr('style', style);
							  
	}
}

function initCicle(group, bounds, id, style){
	if(group === undefined){
		return svg.append('circle')
							    .attr('id',id)
							    .attr('cx',bounds[0])
							    .attr('cy',bounds[1])
							    .attr('r',bounds[2])
							    .attr('style', style);
	}
	else{
		return group.append('circle')
							    .attr('id',id)
							    .attr('cx',bounds[0])
							    .attr('cy',bounds[1])
							    .attr('r',bounds[2])
							    .attr('style', style);
							  
	}
}

function initGroup(parentGroup, id){
	if( parentGroup === undefined ){
		return svg.append('g').attr('id',id);
	}
	
	return parentGroup.append('g').attr('id',id);
}

function drawStatusIndicator(group, rect, circle, idSubtype, statusDatum){
	
	/* if status data is not available, to not draw status indicator*/
	if(statusData.length < 1){
			return;
	}
	
	var statusRectWidth = parseFloat(circle[0][0].attributes['r'].value) * 0.5;
	var statusRectHeight = parseFloat(circle[0][0].attributes['r'].value) * 0.25;
	var statusRectOffsetX = fontSize;
	
	var statusGroup = group.append('g').attr('id','statusGroup'+idSubtype);
	
	var statusIndicatorRectOffsets = [20,10];
	
	var statusIndicatorRectBounds = [
		parseFloat(rect[0][0].attributes['x'].value)+parseFloat(rect[0][0].attributes['width'].value) + statusIndicatorRectOffsets[0],
		parseFloat(rect[0][0].attributes['y'].value)+parseFloat(rect[0][0].attributes['height'].value)/4 + statusIndicatorRectOffsets[1],
		statusRectWidth*2,
		statusRectHeight*4
	];
	
	/* rect that wraps all of the statusIndicator content */
	var statusIndicatorRect = initRect(statusGroup, statusIndicatorRectBounds, 'statusIndicatorRect'+idSubtype,
				'fill-opacity:'+ 0 +';stroke:black;stroke-width:0.0');
	
	var statusLabel = statusGroup.append('text')
					 .attr("id","statusLabel"+idSubtype)
	                 .attr('x',statusIndicatorRectBounds[0] + statusIndicatorRectBounds[2]/2)
	                 .attr('y',statusIndicatorRectBounds[1] + fontSize)
	                 .attr("alignment-baseline","middle")
					 .attr("text-anchor","middle")
					 .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
					 .text("Last Status");
					 
	/* rect that wraps only the ui/svg element that shows/represents the last status value (tick(pass) or cross(fail))*/
	var statusRect = statusGroup.append('rect')
							       .attr('id','statusRect'+idSubtype)
							       .attr('x',statusIndicatorRectBounds[0]+statusIndicatorRectBounds[2]/4)
							       .attr('y',parseFloat(statusLabel[0][0].attributes['y'].value)+fontSize*1.5)
							       .attr('width',statusRectWidth)
							       .attr('height',statusRectWidth)
							       .style({'fill':'none', 'stroke':'black', 'stroke-width':'0.0'});				
	
	// PASS OR FAIL				 
	var statusText = statusGroup.append('text')
							.attr("id","statusText"+idSubtype)
							.attr('x',parseFloat(statusLabel[0][0].attributes['x'].value))
						    .attr('y',parseFloat(statusLabel[0][0].attributes['y'].value)+fontSize*1.2)
							.attr("alignment-baseline","middle")
							.attr("text-anchor","middle");
	
	if(statusDatum['Last Run Status'].includes("FAIL")){
		var lineLeft = statusGroup.append('line')
						  .attr("id","lineLeft"+idSubtype)
			              .attr('x1',parseFloat(statusRect[0][0].attributes['x'].value))
			              .attr('y1',parseFloat(statusRect[0][0].attributes['y'].value))
			              .attr('x2',parseFloat(statusRect[0][0].attributes['x'].value))
			              .attr('y2',parseFloat(statusRect[0][0].attributes['y'].value))
			              .attr("stroke-width",4.0)
			              .attr("stroke",COLOR_RED);
			              
		var lineRight = statusGroup.append('line')
			                   .attr("id","lineRight"+idSubtype)
			                   .attr('x1',parseFloat(statusRect[0][0].attributes['x'].value) + (parseFloat(statusRect[0][0].attributes['width'].value)))
			                   .attr('y1',parseFloat(statusRect[0][0].attributes['y'].value))
			                   .attr('x2',parseFloat(statusRect[0][0].attributes['x'].value) + (parseFloat(statusRect[0][0].attributes['width'].value)))
			                   .attr('y2',parseFloat(statusRect[0][0].attributes['y'].value))
			                   .attr("stroke-width",4.0)
			                   .attr("stroke",COLOR_RED);
			                  
		d3.select("#lineLeft"+idSubtype)
	      .transition()
	      .attr('x2', parseFloat(statusRect[0][0].attributes['x'].value) + (parseFloat(statusRect[0][0].attributes['width'].value))).duration(300).delay(2000) 
	      .attr('y2', parseFloat(statusRect[0][0].attributes['y'].value) + (parseFloat(statusRect[0][0].attributes['height'].value))).duration(300).delay(2000);
	      
	      d3.select("#lineRight"+idSubtype)
	        .transition()
	        .attr('x2',parseFloat(statusRect[0][0].attributes['x'].value)).duration(300).delay(2500)
	        .attr('y2',parseFloat(statusRect[0][0].attributes['y'].value) + (parseFloat(statusRect[0][0].attributes['height'].value)))
	        .duration(300)
	        .delay(2500)
	        .each("end", function(){
				statusText.style({'fill':COLOR_RED, 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				          .text(statusDatum['Last Run Status']);
			});
	}
	else{
		
		var statusRectBounds = [
					parseFloat(statusRect[0][0].attributes['x'].value),
					parseFloat(statusRect[0][0].attributes['y'].value),
					parseFloat(statusRect[0][0].attributes['x'].value) + parseFloat(statusRect[0][0].attributes['width'].value),
					parseFloat(statusRect[0][0].attributes['y'].value) + parseFloat(statusRect[0][0].attributes['height'].value),
					parseFloat(statusRect[0][0].attributes['width'].value),
					parseFloat(statusRect[0][0].attributes['height'].value)
		];
		
		var pivot = [
				parseFloat(statusRect[0][0].attributes['x'].value),
				parseFloat(statusRect[0][0].attributes['y'].value)
		];
		
			
		var lineLeft = statusGroup.append('line')
			                   .attr("id","lineLeft"+idSubtype)
			                   .attr('x1',statusRectBounds[0] + 0)
			                   .attr('y1',statusRectBounds[1] + statusRectBounds[4] * 0.5)
			                   .attr('x2',statusRectBounds[0] + 0)
			                   .attr('y2',statusRectBounds[1] + statusRectBounds[4] * 0.5)
			                   .attr("stroke-width",4.0)
			                   .attr("transform","rotate(-29,"+pivot[0]+","+pivot[1]+")")
			                   .attr("stroke",COLOR_GREEN);
			    
		pivot = [
			statusRectBounds[0] + statusRectBounds[4],
			statusRectBounds[1]
		];
			                   
		var lineRight = statusGroup.append('line')
			                   .attr("id","lineRight"+idSubtype)
			                   .attr('x1',statusRectBounds[0])
			                   .attr('y1',statusRectBounds[1])
			                   .attr('x2',statusRectBounds[0])
			                   .attr('y2',statusRectBounds[1])
			                   .attr("transform","rotate(-56,"+pivot[0]+","+pivot[1]+")")
			                   .attr("stroke-width",4.0)
			                   .attr("stroke",COLOR_GREEN);
			                   
			                   
			
			                  
		d3.select("#lineLeft"+idSubtype)
			  .transition()
			  .attr('y2',statusRectBounds[1] + statusRectBounds[4] * 0.95).duration(300).delay(2000)
			  .each("end", function(){
					d3.select("#lineRight"+idSubtype)
					  .transition()
	                  .attr('x2',statusRectBounds[2])
	                  .duration(300)
	                  .each("end", function(){
							statusText.style({'fill':COLOR_GREEN, 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				                      .text(statusDatum['Last Run Status']);
					  });
		 });
	}
	
}
