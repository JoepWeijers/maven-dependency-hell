package com.topdesk.maven_hell.b;

import com.topdesk.maven_hell.c.ThisClassInCDependsOnGuava20;

public class ThisClassInBDependsOnGuava20 {
	public static void methodOnlyInGuava20() {
		ThisClassInCDependsOnGuava20.methodOnlyInGuava20();
	}
}
