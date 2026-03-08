package com.GenStack.interfaces;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.common.io.IoServiceFactoryFactory;
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

import com.GenStack.interfaces.BaseInterface;
import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;
import com.GenStack.helper.IPAddressHelper;
import com.GenStack.interfaces.ssh.CustomCommandFactory;

public class SshdInterface extends BaseInterface {
    private final Scanner scanner;
    private SshServer sshd;
    private List<User> users = new ArrayList<>();
    private String configFile = "config/sshinterface.properties";
    private String serviceAddress = "172.32.0.11";
    private int servicePort = 32722;
    private JSONObject commands;

    private void loadProperties(String appDataFolder) {
        Properties props = new Properties();
        try (InputStream input = getConfigInputStream(appDataFolder, configFile)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + configFile);
                return;
            }

            // Load properties file
            props.load(input);
            this.servicePort = Integer.parseInt(props.getProperty("ssh.port"));
            this.serviceAddress = IPAddressHelper.getLocalIPAddress();

            for (int i = 1; ; i++) {
                String username = props.getProperty("sshuser" + i + ".username");
                String password = props.getProperty("sshuser" + i + ".password");
                if (username == null || password == null) {
                    break;
                }
                users.add(new User(username, password));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SshdInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        super(callback, out, in, appDataFolder);
        this.scanner = new Scanner(System.in);
        loadProperties(appDataFolder);
        printServiceDetails();
    }
    public SshdInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public SshdInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public SshdInterface(ResponseCallbackInterface callback) {
        this(callback, System.out, System.in, null);
    }

    private void printServiceDetails() {
        // Print the connection details directly
        System.out.println("SSHService Details:");
        System.out.println(" SSHService Address: " + serviceAddress);
        System.out.println(" SSHService Port: " + servicePort);
    }

    @Override
    public JSONObject executeCommands(JSONObject commands) {
        this.commands = commands;
        sshCreateServer(servicePort);
        try {
            start(); // Start the server upon instantiation

            try {
                // Keep the service running while the running flag is true
                while (getRunningFlag()) {
                    Thread.sleep(100); // Short sleep to yield control
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Service thread was interrupted.");
            } finally {
                shutDown(); // Call shutdown method before exiting
            }
            return new JSONObject().put("SshInterface", "stopped");
        } catch (IOException e) {
            System.out.println("Server error."+e);
            return new JSONObject().put("SshdInterface", "failed");
        }
    }

    public void sshCreateServer(int port) {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        sshd.setPasswordAuthenticator((user, pwd, session) -> 
                users.stream().anyMatch(u -> u.username.equals(user) && u.password.equals(pwd))
        );

        sshd.setIoServiceFactoryFactory(new Nio2ServiceFactoryFactory());

        CustomCommandFactory customCommandFactory = new CustomCommandFactory();
        customCommandFactory.setConsoleSettings(callback,commands);
        sshd.setCommandFactory(customCommandFactory);

        sshd.setShellFactory(new org.apache.sshd.server.shell.ShellFactory() {
            @Override
            public Command createShell(ChannelSession channel) throws IOException {
                return customCommandFactory.createCommand(channel, "");
            }
        });
    }

    public void start() throws IOException {
        if (sshd != null) {
            sshd.start();
            System.out.println("SSH Server started on port: " + sshd.getPort());
        }
    }

    public void shutDown() {
        if (sshd != null) {
            try {
                sshd.stop();
                System.out.println("SSH Server stopped.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Example User class to store user data
    private static class User {
        String username;
        String password;

        User(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
