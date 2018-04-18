package com.topdesk.maven_hell.c;

import com.google.common.graph.GraphBuilder;

public class ThisClassInCDependsOnGuava20 {
	public static void methodOnlyInGuava20() {
		GraphBuilder.undirected().build();
	}
}
