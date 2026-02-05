package org.example.tool;

import java.util.List;

public class FeatureArgument {
	public enum Type {
		TEXT,
		FILE,
		CHOICE,
		FLAG,
		MAPPING
	}

	private final String key;
	private final String label;
	private final boolean required;
	private final Type type;
	private final List<String> choices;

	private FeatureArgument(String key, String label, boolean required, Type type, List<String> choices) {
		this.key = key;
		this.label = label;
		this.required = required;
		this.type = type;
		this.choices = choices;
	}

	public static FeatureArgument text(String key, String label, boolean required) {
		return new FeatureArgument(key, label, required, Type.TEXT, List.of());
	}

	public static FeatureArgument file(String key, String label, boolean required) {
		return new FeatureArgument(key, label, required, Type.FILE, List.of());
	}

	public static FeatureArgument choice(String key, String label, boolean required, List<String> choices) {
		return new FeatureArgument(key, label, required, Type.CHOICE, choices);
	}

	public static FeatureArgument flag(String key, String label) {
		return new FeatureArgument(key, label, false, Type.FLAG, List.of());
	}

	public static FeatureArgument mapping(String key, String label, boolean required) {
		return new FeatureArgument(key, label, required, Type.MAPPING, List.of());
	}

	public String key() {
		return key;
	}

	public String label() {
		return label;
	}

	public boolean required() {
		return required;
	}

	public Type type() {
		return type;
	}

	public List<String> choices() {
		return choices;
	}
}
