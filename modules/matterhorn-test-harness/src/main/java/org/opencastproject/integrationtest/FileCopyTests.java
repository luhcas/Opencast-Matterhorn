package org.opencastproject.integrationtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class FileCopyTests {

    private static File sourceDir;
    private static File destDir;
    private static int numCopies;

    private static ExecutorService executor;

    // /Users/john/dev/source
    /**
     * @param args
     */
    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(new Option("s", "source", true, "source directory"));
        options.addOption(new Option("d", "dest", true, "destination directory"));
        options.addOption(new Option("n", "number", true, "number of concurrent copies to make"));
        options.addOption(new Option("m", "message", true, "optional message describing test"));
        
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.println("Parsing commandline failed: " + e.getMessage());
            System.exit(1);
        }

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar matterhorn-test-harness-<version>-jar-with-dependencies.jar>", options);
            System.exit(0);
        }

        if (line.hasOption("source")) {
            String sourceDirName = line.getOptionValue("source");
            sourceDir = new File(sourceDirName);
        }
        else {
            System.out.println("missing required parameter -s or --source");
            System.exit(1);
        }

        if (line.hasOption("dest")) {
            String destDirName = line.getOptionValue("dest");
            destDir = new File(destDirName);
        }
        else {
            System.out.println("missing required parameter -d or --dest");
            System.exit(1);
        }

        if (line.hasOption("number")) {
            String numCopiesStr = line.getOptionValue("number");
            numCopies = Integer.valueOf(numCopiesStr);
        }
        else {
            System.out.println("missing required parameter -n or --number");
            System.exit(1);
        }
        
        if (line.hasOption("message")) {
            String message = line.getOptionValue("message");
            System.out.println(message);
        }

        File[] sourceFiles = sourceDir.listFiles();
        numCopies = sourceFiles.length < numCopies ? sourceFiles.length : numCopies;
        executor = Executors.newFixedThreadPool(numCopies);
        Runnable copyTask;
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(numCopies);
        for (int i = 0; i < numCopies; i++) {
            final File inputFile = sourceFiles[i];
            final File outputFile = new File(destDir, inputFile.getName());
            copyTask = new Runnable() {
                final int BUFF_SIZE = 1000000;
                final byte[] buffer = new byte[BUFF_SIZE];

                public void run() {
                    try {
                        startGate.await();
                        InputStream in = null;
                        OutputStream out = null;
                        int counter = 0;
                        try {
                            String threadName = Thread.currentThread().getName().trim();
                            char lastThreadNameChar = threadName.charAt(threadName.length() - 1);
                            System.out.println("copying " + inputFile.getCanonicalPath() + " of size " + inputFile.length()/1000000 + " MB to " + outputFile.getCanonicalPath() + " in thread "
                                    + threadName);
                            in = new FileInputStream(inputFile);
                            out = new FileOutputStream(outputFile);
                            while (true) {
                                int amountRead = in.read(buffer);
                                if (amountRead == -1) {
                                    break;
                                }
                                out.write(buffer, 0, amountRead);
                                counter++;
                                if ((counter % 10) == 0) System.out.print(lastThreadNameChar);
                            }
                        }
                        finally {
                            if (in != null) {
                                in.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        }
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finally {
                        endGate.countDown();
                        System.out.println("\nFile copy completed in thread " + Thread.currentThread().getName());
                    }
                }
            };
            executor.execute(copyTask);
        }
        long start = System.nanoTime();
        startGate.countDown();
        try {
            endGate.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        long end = System.nanoTime();
        System.out.println("Copied " + numCopies + " files in " + (float) (end - start) / 1000000000 + " seconds");
        System.exit(0);
    }

}
