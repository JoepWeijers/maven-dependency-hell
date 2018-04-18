package com.topdesk.maven_hell.a;

import com.google.common.base.Equivalences;
import com.google.common.collect.ImmutableMap;

public class ThisClassDependsOnGuava10 {
	public static void methodOnlyInGuava10() {
		Equivalences.identity();
	}
	
	public static void methodBothInGuava10And20() {
		ImmutableMap.of();
	}
}
