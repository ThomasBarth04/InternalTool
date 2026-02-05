package org.example.tool;

public interface ToolFeature {
	String name();

	String description();

	String usage();

	default java.util.List<FeatureArgument> arguments() {
		return java.util.List.of();
	}

	void configure(String[] args);

	int run() throws Exception;
}
