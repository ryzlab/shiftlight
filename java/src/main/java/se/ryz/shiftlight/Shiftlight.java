package se.ryz.shiftlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class Shiftlight {
    private static Animation animation;
    private static AnimationPanel animationPanel;

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
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        
        JButton programButton = new JButton("Program");
        programButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Implement program functionality
                JOptionPane.showMessageDialog(frame, "Program functionality not yet implemented");
            }
        });
        
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
        // TODO: Implement file saving logic
        System.out.println("Save to file: " + file.getAbsolutePath());
        JOptionPane.showMessageDialog(null, "Saving to: " + file.getAbsolutePath() + "\n(Implementation pending)");
    }
}

