package com.nqnstudios.shellcraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ShellCore {

    private class OutputThread extends Thread {
        ShellCore core;
        public OutputThread(ShellCore core) {
            this.core = core;
        }

        public void run() {
            System.out.println("Starting thread");
            while (true) {
                try {
                    core.dumpOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a ShellCore and debug it in a repl
        ShellCore core = new ShellCore();

        Scanner sc = new Scanner(System.in);

        System.out.println("Testing ShellCore");
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

    public void process(String message) throws IOException, InterruptedException {
        if (currentProcess != null && currentProcess.isAlive()) {
            processInput.write(message + "\n");
            processInput.flush();
        } else {
            builder.command("cmd.exe", "/c", message + "\n");
            currentProcess = builder.start();

            OutputStream stdin = currentProcess.getOutputStream();
            InputStream stdout = currentProcess.getInputStream();
            InputStream stderr = currentProcess.getErrorStream();

            processInput = new BufferedWriter(new OutputStreamWriter(stdin));
            processOutput = new BufferedReader(new InputStreamReader(stdout));
            processError = new BufferedReader(new InputStreamReader(stderr));

            if (outputThread == null) {
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
            if (currentProcess.isAlive()) {
                int c = -1;
                while (processOutput.ready() && (c = processOutput.read()) != -1) {
                    output += (char)c;
                }
                while (processError.ready() && (c = processError.read()) != -1) {
                    output += (char)c;
                }
                if (output == null) output = "";
                if (error == null) error = "";
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