package com.topdesk.maven_hell.problem;

import com.topdesk.maven_hell.a.ThisClassInADependsOnGuava10;

public class H3CallingGuava10 {
	public static void main(String[] args) {
		ThisClassInADependsOnGuava10.methodBothInGuava10And20();
		System.out.println("Done");
	}
}
