package com.nqnstudios.shellcraft;

import java.io.*;
import java.util.Scanner;
import org.apache.logging.log4j.Logger;

public class ShellCore {

    private class OutputThread extends Thread {
        ShellCore core;
        public OutputThread(ShellCore core) {
            this.core = core;
        }

        public void run() {
            while (true) {
                try {
                    core.dumpOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean debug = false;
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a ShellCore and debug it in a repl
        ShellCore core = new ShellCore();
        core.debug = true;

        Scanner sc = new Scanner(System.in);

        System.out.println("Testing ShellCore. Enter commands:");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            core.process(line);
        }
    }

    private Process currentProcess;
    private ProcessBuilder builder;
    private boolean isWindows;

    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;
    private OutputThread outputThread;

    private Logger logger = null;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    private void log(String message) {
        if (logger != null) {
            logger.debug(message);
        }
        System.out.println(message);
    }

    public ShellCore() throws IOException {
        isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");

        builder = new ProcessBuilder();

        String homeDir = System.getenv("HOME");
        if (homeDir == null) {
            homeDir = System.getProperty("user.home");
        }
        builder.directory(new File(homeDir));
    }

    public void process(String message) throws IOException {
        if (currentProcess != null && currentProcess.isAlive()) {
            processInput.write(message + "\n");
            processInput.flush();
        } else {
            if (isWindows) {
                builder.command("cmd.exe", "/c", message);
            } else {
                builder.command("/bin/bash", "-c", message);
            }

            log("Starting " + message);
            currentProcess = builder.start();

            OutputStream stdin = currentProcess.getOutputStream();
            InputStream stdout = currentProcess.getInputStream();
            InputStream stderr = currentProcess.getErrorStream();

            processInput = new BufferedWriter(new OutputStreamWriter(stdin));
            processOutput = new BufferedReader(new InputStreamReader(stdout));
            processError = new BufferedReader(new InputStreamReader(stderr));

            if (outputThread == null && debug) {
                outputThread = new OutputThread(this);
                outputThread.start();
            }
        }
    }

    public void tick() {

    }

    public String takeOutput() throws IOException {
        String output = "";
        String error = "";

        if (currentProcess != null) {
            int c = -1;
            while (processOutput.ready() && (c = processOutput.read()) != -1) {
                output += (char)c;
            }
            while (processError.ready() && (c = processError.read()) != -1) {
                output += (char)c;
            }
            if (output == null) output = "";
            if (error == null) error = "";

            if (!currentProcess.isAlive()) {
                log("Current process died with exit code" + currentProcess.exitValue());
                currentProcess = null;
            }
        }

        return output + error;
    }

    public void dumpOutput() throws IOException {
        String output = takeOutput();
        if (output.length() > 0)
            System.out.println(output);
    }

}