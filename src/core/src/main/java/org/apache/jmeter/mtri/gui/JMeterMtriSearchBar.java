package org.apache.jmeter.mtri.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.util.JMeterUtils;

/**
 * Simple Search Bar component for JMeter Toolbar
 */
public class JMeterMtriSearchBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField searchField;
    private final JButton searchButton;
    private final JButton clearButton;

    public JMeterMtriSearchBar() {
        super();

        searchField = new JTextField(15);
        searchField.setToolTipText(JMeterUtils.getResString("search_mtri_tooltip")); // Tooltip for search field

        searchButton = new JButton(JMeterUtils.getResString("search_mtri"));
        clearButton = new JButton(JMeterUtils.getResString("search_mtri_reset"));

        initComponents();
    }

    private void initComponents() {
        setOpaque(false); // khớp với toolbar dark mode

        // Search field: Enter = search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    System.out.println("Hello, Enter get keydown: " + searchField.getText());
                    // performSearch();
                }
            }
        });

        // Search button
        searchButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Hello, World!" + searchField.getText());
            // performSearch();
        });
        
        // Clear button
        clearButton.addActionListener(e -> {
            searchField.setText("");
            JOptionPane.showMessageDialog(this, "Hello, Reset!");
            // clearSearch();
        });

        add(searchField);
        add(searchButton);
        add(clearButton);
    }

    // private void performSearch() {
    //     String text = searchField.getText().trim();
    //     if (!text.isEmpty()) {
    //         ActionRouter.getInstance().actionPerformed(
    //             new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ActionNames.SEARCH_TREE + ":" + text)
    //         );
    //     }
    // }

    // private void clearSearch() {
    //     searchField.setText("");
    //     ActionRouter.getInstance().actionPerformed(
    //         new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ActionNames.SEARCH_RESET)
    //     );
    // }

    /**
     * Thêm vào toolbar với style phù hợp
     */
    public void addToToolbar(JToolBar toolBar) {
        toolBar.add(this);
    }

    /**
     * Optional: Set preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, 280); // Rộng hơn một chút cho search
        return d;
    }
}
