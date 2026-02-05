package org.example;

import java.util.Arrays;

public class Main {
	public static void main(String[] args) {
		App app = new GuiApp();
		String[] remaining = stripModeArg(args);
		app.run(remaining);
	}


	private static String[] stripModeArg(String[] args) {
		if (args.length == 0) {
			return args;
		}
		String mode = args[0];
		if ("gui".equalsIgnoreCase(mode) || "--gui".equalsIgnoreCase(mode)
				|| "cli".equalsIgnoreCase(mode) || "--cli".equalsIgnoreCase(mode)) {
			return Arrays.copyOfRange(args, 1, args.length);
		}
		return args;
	}
}
