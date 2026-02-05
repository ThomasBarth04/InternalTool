package org.example.tool;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExcelCompareFeature implements ToolFeature {
	private Path leftPath;
	private Path rightPath;
	private Mode mode = Mode.UNION;
	private String sheetName;
	private final java.util.List<HeaderPair> mappings = new java.util.ArrayList<>();

	private enum Mode {
		UNION,
		LEFT_ONLY,
		RIGHT_ONLY,
		SYMMETRIC_DIFF,
		CHANGES
	}

	@Override
	public String name() {
		return "excel-compare";
	}

	@Override
	public String description() {
		return "Compare rows between two Excel files using mapped columns.";
	}

	@Override
	public String usage() {
		return "excel-compare --left <path> --right <path> --map <left=right> [--map <left=right> ...] "
				+ "--mode <union|left-only|right-only|symmetric-diff|changes> [--sheet <name>]";
	}

	@Override
	public java.util.List<FeatureArgument> arguments() {
		return java.util.List.of(
				FeatureArgument.file("left", "Left Excel file", true),
				FeatureArgument.file("right", "Right Excel file", true),
				FeatureArgument.choice(
						"mode",
						"Comparison mode",
						false,
						java.util.List.of("union", "left-only", "right-only", "symmetric-diff", "changes (old - new)")
				),
				FeatureArgument.text("sheet", "Sheet name (optional)", false),
				FeatureArgument.mapping("map", "Header mapping", true)
		);
	}

	@Override
	public void configure(String[] args) {
		CliArgs cli = new CliArgs(args);
		leftPath = Path.of(cli.getRequired("left"));
		rightPath = Path.of(cli.getRequired("right"));
		sheetName = cli.getOptional("sheet");
		mappings.clear();
		for (String mapArg : cli.getAll("map")) {
			HeaderPair pair = parseMapping(mapArg);
			mappings.add(pair);
		}

		String modeValue = cli.getOptional("mode");
		if (modeValue != null) {
			switch (modeValue.toLowerCase(Locale.ROOT)) {
				case "union":
					mode = Mode.UNION;
					break;
				case "left-only":
					mode = Mode.LEFT_ONLY;
					break;
				case "right-only":
					mode = Mode.RIGHT_ONLY;
					break;
				case "symmetric-diff":
					mode = Mode.SYMMETRIC_DIFF;
					break;
				case "changes":
					mode = Mode.CHANGES;
					break;
				default:
					throw new IllegalArgumentException("Unknown mode: " + modeValue);
			}
		}
	}

	@Override
	public int run() throws Exception {
		validatePath(leftPath, "left");
		validatePath(rightPath, "right");
		if (mappings.isEmpty()) {
			throw new IllegalArgumentException("At least one --map <left=right> is required");
		}

		SheetData leftData = readSheetData(leftPath);
		SheetData rightData = readSheetData(rightPath);
		validateMappings(leftData.headers, rightData.headers);

		Set<Key> leftKeys = extractKeys(leftData, true);
		Set<Key> rightKeys = extractKeys(rightData, false);
		LinkedHashMap<Key, RowData> leftRows = extractRows(leftData, true);
		LinkedHashMap<Key, RowData> rightRows = extractRows(rightData, false);

		Set<Key> result = new LinkedHashSet<>();
		switch (mode) {
			case UNION:
				printSection("LEFT", leftData.headers, listRows(leftRows, leftKeys));
				Set<Key> rightOnlyUnion = new LinkedHashSet<>(rightKeys);
				rightOnlyUnion.removeAll(leftKeys);
				if (!rightOnlyUnion.isEmpty()) {
					System.out.println();
					printSection("RIGHT_ONLY", rightData.headers, listRows(rightRows, rightOnlyUnion));
				}
				return 0;
			case LEFT_ONLY:
				result.addAll(leftKeys);
				result.removeAll(rightKeys);
				printSection(null, leftData.headers, listRows(leftRows, result));
				return 0;
			case RIGHT_ONLY:
				result.addAll(rightKeys);
				result.removeAll(leftKeys);
				printSection(null, rightData.headers, listRows(rightRows, result));
				return 0;
			case SYMMETRIC_DIFF:
				Set<Key> leftOnly = new LinkedHashSet<>(leftKeys);
				leftOnly.removeAll(rightKeys);
				Set<Key> rightOnly = new LinkedHashSet<>(rightKeys);
				rightOnly.removeAll(leftKeys);
				printSection("LEFT_ONLY", leftData.headers, listRows(leftRows, leftOnly));
				if (!rightOnly.isEmpty()) {
					System.out.println();
					printSection("RIGHT_ONLY", rightData.headers, listRows(rightRows, rightOnly));
				}
				return 0;
			case CHANGES:
				Set<Key> newContacts = new LinkedHashSet<>(rightKeys);
				newContacts.removeAll(leftKeys);
				Set<Key> deprecated = new LinkedHashSet<>(leftKeys);
				deprecated.removeAll(rightKeys);
				printSection("NEW", rightData.headers, listRows(rightRows, newContacts));
				System.out.println();
				printSection("DEPRECATED", leftData.headers, listRows(leftRows, deprecated));
				return 0;
			default:
				throw new IllegalStateException("Unhandled mode: " + mode);
		}
	}

	private void printSection(String label, List<String> headers, List<RowData> rows) {
		if (label != null) {
			System.out.println("[" + label + "]");
		}
		System.out.println(toCsvLine(headers));
		for (RowData row : rows) {
			System.out.println(toCsvLine(row.values));
		}
	}

	private void validatePath(Path path, String label) {
		if (path == null || !Files.exists(path)) {
			throw new IllegalArgumentException("Missing or invalid --" + label + " path: " + path);
		}
	}

	private SheetData readSheetData(Path path) throws Exception {
		try (InputStream input = new FileInputStream(path.toFile());
				 Workbook workbook = WorkbookFactory.create(input)) {
			Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
			if (sheet == null) {
				throw new IllegalArgumentException("Sheet not found: " + sheetName);
			}
			Row header = sheet.getRow(0);
			Map<String, Integer> headerIndex = new HashMap<>();
			java.util.List<String> headers = new ArrayList<>();
			if (header != null) {
				DataFormatter formatter = new DataFormatter();
				for (Cell cell : header) {
					String value = formatter.formatCellValue(cell).trim();
					headers.add(value);
					if (!value.isEmpty() && !headerIndex.containsKey(value)) {
						headerIndex.put(value, cell.getColumnIndex());
					}
				}
			}
			return new SheetData(sheet, headers, headerIndex);
		}
	}

	private void validateMappings(java.util.List<String> leftHeaders, java.util.List<String> rightHeaders) {
		for (HeaderPair pair : mappings) {
			if (!leftHeaders.contains(pair.left())) {
				throw new IllegalArgumentException("Left header not found: " + pair.left());
			}
			if (!rightHeaders.contains(pair.right())) {
				throw new IllegalArgumentException("Right header not found: " + pair.right());
			}
		}
	}

	private Set<Key> extractKeys(SheetData data, boolean useLeft) {
		Set<Key> keys = new LinkedHashSet<>();
		DataFormatter formatter = new DataFormatter();
		int lastRow = data.sheet.getLastRowNum();
		for (int i = 1; i <= lastRow; i++) {
			Row row = data.sheet.getRow(i);
			if (row == null) {
				continue;
			}
			java.util.List<String> values = new ArrayList<>();
			boolean allBlank = true;
			for (HeaderPair pair : mappings) {
				String header = useLeft ? pair.left() : pair.right();
				Integer index = data.headerIndex.get(header);
				String value = index == null ? "" : formatter.formatCellValue(row.getCell(index)).trim();
				if (!value.isEmpty()) {
					allBlank = false;
				}
				values.add(value);
			}
			if (!allBlank) {
				keys.add(new Key(java.util.List.copyOf(values)));
			}
		}
		return keys;
	}

	private LinkedHashMap<Key, RowData> extractRows(SheetData data, boolean useLeft) {
		LinkedHashMap<Key, RowData> rows = new LinkedHashMap<>();
		DataFormatter formatter = new DataFormatter();
		int lastRow = data.sheet.getLastRowNum();
		for (int i = 1; i <= lastRow; i++) {
			Row row = data.sheet.getRow(i);
			if (row == null) {
				continue;
			}
			List<String> values = new ArrayList<>();
			for (int col = 0; col < data.headers.size(); col++) {
				String value = formatter.formatCellValue(row.getCell(col)).trim();
				values.add(value);
			}

			List<String> keyValues = new ArrayList<>();
			boolean allBlank = true;
			for (HeaderPair pair : mappings) {
				String header = useLeft ? pair.left() : pair.right();
				Integer index = data.headerIndex.get(header);
				String value = index == null ? "" : formatter.formatCellValue(row.getCell(index)).trim();
				if (!value.isEmpty()) {
					allBlank = false;
				}
				keyValues.add(value);
			}
			if (allBlank) {
				continue;
			}
			Key key = new Key(List.copyOf(keyValues));
			rows.putIfAbsent(key, new RowData(List.copyOf(values)));
		}
		return rows;
	}

	private HeaderPair parseMapping(String mapArg) {
		int index = mapArg.indexOf('=');
		if (index <= 0 || index == mapArg.length() - 1) {
			throw new IllegalArgumentException("Invalid mapping (use left=right): " + mapArg);
		}
		String left = mapArg.substring(0, index).trim();
		String right = mapArg.substring(index + 1).trim();
		if (left.isEmpty() || right.isEmpty()) {
			throw new IllegalArgumentException("Invalid mapping (use left=right): " + mapArg);
		}
		return new HeaderPair(left, right);
	}

	private List<RowData> listRows(LinkedHashMap<Key, RowData> rows, Set<Key> keys) {
		List<RowData> list = new ArrayList<>();
		for (Map.Entry<Key, RowData> entry : rows.entrySet()) {
			if (keys.contains(entry.getKey())) {
				list.add(entry.getValue());
			}
		}
		return list;
	}

	private String toCsvLine(List<String> values) {
		List<String> escaped = new ArrayList<>();
		for (String value : values) {
			String safe = value == null ? "" : value;
			boolean needsQuotes = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
			if (needsQuotes) {
				safe = "\"" + safe.replace("\"", "\"\"") + "\"";
			}
			escaped.add(safe);
		}
		return String.join(",", escaped);
	}

	private record SheetData(Sheet sheet, java.util.List<String> headers, Map<String, Integer> headerIndex) {
	}

	private record HeaderPair(String left, String right) {
	}

	private record Key(java.util.List<String> values) {
	}

	private record RowData(List<String> values) {
	}
}
