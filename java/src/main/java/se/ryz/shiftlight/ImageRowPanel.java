package se.ryz.shiftlight;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class ImageRowPanel extends JPanel {
    private JTextField csvTextField;
    private JButton startColorButton;
    private JButton endColorButton;
    private Color startColor;
    private Color endColor;
    private Image currentImage;
    private Runnable onRemoveCallback;
    private Runnable onValidityChangedCallback;

    public ImageRowPanel() {
        this.startColor = Color.BLACK;
        this.endColor = Color.BLACK;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Prevent vertical stretching
        setAlignmentX(Component.LEFT_ALIGNMENT); // Align to left in BoxLayout

        // CSV text field on the left
        csvTextField = new JTextField();
        csvTextField.setPreferredSize(new Dimension(400, 30));
        csvTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        csvTextField.setAlignmentY(Component.CENTER_ALIGNMENT);
        csvTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        csvTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateColorsFromCsv();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateColorsFromCsv();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateColorsFromCsv();
            }
        });

        // Color picker buttons on the right
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));
        colorPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        startColorButton = new JButton("Start RGB");
        startColorButton.setPreferredSize(new Dimension(100, 30));
        startColorButton.setMaximumSize(new Dimension(100, 30));
        startColorButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        startColorButton.setBackground(startColor);
        startColorButton.setOpaque(true);
        startColorButton.setBorderPainted(true);
        startColorButton.addActionListener(e -> pickStartColor());

        endColorButton = new JButton("End RGB");
        endColorButton.setPreferredSize(new Dimension(100, 30));
        endColorButton.setMaximumSize(new Dimension(100, 30));
        endColorButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        endColorButton.setBackground(endColor);
        endColorButton.setOpaque(true);
        endColorButton.setBorderPainted(true);
        endColorButton.addActionListener(e -> pickEndColor());

        // Arrow buttons to copy colors
        JButton leftArrowButton = new JButton("←");
        leftArrowButton.setPreferredSize(new Dimension(30, 30));
        leftArrowButton.setMaximumSize(new Dimension(30, 30));
        leftArrowButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        leftArrowButton.setToolTipText("Copy End RGB to Start RGB");
        leftArrowButton.addActionListener(e -> copyEndToStart());

        JButton rightArrowButton = new JButton("→");
        rightArrowButton.setPreferredSize(new Dimension(30, 30));
        rightArrowButton.setMaximumSize(new Dimension(30, 30));
        rightArrowButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        rightArrowButton.setToolTipText("Copy Start RGB to End RGB");
        rightArrowButton.addActionListener(e -> copyStartToEnd());

        JButton removeButton = new JButton("Remove");
        removeButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        removeButton.addActionListener(e -> {
            if (onRemoveCallback != null) {
                onRemoveCallback.run();
            }
        });

        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(startColorButton);
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(leftArrowButton);
        colorPanel.add(Box.createHorizontalStrut(2));
        colorPanel.add(rightArrowButton);
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(endColorButton);
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(removeButton);

        add(csvTextField, BorderLayout.CENTER);
        add(colorPanel, BorderLayout.EAST);
    }

    private void updateColorsFromCsv() {
        String csvLine = csvTextField.getText().trim();
        if (csvLine.isEmpty()) {
            currentImage = null;
            // Clear error indication for empty CSV
            csvTextField.setToolTipText(null);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
            return;
        }

        try {
            Image image = new Image(csvLine);
            this.currentImage = image;
            
            // Update colors from the image
            startColor = new Color(
                Math.max(0, Math.min(255, image.getStartRed())),
                Math.max(0, Math.min(255, image.getStartGreen())),
                Math.max(0, Math.min(255, image.getStartBlue()))
            );
            endColor = new Color(
                Math.max(0, Math.min(255, image.getEndRed())),
                Math.max(0, Math.min(255, image.getEndGreen())),
                Math.max(0, Math.min(255, image.getEndBlue()))
            );

            startColorButton.setBackground(startColor);
            endColorButton.setBackground(endColor);
            // Clear any error indication on successful parse
            csvTextField.setToolTipText(null);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
        } catch (IllegalArgumentException e) {
            // Invalid CSV, print error message
            System.err.println("Invalid CSV: " + csvLine);
            System.err.println("Error: " + e.getMessage());
            // Visual feedback: set tooltip and red border
            csvTextField.setToolTipText("Invalid CSV: " + e.getMessage());
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
        }
    }

    private void pickStartColor() {
        Color newColor = JColorChooser.showDialog(this, "Pick Start RGB Color", startColor);
        if (newColor != null) {
            startColor = newColor;
            startColorButton.setBackground(startColor);
            updateCsvFromColors();
        }
    }

    private void pickEndColor() {
        Color newColor = JColorChooser.showDialog(this, "Pick End RGB Color", endColor);
        if (newColor != null) {
            endColor = newColor;
            endColorButton.setBackground(endColor);
            updateCsvFromColors();
        }
    }

    private void copyStartToEnd() {
        endColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue());
        endColorButton.setBackground(endColor);
        updateCsvFromColors();
    }

    private void copyEndToStart() {
        startColor = new Color(endColor.getRed(), endColor.getGreen(), endColor.getBlue());
        startColorButton.setBackground(startColor);
        updateCsvFromColors();
    }

    private void updateCsvFromColors() {
        if (currentImage != null) {
            try {
                // Get the current CSV line
                String csvLine = currentImage.toCsvLine();
                String trimmed = csvLine.trim();
                
                // Find the bracket part (first value)
                int bracketStart = trimmed.indexOf('[');
                int bracketEnd = trimmed.indexOf(']');
                
                if (bracketStart != 0 || bracketEnd == -1) {
                    return; // Invalid format, can't update
                }
                
                // Extract the bracket part (including brackets)
                String ledPart = trimmed.substring(bracketStart, bracketEnd + 1);
                
                // Extract the rest after the closing bracket and comma
                String rest = trimmed.substring(bracketEnd + 1).trim();
                if (!rest.startsWith(",")) {
                    return; // Missing comma after bracket
                }
                rest = rest.substring(1); // Remove the comma
                
                // Split the remaining part by commas (these are the actual separators)
                String[] parts = rest.split(",");
                
                if (parts.length != 9) {
                    return; // Invalid number of parts
                }
                
                // Update RGB values in the CSV (indices are now correct)
                parts[2] = String.valueOf(startColor.getRed());   // startRed
                parts[3] = String.valueOf(startColor.getGreen()); // startGreen
                parts[4] = String.valueOf(startColor.getBlue()); // startBlue
                parts[5] = String.valueOf(endColor.getRed());     // endRed
                parts[6] = String.valueOf(endColor.getGreen());   // endGreen
                parts[7] = String.valueOf(endColor.getBlue());    // endBlue
                
                // Reconstruct the CSV line
                String updatedCsv = ledPart + "," + String.join(",", parts);
                csvTextField.setText(updatedCsv);
                
                // Update current image
                currentImage = new Image(updatedCsv);
            } catch (Exception e) {
                // Failed to update, ignore
            }
        }
    }

    public String getCsvLine() {
        return csvTextField.getText().trim();
    }

    public void setCsvLine(String csvLine) {
        csvTextField.setText(csvLine);
        updateColorsFromCsv();
    }

    public void setOnRemoveCallback(Runnable callback) {
        this.onRemoveCallback = callback;
    }

    public Image getImage() {
        return currentImage;
    }

    public void addCsvDocumentListener(javax.swing.event.DocumentListener listener) {
        csvTextField.getDocument().addDocumentListener(listener);
    }

    public boolean isCsvValid() {
        String csvLine = csvTextField.getText().trim();
        if (csvLine.isEmpty()) {
            return true; // Empty is considered valid (not invalid)
        }
        try {
            new Image(csvLine);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void setOnValidityChangedCallback(Runnable callback) {
        this.onValidityChangedCallback = callback;
    }

    private void notifyValidityChanged() {
        if (onValidityChangedCallback != null) {
            onValidityChangedCallback.run();
        }
    }
}
