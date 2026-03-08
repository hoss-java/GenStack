package com.GenStack.interfaces;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

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

public class SshClientInterface extends BaseInterface {
    private SshClient sshClient;
    private ClientSession session;
    private ChannelShell channel;

    private List<User> users = new ArrayList<>();
    private String configFile = "config/sshinterface.properties";
    private String serviceAddress = "172.32.0.11";
    private int servicePort = 32722;

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

    public SshClientInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        super(callback, out, in, appDataFolder);
        loadProperties(appDataFolder);
    }
    public SshClientInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public SshClientInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public SshClientInterface(ResponseCallbackInterface callback) {
        this(callback, System.out, System.in, null);
    }

    @Override
    public JSONObject executeCommands(JSONObject commands) {
        if (users.isEmpty()) {
            System.out.println("No users available. Cannot connect to SSH.");
            return new JSONObject().put("SshClientInterface", "failed");
        }

        this.sshClient = SshClient.setUpDefaultClient();
        try {
            // Connect and open interactive shell
            connectAndOpenShell();
            
            // Wait for user to type 'exit' and close the shell
            waitForShellClose();
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Disconnect when shell closes
            try {
                disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new JSONObject().put("SshClientInterface", "Exit");
    }

    /**
     * Connect to the SSH server and open an interactive shell
     */
    public void connectAndOpenShell() throws IOException {
        // Get the first user
        User firstUser = users.get(0);

        sshClient.start();
        
        // Connect to server
        session = sshClient.connect(firstUser.username, serviceAddress, servicePort)
            .verify(10, TimeUnit.SECONDS)
            .getSession();
        
        // Authenticate
        session.addPasswordIdentity(firstUser.password);
        session.auth()
            .verify(10, TimeUnit.SECONDS);
        
        System.out.println("Connected to " + serviceAddress + ":" + servicePort);
        System.out.println("Type 'exit' to disconnect\n");
        
        // Open shell channel
        channel = session.createShellChannel();
        channel.setIn(System.in);
        channel.setOut(System.out);
        channel.setErr(System.err);
        
        // Open the channel and interact
        channel.open().verify(5, TimeUnit.SECONDS);
    }
    
    /**
     * Wait for the shell to close (when user types exit)
     */
    public void waitForShellClose() throws IOException {
        // Wait until the channel is closed
        while (channel.isOpen()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Handle or restore interrupt status
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Disconnect from the SSH server
     */
    public void disconnect() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (session != null && session.isOpen()) {
            session.close();
        }
        if (sshClient != null && sshClient.isOpen()) {
            sshClient.stop();
        }
        System.out.println("\nDisconnected from " + serviceAddress);
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
