package example;

import org.apache.commons.cli.*;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.PrintWriter;

public class NewCLI {
    private static Options OPTIONS = new Options();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;
    public static void main(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();
        // help
        OPTIONS.addOption("h","help",false,"usage help");

        // port
        OPTIONS.addOption(Option.builder("t").hasArg(true).longOpt("task").type(String.class)
                .desc("target task").build());
        try {
            commandLine = commandLineParser.parse(OPTIONS, args);
            if (commandLine.hasOption("h")){
                System.out.println( getHelpString());
            }
            if (commandLine.hasOption("t")){
                String task = commandLine.getOptionValue("t");
                System.out.println(task);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage() + "\n" + getHelpString());
            System.exit(0);
        }
    }

    private static String getHelpString() {
        if (HELP_STRING == null) {
            HelpFormatter helpFormatter = new HelpFormatter();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH, "regs4j -help", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        return HELP_STRING;
    }
}
