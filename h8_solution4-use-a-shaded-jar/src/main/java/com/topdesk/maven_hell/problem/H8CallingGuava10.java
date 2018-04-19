package com.topdesk.maven_hell.problem;

import com.topdesk.maven_hell.a.ThisClassDependsOnGuava10;

public class H8CallingGuava10 {
	public static void main(String[] args) {
		ThisClassDependsOnGuava10.methodOnlyInGuava10();
		System.out.println("Done");
	}
}
