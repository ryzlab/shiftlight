package se.ryz.shiftlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import com.fazecast.jSerialComm.SerialPort;

public class Shiftlight {
    private static Animation animation;
    private static AnimationPanel animationPanel;
    private static JButton saveButton;
    private static SerialPortComboBox serialPortComboBox;

    public static void main(String[] args) {
        // Initialize the animation model
        animation = new Animation();
        
        // Create and show the GUI
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Shiftlight");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create the animation panel
        animationPanel = new AnimationPanel(animation);
        frame.add(animationPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Add serial port combo box
        JLabel serialPortLabel = new JLabel("Serial Port:");
        serialPortComboBox = new SerialPortComboBox();
        buttonPanel.add(serialPortLabel);
        buttonPanel.add(serialPortComboBox);
        
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFromFile();
            }
        });
        
        saveButton = new JButton("Save");
        saveButton.setToolTipText("All rows must have valid CSV before saving");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        
        // Listen to validity changes from animation panel
        animationPanel.addPropertyChangeListener("allRowsValid", evt -> {
            boolean allValid = (Boolean) evt.getNewValue();
            saveButton.setEnabled(allValid);
        });
        
        // Set initial state (after panel is fully initialized)
        SwingUtilities.invokeLater(() -> {
            saveButton.setEnabled(animationPanel.areAllRowsValid());
        });
        
        JButton programButton = new JButton("Program");
        programButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                programToShiftlight(frame);
            }
        });
        
        JButton testerButton = new JButton("Test");
        TestDialog testerDialog = new TestDialog(frame, serialPortComboBox);
        testerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testerDialog.setVisible(true);
            }
        });
        
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(programButton);
        buttonPanel.add(testerButton);
        
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Set frame properties
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Animation");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            handleSaveFile(selectedFile);
        }
    }

    private static void handleSaveFile(File file) {
        try {
            // Get variables and CSV lines from animation panel
            String variablesText = animationPanel.getVariablesText();
            List<String> csvLines = animationPanel.getAllCsvLines();
            
            // Write to file
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Write variables section
                writer.println("# Variables");
                if (variablesText != null && !variablesText.trim().isEmpty()) {
                    writer.println(variablesText);
                }
                writer.println();
                
                // Write CSV lines section
                writer.println("# Animation CSV Lines");
                for (String csvLine : csvLines) {
                    writer.println(csvLine);
                }
            }
            
            JOptionPane.showMessageDialog(null, "File saved successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving file: " + e.getMessage(), 
                "Save Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Animation");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            handleLoadFile(selectedFile);
        }
    }

    private static void handleLoadFile(File file) {
        try {
            List<String> allLines = Files.readAllLines(file.toPath());
            List<String> variables = new ArrayList<>();
            List<String> csvLines = new ArrayList<>();
            
            boolean inVariablesSection = false;
            boolean inCsvSection = false;
            
            for (String line : allLines) {
                String trimmedLine = line.trim();
                
                // Handle section markers
                if (trimmedLine.equals("# Variables")) {
                    inVariablesSection = true;
                    inCsvSection = false;
                    continue;
                } else if (trimmedLine.equals("# Animation CSV Lines")) {
                    inVariablesSection = false;
                    inCsvSection = true;
                    continue;
                }
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }
                
                // Add to appropriate section
                if (inVariablesSection || (!inCsvSection && csvLines.isEmpty())) {
                    // If we haven't seen the CSV section marker yet, assume it's a variable
                    variables.add(trimmedLine);
                } else if (inCsvSection || (!inVariablesSection && !variables.isEmpty())) {
                    // If we've seen variables or the CSV marker, it's a CSV line
                    csvLines.add(trimmedLine);
                }
            }
            
            // If no section markers were found, try to detect: variables are lines with "=", CSV lines are the rest
            if (variables.isEmpty() && csvLines.isEmpty()) {
                variables.clear();
                csvLines.clear();
                for (String line : allLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                        continue;
                    }
                    if (trimmedLine.contains("=") && !trimmedLine.startsWith("[")) {
                        variables.add(trimmedLine);
                    } else if (trimmedLine.startsWith("[")) {
                        csvLines.add(trimmedLine);
                    }
                }
            }
            
            // Filter out any empty strings that might have been added
            variables.removeIf(s -> s == null || s.trim().isEmpty());
            csvLines.removeIf(s -> s == null || s.trim().isEmpty());
            
            // Load into animation panel (this will replace all current rows)
            animationPanel.loadFromFile(variables, csvLines);
            
            JOptionPane.showMessageDialog(null, "File loaded successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error loading file: " + e.getMessage(), 
                "Load Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void programToShiftlight(JFrame parentFrame) {
        String selectedPort = serialPortComboBox.getSelectedPortName();
        if (selectedPort == null || selectedPort.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, 
                "Please select a serial port", 
                "No Port Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("Selected Serial Port: " + selectedPort);
        
        SerialPort port = SerialPort.getCommPort(selectedPort);
        if (port == null) {
            JOptionPane.showMessageDialog(parentFrame, 
                "Serial port not found: " + selectedPort, 
                "Port Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Open port and wait for READY
            port = openPortAndWaitForReady(port, parentFrame);
            if (port == null) {
                // Error already shown in openPortAndWaitForReady
                return;
            }
            
            // Get program output
            String programOutput = animationPanel.generateProgramOutput();
            System.out.println(programOutput);

            // Send HELLO and wait for OK
            System.out.println("Sending HELLO...");
            port.getOutputStream().write("HELLO\n".getBytes());
            port.getOutputStream().flush();

            // Read lines until "OK" appears, ignoring lines starting with "#"
            String response = readUntilOK(port, 2000); // 2 second timeout
            if (response == null || !response.trim().equals("OK")) {
                port.closePort();
                String errorMsg = response == null ? "(no response)" : response;
                JOptionPane.showMessageDialog(parentFrame, 
                    "No shiftlight connected to port " + selectedPort + ".\n" +
                    "Expected 'OK' but received: " + errorMsg, 
                    "Connection Failed", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            System.out.println("Received OK, shiftlight connected");

            // Send program output line by line
            String[] lines = programOutput.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                System.out.println("Sending: " + line);
                port.getOutputStream().write((line + "\n").getBytes());
                port.getOutputStream().flush();

                // Wait for OK response
                response = readResponse(port, 2000); // 2 second timeout
                if (!response.trim().equals("OK")) {
                    port.closePort();
                    JOptionPane.showMessageDialog(parentFrame, 
                        "Programming failed: No response from shiftlight.\n" +
                        "Last line sent: " + line + "\n" +
                        "Expected 'OK' but received: " + (response.isEmpty() ? "(no response)" : response), 
                        "Programming Failed", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.out.println("Received OK for line: " + line);
            }

            port.closePort();
            JOptionPane.showMessageDialog(parentFrame, 
                "Programming completed successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            if (port.isOpen()) {
                port.closePort();
            }
            JOptionPane.showMessageDialog(parentFrame, 
                "Error during programming: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static String readResponse(SerialPort port, int timeoutMs) {
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (port.bytesAvailable() > 0) {
                    byte[] buffer = new byte[port.bytesAvailable()];
                    int bytesRead = port.readBytes(buffer, buffer.length);
                    if (bytesRead > 0) {
                        response.append(new String(buffer, 0, bytesRead));
                        // Check if we have a complete line (ends with newline)
                        if (response.toString().contains("\n")) {
                            break;
                        }
                    }
                }
                Thread.sleep(10); // Small delay to avoid busy waiting
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return response.toString();
    }
    
    /**
     * Opens the serial port and waits for "READY" message, ignoring comment lines starting with "#".
     * Returns the port if "READY" is received within 2 seconds, or null if timeout/error occurs.
     */
    static SerialPort openPortAndWaitForReady(SerialPort port, JFrame parentFrame) {
        // Set baud rate
        port.setBaudRate(9600);
        
        // Open the port (this will cause Arduino to reset)
        if (!port.openPort()) {
            JOptionPane.showMessageDialog(parentFrame, 
                "Failed to open serial port: " + port.getSystemPortName(), 
                "Port Error", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        // Give the port time to initialize after reset
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            port.closePort();
            return null;
        }
        
        // Read lines until "READY" appears, ignoring lines starting with "#"
        String result = readUntilReady(port, 2000); // 2 second timeout
        if (result == null || !result.equals("READY")) {
            port.closePort();
            String errorMsg = result == null ? "(no response)" : result;
            JOptionPane.showMessageDialog(parentFrame, 
                "Device did not become ready on port " + port.getSystemPortName() + ".\n" +
                "Expected 'READY' but received: " + errorMsg, 
                "Connection Failed", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        System.out.println("Device is READY");
        return port;
    }
    
    /**
     * Reads lines from the serial port until "READY" appears, ignoring lines starting with "#".
     * Returns "READY" if found, or null if timeout occurs without finding "READY".
     */
    private static String readUntilReady(SerialPort port, int timeoutMs) {
        StringBuilder allLines = new StringBuilder();
        long startTime = System.currentTimeMillis();
        String currentLine = "";
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (port.bytesAvailable() > 0) {
                    byte[] buffer = new byte[port.bytesAvailable()];
                    int bytesRead = port.readBytes(buffer, buffer.length);
                    if (bytesRead > 0) {
                        String newData = new String(buffer, 0, bytesRead);
                        currentLine += newData;
                        
                        // Process complete lines
                        while (currentLine.contains("\n")) {
                            int newlineIndex = currentLine.indexOf("\n");
                            String line = currentLine.substring(0, newlineIndex).trim();
                            currentLine = currentLine.substring(newlineIndex + 1);
                            
                            // Ignore empty lines and lines starting with "#"
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                // Check if this line is "READY"
                                if (line.equals("READY")) {
                                    return "READY";
                                }
                                // Store non-comment lines for error reporting
                                if (allLines.length() > 0) {
                                    allLines.append("\n");
                                }
                                allLines.append(line);
                            }
                        }
                    }
                }
                Thread.sleep(10); // Small delay to avoid busy waiting
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Timeout occurred, return what we received (or null if nothing)
        return allLines.length() > 0 ? allLines.toString() : null;
    }
    
    /**
     * Reads lines from the serial port until "OK" appears, ignoring lines starting with "#".
     * Returns "OK" if found, or null if timeout occurs without finding "OK".
     */
    private static String readUntilOK(SerialPort port, int timeoutMs) {
        StringBuilder allLines = new StringBuilder();
        long startTime = System.currentTimeMillis();
        String currentLine = "";
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (port.bytesAvailable() > 0) {
                    byte[] buffer = new byte[port.bytesAvailable()];
                    int bytesRead = port.readBytes(buffer, buffer.length);
                    if (bytesRead > 0) {
                        String newData = new String(buffer, 0, bytesRead);
                        currentLine += newData;
                        
                        // Process complete lines
                        while (currentLine.contains("\n")) {
                            int newlineIndex = currentLine.indexOf("\n");
                            String line = currentLine.substring(0, newlineIndex).trim();
                            currentLine = currentLine.substring(newlineIndex + 1);
                            
                            // Ignore empty lines and lines starting with "#"
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                // Check if this line is "OK"
                                if (line.equals("OK")) {
                                    return "OK";
                                }
                                // Store non-comment lines for error reporting
                                if (allLines.length() > 0) {
                                    allLines.append("\n");
                                }
                                allLines.append(line);
                            }
                        }
                    }
                }
                Thread.sleep(10); // Small delay to avoid busy waiting
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Timeout occurred, return what we received (or null if nothing)
        return allLines.length() > 0 ? allLines.toString() : null;
    }
}

