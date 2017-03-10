var svg;
var svgWidth = 0;
var svgHeight = 0;

var svgRect;
var svgRectWidth = 0;
var svgRectHeight = 0;
var svgRectX = 10;
var svgRectY = 10;
var svgRectRX = 20;
var svgRectRY = 20;

var suiteData;
var testcasesData;
var dbname;

var COLOR_RED = "#FF0000";
var COLOR_YELLOW = "#FFC000";
var COLOR_GREEN = "#9DBB61";

var SVG_RECT_COLOR = "#F2F2F2";
var colors = [COLOR_GREEN, COLOR_RED, COLOR_YELLOW];

var fontFace = 'Open Sans';
var fontSize = 18;

d3.select(window).on('resize', resize);

function setTestcasesData(data){
	testcasesData = data;
}

function setSuiteData(data){
	suiteData = data;
}

function setDbName(name){
	dbname = name
}

function resize(){
	svgWidth = $(window).innerWidth() * 0.9;
	svgHeight = $(window).innerHeight() * 0.75;
	init();
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

function init(){
	
	removeSvg();
	
	if(testcasesData != undefined && testcasesData[0][0].length > 0){
		svgHeight = ((testcasesData[0][0].length) * (35 * 1.5)) + 70;
	}
	
	if(svgHeight < 320){
		svgHeight = 320;
	}
	
	
	initSvg();
	
	initSvgRect();
	
	if(suiteData === undefined || suiteData[0][0].length == 0){
		drawNoData();
	}
	
	var productGroup = svg.selectAll('.productGroup')
	  .data(suiteData)
	  .enter()
	  .append('g')
	  .attr('class', 'productGroup')
	  .attr('id', function(d,i){return 'productGroup'+i;})
	  .each(function(d,i){
		  var productAndVersionIdx = i;
		  var productAndVersionChartData = d;
		  drawProductAndVersionGroup(d3.select(this), d, i);
		  
		  var productAndVersionGroupBoundingRect = d3.select(this).select('rect');
		  
		  var buildTypeGroup = d3.select(this).selectAll('.buildTypeGroup')
						.data(d)
						.enter()
						.append('g')
						.attr('class', 'buildTypeGroup')
						.attr('id', function(d,i){return 'buildTypeGroup'+productAndVersionIdx+d.type.replace(' ','');})
						.each(function(d,i){
								drawBuildTypeGroup(d3.select(this), productAndVersionGroupBoundingRect, d, productAndVersionChartData, productAndVersionIdx, i);
						});
						
	  });
	
}

function drawNoData(){
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

function drawProductAndVersionGroup(group, data, idx){
	
	var productAndVersionChartData = data;
		
	var offsets = [0,10];
	
	// space between differend productAndVersionGroupRects
	var rectToRectPadding = 20;
	
	var screenRatio = svgRectWidth/svgRectHeight;
	
	var productAndVersionGroupBoundingRectW = (((svgRectWidth-(offsets[0]*2))/suiteData.length)-rectToRectPadding)/4;
	
	if(productAndVersionGroupBoundingRectW > 400){
		productAndVersionGroupBoundingRectW = 400;
	}
		
	var productAndVersionGroupBoundingRectH = (svgRectHeight-(offsets[1]*2));
	
	if(productAndVersionGroupBoundingRectH > 200){
		productAndVersionGroupBoundingRectH = 200;
	}
		
	var productAndVersionGroupBoundingRectX = (svgRectX + offsets[0]) + (+ rectToRectPadding/2) + (idx*(productAndVersionGroupBoundingRectW)) + rectToRectPadding*idx;
	
	var productAndVersionGroupBoundingRectY = svgRectY+(offsets[1]);
		
			
	var bounds = [
						productAndVersionGroupBoundingRectX,
						productAndVersionGroupBoundingRectY,
						productAndVersionGroupBoundingRectW,
						productAndVersionGroupBoundingRectH
	];
	
	var rectColor = 'rgb('+(Math.floor(255*Math.random()))+','+(Math.floor(255*Math.random()))+','+(Math.floor(255*Math.random()))+')';
			
	var productAndVersionGroupBoundingRect = initRect(group, bounds, 
					'productAndVersionGroupBoundingRect'+idx,
					'fill:'+ SVG_RECT_COLOR +';stroke:black;stroke-width:0.0');
}

function drawBuildTypeGroup(buildTypeGroup, productAndVersionGroupBoundingRect, data, productAndVersionChartData, productAndVersionIdx, buildTypeIdx){
	
	var buildTypeChartData = data;
	var buildTypeTestcasesData = testcasesData[productAndVersionIdx][buildTypeIdx];

	var offsets = [10,10];
		
	var buildTypeGroupBoundingRectW = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['width'].value);
		
	var buildTypeGroupBoundingRectH = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['height'].value)/productAndVersionChartData.length;
		
	var buildTypeGroupBoundingRectX = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['x'].value);
		
	var buildTypeGroupBoundingRectY = parseFloat(productAndVersionGroupBoundingRect[0][0].attributes['y'].value) + (buildTypeIdx)*buildTypeGroupBoundingRectH;
	
	var buildTypeGroupBoundingRectBounds = [
						buildTypeGroupBoundingRectX,
						buildTypeGroupBoundingRectY,
						buildTypeGroupBoundingRectW,
						buildTypeGroupBoundingRectH
			];
			
	var buildTypeGroupBoundingRect = initRect(buildTypeGroup, buildTypeGroupBoundingRectBounds, 
					'buildTypeGroupBoundingRect'+productAndVersionIdx+data.type.replace(' ',''),
					'fill-opacity:'+ 0 +';stroke:black;stroke-width:0.0');
					
	var size = (buildTypeGroupBoundingRectBounds[2] >= buildTypeGroupBoundingRectBounds[3]) ? buildTypeGroupBoundingRectBounds[3] : buildTypeGroupBoundingRectBounds[2];								
	fontSize = Math.ceil(size * 0.07);
									
	drawChartDatum(buildTypeGroup, buildTypeGroupBoundingRect, data, productAndVersionIdx+data.type.replace(' ',''));
	
	var suitesDatum = testcasesData[productAndVersionIdx][buildTypeIdx];
	
	var suitesDatumGroup = initGroup(buildTypeGroup, 'suitesDatumGroup'+productAndVersionIdx+data.type.replace(' ',''));
	
	var suitesDatumBoundingRectOffsets = [50,0];
	
	var suitesDatumBoundingRectBounds = [
		parseFloat(buildTypeGroupBoundingRect[0][0].attributes['x'].value)+parseFloat(buildTypeGroupBoundingRect[0][0].attributes['width'].value) 
			+ suitesDatumBoundingRectOffsets[0],
		parseFloat(buildTypeGroupBoundingRect[0][0].attributes['y'].value) - suitesDatumBoundingRectOffsets[1],
		svgRectWidth-parseFloat(buildTypeGroupBoundingRect[0][0].attributes['width'].value) - (offsets[0]*2) - suitesDatumBoundingRectOffsets[0],
		svgRectHeight  - (offsets[1]*2) - suitesDatumBoundingRectOffsets[1]
	];
	
	var suitesDatumBoundingRect = initRect(suitesDatumGroup, suitesDatumBoundingRectBounds, 
						'suitesDatumBoundingRect'+productAndVersionIdx+data.type.replace(' ',''), 'fill:'+ SVG_RECT_COLOR +';stroke:black;stroke-width:0.0');
	
	if(suitesDatum.length > 0){
		drawSuitesDatum(suitesDatum, suitesDatumGroup, productAndVersionIdx+data.type.replace(' ',''), suitesDatumBoundingRect);
		drawStatusIndicator(suitesDatumGroup, buildTypeGroupCircleRectOverlay, buildTypeGroupCircle, productAndVersionIdx+data.type.replace(' ',''), statusData[0][0]);
	}
	else{
		drawNoSuitesDatum(suitesDatumGroup, suitesDatumBoundingRect);
	}
}

function drawChartDatum(group, boundingRect, datum, idSubtype){
	
	var titleOffsetY = fontSize * 2;
	
	var productText = group.append("text")
					   .attr('id','productText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize*1.33+'px '+fontFace, 'font-weight': 'bold'})
					   .attr("x",parseFloat(boundingRect[0][0].attributes['x'].value)+parseFloat(boundingRect[0][0].attributes['width'].value)/2)
					   .attr("y",parseFloat(boundingRect[0][0].attributes['y'].value)+titleOffsetY/2)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.type+" tests");
		               
	var versionText = group.append("text")
					   .attr('id','versionText'+idSubtype)
				       .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize*1.2+'px '+fontFace, 'font-weight': 'bold'})
					   .attr("x",parseFloat(productText[0][0].attributes['x'].value))
					   .attr("y",parseFloat(productText[0][0].attributes['y'].value) + titleOffsetY)
					   .attr("alignment-baseline","middle")
					   .attr("text-anchor","middle")
		               .text(datum.name);
	
}

function drawSuitesDatum(suitesDatum, suitesDatumGroup, idSubtype, suitesDatumBoundingRect){
	
	if(fontSize > 14){
		fontSize = 14;
	}

	rectBounds = [
					parseFloat(suitesDatumBoundingRect[0][0].attributes['x'].value),
					parseFloat(suitesDatumBoundingRect[0][0].attributes['y'].value),
					parseFloat(suitesDatumBoundingRect[0][0].attributes['x'].value) + parseFloat(suitesDatumBoundingRect[0][0].attributes['width'].value),
					parseFloat(suitesDatumBoundingRect[0][0].attributes['y'].value) + parseFloat(suitesDatumBoundingRect[0][0].attributes['height'].value),
					parseFloat(suitesDatumBoundingRect[0][0].attributes['width'].value),
					parseFloat(suitesDatumBoundingRect[0][0].attributes['height'].value)			
	],
	rectWidth = rectBounds[4] / 2.8,
	rectHeight = 35,
	rectRx = 20,
	rectRy = 20,
	paddingY = parseFloat(suitesDatumBoundingRect[0][0].attributes['y'].value)+rectHeight,// initial Y position of the first rects for the first record in suitesDatum
	paddingX = -parseFloat(suitesDatumBoundingRect[0][0].attributes['width'].value) + 10, // initial X position of the last rects for the first record in suitesDatum , for the all runs rect
	textYPercentagePadding = 1.5,
	colorScale = d3.scale.quantize().domain([50,100]).range([COLOR_RED, COLOR_YELLOW, COLOR_GREEN]),
	stepY = rectHeight * 1.5; // offset for the next suiteData row holder will be (by Y)
	
	var suitesDatumRowHolder = suitesDatumGroup.selectAll(".suitesDatum")
						 .data(suitesDatum)
						 .enter().append("g").attr('class','suitesDatum');
						 
	var allRunsRect = suitesDatumRowHolder.append('rect')
				   .attr("id",function(d,i){return "allRunsRect"+i+idSubtype;})
			       .attr('width',rectWidth)
			       .attr('height',rectHeight)
			       .attr('fill',function (d, i){return colorScale(parseFloat(d['All Runs']));})
			       .attr('style','stroke:black;stroke-width:1;')
			       .attr('rx',rectRx)
			       .attr('ry',rectRy)
			       .attr('x',parseFloat(suitesDatumBoundingRect[0][0].attributes['x'].value) - rectWidth - paddingX)
			       .attr('y',function (d, i){return paddingY + i * stepY;});
			       
	var thisRunRect = suitesDatumRowHolder.append('rect')
				   .attr("id",function(d,i){return "thisRunRect"+i+idSubtype;})
			       .attr('width',rectWidth)
			       .attr('height',rectHeight)
			       .attr('fill',function (d, i){return colorScale(parseFloat(d['This Run']));})
			       .attr('style','stroke:black;stroke-width:1;')
			       .attr('rx',rectRx)
			       .attr('ry',rectRy)
			       .attr('x',function (d){
					   var bounds = [
								parseFloat(allRunsRect[0][0].attributes['x'].value),
								parseFloat(allRunsRect[0][0].attributes['y'].value),
								parseFloat(allRunsRect[0][0].attributes['x'].value) + parseFloat(allRunsRect[0][0].attributes['width'].value),
								parseFloat(allRunsRect[0][0].attributes['y'].value) + parseFloat(allRunsRect[0][0].attributes['height'].value),
								parseFloat(allRunsRect[0][0].attributes['width'].value),
								parseFloat(allRunsRect[0][0].attributes['height'].value)			
					   ];
					   return bounds[0] - bounds[4] / 3.5;
				   })
			       .attr('y',function (d, i){return paddingY + i * stepY;});
			       
	var totalRunsRect = suitesDatumRowHolder.append('rect')
				    .attr("id",function(d,i){return "totalRunsRect"+i+idSubtype;})
				   .attr('width',rectWidth)
				   .attr('height',rectHeight)
				   .attr('style','fill:#F2F2F2;stroke:black;stroke-width:1')
				   .attr('rx',rectRx)
				   .attr('ry',rectRy)
				   .attr('x',function (d){
					   var bounds = [
								parseFloat(thisRunRect[0][0].attributes['x'].value),
								parseFloat(thisRunRect[0][0].attributes['y'].value),
								parseFloat(thisRunRect[0][0].attributes['x'].value) + parseFloat(thisRunRect[0][0].attributes['width'].value),
								parseFloat(thisRunRect[0][0].attributes['y'].value) + parseFloat(thisRunRect[0][0].attributes['height'].value),
								parseFloat(thisRunRect[0][0].attributes['width'].value),
								parseFloat(thisRunRect[0][0].attributes['height'].value)			
					   ];
					   return bounds[0] - bounds[4] / 3.5;
				   })
				   .attr('y',function (d, i){return paddingY + i * stepY;});
				   
	var lastRunRect = suitesDatumRowHolder.append('rect')
				   .attr("id",function(d,i){return "lastRunRect"+i+idSubtype;})
			       .attr('width',rectWidth)
			       .attr('height',rectHeight)
			       .attr('style','fill:#F2F2F2;stroke:black;stroke-width:1')
			       .attr('rx',rectRx)
			       .attr('ry',rectRy)
			       .attr('x',function (d){
					   var bounds = [
								parseFloat(totalRunsRect[0][0].attributes['x'].value),
								parseFloat(totalRunsRect[0][0].attributes['y'].value),
								parseFloat(totalRunsRect[0][0].attributes['x'].value) + parseFloat(totalRunsRect[0][0].attributes['width'].value),
								parseFloat(totalRunsRect[0][0].attributes['y'].value) + parseFloat(totalRunsRect[0][0].attributes['height'].value),
								parseFloat(totalRunsRect[0][0].attributes['width'].value),
								parseFloat(totalRunsRect[0][0].attributes['height'].value)			
					   ];
					   return bounds[0] - bounds[4] / 3.0;
				   })
			       .attr('y',function (d, i){return paddingY + i * stepY;});
			       
	var lastBuildRect = suitesDatumRowHolder.append('rect')
				   .attr("id",function(d,i){return "lastBuildRect"+i+idSubtype;})
			       .attr('width',rectWidth)
			       .attr('height',rectHeight)
			       .attr('style','fill:#F2F2F2;stroke:black;stroke-width:1')
			       .attr('rx',rectRx)
			       .attr('ry',rectRy)
			       .attr('x',function (d){
					   var bounds = [
								parseFloat(lastRunRect[0][0].attributes['x'].value),
								parseFloat(lastRunRect[0][0].attributes['y'].value),
								parseFloat(lastRunRect[0][0].attributes['x'].value) + parseFloat(lastRunRect[0][0].attributes['width'].value),
								parseFloat(lastRunRect[0][0].attributes['y'].value) + parseFloat(lastRunRect[0][0].attributes['height'].value),
								parseFloat(lastRunRect[0][0].attributes['width'].value),
								parseFloat(lastRunRect[0][0].attributes['height'].value)			
					   ];
					   return bounds[0] - bounds[4] / 2.75 - 5; // magic number 5
				   })
			       .attr('y',function (d, i){return paddingY + i * stepY;});
			       
			       
	var nameRect = suitesDatumRowHolder.append('rect')
				   .attr("id",function(d,i){return "nameRect"+i+idSubtype;})
			       .attr('width',rectWidth)
			       .attr('height',rectHeight)
			       .attr('fill',function (d, i){return colorScale(parseFloat(d['This Run']));})
			       .attr('style','stroke:black;stroke-width:1;')
			       .attr('rx',rectRx)
			       .attr('ry',rectRy)
			       .attr('x',function (d){
					   var bounds = [
								parseFloat(lastBuildRect[0][0].attributes['x'].value),
								parseFloat(lastBuildRect[0][0].attributes['y'].value),
								parseFloat(lastBuildRect[0][0].attributes['x'].value) + parseFloat(lastBuildRect[0][0].attributes['width'].value),
								parseFloat(lastBuildRect[0][0].attributes['y'].value) + parseFloat(lastBuildRect[0][0].attributes['height'].value),
								parseFloat(lastBuildRect[0][0].attributes['width'].value),
								parseFloat(lastBuildRect[0][0].attributes['height'].value)			
					   ];
					   return bounds[0] - bounds[4] / 2.25 - 5; // magic number 5
				   })
			       .attr('y',function (d, i){return paddingY + i * stepY;});
			       
	// texts for rectangles
	
	
	var nameRectBounds = [
					parseFloat(nameRect[0][0].attributes['x'].value),
					parseFloat(nameRect[0][0].attributes['y'].value),
					parseFloat(nameRect[0][0].attributes['x'].value) + parseFloat(nameRect[0][0].attributes['width'].value),
					parseFloat(nameRect[0][0].attributes['y'].value) + parseFloat(nameRect[0][0].attributes['height'].value),
					parseFloat(nameRect[0][0].attributes['width'].value),
					parseFloat(nameRect[0][0].attributes['height'].value)			
	];
	
	var lastBuildRectBounds = [
					parseFloat(lastBuildRect[0][0].attributes['x'].value),
					parseFloat(lastBuildRect[0][0].attributes['y'].value),
					parseFloat(lastBuildRect[0][0].attributes['x'].value) + parseFloat(lastBuildRect[0][0].attributes['width'].value),
					parseFloat(lastBuildRect[0][0].attributes['y'].value) + parseFloat(lastBuildRect[0][0].attributes['height'].value),
					parseFloat(lastBuildRect[0][0].attributes['width'].value),
					parseFloat(lastBuildRect[0][0].attributes['height'].value)			
	];
	
	var lastRunRectBounds = [
					parseFloat(lastRunRect[0][0].attributes['x'].value),
					parseFloat(lastRunRect[0][0].attributes['y'].value),
					parseFloat(lastRunRect[0][0].attributes['x'].value) + parseFloat(lastRunRect[0][0].attributes['width'].value),
					parseFloat(lastRunRect[0][0].attributes['y'].value) + parseFloat(lastRunRect[0][0].attributes['height'].value),
					parseFloat(lastRunRect[0][0].attributes['width'].value),
					parseFloat(lastRunRect[0][0].attributes['height'].value)			
	];
	
	var totalRunsRectBounds = [
					parseFloat(totalRunsRect[0][0].attributes['x'].value),
					parseFloat(totalRunsRect[0][0].attributes['y'].value),
					parseFloat(totalRunsRect[0][0].attributes['x'].value) + parseFloat(totalRunsRect[0][0].attributes['width'].value),
					parseFloat(totalRunsRect[0][0].attributes['y'].value) + parseFloat(totalRunsRect[0][0].attributes['height'].value),
					parseFloat(totalRunsRect[0][0].attributes['width'].value),
					parseFloat(totalRunsRect[0][0].attributes['height'].value)			
	];
	
	var thisRunRectBounds = [
					parseFloat(thisRunRect[0][0].attributes['x'].value),
					parseFloat(thisRunRect[0][0].attributes['y'].value),
					parseFloat(thisRunRect[0][0].attributes['x'].value) + parseFloat(thisRunRect[0][0].attributes['width'].value),
					parseFloat(thisRunRect[0][0].attributes['y'].value) + parseFloat(thisRunRect[0][0].attributes['height'].value),
					parseFloat(thisRunRect[0][0].attributes['width'].value),
					parseFloat(thisRunRect[0][0].attributes['height'].value)			
	];
	
	var allRunsRectBounds = [
					parseFloat(allRunsRect[0][0].attributes['x'].value),
					parseFloat(allRunsRect[0][0].attributes['y'].value),
					parseFloat(allRunsRect[0][0].attributes['x'].value) + parseFloat(allRunsRect[0][0].attributes['width'].value),
					parseFloat(allRunsRect[0][0].attributes['y'].value) + parseFloat(allRunsRect[0][0].attributes['height'].value),
					parseFloat(allRunsRect[0][0].attributes['width'].value),
					parseFloat(allRunsRect[0][0].attributes['height'].value)			
	];
	
	var nameText = suitesDatumRowHolder.append('text')
	               .attr("id",function(d,i){return "nameText"+i+idSubtype;})
				   .attr('x',function (d){
					   
					   var x = nameRectBounds[0] + nameRectBounds[4] / 2;
					   
					   return x;
				   })
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (nameRectBounds[5] / textYPercentagePadding);
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","middle")
				   .text(function (d){
					   
					   return resizeText(nameRectBounds[4]*0.95, d['Name']);
					   
					   
				   })
				   .on('mouseover', function(d) {
	                       d3.select(this).style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace,'text-decoration':'underline','cursor':'pointer'});
	               })
	               .on('mouseout', function() {
						d3.select(this).style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace,'text-decoration':'none'});
	               })
	               .on('click', function(d){
	               		var linkTarget = window.location.href.substring(0,window.location.href.lastIndexOf('/'))+"/testcase?&testcaseId="+d.Id+"&dbname="+dbname;
				   		window.location.href=linkTarget;
				   });
				   
				   
				   
	var lastBuildText = suitesDatumRowHolder.append('text')
				   .attr("id",function(d,i){return "lastBuildText"+i+idSubtype;})
				   .attr('x',function (d){
					   
					   var x = lastBuildRectBounds[0] + lastBuildRectBounds[4] - 10; // magic number 10
					   
					   return x;
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (lastBuildRectBounds[5] / textYPercentagePadding);
				   })
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .text(function (d){
					   
					   var maxLen = lastBuildRectBounds[2]*0.95-nameRectBounds[2]*0.95;
					   
					   return resizeText(maxLen, d['Last Build']);
				   });
				   
				   
	var lastRunText = suitesDatumRowHolder.append('text')
				   .attr("id",function(d,i){return "LastRunText"+i+idSubtype;})
				   .attr('x',function (d){
					   
					   var x = lastRunRectBounds[0] + lastRunRectBounds[4] - 10; // magic number 10
					   
					   
					   return x;
				   })
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (lastRunRectBounds[5] / textYPercentagePadding);
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .text(function (d){
					   var maxLen = lastRunRectBounds[2]*0.8-lastBuildRectBounds[2]*0.8;
					   
					   return resizeText(maxLen, d['Last Run']);
				   });
				   
				   
	var totalRunsText = suitesDatumRowHolder.append('text')
				   .attr("id",function(d,i){return "totalRunsText"+i+idSubtype;})
				   .attr('x',function (d){
					   
					   var x = totalRunsRectBounds[0] + totalRunsRectBounds[4] - 10; // magic number 10
					   
					   return x;
				   })
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (totalRunsRectBounds[5] / textYPercentagePadding);
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .text(function (d){
					   var maxLen = totalRunsRectBounds[2]*0.95-lastRunRectBounds[2]*0.95;
					   
					   return resizeText(maxLen, d['Total Runs']);
				   });
				   

	var thisRunText = suitesDatumRowHolder.append('text')
				   .attr("id",function(d,i){return "thisRunText"+i+idSubtype;})
				   .attr('x',function (d){
					   var x = thisRunRectBounds[0] + thisRunRectBounds[4] - 10;// magic number 10
					   
					   return x;
				   })
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (thisRunRectBounds[5] / textYPercentagePadding);
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .text(function (d){
					   
					   return resizeText(thisRunRectBounds[4], d['This Run']);
				   })
				   .on('mouseover', function(d) {
	                       d3.select(this).style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace,'text-decoration':'underline','cursor':'pointer'});
	               })
	               .on('mouseout', function() {
						d3.select(this).style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace,'text-decoration':'none'});
	               })
	               .on('click', function(d){
	               		var linkTarget = window.location.href.substring(0,window.location.href.lastIndexOf('/'))+"/testcase?&testcaseId="+d.Id+"&dbname="+dbname;
				   		window.location.href=linkTarget;
				   });
				   
				   
				   
	var allRunsText = suitesDatumRowHolder.append('text')
				   .attr("id",function(d,i){return "allRunsText"+i+idSubtype;})
				   .attr('x',function (d){
					   var x = allRunsRectBounds[0] + allRunsRectBounds[4] - 10; // magic number 10
					   
					   return x;
				   })
			       .attr('y',function (d, i){
					   return paddingY + i * stepY + (allRunsRectBounds[5] / textYPercentagePadding);
				   })
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .text(function (d){
					   
					   var maxLen = allRunsRectBounds[2]*0.95-totalRunsRectBounds[2]*0.95;
					   
					   return resizeText(allRunsRectBounds[4], d['All Runs']);
	});
	
	//labels
	
	var statusLabelX = nameRectBounds[0] + nameRectBounds[4] / 2 - 40; // magic number 40
	
	var suitesLabelsGroup = suitesDatumGroup.append('g').attr("id",function(d,i){return "suitesLabelGroup"+i+idSubtype;})
	
	//Status [TEST:PASS/FAIL] label
	//consists of four <text> elements for each word, except (:)		
	suitesLabelsGroup.append('text')
				   .attr('x', statusLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","middle")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("Test:");
		   
	suitesLabelsGroup.append('text')
				   .attr('x', statusLabelX + fontSize*2.5) // magic number 40
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 40
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","middle")
				   .style({'fill':COLOR_GREEN, 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("PASS");
		   
	suitesLabelsGroup.append('text')
				   .attr('x', statusLabelX + fontSize*2.5 + fontSize*1.5) // magic number 40 , 22
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 40
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","middle")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("/");
		   
	suitesLabelsGroup.append('text')
				   .attr('x', statusLabelX + fontSize*2.5 + fontSize*1.5 + fontSize*1.35) // magic number 40 , 22 , 20
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","middle")
				   .style({'fill':COLOR_RED, 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("FAIL");
				   
	
	var lastBuildLabelX = lastBuildRectBounds[0] + lastBuildRectBounds[4] - 10; // magic number 10
	
	//Last build label
	suitesLabelsGroup.append('text')
				   .attr('x', lastBuildLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("Last Build #");
				   
	
	var lastRunLabelX = lastRunRectBounds[0] + lastRunRectBounds[4] - 15; // magic number 15
	
	//Last run label
	suitesLabelsGroup.append('text')
				   .attr('x', lastRunLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("Last Run");
				   
	
	var totalRunsLabelX = totalRunsRectBounds[0] + totalRunsRectBounds[4] - 15; // magic number 15
	
	//Total runs label
	suitesLabelsGroup.append('text')
				   .attr('x', totalRunsLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("Total Runs");
				   
	
	var thisRunLabelX = thisRunRectBounds[0] + thisRunRectBounds[4] - 15; // magic number 15
	
	//This run label
	suitesLabelsGroup.append('text')
				   .attr('x', thisRunLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("This Run");
				   
	
	var allRunsLabelX = allRunsRectBounds[0] + allRunsRectBounds[4] - 15; // magic number 15
	
	//All runs label
	suitesLabelsGroup.append('text')
				   .attr('x', allRunsLabelX)
				   .attr('y', function(d,i){ return paddingY - 5 + i * stepY;}) // magic number 5
				   .attr("alignment-baseline","middle")
				   .attr("text-anchor","end")
				   .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
				   .text("All Runs");

}

function drawNoSuitesDatum(suitesDatumGroup, suitesDatumBoundingRect){
	
	var paddingY = parseFloat(suitesDatumBoundingRect[0][0].attributes['height'].value)/2;
	
	var paddingX = parseFloat(suitesDatumBoundingRect[0][0].attributes['width'].value)/2;
	
	var noSuitesDataText = suitesDatumGroup.append('text')
	                            .attr('id','noSuitesDataText')
	                            .attr('x',parseFloat(suitesDatumBoundingRect[0][0].attributes['x'].value) + paddingX)
	                            .attr('y',parseFloat(suitesDatumBoundingRect[0][0].attributes['y'].value) + paddingY)
	                            .style({'fill':'black', 'stroke':'none', 'stroke-width':'0', 'font':+fontSize+'px '+fontFace})
								.attr("alignment-baseline","middle")
								.attr("text-anchor","middle")
	                            .text('No Data found.');
}

function resizeText(maxLen, text){
		   
	var totalTextSize = (text.length * fontSize)/2;
					   
	var difference = (totalTextSize - (maxLen))/fontSize;
					   
	if(maxLen < totalTextSize){
			var endIdx = (text.length - (difference*2)) - 3;
			return text.substring(0,endIdx)+'...';
	}
					   
					   
	return text;

}
