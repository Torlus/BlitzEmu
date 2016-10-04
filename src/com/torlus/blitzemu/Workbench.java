package com.torlus.blitzemu;

import java.util.TreeMap;
import java.util.Vector;

public class Workbench {
	// Copypasta of "Workspace" of the JagNetlists compiler project.
	// I don't know if this class will be of any use, but I couldn't 
	// let such an Amigq-related project without this pun.
	
	private Vector<TreeMap<String, Value>> variables = new Vector<>();
	
	public void enterScope() {
		variables.add(new TreeMap<String, Value>());
	}
	public void exitScope() {
		variables.remove(variables.size() - 1);
	}
	
	public void setVar(int level, String name, Value value) {
		TreeMap<String, Value> scope = null;
		for(int n = level; n >= 0; n--) {
			scope = variables.get(n);
			if (scope.get(name) != null) {
				break;
			}
			scope = null;
		}
		if (scope == null) {
			scope = variables.get(level);
		}
		scope.put(name, value);
	}
	
	public Value getVar(int level, String name) {
		TreeMap<String, Value> scope = null;
		Value value = null;
		for(int n = level; n >= 0; n--) {
			scope = variables.get(n);
			value = scope.get(name); 
			if (value != null) {
				return value;
			}
		}
		return null;
	}
	
	public Value evalFunction(String name, Vector<Value> params) throws Exception {
		throw new Exception("Unknown Function " + name);
	}

	public void evalCommand(String name, Vector<Value> params) throws Exception {
		if ("Print".equals(name)) {
			String line = "";
			for(int n = 0; n < params.size(); n++) {
				Value arg = params.get(n);
				switch(arg.type) {
				case BOOLEAN:
					line += (arg.boolValue ? "True" : "False");
					break;
				case FLOAT:
					line += arg.floatValue;
					break;
				case INTEGER:
					line += arg.intValue;
					break;
				case STRING:
					line += arg.value;
					break;
				default:
					line += "<undefined>";
					break;
				}
			}
			System.out.println(line);
		} else {
			throw new Exception("Unknown Command " + name);			
		}
	}
	
}
