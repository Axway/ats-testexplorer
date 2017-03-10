google.charts.load('current', {
	packages : [ 'treemap' ]
});

google.load('visualization', '1.0', {
	'packages' : [ 'treemap' ]
});

var treemapData = null;
var treemapOptions = null;
var chart = null;
var hiddenValue = null;
var dbName = null;
var testcasesIdsMap = null;


function setTestcasesIdsMap(map){
	testcasesIdsMap = map
}

function setDbName(dbname){
	dbName = dbname;
}

function setHiddenValue(value) {
	hiddenValue = value;
}

function getHiddenValue() {
	return hiddenValue;
}

function getTestcaseId(testcaseFullName){
	for (d of testcasesIdsMap){
		if (d.name.includes(testcaseFullName)){
			return d.id;
		}
	}
}

function openTestExplorerPage(dbName, testcaseFullName) {
	var url = window.location.href.substring(0,
	window.location.href.lastIndexOf('/'))
	+"/testcase?&testcaseId=" + getTestcaseId(testcaseFullName)+ "&dbname=" + dbName;
	
	window.open(url,'_blank');
}

function generateTooltip(row) {
	var lastHiddenValue = getHiddenValue();
	var tooltip;
	if(treemapData.getValue(row, 0).includes('/')){
		setHiddenValue('testcases');
	}
	if (getHiddenValue().includes('groups')) {
		if (row == 0) {
			var name = treemapData.getValue(row, 0);

			tooltip =  '<div style="background:#fd9; padding:10px; border-style:solid;">'
					+ "Total testcases executions: "
					+ treemapData.getValue(row, 2)
					+ "<br/>"
					+ "Pass percentage: "
					+ treemapData.getValue(row, 3).toFixed(0) + "%<br/>";
		} else {
			var name = treemapData.getValue(row, 0);

			tooltip = '<div style="background:#fd9; padding:10px; border-style:solid;">'
					+ "Group: <b>"
					+ name
					+ "</b><br/>"
					+ "Total testcases executions: "
					+ treemapData.getValue(row, 2)
					+ "<br/>"
					+ "Pass percentage: "
					+ treemapData.getValue(row, 3).toFixed(0) + "%<br/>";
		}

	} else {
		if (!treemapData.getValue(row, 0).includes('/')) {
			var name = treemapData.getValue(row, 0);

			tooltip = '<div style="background:#fd9; padding:10px; border-style:solid;">'
					+ "Group: <b>"
					+ name
					+ "</b><br/>"
					+ "Total testcases executions: <b>"
					+ treemapData.getValue(row, 2)
					+ "</b><br/>"
					+ "Pass percentage: <b>"
					+ treemapData.getValue(row, 3).toFixed(0) + "%</b><br/>";
		} else {
			var names = treemapData.getValue(row, 0).split('/');

			tooltip = '<div style="background:#fd9; padding:10px; border-style:solid;">'
					+ "Testcase: <b>"
					+ names[0]
					+ "</b><br/>"
					+ "Scenario: <b>"
					+ names[1]
					+ "</b><br/>"
					+ "Suite: <b>"
					+ names[2]
					+ "</b><br/>"
					+ "Group: <b>"
					+ names[3]
					+ "</b><br/>"
					+ "Total executions: <b>"
					+ treemapData.getValue(row, 2)
					+ "</b><br/>"
					+ "Pass percentage: <b>"
					+ treemapData.getValue(row, 3).toFixed(0) + "%</b><br/>";
		}
	}
	
	setHiddenValue(lastHiddenValue);
	
	return tooltip;

}

function drawTreemap(data, options) {

	treemapData = data;

	treemapOptions = options;

	chart = new google.visualization.TreeMap(document
			.getElementById('chart_div'));

	google.visualization.events.addListener(chart, 'select', selectHandler);
	
	google.visualization.events.addListener(chart, 'rollup', rollupHandler);

	chart.draw(treemapData, treemapOptions);

	function selectHandler(e) {
		var group = document.getElementById('group');
		var selection = chart.getSelection();
		var value = '';

		for (var i = 0; i < selection.length; i++) {
			var item = selection[i];
			if (item.row != null && item.column != null) {
				value = data.getFormattedValue(item.row, item.column);
			} else if (item.row != null) {
				value = data.getFormattedValue(item.row, 0);
			} else if (item.column != null) {
				value = data.getFormattedValue(0, item.column);
			}
		}
		if (hiddenValue.includes("testcases")) {
			hiddenValue = value;
			openTestExplorerPage(dbName, value.split('/')[0]);
		} else {
			hiddenValue = "testcases";
			group.value = value;
			$(".filterContent").hide();
		}
	}
	
	function rollupHandler(e) {
		hiddenValue = "groups";
		group.value = "";
		$(".filterContent").show();
	}
	
}
