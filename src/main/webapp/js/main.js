function getElementsByClassName(oElm, strTagName, strClassName) {
    var arrElements = (strTagName == "*" && oElm.all) ? oElm.all : oElm.getElementsByTagName(strTagName);
    var arrReturnElements = new Array();
    strClassName = strClassName.replace(/\-/g, "\\-");
    var oRegExp = new RegExp("(^|\\s)" + strClassName + "(\\s|$)");
    var oElement;
    for ( var i = 0; i < arrElements.length; i++) {
        oElement = arrElements[i];
        if (oRegExp.test(oElement.className)) {
            arrReturnElements.push(oElement);
        }
    }
    return arrReturnElements;
}

function showOrHideFilter(el) {
    var content = getElementsByClassName(el.parentNode, 'div', 'searchHeaderContent')[0];
    if (content.style.display != 'block') {
        content.style.display = 'block';
        getElementsByClassName(el, 'img', 'arrowUp')[0].style.display = 'block';
        getElementsByClassName(el, 'img', 'arrowDown')[0].style.display = 'none';
    } else {
        content.style.display = 'none';
        getElementsByClassName(el, 'img', 'arrowUp')[0].style.display = 'none';
        getElementsByClassName(el, 'img', 'arrowDown')[0].style.display = 'block';
    }
}

function showOrHideTestDetails(display) {
	var content = document.getElementById('testDetails');
	if(display) {
		content.style.display = 'block';
		document.getElementsByClassName('arrowRight')[0].style.display = 'none';
		document.getElementsByClassName('arrowDown')[0].style.display = 'block';
	}
	else {
		content.style.display = 'none';
		document.getElementsByClassName('arrowRight')[0].style.display = 'block';
		document.getElementsByClassName('arrowDown')[0].style.display = 'none';
	}
}

function changeClass(element, newClass) {
    element.className = newClass;
}

function changeStatTableRowClass(element) {
    if (element.checked) {
        changeClass(element.parentNode.parentNode, 'selected');
    } else {
        changeClass(element.parentNode.parentNode, 'notSelected');
    }
}

function clickSelectAllCheckbox(element) {
    var tr = element.parentNode.parentNode;
    var trElements = getElementsByClassName(tr, 'input', 'allMachinesCheckbox');
    trElements[0].click();
}

function selectAllCheckboxes(element, tableClassName) {
    var tr = element.parentNode.parentNode;
    var machineCheckboxes = getElementsByClassName(tr, 'input', 'machineCheckbox');
    for ( var i = 0; i < machineCheckboxes.length; i++) {
        if ((!element.checked && machineCheckboxes[i].checked)
                || (element.checked && !machineCheckboxes[i].checked)) {

            machineCheckboxes[i].checked = !machineCheckboxes[i].checked;
        }
    }
    // the focus must be set to some element in the form, to be submitted with
    // Enter key
    element.focus();
    markTableCellsAccToSelectedCheckboxes(tableClassName);
}

function unselectMainTrCheckbox(element, tableClassName) {
    var tr = element.parentNode.parentNode;
    var trElements = getElementsByClassName(tr, 'input', 'allMachinesCheckbox');
    if (!element.checked && trElements[0].checked) {

        trElements[0].checked = false;
    } else if (!trElements[0].checked) {
        var allAreChecked = true;
        var machineCheckboxes = getElementsByClassName(tr, 'input', 'machineCheckbox');
        for ( var i = 0; i < machineCheckboxes.length; i++) {
            if (!machineCheckboxes[i].checked) {
                allAreChecked = false;
                break;
            }
        }
        if (allAreChecked) {
            trElements[0].checked = true;
        }
    }
    // the focus must be set to some element in the form, to be submitted with
    // Enter key
    element.focus();
    markTableCellsAccToSelectedCheckboxes(tableClassName);
}

function markTableCellsAccToSelectedCheckboxes(tableId) {

    var tr = document.getElementById(tableId).rows;
    var td = null;
    for ( var i = 0; i < tr.length; ++i) {

        var mainCheckboxes = getElementsByClassName(tr[i], 'input', 'allMachinesCheckbox');
        if (mainCheckboxes.length > 0) {
            if (mainCheckboxes[0] != null && mainCheckboxes[0].checked) {

                tr[i].style.backgroundColor = '#DFF1FF';
            } else {

                tr[i].style.backgroundColor = 'transparent';
                td = tr[i].cells;
                for ( var j = 0; j < td.length; ++j) {
                    var checkboxes = getElementsByClassName(td[j], 'input', 'machineCheckbox');
                    if (checkboxes.length > 0 && checkboxes[0] != null && checkboxes[0].checked) {
                        td[j].style.backgroundColor = '#DFF1FF';
                    } else {
                        td[j].style.backgroundColor = 'transparent';
                    }
                }
            }
        }
    }
}

function checkFirstCheckbox(element) {

    var checkboxes = getElementsByClassName(element.parentNode, 'input', 'checkbox');
    if (checkboxes.length > 0 && checkboxes[0] != null) {
        checkboxes[0].checked = !checkboxes[0].checked;
    }
}

var refreshButtonClicked = false;

function clickDisplayCharButton( isRefreshButtonClicked ) {

    refreshButtonClicked = isRefreshButtonClicked;
    getElementsByClassName(document, 'input', 'hiddenDisplayChartButton')[0].click();
    return false;
}

function showOrHideTableRows(tableId, afterRow, haveCheckboxColumn) {

    var isHideAction = false;
    var tableElement = document.getElementById(tableId);
    var trs = tableElement.rows;
    for (var i=afterRow; i<trs.length; i++) {
        if (trs[i].style.display == 'none') {
            trs[i].style.display = 'table-row';
        } else {
            trs[i].style.display = 'none';
            isHideAction = true;
        }
    }
    if (trs[0] != null) {
        var tds = trs[0].cells;
        var mainColumns = 2;
        if (!haveCheckboxColumn) mainColumns = 1;
        for (var i=0; i<mainColumns; i++) {
            if (tds[i] != null) {
                if (isHideAction) {
                    tds[i].style.borderBottom='none';
                    tds[i].style.borderRight='none';
                    if (i==0 && haveCheckboxColumn) tds[i].style.width='30px';
                } else {
                    tds[i].style.borderBottom='1px solid #EEEEEE';
                    if (i==0 && haveCheckboxColumn) tds[i].style.width='auto';
                    if (!haveCheckboxColumn || i!=0) tds[i].style.borderRight='1px solid #DDDDDD';
                }
            }
        }
        for (var i=mainColumns; i<tds.length; i++) {
            if (isHideAction) {
                tds[i].style.visibility = 'collapse';
            } else {
                tds[i].style.visibility = 'visible';
            }
        }
        if (isHideAction) {
            getElementsByClassName(trs[0], 'img', 'arrowUD')[0].src = 'images/down.png';
            tableElement.style.width = '100%';
            if (tableElement.parentNode.tagName.toLowerCase() == 'div') {
                tableElement.parentNode.style.overflow = 'hidden';
            }
        } else {
            getElementsByClassName(trs[0], 'img', 'arrowUD')[0].src = 'images/up.png';
            tableElement.style.width = '100%';
            if (tableElement.parentNode.tagName.toLowerCase() == 'div') {
                tableElement.parentNode.style.overflow = 'visible';
            }
        }
    }

    try {
        var trElement = tableElement.parentNode.parentNode;
        if (trElement.tagName.toUpperCase() == 'TR') {

            var settingsPannel = getElementsByClassName(trElement, 'a', 'settingsTableShowButton')[0];
            if (isHideAction) {
                settingsPannel.style.display = 'none';
            } else {
                settingsPannel.style.display = 'block';
            }
        }
    } catch(err) {}

}

function showHiddenStatChildren( element ) {

    // do not show hidden statistics when the System custom interval is open
    var systemCustomIntervalInputEl = getElementsByClassName(document, 'input', 'sysIntervalInput');
    if (systemCustomIntervalInputEl != null && systemCustomIntervalInputEl.length > 0) {
        return;
    }

    var trElement = findParentElement(element, 'tr');
    var tableElement = findParentElement(trElement, 'table');
    var trs = tableElement.rows;
    var start = false;
    for (var i=0; i<trs.length; i++) {
        if (start) {
            if (trs[i].className.indexOf("hiddenStatRow") != -1) {
                if (trs[i].style.display == 'none' || trs[i].style.display == '') {
                    trs[i].style.display = 'table-row';
                } else {
                    trs[i].style.display = 'none';
                }
            } else {
                return;
            }
        }
        if (trs[i] == trElement) {
            start = true;
        }
    }
}

function findParentElement( element, tagName) {

    var el = element.parentNode;
    while(el.tagName.toLowerCase() != tagName) {
        el = el.parentNode;
        if (el.tagName.toLowerCase() == 'body') {
            return null;
        }
    }
    return el;
}


/* CAPTURE F5 */
document.onkeydown = function(e){
    var event = e || window.event;
    if(event.keyCode == 116) { // Capture F5
        var rb = getElementsByClassName(document, 'input', 'refreshButton');
        if (rb != null && rb.length > 0) {

            rb[0].click();
            return false;
        }
    }
};

/* cookie support */
var Cookies = {
  createCookie: function(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime()+(days*24*60*60*1000));
        expires = "; expires="+date.toGMTString();
      } else {
        expires = "";
      }
      // alert('setting cookie: ' + name+"="+value+expires+"; path=/");
      document.cookie = name+"="+value+expires+"; path=/";
  },

  readCookie: function(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
  },

  eraseCookie: function (name) {
    createCookie(name,"",-1);
  }
};

/* Hiding and showing elements by Id */
function setElement(name, hideIt) {
  var  value = 'block';
  if (hideIt == 'hide') {
    value = 'none';
  };
  document.getElementById(name).style.display=value;
}

/* manage top notification area. Currently for IE usage warning */
var notifyAreaCookieName = 'TEData';

function checkIeNotification(areaID) {
  if (navigator.appName == 'Microsoft Internet Explorer') {
    if( !Cookies.readCookie(notifyAreaCookieName)) {
      setElement(areaID, 'show');
    } else {
      setElement(areaID, 'hide');
    }
  }
}

function hideElementPermanently(idArea, cookieValue) {
  setElement(idArea, 'hide');
  Cookies.createCookie(notifyAreaCookieName, cookieValue, 14); // 14 days persistence
}

function showHideIntervalPopup( hide, elementClassName ) {
    var popupEl = getElementsByClassName(document,'div',elementClassName)[0];
    if (popupEl) {
        if ( (hide && hide == 'true' ) || popupEl.style.display == 'block') {
            popupEl.style.display = 'none';
        } else {
            popupEl.style.display = 'block';
        }
    }
}

function setIntervalNewValue( newValue, elementClassName ) {
    getElementsByClassName(document,'input',elementClassName)[0].value = newValue;
}

/**
 * @return timezone offset without 'Daylight Saving Time'
 */
Date.prototype.getTimezoneOffsetWithoutDST = function() {
    var jan = new Date(this.getFullYear(), 0, 1);
    var jul = new Date(this.getFullYear(), 6, 1);
    return Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
};

/**
 *
 * @returns <code>true</code> if 'Daylight Saving Time' is in effect
 */
Date.prototype.isDST = function() {
    return this.getTimezoneOffset() < this.getTimezoneOffsetWithoutDST();
};
function getTableColumnDefinitions() {

	var tables = document.getElementsByClassName('imxt-head');
	// we need the last created table, so we will first see how many tables are there
	var table = document.getElementsByClassName('imxt-head')[tables.length - 1] 
	
	var thList = table.getElementsByTagName('th');

	var tableDefinition = new Array();

	for (var i = 0; i < thList.length - 2; i++) {

		var colName = thList[i].innerHTML.replace(/<[^>]*>/g, "").replace(
				/[\s']*/g, "");
		var colLength = document.defaultView.getComputedStyle(thList[i], null)
				.getPropertyValue("width").replace("px", "");

		tableDefinition.push(colName + ":" + colLength);

	}

	// return tableDefinition;
	document.getElementById('columnDefinitions').value = tableDefinition;
};

/* When the user clicks on the button, 
toggle between hiding and showing the dropdown content */
function showOrHideRepresentationDropdownList() {
	if(document.getElementById("representationDropdown").classList.contains('show')){
		console.log(document.getElementById("representationViewDropdown").classList.contains('show'));
		closeDropDownList();
	}
	else{
		console.log(document.getElementById("representationDropdown").classList.contains('show'));
		document.getElementById("representationDropdown").classList.toggle("show");
	}
    
}

// Close the dropdown if the user clicks outside of it
window.onclick = function(event) {
	if (!event.target.matches('.dropbtn')) {
		closeDropDownList();
	}
}

function closeDropDownList(){
	var dropdowns = document.getElementsByClassName("dropdown-content");
    var i;
    for (i = 0; i < dropdowns.length; i++) {
		var openDropdown = dropdowns[i];
      	if (openDropdown.classList.contains('show')) {
        	openDropdown.classList.remove('show');
      	}
    }
}

function populateFilterDataPanel(data){
	var productName = document.getElementById('productNameHolder');
	if(data['ProductName'].includes('null')){
		productName.style.display = 'none';
	}
	else{
		productName.style.display = 'inline';
		productName.innerHTML='Product Name: <b>'+data['ProductName']+'</b>'
	}
	
	var versionNames = document.getElementById('versionNamesHolder');
	if(data['VersionNames'].includes('null')){
		versionNames.style.display = 'none';
	}
	else{
		versionNames.style.display = 'inline';
		versionNames.innerHTML='Version Names: <b>'+data['VersionNames']+'</b>'
	}
	
	var groupNames = document.getElementById('groupNamesHolder');
	if(data['GroupNames'].includes('null')){
		groupNames.style.display = 'none';
	}
	else{
		groupNames.style.display = 'inline';
		groupNames.innerHTML='Group Names: <b>'+data['GroupNames']+'</b>'
	}
	
	var groupContains = document.getElementById('groupContainsHolder');
	if(data['GroupContains'].includes('null')){
		groupContains.style.display = 'none';
	}
	else{
		groupContains.style.display = 'inline';
		groupContains.innerHTML='Group contains: <b>'+data['GroupContains']+'</b>'
	}
	
	var startedAfter = document.getElementById('startedAfterHolder');
	if(data['StartedAfter'].includes('null')){
		startedAfter.style.display = 'none';
	}
	else{
		startedAfter.style.display = 'inline';
		startedAfter.innerHTML='Started After: <b>'+data['StartedAfter']+'</b>'
	}
	
	var startedBefore = document.getElementById('startedBeforeHolder');
	if(data['StartedBefore'].includes('null')){
		startedBefore.style.display = 'none';
	}
	else{
		startedBefore.style.display = 'inline';
		startedBefore.innerHTML='Started Before: <b>'+data['StartedBefore']+'</b>'
	}
}
