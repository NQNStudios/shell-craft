package com.nqnstudios.shellcraft;

import java.io.*;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

enum ProcessResultType {
    NotProcessed,
    NoShell,
    NoOutput,
    Output
}

class ProcessResult {
    public ProcessResultType type;
    public String output;

    public ProcessResult(ProcessResultType type, String output) {
        this.type = type;
        this.output = output;
    }
}

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
    public static void main(String[] args) {
        // Create a ShellCore and debug it in a repl
        ShellCore core = new ShellCore();
        core.debug = true;

        core.start("cmd.exe");
        Scanner sc = new Scanner(System.in);

        System.out.println("Testing ShellCore. Enter commands:");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            ProcessResult result = null;
            try {
                result = core.process(line);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            switch (result.type) {
                case NoShell:
                    // Shouldn't happen
                    break;
                case NoOutput:
                    break;
                case Output:
                    System.out.println(result.output);
                    break;
                case NotProcessed:
                    System.out.println("Not a ShellCraft command");
                    break;
            }
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

    private String defaultShell;
    private String defaultOutputArg;

    public ShellCore() {
        isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");

        if (isWindows) {
            defaultShell = "cmd.exe";
            defaultOutputArg = "/c";
        } else {
            defaultShell = "/bin/bash";
            defaultOutputArg = "-c";
        }

        builder = new ProcessBuilder();

        String homeDir = System.getenv("HOME");
        if (homeDir == null) {
            homeDir = System.getProperty("user.home");
        }
        builder.environment().put("HOME", homeDir);
        builder.directory(new File(homeDir));
    }

    /**
     *
     * @param shellWithArgs
     * @return Whether this successfully started a process
     */
    public boolean start(String shellWithArgs) {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
        }

        if (shellWithArgs.equals("")) {
            shellWithArgs = defaultShell;
        }

        builder.command(shellWithArgs);
        try {
            currentProcess = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

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

        return true;
    }

    /**
     * Process a message the player tried to send
     * @param message
     * @return Null if the message was not for ShellCraft, otherwise, a string to send to chat from the player.
     * @throws IOException
     */
    public ProcessResult process(String message) throws IOException, InterruptedException {
        boolean commandForOutput = message.startsWith("!");
        boolean commandForResult = message.startsWith("?");
        if (commandForOutput || commandForResult) {
            message = message.substring(1); // Drop the ! or ?
            if (commandForResult) {
                if (currentProcess == null || !currentProcess.isAlive()) {
                    return new ProcessResult(ProcessResultType.NoShell, "");
                } else {
                    processInput.write(message + "\n");
                    processInput.flush();
                    return new ProcessResult(ProcessResultType.NoOutput, "");
                }
            } else {
                Process process = new ProcessBuilder().command(defaultShell, defaultOutputArg, message).start();
                process.waitFor();
                StringWriter writer = new StringWriter();
                IOUtils.copy(process.getInputStream(), writer, "UTF-8");
                String output = writer.toString();
                return new ProcessResult(ProcessResultType.Output, output);
            }
        }
        else {
           return new ProcessResult(ProcessResultType.NotProcessed, "");
        }
    }

    public void tick() {

    }

    public String takeOutput() throws IOException {
        StringBuilder output = new StringBuilder();

        if (currentProcess != null) {
            int c = -1;
            while (processOutput.ready() && (c = processOutput.read()) != -1) {
                output.append((char) c);
            }
            while (processError.ready() && (c = processError.read()) != -1) {
                output.append((char) c);
            }

            if (!currentProcess.isAlive()) {
                log("Current process died with exit code" + currentProcess.exitValue());
                currentProcess = null;
            }
        }

        return output.toString();
    }

    public void dumpOutput() throws IOException {
        String output = takeOutput();
        if (output.length() > 0)
            System.out.println(output);
    }

}