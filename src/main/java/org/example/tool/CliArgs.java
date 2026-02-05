package org.example.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliArgs {
	private final Map<String, List<String>> values = new HashMap<>();

	public CliArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("Unexpected argument: " + arg);
			}
			String key = arg.substring(2);
			String value = "true";
			if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
				value = args[i + 1];
				i++;
			}
			values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
		}
	}

	public String getRequired(String key) {
		List<String> list = values.get(key);
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Missing --" + key + " argument");
		}
		return list.get(0);
	}

	public String getOptional(String key) {
		List<String> list = values.get(key);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	public List<String> getAll(String key) {
		List<String> list = values.get(key);
		return list == null ? List.of() : List.copyOf(list);
	}

	public boolean has(String key) {
		return values.containsKey(key);
	}

	@Override
	public String toString() {
		return Arrays.toString(values.entrySet().toArray());
	}
}
