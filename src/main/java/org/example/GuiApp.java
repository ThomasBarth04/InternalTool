package org.example;

import org.example.tool.ExcelCompareFeature;
import org.example.tool.ExcelHeaderReader;
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
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GuiApp implements App {
	private final FeatureRegistry registry = new FeatureRegistry();
	private final Map<String, Object> inputComponents = new HashMap<>();
	private JTextArea outputArea;
	private JPanel argsPanel;
	private JLabel featureDescription;
	private JComboBox<String> featureSelector;
	private Timer runTimer;
	private SwingWorker<Integer, Void> activeWorker;
	private boolean pendingRun;

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
		JButton exportButton = new JButton("Export Output");
		exportButton.addActionListener(event -> exportOutput());
		bottomPanel.add(exportButton, BorderLayout.WEST);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(centerPanel, BorderLayout.CENTER);
		frame.add(bottomPanel, BorderLayout.SOUTH);

		rebuildArgsPanel();
		scheduleRun();

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
		scale = Math.min(scale, 2.0);
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof Font font) {
				Font scaled = font.deriveFont((float) (font.getSize2D() * scale));
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
					field.getDocument().addDocumentListener(new SimpleChangeListener(this::scheduleRun));
					argsPanel.add(field, constraints);
					inputComponents.put(argument.key(), field);
				}
				case FILE -> {
					JPanel filePanel = new JPanel(new BorderLayout(6, 0));
					JTextField field = new JTextField();
					field.getDocument().addDocumentListener(new SimpleChangeListener(this::scheduleRun));
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
					choices.addActionListener(event -> scheduleRun());
					argsPanel.add(choices, constraints);
					inputComponents.put(argument.key(), choices);
				}
				case FLAG -> {
					JCheckBox checkBox = new JCheckBox();
					checkBox.addActionListener(event -> scheduleRun());
					argsPanel.add(checkBox, constraints);
					inputComponents.put(argument.key(), checkBox);
				}
				case MAPPING -> {
					MappingPanel mappingPanel = new MappingPanel();
					argsPanel.add(mappingPanel, constraints);
					inputComponents.put(argument.key(), mappingPanel);
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
		scheduleRun();
	}

	private void chooseFile(JTextField field) {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(argsPanel);
		if (result == JFileChooser.APPROVE_OPTION) {
			field.setText(chooser.getSelectedFile().getAbsolutePath());
			scheduleRun();
		}
	}

	private void scheduleRun() {
		if (runTimer == null) {
			runTimer = new Timer(400, event -> startRun());
			runTimer.setRepeats(false);
		}
		pendingRun = true;
		runTimer.restart();
	}

	private void startRun() {
		if (!pendingRun) {
			return;
		}
		if (activeWorker != null) {
			return;
		}
		pendingRun = false;
		runSelectedFeature();
	}

	private void runSelectedFeature() {
		ToolFeature feature = getSelectedFeature();
		List<String> args = new ArrayList<>();

		for (FeatureArgument argument : feature.arguments()) {
			Object component = inputComponents.get(argument.key());

			if (argument.type() == FeatureArgument.Type.FLAG) {
				String value = readComponentValue(component);
				if ("true".equalsIgnoreCase(value)) {
					args.add("--" + argument.key());
				}
				continue;
			}

			if (argument.type() == FeatureArgument.Type.MAPPING) {
				MappingPanel mappingPanel = (MappingPanel) component;
				List<HeaderPair> pairs = mappingPanel.getMappings();
				if (argument.required() && pairs.isEmpty()) {
					outputArea.setText("Waiting for required inputs.");
					return;
				}
				for (HeaderPair pair : pairs) {
					args.add("--" + argument.key());
					args.add(pair.left + "=" + pair.right);
				}
				continue;
			}

			String value = readComponentValue(component);
			if (argument.required() && (value == null || value.isBlank())) {
				outputArea.setText("Waiting for required inputs.");
				return;
			}

			if (value != null && !value.isBlank()) {
				args.add("--" + argument.key());
				args.add(value);
			}
		}

		outputArea.setText("Running...");
		activeWorker = new SwingWorker<>() {
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
				activeWorker = null;
				if (pendingRun) {
					startRun();
				}
			}
		};
		activeWorker.execute();
	}

	private void exportOutput() {
		String output = outputArea.getText();
		if (output == null || output.isBlank()) {
			outputArea.setText("Nothing to export yet.");
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export Output");
		FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx");
		chooser.addChoosableFileFilter(xlsxFilter);
		chooser.setFileFilter(xlsxFilter);
		int result = chooser.showSaveDialog(argsPanel);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		Path target = ensureExtension(chooser.getSelectedFile().toPath(), "xlsx");
		try {
			writeXlsx(target, output);
		} catch (IOException exception) {
			outputArea.setText("Failed to export: " + exception.getMessage());
		}
	}

	private Path ensureExtension(Path path, String extension) {
		String filename = path.getFileName().toString();
		String lower = filename.toLowerCase(Locale.ROOT);
		if (lower.endsWith("." + extension)) {
			return path;
		}
		return path.resolveSibling(filename + "." + extension);
	}

	private void writeXlsx(Path target, String output) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Output");
			String[] lines = output.split("\\R", -1);
			for (int i = 0; i < lines.length; i++) {
				List<String> cells = parseCsvLine(lines[i]);
				Row row = sheet.createRow(i);
				for (int col = 0; col < cells.size(); col++) {
					row.createCell(col).setCellValue(cells.get(col));
				}
			}
			autoSizeColumns(sheet);
			try (FileOutputStream out = new FileOutputStream(target.toFile())) {
				workbook.write(out);
			}
		}
	}

	private List<String> parseCsvLine(String line) {
		List<String> cells = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}
			if (ch == ',' && !inQuotes) {
				cells.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(ch);
		}
		cells.add(current.toString());
		return cells;
	}

	private void autoSizeColumns(Sheet sheet) {
		int maxColumns = 0;
		for (Row row : sheet) {
			if (row.getLastCellNum() > maxColumns) {
				maxColumns = row.getLastCellNum();
			}
		}
		for (int col = 0; col < maxColumns; col++) {
			sheet.autoSizeColumn(col);
		}
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

	private class MappingPanel extends JPanel {
		private final JComboBox<String> leftCombo = new JComboBox<>();
		private final JComboBox<String> rightCombo = new JComboBox<>();
		private final DefaultListModel<HeaderPair> mappingModel = new DefaultListModel<>();
		private final JList<HeaderPair> mappingList = new JList<>(mappingModel);

		private MappingPanel() {
			super(new BorderLayout(6, 6));
			JPanel controls = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(2, 2, 2, 2);
			constraints.gridy = 0;
			constraints.fill = GridBagConstraints.HORIZONTAL;

			JButton scanButton = new JButton("Scan Headers");
			scanButton.addActionListener(event -> scanHeaders());
			constraints.gridx = 0;
			constraints.gridwidth = 3;
			controls.add(scanButton, constraints);

			constraints.gridy++;
			constraints.gridwidth = 1;
			constraints.gridx = 0;
			controls.add(new JLabel("Left"), constraints);
			constraints.gridx = 1;
			controls.add(new JLabel("Right"), constraints);

			constraints.gridy++;
			constraints.gridx = 0;
			constraints.weightx = 1;
			controls.add(leftCombo, constraints);
			constraints.gridx = 1;
			controls.add(rightCombo, constraints);
			constraints.gridx = 2;
			constraints.weightx = 0;
			JButton addButton = new JButton("Add");
			addButton.addActionListener(event -> addMapping());
			controls.add(addButton, constraints);

			constraints.gridy++;
			constraints.gridx = 0;
			constraints.gridwidth = 1;
			JButton removeButton = new JButton("Remove");
			removeButton.addActionListener(event -> removeMapping());
			controls.add(removeButton, constraints);
			constraints.gridx = 1;
			JButton clearButton = new JButton("Clear");
			clearButton.addActionListener(event -> clearMappings());
			controls.add(clearButton, constraints);

			mappingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane mappingScroll = new JScrollPane(mappingList);
			mappingScroll.setBorder(BorderFactory.createTitledBorder("Mappings"));

			add(controls, BorderLayout.NORTH);
			add(mappingScroll, BorderLayout.CENTER);
		}

		private void scanHeaders() {
			String leftPath = textValue("left");
			String rightPath = textValue("right");
			String sheet = textValue("sheet");
			if (leftPath == null || leftPath.isBlank() || rightPath == null || rightPath.isBlank()) {
				outputArea.setText("Provide both left and right file paths before scanning.");
				return;
			}

			try {
				ExcelHeaderReader reader = new ExcelHeaderReader();
				List<String> leftHeaders = reader.readHeaders(Path.of(leftPath), sheet);
				List<String> rightHeaders = reader.readHeaders(Path.of(rightPath), sheet);
				refreshCombo(leftCombo, leftHeaders);
				refreshCombo(rightCombo, rightHeaders);
				outputArea.setText("Headers loaded. Select fields and click Add.");
				scheduleRun();
			} catch (Exception exception) {
				outputArea.setText("Failed to scan headers: " + exception.getMessage());
			}
		}

		private void refreshCombo(JComboBox<String> combo, List<String> values) {
			combo.removeAllItems();
			for (String value : values) {
				if (value != null && !value.isBlank()) {
					combo.addItem(value);
				}
			}
			combo.setSelectedIndex(combo.getItemCount() > 0 ? 0 : -1);
		}

		private void addMapping() {
			Object left = leftCombo.getSelectedItem();
			Object right = rightCombo.getSelectedItem();
			if (left == null || right == null) {
				outputArea.setText("Select both left and right headers.");
				return;
			}
			HeaderPair pair = new HeaderPair(left.toString(), right.toString());
			mappingModel.addElement(pair);
			scheduleRun();
		}

		private void removeMapping() {
			int index = mappingList.getSelectedIndex();
			if (index >= 0) {
				mappingModel.remove(index);
				scheduleRun();
			}
		}

		private void clearMappings() {
			mappingModel.clear();
			scheduleRun();
		}

		private List<HeaderPair> getMappings() {
			List<HeaderPair> list = new ArrayList<>();
			for (int i = 0; i < mappingModel.size(); i++) {
				list.add(mappingModel.get(i));
			}
			return list;
		}
	}

	private static class SimpleChangeListener implements javax.swing.event.DocumentListener {
		private final Runnable callback;

		private SimpleChangeListener(Runnable callback) {
			this.callback = callback;
		}

		@Override
		public void insertUpdate(javax.swing.event.DocumentEvent event) {
			callback.run();
		}

		@Override
		public void removeUpdate(javax.swing.event.DocumentEvent event) {
			callback.run();
		}

		@Override
		public void changedUpdate(javax.swing.event.DocumentEvent event) {
			callback.run();
		}
	}

	private String textValue(String key) {
		Object component = inputComponents.get(key);
		if (component instanceof JTextField field) {
			return field.getText();
		}
		return null;
	}

	private static class HeaderPair {
		private final String left;
		private final String right;

		private HeaderPair(String left, String right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public String toString() {
			return left + " -> " + right;
		}
	}
}
