package com.dragonzone.network;

import java.util.List;

public class Network {
	private List<Node> nodes;
	private List<Edge> edges;
	public List<Node> getNodes() {
		return nodes;
	}
	public void setNodes(List<Node> nodeList) {
		this.nodes = nodeList;
	}
	public List<Edge> getEdges() {
		return edges;
	}
	public void setEdges(List<Edge> edgeList) {
		this.edges = edgeList;
	}
}
