package org.example.tool;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExcelHeaderReader {
	public List<String> readHeaders(Path path, String sheetName) throws Exception {
		if (sheetName != null && sheetName.isBlank()) {
			sheetName = null;
		}
		try (InputStream input = new FileInputStream(path.toFile());
				 Workbook workbook = WorkbookFactory.create(input)) {
			Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
			if (sheet == null) {
				throw new IllegalArgumentException("Sheet not found: " + sheetName);
			}
			Row header = sheet.getRow(0);
			List<String> fields = new ArrayList<>();
			if (header == null) {
				return fields;
			}
			DataFormatter formatter = new DataFormatter();
			for (Cell cell : header) {
				String value = formatter.formatCellValue(cell).trim();
				fields.add(value);
			}
			return fields;
		}
	}
}
