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

    public AnimationPanel(Animation animation) {
        this.animation = animation;
        this.imageRowPanels = new ArrayList<>();
        initializeComponents();
        setupAnimationListener();
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

    private void refreshImageRows() {
        imageRowsPanel.removeAll();
        imageRowPanels.clear();

        List<Image> images = animation.getImages();
        for (Image image : images) {
            ImageRowPanel rowPanel = new ImageRowPanel();
            rowPanel.setCsvLine(image.toCsvLine());
            // Store reference to the image for reliable removal
            final Image imageRef = image;
            rowPanel.setOnRemoveCallback(() -> {
                animation.remove(imageRef);
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
        emptyRow.setOnRemoveCallback(() -> {
            if (imageRowPanels.contains(emptyRow)) {
                imageRowsPanel.remove(emptyRow);
                imageRowPanels.remove(emptyRow);
                updateButtonStates();
                revalidate();
                repaint();
            }
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
        newRow.setOnRemoveCallback(() -> {
            imageRowsPanel.remove(newRow);
            imageRowPanels.remove(newRow);
            updateButtonStates();
            revalidate();
            repaint();
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
        if (addRowButton != null) {
            addRowButton.setEnabled(allValid);
        }
        // Notify parent to update save button
        firePropertyChange("allRowsValid", !allValid, allValid);
    }

    public String getVariablesText() {
        return variablesTextArea.getText();
    }

    public void setVariablesText(String text) {
        variablesTextArea.setText(text);
    }
}

