function saveDrawingSurface() {
   drawingSurfaceImageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
}

function restoreDrawingSurface() {
	ctx.putImageData(drawingSurfaceImageData, 0, 0);
}

function selectNodesFromHighlight() {
	var fromX, toX, fromY, toY;
	var nodesIdInDrawing = [];
	var xRange = getStartToEnd(rect.startX, rect.w);
	var yRange = getStartToEnd(rect.startY, rect.h);

	var allNodes = nodes.get();
	for (var i = 0; i < allNodes.length; i++) {
		var curNode = allNodes[i];
		var nodePosition = network.getPositions([curNode.id]);
		var nodeXY = network.canvasToDOM({x: nodePosition[curNode.id].x, y: nodePosition[curNode.id].y});
		if (xRange.start <= nodeXY.x && nodeXY.x <= xRange.end && yRange.start <= nodeXY.y && nodeXY.y <= yRange.end) {
			nodesIdInDrawing.push(curNode.id);
		}
	}
	network.selectNodes(nodesIdInDrawing);
	$.notify({
		message: nodesIdInDrawing.length + " nodes selected." 
	},{
		type: 'success'
	});
}

function getStartToEnd(start, theLen) {
	return theLen > 0 ? {start: start, end: start + theLen} : {start: start + theLen, end: start};
}
