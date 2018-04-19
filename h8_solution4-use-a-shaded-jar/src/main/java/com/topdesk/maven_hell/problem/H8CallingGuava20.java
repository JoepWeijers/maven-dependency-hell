package com.topdesk.maven_hell.problem;

import com.topdesk.maven_hell.b.ThisClassInBDependsOnGuava20;

public class H8CallingGuava20 {
	public static void main(String[] args) {
		ThisClassInBDependsOnGuava20.methodOnlyInGuava20();
		System.out.println("Done");
	}
}
