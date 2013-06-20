/*
 * Copyright (C) 2013 Joshua Michael Hertlein <jmhertlein@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jmhertlein.core.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import net.jmhertlein.core.mail.GoogleMail;

/**
 * The BugReportDaemon class is the main class for the BRD project. This class
 * handles the CLI front-end to the daemon.
 *
 * @author joshua
 */
public class BugReportDaemon {

    private static ConnectionListenTask connectionListener;
    private static LinkedHashSet<BugReport> reports;
    private static boolean done, running;
    private static Scanner scan;
    private static Thread th;
    private static String emailDestination, emailSenderName, emailSenderPassword;

    public static void main(String[] args) {
        setupShutdownHook();
        reports = new LinkedHashSet<>();
        running = false;
        done = false;
        scan = new Scanner(System.in);

        try {
            File f = new File("./brd.config");

            Scanner fileScan = new Scanner(f);
            if (fileScan.hasNextLine()) {
                emailDestination = fileScan.nextLine().trim();
            }
            if (fileScan.hasNextLine()) {
                emailSenderName = fileScan.nextLine().trim();
            }
            if (fileScan.hasNextLine()) {
                emailSenderPassword = fileScan.nextLine().trim();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BugReportDaemon.class.getName()).log(Level.SEVERE, null, ex);
        }



        start();

        printMenu();

        while (!done) {
            String resp = scan.nextLine().trim();

            switch (resp) {
                default:
                    System.out.println("Invalid command");
                    printMenu();
                    break;
                case "stop":
                    stop();
                    break;
                case "start":
                    start();
                    break;
                case "dump":
                    dumpReportsToFile(reports);
                    break;
                case "exit":
                    quit();
                    break;
                case "help":
                    printMenu();
                    break;

                case "info":
                    System.out.println("Currently holding " + reports.size() + " reports in volatile memory.");
                    break;

                case "clear":
                    reports.clear();
                    System.out.println("Deleted reports.");
                    break;

                case "print":
                    print();
                    break;

                case "testmail":
                    sendTestEmail();
                    System.out.println("Sent test email.");
                    break;
            }
        }


    }

    private static void printMenu() {
        System.out.println("Options are:");
        System.out.println("dump - dumps all reports to file");
        System.out.println("stop - stop listening on port");
        System.out.println("exit - quit program");
        System.out.println("start - start listening on port");
        System.out.println("info - prints info about current state");
        System.out.println("clear - delete currently held reports");
        System.out.println("print - print all currently held reports");
        System.out.println("testmail - send a test email");
    }

    private static void dumpReportsToFile(Set<BugReport> reports) {
        File f = new File("./bug_reports0");
        int c = 0;
        while (f.exists()) {
            c++;
            f = new File("./bug_reports" + c);
        }

        try {
            f.createNewFile();
        } catch (IOException ex) {
            System.err.println("Error making file: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        try (PrintStream ps = new PrintStream(f)) {
            for (BugReport report : reports) {
                ps.println(report.toString());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BugReportDaemon.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        System.out.println("Reports dumped.");
    }

    /**
     * Stops the daemon from listening on the network port. Does NOT cause the
     * daemon to exit- it will continue to respond to stdin
     */
    public static void stop() {
        if (!running) {
            System.out.println("Already stopped.");
            return;
        }
        connectionListener.stop();
        connectionListener = null;
        System.out.println("Waiting for connection thread to join...");
        try {
            th.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(BugReportDaemon.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Joined.");

        th = null;
        running = false;
        System.out.println("Stopped.");
    }

    /**
     * Prints all reports to stdout
     */
    public static void print() {
        System.out.println("Printing reports...");
        for (BugReport report : reports) {
            System.out.println(report.toString());
        }
    }

    /**
     * Starts the daemon listening on the network port.
     */
    public static void start() {
        if (running) {
            System.out.println("Already listening.");
            return;
        }
        connectionListener = new ConnectionListenTask(reports);
        th = new Thread(connectionListener);
        th.start();
        running = true;
        System.out.println("Started.");
    }

    /**
     * Causes the daemon to exit. The CLI will stop responding. All threads will
     * be requested to terminate
     */
    public static void quit() {
        System.out.println("Quitting...");
        stop();
        done = true;
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (running) {
                    quit();
                }
                if (reports.size() > 0) {
                    dumpReportsToFile(reports);
                }
            }
        }));
    }

    public static String getEmailDestination() {
        return emailDestination;
    }

    public static String getEmailSenderName() {
        return emailSenderName;
    }

    public static String getEmailSenderPassword() {
        return emailSenderPassword;
    }

    private static void sendTestEmail() {
        if (!canSendEmail()) {
            System.out.println("Error: Email information not set in brd.config");
            return;
        }
        try {
            GoogleMail.send(BugReportDaemon.getEmailSenderName(), BugReportDaemon.getEmailSenderPassword(), BugReportDaemon.getEmailDestination(), "Test Message from JBRD", "This is a test message.");
        } catch (AddressException ex) {
            Logger.getLogger(BugReportDaemon.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessagingException ex) {
            Logger.getLogger(BugReportDaemon.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean canSendEmail() {
        return emailDestination != null && emailSenderName != null && emailSenderPassword != null;
    }
}