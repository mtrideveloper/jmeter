package org.apache.jmeter.mtri.gui;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Search Bar nâng cao cho phép tìm kiếm và highlight trên TẤT CẢ các thành phần
 * chứa Text
 * (JTextField, JLabel, JCheckBox, JComboBox...) trong View hiện tại của JMeter.
 */
public class MtriFindInCurrentViewSearchBar extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MtriFindInCurrentViewSearchBar.class);
    private static final long serialVersionUID = 1L;

    private final JTextField searchField;
    private final JButton searchButton;
    private final JButton clearButton;

    // Quản lý riêng highlight cho JTextComponent (Highlight từng từ)
    private final Map<JTextComponent, List<Object>> textHighlightMap = new HashMap<>();

    // Quản lý Border highlight cho các thành phần khác (JLabel, JCheckBox...) để
    // khôi phục lại Border gốc
    private final Map<JComponent, Border> originalBordersMap = new HashMap<>();

    // Quản lý highlight cho JTable
    private final Map<JTable, List<CellHighlight>> tableHighlightMap = new HashMap<>();

    // Class chứa thông tin cell cần highlight
    private static class CellHighlight {
        final int row;
        final int column;

        private CellHighlight(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    // Custom Renderer để highlight cell
    private static class HighlightTableCellRenderer extends DefaultTableCellRenderer {
        private final List<CellHighlight> highlights;
        private final Color yellowHighlightColor = new Color(200, 200, 0, 100);
        // private final Color blueHighlightColor = new Color(0, 0, 200, 100);

        private HighlightTableCellRenderer(List<CellHighlight> highlights) {
            this.highlights = highlights;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Kiểm tra cell này có nằm trong danh sách highlight không
            boolean shouldHighlight = highlights.stream()
                    .anyMatch(h -> h.row == row && h.column == column);

            if (shouldHighlight) {
                c.setBackground(yellowHighlightColor);
            } else if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(table.getBackground());
            }

            return c;
        }
    }

    public MtriFindInCurrentViewSearchBar() {
        super();
        searchField = new JTextField(18);
        searchField.setToolTipText(JMeterUtils.getResString("search_mtri_tooltip"));

        searchButton = new JButton(JMeterUtils.getResString("search_mtri"));
        clearButton = new JButton(JMeterUtils.getResString("search_mtri_reset"));

        initComponents();
    }

    private void initComponents() {
        setOpaque(false);

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        searchButton.addActionListener(e -> performSearch());
        clearButton.addActionListener(e -> clearSearch());

        add(searchField);
        add(searchButton);
        add(clearButton);
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }

        clearHighlights();

        JMeterGUIComponent currentGui = GuiPackage.getInstance().getCurrentGui();
        if (!(currentGui instanceof Container container)) {
            showNotFoundMessage(searchText + "class type: " + currentGui.getClass().getName());
            return;
        }

        // 1. Quét tất cả các component có khả năng chứa text
        List<Component> matchableComponents = new ArrayList<>();
        findMatchableComponents(container, matchableComponents);

        boolean foundAny = false;
        Component firstMatchComponent = null;
        int totalMatches = 0;

        Highlighter.HighlightPainter textPainter = new DefaultHighlighter.DefaultHighlightPainter(
                new Color(200, 200, 0, 100));

        Border componentHighlightBorder = BorderFactory.createLineBorder(new Color(255, 140, 0), 2); // Viền Cam nổi bật

        // 2. Tiến hành kiểm tra và highlight dựa trên loại Component
        for (Component comp : matchableComponents) {
            String content = extractTextFromComponent(comp);
            if (content.isEmpty()) {
                continue;
            }

            // System.out.println("Checking component: " + comp.getClass().getName());

            List<Integer> matchPositions = findAllMatches(content, searchText);
            if (matchPositions.isEmpty()) {
                continue;
            }

            totalMatches += matchPositions.size();

            if (comp instanceof JTextComponent jTextComp) {
                // Sử dụng cơ chế Highlighter truyền thống cho ô nhập liệu
                List<Object> tags = new ArrayList<>();
                Highlighter highlighter = jTextComp.getHighlighter();
                for (int start : matchPositions) {
                    try {
                        Object tag = highlighter.addHighlight(start, start + searchText.length(), textPainter);
                        tags.add(tag);
                    } catch (BadLocationException ex) {
                        logger.error("Lỗi khi tạo highlight văn bản: {}", ex.getMessage());
                    }
                }
                textHighlightMap.put(jTextComp, tags);

                if (!foundAny) {
                    foundAny = true;
                    firstMatchComponent = jTextComp;
                    jTextComp.setCaretPosition(matchPositions.get(0));
                    jTextComp.moveCaretPosition(matchPositions.get(0) + searchText.length());
                }
            } else if (comp instanceof JComponent jComp) {
                if (jComp instanceof JTable table) {
                    highlightTableCells(table, searchText);
                } else {
                    // border các thành phần tĩnh như JLabel, JCheckBox, JComboBox
                    if (!originalBordersMap.containsKey(jComp)) {
                        originalBordersMap.put(jComp, jComp.getBorder());
                    }
                    // Hợp nhất viền highlight với viền cũ để tránh mất khoảng cách căn lề (padding)
                    jComp.setBorder(BorderFactory.createCompoundBorder(componentHighlightBorder, jComp.getBorder()));
                }
                // Đặt focus vào component
                if (!foundAny) {
                    foundAny = true;
                    firstMatchComponent = jComp;
                }
            }
        }

        // 3. Điều hướng trỏ chuột focus tới kết quả đầu tiên tìm thấy
        if (foundAny) {
            firstMatchComponent.requestFocusInWindow();
            // logger không hiển thị trong terminal đang runGui
            // logger.info("MtriSearch - {} results found for: `{}`", totalMatches,
            // searchText);
            System.out.println("MtriSearch - " + totalMatches + " results found for: `" + searchText + "`");
        } else {
            if (firstMatchComponent == null) {
                System.out.println("firstMatchComponent is null!");
                showNotFoundMessage(searchText + " not found in current view!");
                return;
            }
            showNotFoundMessage(
                    searchText + " (firstMatchComponent type: " + firstMatchComponent.getClass().getName() + ")");
        }
    }

    /**
     * Đệ quy tìm kiếm tất cả các Component có khả năng hiển thị Text trong giao
     * diện hiện tại
     */
    private static void findMatchableComponents(Container container, List<Component> result) {
        if (container == null) {
            return;
        }

        for (Component c : container.getComponents()) {
            if (!c.isVisible()
            // || !c.isEnabled()
            ) {
                continue;
            }

            // System.out.println("Scanning container: " + container.getClass().getName());

            // JTABLE TRONG SCROLLPANE
            if (c instanceof JScrollPane scrollPane) {
                JViewport viewport = scrollPane.getViewport();
                if (viewport != null) {
                    Component view = viewport.getView();
                    if (view instanceof JTable table && table.isVisible()) {
                        System.out.println(">>> FOUND JTable: " + table.getClass().getName());
                        result.add(table);
                    }
                }
                // Vẫn tiếp tục đệ quy để tìm các component khác bên trong
            }

            // Đăng ký các Component gắn text trực tiếp được vào phạm vi tìm kiếm
            if (c instanceof JTextComponent ||
                    c instanceof JLabel ||
                    c instanceof AbstractButton ||
                    c instanceof JComboBox) {
                result.add(c);
            }

            if (c instanceof Container nestedContainer) {
                findMatchableComponents(nestedContainer, result);
            }
        }
    }

    /**
     * Trích xuất chuỗi văn bản một cách an toàn dựa trên từng loại Component cụ thể
     */
    private static String extractTextFromComponent(Component c) {
        if (c instanceof JTextComponent jTextComp) {
            return jTextComp.getText();
        } else if (c instanceof JLabel label) {
            return label.getText();
        } else if (c instanceof AbstractButton button) {
            return button.getText();
        } else if (c instanceof JComboBox<?> comboBox) {
            Object selectedItem = comboBox.getSelectedItem();
            return selectedItem != null ? selectedItem.toString() : "";
        } else if (c instanceof JTable table) {
            return extractTextFromTable(table);
        }
        return "";
    }

    private static List<Integer> findAllMatches(String content, String searchText) {
        List<Integer> positions = new ArrayList<>();
        if (content == null || searchText == null || searchText.isEmpty()) {
            return positions;
        }

        String textLC = content.toLowerCase(Locale.ROOT);
        String patternLC = searchText.toLowerCase(Locale.ROOT);

        int index = 0;
        while ((index = textLC.indexOf(patternLC, index)) >= 0) {
            positions.add(index);
            index += patternLC.length();
        }
        return positions;
    }

    private void clearHighlights() {
        // 1. Xóa highlight trên các ô nhập liệu (JTextComponent)
        for (Map.Entry<JTextComponent, List<Object>> entry : textHighlightMap.entrySet()) {
            JTextComponent comp = entry.getKey();
            Highlighter highlighter = comp.getHighlighter();
            for (Object tag : entry.getValue()) {
                highlighter.removeHighlight(tag);
            }
        }
        textHighlightMap.clear();

        // 2. Khôi phục lại Border nguyên bản cho JLabel, JCheckBox, JComboBox
        for (Map.Entry<JComponent, Border> entry : originalBordersMap.entrySet()) {
            entry.getKey().setBorder(entry.getValue());
        }
        originalBordersMap.clear();

        // Xóa highlight JTable
        for (Map.Entry<JTable, List<CellHighlight>> entry : tableHighlightMap.entrySet()) {
            JTable table = entry.getKey();
            // Khôi phục renderer mặc định
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
            table.repaint();
        }
        tableHighlightMap.clear();
    }

    private void clearSearch() {
        searchField.setText("");
        clearHighlights();
    }

    private void showNotFoundMessage(String text) {
        JOptionPane.showMessageDialog(this,
                "\"" + text + "\" not found.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public void addToToolbar(JToolBar toolBar) {
        toolBar.add(this);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, 340);
        return d;
    }

    /**
     * Trích xuất tất cả text từ JTable để tìm kiếm (header + data)
     */
    private static String extractTextFromTable(JTable table) {
        if (table == null || table.getColumnCount() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Lấy tên các cột (header)
        for (int col = 0; col < table.getColumnCount(); col++) {
            Object headerValue = table.getColumnModel().getColumn(col).getHeaderValue();
            if (headerValue != null) {
                sb.append(headerValue.toString()).append(" ");
            }
        }

        // Lấy dữ liệu tất cả các ô
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    sb.append(value.toString()).append(" ");
                }
            }
        }

        return sb.toString();
    }

    private void highlightTableCells(JTable table, String searchText) {
        List<CellHighlight> highlights = new ArrayList<>();
        String searchTextLC = searchText.toLowerCase(Locale.ROOT);

        for (int row = 0; row < table.getRowCount(); row++) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                // table này có giá trị tại cell(row, col)
                Object value = table.getValueAt(row, col);
                if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(searchTextLC)) {
                    // Thêm cell này vào danh sách cần highlight
                    highlights.add(new CellHighlight(row, col));
                }
            }
        }

        if (!highlights.isEmpty()) {
            tableHighlightMap.put(table, highlights);
            table.setDefaultRenderer(Object.class, new HighlightTableCellRenderer(highlights));
            table.repaint();

            // Scroll đến cell đầu tiên tìm thấy
            CellHighlight first = highlights.get(0);
            table.scrollRectToVisible(table.getCellRect(first.row, first.column, true));

            System.out.println(">>> Highlighted JTable: " + table.getClass().getName() + " with "
                    + highlights.size() + " matches.");
        }
    }
}