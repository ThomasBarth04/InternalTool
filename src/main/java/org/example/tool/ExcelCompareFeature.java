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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ExcelCompareFeature implements ToolFeature {
	private Path leftPath;
	private Path rightPath;
	private Mode mode = Mode.UNION;
	private String sheetName;

	private enum Mode {
		UNION,
		LEFT_ONLY,
		RIGHT_ONLY,
		SYMMETRIC_DIFF
	}

	@Override
	public String name() {
		return "excel-compare";
	}

	@Override
	public String description() {
		return "Compare header fields between two Excel files.";
	}

	@Override
	public String usage() {
		return "excel-compare --left <path> --right <path> --mode <union|left-only|right-only|symmetric-diff> [--sheet <name>]";
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
						java.util.List.of("union", "left-only", "right-only", "symmetric-diff")
				),
				FeatureArgument.text("sheet", "Sheet name (optional)", false)
		);
	}

	@Override
	public void configure(String[] args) {
		CliArgs cli = new CliArgs(args);
		leftPath = Path.of(cli.getRequired("left"));
		rightPath = Path.of(cli.getRequired("right"));
		sheetName = cli.getOptional("sheet");

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
				default:
					throw new IllegalArgumentException("Unknown mode: " + modeValue);
			}
		}
	}

	@Override
	public int run() throws Exception {
		validatePath(leftPath, "left");
		validatePath(rightPath, "right");

		Set<String> leftFields = readHeaderFields(leftPath);
		Set<String> rightFields = readHeaderFields(rightPath);

		Set<String> result = new LinkedHashSet<>();
		switch (mode) {
			case UNION:
				result.addAll(leftFields);
				result.addAll(rightFields);
				break;
			case LEFT_ONLY:
				result.addAll(leftFields);
				result.removeAll(rightFields);
				break;
			case RIGHT_ONLY:
				result.addAll(rightFields);
				result.removeAll(leftFields);
				break;
			case SYMMETRIC_DIFF:
				Set<String> union = new LinkedHashSet<>(leftFields);
				union.addAll(rightFields);
				Set<String> intersection = new LinkedHashSet<>(leftFields);
				intersection.retainAll(rightFields);
				union.removeAll(intersection);
				result.addAll(union);
				break;
			default:
				throw new IllegalStateException("Unhandled mode: " + mode);
		}

		for (String field : result) {
			System.out.println(field);
		}
		return 0;
	}

	private void validatePath(Path path, String label) {
		if (path == null || !Files.exists(path)) {
			throw new IllegalArgumentException("Missing or invalid --" + label + " path: " + path);
		}
	}

	private Set<String> readHeaderFields(Path path) throws Exception {
		try (InputStream input = new FileInputStream(path.toFile());
				 Workbook workbook = WorkbookFactory.create(input)) {
			Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
			if (sheet == null) {
				throw new IllegalArgumentException("Sheet not found: " + sheetName);
			}
			Row header = sheet.getRow(0);
			Set<String> fields = new LinkedHashSet<>();
			if (header == null) {
				return fields;
			}
			DataFormatter formatter = new DataFormatter();
			for (Cell cell : header) {
				String value = formatter.formatCellValue(cell).trim();
				if (!value.isEmpty()) {
					fields.add(value);
				}
			}
			return fields;
		}
	}
}
