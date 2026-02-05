package org.example.tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CliArgs {
	private final Map<String, String> values = new HashMap<>();

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
			values.put(key, value);
		}
	}

	public String getRequired(String key) {
		String value = values.get(key);
		if (value == null) {
			throw new IllegalArgumentException("Missing --" + key + " argument");
		}
		return value;
	}

	public String getOptional(String key) {
		return values.get(key);
	}

	public boolean has(String key) {
		return values.containsKey(key);
	}

	@Override
	public String toString() {
		return Arrays.toString(values.entrySet().toArray());
	}
}
