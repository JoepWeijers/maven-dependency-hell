package com.topdesk.maven_hell.problem;

import com.topdesk.maven_hell.a.ThisClassInADependsOnGuava10;

public class H6CallingGuava10 {
	public static void main(String[] args) {
		ThisClassInADependsOnGuava10.methodOnlyInGuava10();
		System.out.println("Done");
	}
}
