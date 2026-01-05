package se.ryz.shiftlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Shiftlight {
    private static Animation animation;
    private static AnimationPanel animationPanel;
    private static JButton saveButton;

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
                String programOutput = animationPanel.generateProgramOutput();
                System.out.println(programOutput);
            }
        });
        
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(programButton);
        
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
}

