package org.example.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureRegistry {
	private final Map<String, ToolFeature> features = new HashMap<>();

	public void register(ToolFeature feature) {
		features.put(feature.name(), feature);
	}

	public ToolFeature get(String name) {
		return features.get(name);
	}

	public List<ToolFeature> list() {
		List<ToolFeature> list = new ArrayList<>(features.values());
		list.sort(Comparator.comparing(ToolFeature::name));
		return list;
	}
}
