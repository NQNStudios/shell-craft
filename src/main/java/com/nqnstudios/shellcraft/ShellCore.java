package com.nqnstudios.shellcraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class ShellCore {
    public static void main(String[] args) {
        // Create a ShellCore and debug it in a repl
        ShellCore core = new ShellCore();

        Scanner sc = new Scanner(System.in);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            core.process(line);
            core.dumpOutput();
        }
    }

    public void process(String message) {
        output(message);
    }

    public void tick() {

    }

    private void output(String message) {
        _output += message + "\n";
    }

    private String _output = "";
    public String takeOutput() {
        String temp = _output;
        _output = "";
        return temp;
    }
    public void dumpOutput() {
        if (_output.length() > 0)
            System.out.println(_output);
        _output = "";
    }

}