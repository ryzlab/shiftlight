package se.ryz.shiftlight;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
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
    private Runnable onDuplicateCallback;
    private Runnable onValidityChangedCallback;
    private java.util.function.Consumer<String> onTooltipChangedCallback;
    private VariableParser variableParser;

    public ImageRowPanel() {
        this.startColor = Color.BLACK;
        this.endColor = Color.BLACK;
        this.variableParser = null;
        initializeComponents();
    }

    public void setVariableParser(VariableParser variableParser) {
        this.variableParser = variableParser;
        // Re-validate CSV with new variables and update border/tooltip
        revalidateCsv();
    }

    public void revalidateCsv() {
        String csvLine = csvTextField.getText().trim();
        if (csvLine.isEmpty()) {
            csvTextField.setToolTipText(null);
            notifyTooltipChanged(null);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
            return;
        }

        try {
            // Try to parse with current variable parser
            Image image = variableParser != null ? new Image(csvLine, variableParser) : new Image(csvLine);
            this.currentImage = image;
            
            // Valid CSV - update colors and clear error indication
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
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            updateTooltip();
            notifyValidityChanged();
        } catch (IllegalArgumentException e) {
            // Invalid CSV - show error with field tooltip if available
            String fieldTooltip = getFieldTooltip();
            String errorTooltip;
            if (fieldTooltip != null) {
                errorTooltip = fieldTooltip + " | Invalid CSV: " + e.getMessage();
            } else {
                errorTooltip = "Invalid CSV: " + e.getMessage();
            }
            csvTextField.setToolTipText(errorTooltip);
            notifyTooltipChanged(errorTooltip);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
        }
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
                updateTooltipForCurrentField();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateColorsFromCsv();
                updateTooltipForCurrentField();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateColorsFromCsv();
                updateTooltipForCurrentField();
            }
        });
        
        csvTextField.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                updateTooltipForCurrentField();
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

        JButton duplicateButton = new JButton("Duplicate");
        duplicateButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        duplicateButton.addActionListener(e -> {
            if (onDuplicateCallback != null) {
                onDuplicateCallback.run();
            }
        });

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
        colorPanel.add(duplicateButton);
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
            notifyTooltipChanged(null);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
            return;
        }

        try {
            Image image = variableParser != null ? new Image(csvLine, variableParser) : new Image(csvLine);
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
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            // Update tooltip with evaluated CSV
            updateTooltip();
            notifyValidityChanged();
        } catch (IllegalArgumentException e) {
            // Invalid CSV, print error message
            System.err.println("Invalid CSV: " + csvLine);
            System.err.println("Error: " + e.getMessage());
            // Visual feedback: set tooltip and red border
            String fieldTooltip = getFieldTooltip();
            String errorTooltip;
            if (fieldTooltip != null) {
                errorTooltip = fieldTooltip + " | Invalid CSV: " + e.getMessage();
            } else {
                errorTooltip = "Invalid CSV: " + e.getMessage();
            }
            csvTextField.setToolTipText(errorTooltip);
            notifyTooltipChanged(errorTooltip);
            csvTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            notifyValidityChanged();
        }
    }

    private void updateTooltip() {
        String csvLine = csvTextField.getText().trim();
        if (csvLine.isEmpty()) {
            csvTextField.setToolTipText(null);
            notifyTooltipChanged(null);
            return;
        }

        try {
            // Generate evaluated CSV by creating an Image and converting it back
            Image evaluatedImage = variableParser != null ? new Image(csvLine, variableParser) : new Image(csvLine);
            String evaluatedCsv = evaluatedImage.toCsvLine();
            
            // Only show tooltip if it's different from the original (i.e., has variables)
            if (!evaluatedCsv.equals(csvLine)) {
                String fieldTooltip = getFieldTooltip();
                String newTooltip;
                if (fieldTooltip != null) {
                    newTooltip = fieldTooltip + " | Evaluated: " + evaluatedCsv;
                } else {
                    newTooltip = "Evaluated: " + evaluatedCsv;
                }
                csvTextField.setToolTipText(newTooltip);
                notifyTooltipChanged(newTooltip);
            } else {
                updateTooltipForCurrentField();
            }
        } catch (IllegalArgumentException e) {
            // Invalid CSV, tooltip will be set by error handling or show field tooltip
            updateTooltipForCurrentField();
        }
    }
    
    private void updateTooltipForCurrentField() {
        String csvLine = csvTextField.getText();
        if (csvLine == null || csvLine.trim().isEmpty()) {
            csvTextField.setToolTipText(null);
            notifyTooltipChanged(null);
            return;
        }
        
        String currentTooltip = csvTextField.getToolTipText();
        boolean hasError = currentTooltip != null && currentTooltip.contains("Invalid CSV:");
        
        int caretPosition = csvTextField.getCaretPosition();
        int fieldIndex = getCurrentFieldIndex(csvLine, caretPosition);
        
        if (fieldIndex > 0) {
            String fieldTooltip = getFieldTooltipText(fieldIndex);
            
            // If there's an error, preserve it and update field tooltip
            if (hasError && currentTooltip != null) {
                // Extract error part
                String errorPart = "";
                if (currentTooltip.contains(" | Invalid CSV:")) {
                    errorPart = currentTooltip.substring(currentTooltip.indexOf(" | Invalid CSV:"));
                } else if (currentTooltip.startsWith("Invalid CSV:")) {
                    errorPart = " | " + currentTooltip;
                }
                
                // Check if there's also evaluated CSV
                String evaluatedPart = "";
                if (currentTooltip.contains(" | Evaluated: ")) {
                    int evalIndex = currentTooltip.indexOf(" | Evaluated: ");
                    if (errorPart.isEmpty() || evalIndex < currentTooltip.indexOf("Invalid CSV:")) {
                        evaluatedPart = currentTooltip.substring(evalIndex);
                    }
                }
                
                String newTooltip = fieldTooltip + errorPart + evaluatedPart;
                csvTextField.setToolTipText(newTooltip);
                notifyTooltipChanged(newTooltip);
                return;
            }
            
            // Check if CSV is valid and has variables
            try {
                String trimmed = csvLine.trim();
                if (!trimmed.isEmpty()) {
                    Image evaluatedImage = variableParser != null ? new Image(trimmed, variableParser) : new Image(trimmed);
                    String evaluatedCsv = evaluatedImage.toCsvLine();
                    if (!evaluatedCsv.equals(trimmed)) {
                        // Has variables - show field tooltip and evaluated CSV
                        String newTooltip = fieldTooltip + " | Evaluated: " + evaluatedCsv;
                        csvTextField.setToolTipText(newTooltip);
                        notifyTooltipChanged(newTooltip);
                        return;
                    }
                }
            } catch (IllegalArgumentException e) {
                // Invalid CSV - show field tooltip, error will be added by updateColorsFromCsv/revalidateCsv
                csvTextField.setToolTipText(fieldTooltip);
                notifyTooltipChanged(fieldTooltip);
                return;
            }
            
            // Valid CSV, no variables - just show field tooltip
            csvTextField.setToolTipText(fieldTooltip);
            notifyTooltipChanged(fieldTooltip);
        } else {
            // No valid field detected - clear tooltip unless there's an error
            if (!hasError) {
                csvTextField.setToolTipText(null);
                notifyTooltipChanged(null);
            }
        }
    }
    
    private void notifyTooltipChanged(String tooltipText) {
        if (onTooltipChangedCallback != null) {
            onTooltipChangedCallback.accept(tooltipText);
        }
    }
    
    private int getCurrentFieldIndex(String csvLine, int caretPosition) {
        if (csvLine == null || csvLine.isEmpty() || caretPosition < 0) {
            return -1;
        }
        
        // Find the bracket part (first field)
        int bracketStart = csvLine.indexOf('[');
        int bracketEnd = csvLine.indexOf(']');
        
        if (bracketStart == -1 || bracketEnd == -1) {
            // No brackets found, check if we're before first comma
            int firstComma = csvLine.indexOf(',');
            if (firstComma == -1) {
                return 1; // Only one field (LEDs)
            }
            if (caretPosition < firstComma) {
                return 1; // In first field (LEDs)
            }
            // If at or after first comma, count commas to determine field
            // Field 2 starts after the first comma
            return countCommasAfter(csvLine, firstComma, caretPosition) + 2;
        }
        
        // Check if cursor is inside brackets (field 1: LEDs)
        if (caretPosition >= bracketStart && caretPosition <= bracketEnd) {
            return 1;
        }
        
        // Find the comma after the closing bracket
        int commaAfterBracket = bracketEnd + 1;
        while (commaAfterBracket < csvLine.length() && csvLine.charAt(commaAfterBracket) != ',') {
            commaAfterBracket++;
        }
        
        if (commaAfterBracket >= csvLine.length()) {
            // No comma found - if cursor is after bracket, still in field 1
            if (caretPosition > bracketEnd) {
                return 1;
            }
            return -1;
        }
        
        // If cursor is before the comma after bracket, we're in field 1
        if (caretPosition < commaAfterBracket) {
            return 1;
        }
        
        // If cursor is at or after the comma, we're in the next field
        // Count commas after the bracket comma to determine field index
        // Field 2 starts after the comma after bracket
        int fieldIndex = countCommasAfter(csvLine, commaAfterBracket, caretPosition) + 2;
        
        return fieldIndex;
    }
    
    private int countCommasAfter(String text, int startPos, int endPos) {
        int count = 0;
        for (int i = startPos + 1; i < endPos && i < text.length(); i++) {
            if (text.charAt(i) == ',') {
                count++;
            }
        }
        return count;
    }
    
    private String getFieldTooltip() {
        String csvLine = csvTextField.getText();
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        int caretPosition = csvTextField.getCaretPosition();
        int fieldIndex = getCurrentFieldIndex(csvLine, caretPosition);
        return fieldIndex > 0 ? getFieldTooltipText(fieldIndex) : null;
    }
    
    private String getFieldTooltipText(int fieldIndex) {
        // Determine if we're in leeway field based on CSV content
        String csvLine = csvTextField.getText();
        boolean isLeewayField = isLeewayField(csvLine, fieldIndex);
        
        switch (fieldIndex) {
            case 1:
                return "Leds";
            case 2:
                return "Start RPM";
            case 3:
                return "End RPM";
            case 4:
                return "Start Red";
            case 5:
                return "Start Green";
            case 6:
                return "Start Blue";
            case 7:
                return "End Red";
            case 8:
                return "End Green";
            case 9:
                return "End Blue";
            case 10:
                return "Blink type, 0=solid, 1=fade, 2=blink";
            case 11:
                if (isLeewayField) {
                    return "Leeway";
                } else {
                    return "Effect interval";
                }
            case 12:
                return "Leeway";
            default:
                return null;
        }
    }
    
    private boolean isLeewayField(String csvLine, int fieldIndex) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return false;
        }
        
        // Leeway is the last field
        // When blink mode is 0: leeway is field 11
        // When blink mode != 0: leeway is field 12
        if (fieldIndex == 11) {
            // Check if blink mode is 0 by trying to parse
            try {
                String trimmed = csvLine.trim();
                int bracketEnd = trimmed.indexOf(']');
                if (bracketEnd == -1) {
                    return false;
                }
                String rest = trimmed.substring(bracketEnd + 1).trim();
                if (!rest.startsWith(",")) {
                    return false;
                }
                rest = rest.substring(1);
                String[] parts = rest.split(",");
                if (parts.length >= 10) {
                    // Check blink mode (9th value, index 8)
                    try {
                        int blinkMode = Integer.parseInt(parts[8].trim());
                        // If blink mode is 0, field 11 is leeway
                        // If blink mode != 0, field 11 is optional, field 12 is leeway
                        return blinkMode == 0;
                    } catch (NumberFormatException e) {
                        // Can't parse, assume it's not leeway
                        return false;
                    }
                }
            } catch (Exception e) {
                // Can't determine, default to false
                return false;
            }
        }
        return fieldIndex == 12;
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
                
                // Handle leeway: can have 10 values (blink mode 0: 9 existing + blink mode + leeway) 
                // or 11 values (blink mode != 0: 9 existing + blink mode + optional + leeway)
                if (parts.length < 10 || parts.length > 11) {
                    return; // Invalid number of parts
                }
                
                // Update RGB values in the CSV (indices are now correct)
                parts[2] = String.valueOf(startColor.getRed());   // startRed
                parts[3] = String.valueOf(startColor.getGreen()); // startGreen
                parts[4] = String.valueOf(startColor.getBlue()); // startBlue
                parts[5] = String.valueOf(endColor.getRed());     // endRed
                parts[6] = String.valueOf(endColor.getGreen());   // endGreen
                parts[7] = String.valueOf(endColor.getBlue());    // endBlue
                
                // Preserve leeway (last value)
                String leewayValue = parts[parts.length - 1];
                
                // If blinkMode (parts[8]) is 0, remove the optional value (parts[9]) if present, but keep leeway
                String[] finalParts = parts;
                if (parts.length == 11) {
                    try {
                        int blinkMode = Integer.parseInt(parts[8].trim());
                        if (blinkMode == 0) {
                            // Remove the optional value (parts[9]), but keep leeway (parts[10])
                            finalParts = new String[10];
                            System.arraycopy(parts, 0, finalParts, 0, 9);
                            finalParts[9] = leewayValue; // Keep leeway as last value
                        }
                    } catch (NumberFormatException e) {
                        // If blinkMode is not a number, keep all parts
                    }
                }
                
                // Reconstruct the CSV line
                String updatedCsv = ledPart + "," + String.join(",", finalParts);
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

    public void setOnDuplicateCallback(Runnable callback) {
        this.onDuplicateCallback = callback;
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
            if (variableParser != null) {
                new Image(csvLine, variableParser);
            } else {
                new Image(csvLine);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public void setOnValidityChangedCallback(Runnable callback) {
        this.onValidityChangedCallback = callback;
    }

    public void setOnTooltipChangedCallback(java.util.function.Consumer<String> callback) {
        this.onTooltipChangedCallback = callback;
    }
    
    public JTextField getCsvTextField() {
        return csvTextField;
    }

    private void notifyValidityChanged() {
        if (onValidityChangedCallback != null) {
            onValidityChangedCallback.run();
        }
    }
}
