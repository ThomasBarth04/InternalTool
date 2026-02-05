package org.example;

import org.example.tool.ExcelCompareFeature;
import org.example.tool.FeatureArgument;
import org.example.tool.FeatureRegistry;
import org.example.tool.ToolFeature;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiApp implements App {
	private final FeatureRegistry registry = new FeatureRegistry();
	private final Map<String, Object> inputComponents = new HashMap<>();
	private JTextArea outputArea;
	private JPanel argsPanel;
	private JLabel featureDescription;
	private JComboBox<String> featureSelector;

	@Override
	public int run(String[] args) {
		registry.register(new ExcelCompareFeature());

		SwingUtilities.invokeLater(this::buildAndShow);
		return 0;
	}

	private void buildAndShow() {
		applyScaling();

		JFrame frame = new JFrame("Internal Tool");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout(12, 12));

		JPanel topPanel = new JPanel(new BorderLayout(8, 8));
		featureSelector = new JComboBox<>(registry.list().stream().map(ToolFeature::name).toArray(String[]::new));
		featureSelector.addActionListener(event -> rebuildArgsPanel());
		topPanel.add(new JLabel("Feature"), BorderLayout.WEST);
		topPanel.add(featureSelector, BorderLayout.CENTER);
		featureDescription = new JLabel();
		topPanel.add(featureDescription, BorderLayout.SOUTH);
		topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

		argsPanel = new JPanel(new GridBagLayout());
		argsPanel.setBorder(BorderFactory.createTitledBorder("Arguments"));

		JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
		centerPanel.add(argsPanel, BorderLayout.NORTH);

		outputArea = new JTextArea(10, 60);
		outputArea.setEditable(false);
		JScrollPane outputScroll = new JScrollPane(outputArea);
		outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));
		centerPanel.add(outputScroll, BorderLayout.CENTER);
		centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

		JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
		JButton runButton = new JButton("Run");
		runButton.addActionListener(event -> runSelectedFeature());
		bottomPanel.add(runButton, BorderLayout.EAST);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(centerPanel, BorderLayout.CENTER);
		frame.add(bottomPanel, BorderLayout.SOUTH);

		rebuildArgsPanel();

		frame.pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int targetWidth = Math.max(900, (int) (screen.width * 0.6));
		int targetHeight = Math.max(650, (int) (screen.height * 0.6));
		frame.setSize(new Dimension(targetWidth, targetHeight));
		frame.setMinimumSize(new Dimension(700, 500));
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void applyScaling() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		double scale = Math.max(1.0, screen.width / 1920.0);
		scale = Math.min(scale, 1.5);
		Font baseFont = UIManager.getFont("Label.font");
		if (baseFont == null) {
			return;
		}
		Font scaled = baseFont.deriveFont((float) (baseFont.getSize2D() * scale));
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof Font) {
				UIManager.put(key, scaled);
			}
		}
	}

	private void rebuildArgsPanel() {
		argsPanel.removeAll();
		inputComponents.clear();

		ToolFeature feature = getSelectedFeature();
		featureDescription.setText(feature.description());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = 0;
		constraints.insets = new Insets(4, 6, 4, 6);
		constraints.fill = GridBagConstraints.HORIZONTAL;

		for (FeatureArgument argument : feature.arguments()) {
			constraints.gridx = 0;
			constraints.weightx = 0;
			argsPanel.add(new JLabel(labelFor(argument)), constraints);

			constraints.gridx = 1;
			constraints.weightx = 1;

			switch (argument.type()) {
				case TEXT -> {
					JTextField field = new JTextField();
					argsPanel.add(field, constraints);
					inputComponents.put(argument.key(), field);
				}
				case FILE -> {
					JPanel filePanel = new JPanel(new BorderLayout(6, 0));
					JTextField field = new JTextField();
					JButton browse = new JButton("Browse");
					browse.addActionListener(event -> chooseFile(field));
					filePanel.add(field, BorderLayout.CENTER);
					filePanel.add(browse, BorderLayout.EAST);
					argsPanel.add(filePanel, constraints);
					inputComponents.put(argument.key(), field);
				}
				case CHOICE -> {
					JComboBox<String> choices = new JComboBox<>(argument.choices().toArray(String[]::new));
					choices.setSelectedIndex(-1);
					argsPanel.add(choices, constraints);
					inputComponents.put(argument.key(), choices);
				}
				case FLAG -> {
					JCheckBox checkBox = new JCheckBox();
					argsPanel.add(checkBox, constraints);
					inputComponents.put(argument.key(), checkBox);
				}
				default -> {
					JTextField field = new JTextField();
					argsPanel.add(field, constraints);
					inputComponents.put(argument.key(), field);
				}
			}

			constraints.gridy++;
		}

		argsPanel.revalidate();
		argsPanel.repaint();
	}

	private void chooseFile(JTextField field) {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(argsPanel);
		if (result == JFileChooser.APPROVE_OPTION) {
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void runSelectedFeature() {
		ToolFeature feature = getSelectedFeature();
		List<String> args = new ArrayList<>();

		for (FeatureArgument argument : feature.arguments()) {
			Object component = inputComponents.get(argument.key());
			String value = readComponentValue(component);

			if (argument.required() && (value == null || value.isBlank())) {
				outputArea.setText("Missing required input: " + argument.label());
				return;
			}

			if (argument.type() == FeatureArgument.Type.FLAG) {
				if ("true".equalsIgnoreCase(value)) {
					args.add("--" + argument.key());
				}
				continue;
			}

			if (value != null && !value.isBlank()) {
				args.add("--" + argument.key());
				args.add(value);
			}
		}

		outputArea.setText("Running...");
		SwingWorker<Integer, Void> worker = new SwingWorker<>() {
			private String output;

			@Override
			protected Integer doInBackground() {
				PrintStream originalOut = System.out;
				PrintStream originalErr = System.err;
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				PrintStream capture = new PrintStream(buffer);
				System.setOut(capture);
				System.setErr(capture);
				try {
					feature.configure(args.toArray(new String[0]));
					int code = feature.run();
					output = buffer.toString();
					return code;
				} catch (Exception exception) {
					output = exception.getMessage();
					return 1;
				} finally {
					System.setOut(originalOut);
					System.setErr(originalErr);
				}
			}

			@Override
			protected void done() {
				outputArea.setText(output == null ? "" : output);
			}
		};
		worker.execute();
	}

	private ToolFeature getSelectedFeature() {
		String selected = (String) featureSelector.getSelectedItem();
		ToolFeature feature = registry.get(selected);
		if (feature == null) {
			return registry.list().get(0);
		}
		return feature;
	}

	private String labelFor(FeatureArgument argument) {
		if (argument.required()) {
			return argument.label() + " *";
		}
		return argument.label();
	}

	private String readComponentValue(Object component) {
		if (component instanceof JTextField field) {
			return field.getText();
		}
		if (component instanceof JComboBox<?> comboBox) {
			Object selected = comboBox.getSelectedItem();
			return selected == null ? null : selected.toString();
		}
		if (component instanceof JCheckBox checkBox) {
			return String.valueOf(checkBox.isSelected());
		}
		return null;
	}
}
