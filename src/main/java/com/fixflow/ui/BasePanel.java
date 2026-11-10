package com.fixflow.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Abstract base panel providing consistent structure, typography, and polymorphic lifecycle hooks.
 * Demonstrates OOP Abstraction and Polymorphism.
 */
public abstract class BasePanel extends JPanel {

    private final String title;
    private final String description;
    protected final JPanel contentArea;

    public BasePanel(String title, String description) {
        super(new BorderLayout(0, 15));
        this.title = title;
        this.description = description;

        setBorder(new EmptyBorder(25, 30, 25, 30));

        // Add standardized panel header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Content area for subclasses
        contentArea = new JPanel();
        add(contentArea, BorderLayout.CENTER);
    }

    /**
     * Creates a standardized header panel with title and subtitle.
     *
     * @return header component
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 225, 225)));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(descLabel);
        header.add(Box.createRigidArea(new Dimension(0, 12)));

        return header;
    }

    /**
     * Polymorphic lifecycle hook invoked whenever this panel becomes visible in the main view.
     * Subclasses can override this to reload data or refresh tables.
     */
    public void onPanelShown() {
        // Default implementation does nothing
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

