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
        
        JButton addRowButton = new JButton("Add Image Row");
        addRowButton.addActionListener(e -> addImageRow());
        
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
            imageRowPanels.add(rowPanel);
            imageRowsPanel.add(rowPanel);
        }

        // Add an empty row at the end for adding new images
        ImageRowPanel emptyRow = new ImageRowPanel();
        emptyRow.setOnRemoveCallback(() -> {
            if (imageRowPanels.contains(emptyRow)) {
                imageRowsPanel.remove(emptyRow);
                imageRowPanels.remove(emptyRow);
                revalidate();
                repaint();
            }
        });
        imageRowPanels.add(emptyRow);
        imageRowsPanel.add(emptyRow);

        revalidate();
        repaint();
    }

    private void addImageRow() {
        ImageRowPanel newRow = new ImageRowPanel();
        newRow.setOnRemoveCallback(() -> {
            imageRowsPanel.remove(newRow);
            imageRowPanels.remove(newRow);
            revalidate();
            repaint();
        });
        imageRowPanels.add(newRow);
        imageRowsPanel.add(newRow);
        revalidate();
        repaint();
    }

    public String getVariablesText() {
        return variablesTextArea.getText();
    }

    public void setVariablesText(String text) {
        variablesTextArea.setText(text);
    }
}

