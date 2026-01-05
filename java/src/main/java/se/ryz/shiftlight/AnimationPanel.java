package se.ryz.shiftlight;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AnimationPanel extends JPanel {
    private JTextArea variablesTextArea;
    private JPanel imageRowsPanel;
    private Animation animation;
    private final List<ImageRowPanel> imageRowPanels;
    private JButton addRowButton;
    private VariableParser variableParser;
    private final java.util.Map<Image, String> imageToOriginalCsv;

    public AnimationPanel(Animation animation) {
        this.animation = animation;
        this.imageRowPanels = new ArrayList<>();
        this.variableParser = new VariableParser();
        this.imageToOriginalCsv = new java.util.HashMap<>();
        initializeComponents();
        setupAnimationListener();
        setupVariablesListener();
        refreshImageRows();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top part: Variables text area
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JLabel variablesLabel = new JLabel("Variables");
        variablesLabel.setFont(variablesLabel.getFont().deriveFont(Font.BOLD));
        
        variablesTextArea = new JTextArea(5, 40);
        variablesTextArea.setLineWrap(true);
        variablesTextArea.setWrapStyleWord(true);
        variablesTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateVariables();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateVariables();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateVariables();
            }
        });
        JScrollPane variablesScrollPane = new JScrollPane(variablesTextArea);
        variablesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        variablesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        topPanel.add(variablesLabel, BorderLayout.NORTH);
        topPanel.add(variablesScrollPane, BorderLayout.CENTER);

        // Bottom part: Animation image rows
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        JLabel animationLabel = new JLabel("Animation");
        animationLabel.setFont(animationLabel.getFont().deriveFont(Font.BOLD));
        
        imageRowsPanel = new JPanel();
        imageRowsPanel.setLayout(new BoxLayout(imageRowsPanel, BoxLayout.Y_AXIS));
        imageRowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane imageRowsScrollPane = new JScrollPane(imageRowsPanel);
        imageRowsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        imageRowsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        addRowButton = new JButton("Add Image Row");
        addRowButton.setToolTipText("All rows must have valid CSV before adding a new row");
        addRowButton.addActionListener(e -> addImageRow());
        updateButtonStates();
        
        JPanel bottomHeaderPanel = new JPanel(new BorderLayout());
        bottomHeaderPanel.add(animationLabel, BorderLayout.WEST);
        bottomHeaderPanel.add(addRowButton, BorderLayout.EAST);
        
        bottomPanel.add(bottomHeaderPanel, BorderLayout.NORTH);
        bottomPanel.add(imageRowsScrollPane, BorderLayout.CENTER);

        // Add both panels with a splitter
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        splitPane.setResizeWeight(0.3); // Give 30% to top, 70% to bottom
        splitPane.setDividerLocation(200);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
    }

    private void setupAnimationListener() {
        animation.addAnimationListener(event -> {
            SwingUtilities.invokeLater(() -> {
                switch (event.getEventType()) {
                    case IMAGE_ADDED:
                    case IMAGE_REMOVED:
                    case ANIMATION_CLEARED:
                        refreshImageRows();
                        break;
                }
            });
        });
    }

    private void setupVariablesListener() {
        // Variables are updated via document listener on variablesTextArea
    }

    private void updateVariables() {
        try {
            variableParser.parseVariables(variablesTextArea.getText());
            // Variables parsed successfully, update button states
            updateButtonStates();
            // Re-validate all rows to check if they're still valid with new variables
            // This will update borders (red for invalid, gray for valid) and tooltips
            for (ImageRowPanel rowPanel : imageRowPanels) {
                rowPanel.setVariableParser(variableParser);
            }
            updateButtonStates();
        } catch (IllegalArgumentException e) {
            // Invalid variables, but don't prevent UI from working
            // Just keep current variables or empty
            System.err.println("Invalid variables: " + e.getMessage());
        }
    }

    private void refreshImageRows() {
        imageRowsPanel.removeAll();
        imageRowPanels.clear();

        List<Image> images = animation.getImages();
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            ImageRowPanel rowPanel = new ImageRowPanel();
            rowPanel.setVariableParser(variableParser);
            // Use original CSV if available, otherwise use evaluated CSV
            String csvLine = imageToOriginalCsv.getOrDefault(image, image.toCsvLine());
            rowPanel.setCsvLine(csvLine);
            // Store reference to the image for reliable removal
            final Image imageRef = image;
            final int imageIndex = i;
            rowPanel.setOnRemoveCallback(() -> {
                animation.remove(imageRef);
            });
            rowPanel.setOnDuplicateCallback(() -> {
                duplicateRow(rowPanel, imageIndex);
            });
            rowPanel.setOnValidityChangedCallback(() -> updateButtonStates());
            rowPanel.addCsvDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    updateButtonStates();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    updateButtonStates();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    updateButtonStates();
                }
            });
            imageRowPanels.add(rowPanel);
            imageRowsPanel.add(rowPanel);
        }

        // Add an empty row at the end for adding new images
        ImageRowPanel emptyRow = new ImageRowPanel();
        emptyRow.setVariableParser(variableParser);
        emptyRow.setOnRemoveCallback(() -> {
            if (imageRowPanels.contains(emptyRow)) {
                imageRowsPanel.remove(emptyRow);
                imageRowPanels.remove(emptyRow);
                updateButtonStates();
                revalidate();
                repaint();
            }
        });
        emptyRow.setOnDuplicateCallback(() -> {
            duplicateRow(emptyRow, -1);
        });
        emptyRow.setOnValidityChangedCallback(() -> updateButtonStates());
        emptyRow.addCsvDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }
        });
        imageRowPanels.add(emptyRow);
        imageRowsPanel.add(emptyRow);

        updateButtonStates();
        revalidate();
        repaint();
    }

    private void addImageRow() {
        ImageRowPanel newRow = new ImageRowPanel();
        newRow.setVariableParser(variableParser);
        newRow.setOnRemoveCallback(() -> {
            imageRowsPanel.remove(newRow);
            imageRowPanels.remove(newRow);
            updateButtonStates();
            revalidate();
            repaint();
        });
        newRow.setOnDuplicateCallback(() -> {
            duplicateRow(newRow, -1);
        });
        newRow.setOnValidityChangedCallback(() -> updateButtonStates());
        newRow.addCsvDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }
        });
        imageRowPanels.add(newRow);
        imageRowsPanel.add(newRow);
        updateButtonStates();
        revalidate();
        repaint();
    }

    public boolean areAllRowsValid() {
        if (imageRowPanels.isEmpty()) {
            return false;
        }
        
        boolean hasNonEmptyValidRow = false;
        for (ImageRowPanel rowPanel : imageRowPanels) {
            String csvLine = rowPanel.getCsvLine();
            if (!csvLine.isEmpty()) {
                // Non-empty row must be valid
                if (!rowPanel.isCsvValid()) {
                    return false;
                }
                hasNonEmptyValidRow = true;
            }
        }
        
        // At least one non-empty valid row is required, or all rows must be empty (which is invalid)
        return hasNonEmptyValidRow;
    }

    public void updateButtonStates() {
        boolean allValid = areAllRowsValid();
        boolean isEmpty = imageRowPanels.isEmpty() || (imageRowPanels.size() == 1 && imageRowPanels.get(0).getCsvLine().isEmpty());
        
        if (addRowButton != null) {
            // Enable "Add image row" button if all rows are valid OR if there are no rows
            addRowButton.setEnabled(allValid || isEmpty);
        }
        // Notify parent to update save button (save still requires at least one valid row)
        firePropertyChange("allRowsValid", !allValid, allValid);
    }

    public String getVariablesText() {
        return variablesTextArea.getText();
    }

    public void setVariablesText(String text) {
        variablesTextArea.setText(text);
    }

    public String generateProgramOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN\n");
        
        // Get all valid CSV lines from UI rows and evaluate them
        for (ImageRowPanel rowPanel : imageRowPanels) {
            String csvLine = rowPanel.getCsvLine();
            if (!csvLine.isEmpty() && rowPanel.isCsvValid()) {
                try {
                    // Evaluate the CSV line with variables to get the final values
                    Image image = variableParser != null ? new Image(csvLine, variableParser) : new Image(csvLine);
                    sb.append(image.toCsvLine()).append("\n");
                } catch (IllegalArgumentException e) {
                    // Skip invalid lines
                    System.err.println("Skipping invalid CSV line in program output: " + csvLine);
                }
            }
        }
        
        sb.append("END");
        return sb.toString();
    }

    public List<String> getAllCsvLines() {
        List<String> csvLines = new ArrayList<>();
        // Get CSV lines from UI rows (filter out empty and invalid rows)
        for (ImageRowPanel rowPanel : imageRowPanels) {
            String csvLine = rowPanel.getCsvLine();
            if (!csvLine.isEmpty() && rowPanel.isCsvValid()) {
                csvLines.add(csvLine);
            }
        }
        return csvLines;
    }

    public void loadFromFile(List<String> variables, List<String> csvLines) {
        // Clear current animation and original CSV map
        animation.clear();
        imageToOriginalCsv.clear();
        
        // Set variables and parse them
        String variablesText = String.join("\n", variables);
        setVariablesText(variablesText);
        try {
            variableParser.parseVariables(variablesText);
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Some variables may be invalid: " + e.getMessage());
        }
        
        // Temporarily disable animation listener to prevent refresh during loading
        // We'll manually refresh at the end with original CSV preserved
        
        // Add all CSV lines to animation (filter out empty lines)
        // Store original CSV lines before evaluation
        List<String> originalCsvLines = new ArrayList<>();
        for (String csvLine : csvLines) {
            if (csvLine == null) {
                continue;
            }
            String trimmed = csvLine.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                // Store original CSV before adding (which will evaluate it)
                originalCsvLines.add(trimmed);
                animation.add(trimmed, variableParser);
            } catch (IllegalArgumentException e) {
                // Skip invalid lines
                System.err.println("Skipping invalid CSV line: " + csvLine);
            }
        }
        
        // Now map the original CSV lines to the images
        List<Image> images = animation.getImages();
        for (int i = 0; i < Math.min(images.size(), originalCsvLines.size()); i++) {
            imageToOriginalCsv.put(images.get(i), originalCsvLines.get(i));
        }
        
        // refreshImageRows will be called automatically by the animation listener
        // Remove the empty row that refreshImageRows adds
        SwingUtilities.invokeLater(() -> {
            removeEmptyRowIfPresent();
        });
    }

    private void removeEmptyRowIfPresent() {
        // Find and remove the empty row (the last row if it's empty)
        if (!imageRowPanels.isEmpty()) {
            ImageRowPanel lastRow = imageRowPanels.get(imageRowPanels.size() - 1);
            String csvLine = lastRow.getCsvLine();
            if (csvLine.isEmpty()) {
                imageRowsPanel.remove(lastRow);
                imageRowPanels.remove(lastRow);
                revalidate();
                repaint();
            }
        }
    }

    private void duplicateRow(ImageRowPanel sourceRow, int imageIndex) {
        String csvLine = sourceRow.getCsvLine();
        if (csvLine.isEmpty()) {
            return; // Nothing to duplicate
        }

        // Find the index of the source row in the panel
        int sourceIndex = imageRowPanels.indexOf(sourceRow);
        if (sourceIndex == -1) {
            return;
        }

        // Create a new row with the same CSV
        ImageRowPanel newRow = new ImageRowPanel();
        newRow.setVariableParser(variableParser);
        newRow.setCsvLine(csvLine);
        newRow.setOnRemoveCallback(() -> {
            imageRowsPanel.remove(newRow);
            imageRowPanels.remove(newRow);
            // If it's in the animation, remove it
            if (newRow.getImage() != null) {
                animation.remove(newRow.getImage());
            }
            revalidate();
            repaint();
        });
        newRow.setOnDuplicateCallback(() -> {
            duplicateRow(newRow, -1);
        });
        newRow.setOnValidityChangedCallback(() -> updateButtonStates());
        newRow.addCsvDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateButtonStates();
            }
        });

        // Insert the new row right after the source row
        int insertIndex = sourceIndex + 1;
        imageRowPanels.add(insertIndex, newRow);
        
        // BoxLayout doesn't support index-based insertion, so we need to rebuild
        imageRowsPanel.removeAll();
        for (ImageRowPanel row : imageRowPanels) {
            imageRowsPanel.add(row);
        }

        // If the source row is part of the animation, add the duplicate to animation too
        if (imageIndex >= 0 && imageIndex < animation.getImages().size()) {
            try {
                animation.add(csvLine, variableParser);
            } catch (IllegalArgumentException e) {
                // Invalid CSV, don't add to animation
            }
        }

        revalidate();
        repaint();
    }
}

