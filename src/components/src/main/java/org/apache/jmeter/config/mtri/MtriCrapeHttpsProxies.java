package org.apache.jmeter.config.mtri;

import javax.swing.*;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.config.mtri.extensions.ProxyScraper;
import org.apache.jmeter.config.mtri.extensions.ProxyValidator;
import org.apache.jmeter.config.mtri.model.MyProxy;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.threads.ThreadGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MtriCrapeHttpsProxies extends JButton {
    private static final Logger log = LoggerFactory.getLogger(MtriCrapeHttpsProxies.class);

    /// field variables
    private static final String FILENAME = "filename"; //$NON-NLS-1$
    private static final String FILE_ENCODING = "fileEncoding"; //$NON-NLS-1$
    private static final String VARIABLE_NAMES = "variableNames"; //$NON-NLS-1$
    private static final String IGNORE_FIRST_LINE = "ignoreFirstLine"; //$NON-NLS-1$
    private static final String DELIMITER = "delimiter"; //$NON-NLS-1$
    private static final String RECYCLE = "recycle"; //$NON-NLS-1$
    private static final String STOPTHREAD = "stopThread"; //$NON-NLS-1$
    private static final String QUOTED_DATA = "quotedData"; //$NON-NLS-1$
    private static final String SHAREMODE = "shareMode"; //$NON-NLS-1$

    private static final String FILE_NAME = "csv_https_proxies.txt"; // csv bị chặn truy cập
    private static final String OUTPUT_DIR = "D:/Dai-hoc/internship/jmeter/proxies"; // xem xét _<time>

    /**
     * Nơi lưu file = nơi chạy ApacheJMeter.jar + /proxies/csv_https_proxies.txt
     * vd: [root_project]/bin/proxies/csv_https_proxies.txt
     */
    // private static final String OUTPUT_DIR = "proxies";

    /**
     * Số lượng proxy tối đa để scrape.
     * Mặc định 0, tức là không cho phép scrape.
     */
    private int maxProxiesNumber = 0;

    public void setMaxProxiesNumber(int maxProxiesNumber) {
        this.maxProxiesNumber = maxProxiesNumber;
    }

    public String getDirectoryPath() {
        return OUTPUT_DIR;
    }

    public MtriCrapeHttpsProxies(String text) {
        super(text);
        addActionListener(this::crapeHttps);
    }

    public void crapeHttps(final ActionEvent e) {
        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage == null) {
                log.error("GuiPackage instance is null.");
                return;
            }

            // 1. Lấy Node hiện tại đang được chọn trên cây kịch bản (ví dụ: Thread Group)
            JMeterTreeNode currentNode = guiPackage.getTreeListener().getCurrentNode();
            if (currentNode == null) {
                log.error("No node is currently selected in the JMeter tree.");
                return;
            }

            // Tạo Modal Dialog (phủ toàn bộ JMeter)
            JDialog progressDialog = createProgressDialog(guiPackage.getMainFrame());

            // Chạy scrape trên background thread
            new SwingWorker<java.util.List<MyProxy>, String>() {
                @Override
                protected java.util.List<MyProxy> doInBackground() {
                    try {
                        publish("Đang scrape proxies...");
                        ProxyScraper scraper = new ProxyScraper();
                        java.util.List<MyProxy> proxies = scraper.scrapeProxies(maxProxiesNumber);

                        if (!proxies.isEmpty()) {
                            publish("Đang kiểm tra proxy (" + proxies.size() + " proxies)...");
                            ProxyValidator validator = new ProxyValidator();
                            return validator.validateProxies(proxies);
                        }
                        return proxies;
                    } catch (Exception ex) {
                        log.error("Error during scraping", ex);
                        publish("Lỗi: " + ex.getMessage());
                        return null;
                    }
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    // Cập nhật status trên dialog
                    // Bạn có thể thêm JLabel vào dialog để hiển thị chunks.get(0)
                }

                @Override
                protected void done() {
                    progressDialog.dispose(); // Đóng modal

                    try {
                        // lấy từ tham số 1 của SwingWorker
                        java.util.List<MyProxy> aliveProxies = get();
                        if (aliveProxies != null && !aliveProxies.isEmpty()) {
                            addCSVDataSet(guiPackage, aliveProxies.size());
                            JOptionPane.showMessageDialog(guiPackage.getMainFrame(),
                                    "Tìm thấy " + aliveProxies.size() + " proxy sống. Đã cập nhật Threads = Ramp-Up = "
                                            + aliveProxies.size(),
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            // lưu file
                            saveCSVFile(aliveProxies);
                        } else {
                            JOptionPane.showMessageDialog(guiPackage.getMainFrame(),
                                    "Không tìm thấy proxy nào hợp lệ.", "Kết quả",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(guiPackage.getMainFrame(),
                                "Có lỗi xảy ra: " + ex.getMessage(), "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();

            log.info("HTTPS proxies craped successfully.");
            // Hiển thị modal dialog
            progressDialog.setVisible(true);
        } catch (Exception ex) {
            log.error("Error occurred while scraping HTTPS proxies: ", ex);
        }
    }

    private static JDialog createProgressDialog(Frame owner) {
        JDialog dialog = new JDialog(owner, "Scraping HTTPS Proxies...", true); // true = modal
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Đang cào free-proxy https từ free-proxy-list.net...",
                JLabel.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        panel.add(label, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);

        dialog.add(panel);
        return dialog;
    }

    private static void addCSVDataSet(GuiPackage guiPackage, int aliveProxies) {
        try {
            JMeterTreeModel treeModel = guiPackage.getTreeModel();

            // Tìm node Test Plan gốc (cấp cao nhất)
            JMeterTreeNode testPlanNode = (JMeterTreeNode) treeModel.getRoot();
            // root của treeModel thường chứa TestPlanNode ở vị trí đầu tiên
            if (testPlanNode.getChildCount() > 0) {
                testPlanNode = (JMeterTreeNode) testPlanNode.getChildAt(0);
            }

            // Duyệt qua các con của Test Plan để tìm
            // và xóa node cũ nếu trùng tên
            for (int i = testPlanNode.getChildCount() - 1; i >= 0; i--) {
                JMeterTreeNode childNode = (JMeterTreeNode) testPlanNode.getChildAt(i);
                Object userObject = childNode.getUserObject();
                // child node là csv data set config
                if (userObject instanceof CSVDataSet existingCsv) {
                    // Kiểm tra đúng tên để tránh xóa nhầm các
                    // CSV Data Set Config khác nằm ở cấp Test Plan
                    if ("CSV Data Set Config (HTTPS Proxies)".equals(existingCsv.getName())) {
                        treeModel.removeNodeFromParent(childNode);
                        // duyệt ngược danh sách để tránh lỗi ConcurrentModificationException
                        // vd: [a 0, b 1, c 2] -> remove b -> [a 0, c 1] -> i-- là = 0
                        // -> không ảnh hưởng
                        // hoặc tạo 1 list các node cần xóa trước
                    }
                }
            }

            // Khởi tạo đối tượng CSVDataSet mới
            CSVDataSet csv = new CSVDataSet();
            csv.setName("CSV Data Set Config (HTTPS Proxies)");
            csv.setProperty(ConfigTestElement.GUI_CLASS, "org.apache.jmeter.testbeans.gui.TestBeanGUI");
            csv.setProperty(ConfigTestElement.TEST_CLASS, CSVDataSet.class.getName());

            // Cấu hình các thuộc tính
            csv.setProperty(FILENAME, OUTPUT_DIR + "/" + FILE_NAME);
            csv.setProperty(FILE_ENCODING, Charset.defaultCharset().name());
            csv.setProperty(VARIABLE_NAMES, "proxy_ip,proxy_port");
            csv.setProperty(IGNORE_FIRST_LINE, false);
            csv.setProperty(DELIMITER, ",");
            csv.setProperty(QUOTED_DATA, false);
            csv.setProperty(RECYCLE, true);
            csv.setProperty(STOPTHREAD, false);
            csv.setProperty(SHAREMODE, "shareMode.all"); // chia sẻ toàn cục cho tất cả test element

            // Thêm component vào dưới node Test Plan
            // addComponent tự động bọc đối tượng vào một JMeterTreeNode mới và thêm vào
            // treeModel
            JMeterTreeNode newNode = treeModel.addComponent(csv, testPlanNode);
            newNode.setUserObject(csv);

            // Tìm và cập nhật ThreadGroup (cập nhật số threads + ramp-up)
            updateThreadGroups(testPlanNode, aliveProxies);

            JTree jTree = guiPackage.getTreeListener().getJTree();
            TreePath previousPath = jTree.getSelectionPath();

            // Cập nhật cấu trúc cây tại vị trí Test Plan
            treeModel.nodeStructureChanged(testPlanNode);

            // đứng yên tại chỗ đã crape
            jTree.setSelectionPath(previousPath);

            // // Focus vào Node mới tạo
            // TreePath newPath = new TreePath(newNode.getPath());
            // jTree.setSelectionPath(newPath);

            // // Cập nhật GUI để hiển thị node mới thêm vào
            // jTree.updateUI();
            // // hoặc
            // guiPackage.updateCurrentNode();

        } catch (Exception ex) {
            log.error("Failed to add CSVDataSet to Test Plan level", ex);
        }
    }

    private static void saveCSVFile(java.util.List<MyProxy> proxies) {
        try {
            // 1. Tạo folder proxies trong thư mục hiện tại của JMeter
            File outputDir = new File(OUTPUT_DIR);

            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, FILE_NAME);

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), Charset.defaultCharset())) {
                for (MyProxy proxy : proxies) {
                    writer.write(proxy.getIp() + "," + proxy.getPort()); // proxy_ip,proxy_port
                    writer.newLine();
                }

                log.info("Đã lưu {} proxies vào file: {}", proxies.size(), outputFile.getAbsolutePath());

                // Thông báo gui
                JOptionPane.showMessageDialog(null,
                        "Đã lưu " + proxies.size() + " proxies.\n"
                                + outputFile.getAbsolutePath(),
                        "Lưu file thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {
            log.error("Error while saving proxies to file", e);
            JOptionPane.showMessageDialog(null,
                    "Lỗi khi lưu file: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public final void updateInputField(JTextField proxyHost, JTextField proxyPort, JTextField maxProxiesNumberField) {
        this.addActionListener(e -> {
            if (proxyHost == null || proxyPort == null) {
                return;
            }
            proxyHost.setText(JMeterUtils.getResString("proxy_ip_variable"));
            proxyPort.setText(JMeterUtils.getResString("proxy_port_variable"));
            this.setMaxProxiesNumber(Integer.parseInt(maxProxiesNumberField.getText()));
        });
    }

    private static void updateThreadGroups(
            JMeterTreeNode parentNode,
            int aliveProxies) {

        for (int i = 0; i < parentNode.getChildCount(); i++) {
            JMeterTreeNode child = (JMeterTreeNode) parentNode.getChildAt(i);
            Object userObject = child.getUserObject();

            if (userObject instanceof ThreadGroup threadGroup) {
                // Cập nhật số threads = aliveProxies
                // Cập nhật threads = ramp-up
                int value = Math.max(1, aliveProxies);
                threadGroup.setNumThreads(value);
                // Ramp-up
                threadGroup.setRampUp(value);

                // // Refresh GUI của ThreadGroup nếu đang mở
                // JMeterGUIComponent gui = GuiPackage.getInstance().getGui(threadGroup);
                // if (gui != null) {
                // gui.configure(threadGroup); // Cập nhật lại giao diện
                // }

                log.info("Updated ThreadGroup '{}' : threads={}, ramp-up={}",
                        threadGroup.getName(), value, value);
                System.out.println("Updated ThreadGroup '" + threadGroup.getName() + "' : threads=" + value
                        + ", ramp-up=" + value);
            }

            // // Đệ quy nếu muốn tìm sâu hơn (nếu ThreadGroup nằm trong Controller khác)
            // if (child.getChildCount() > 0) {
            // updateThreadGroups(treeModel, child, maxProxies);
            // }
        }
    }

    // private void addCSVDataSet(
    // JMeterTreeNode currentNode,
    // java.util.List<MyProxy> aliveProxies, GuiPackage guiPackage) {
    // try {
    // // 2. Khởi tạo trực tiếp đối tượng dữ liệu CSVDataSet
    // ConfigTestElement csvConfigElement = new CSVDataSet();

    // // Thiết lập các thuộc tính mặc định cho đối tượng (tên hiển thị trên cây)
    // csvConfigElement.setProperty(ConfigTestElement.NAME,
    // "CSV Data Set Config (HTTPS Proxies)");
    // csvConfigElement.setProperty(ConfigTestElement.GUI_CLASS,
    // "org.apache.jmeter.testbeans.gui.TestBeanGUI");
    // csvConfigElement.setProperty(ConfigTestElement.TEST_CLASS,
    // csvConfigElement.getClass().getName());

    // // tạo mô hình Cây (Tree Model) jmeter
    // JMeterTreeModel treeModel = guiPackage.getTreeModel();
    // // thêm node CSVDataSet là con của current node (e.g. Thread Group test
    // element)
    // JMeterTreeNode newNode = treeModel.addComponent(csvConfigElement,
    // currentNode);

    // // đăng ký node id
    // newNode.setUserObject(csvConfigElement);
    // // thông báo tree model có thay đổi cấu trúc để cập nhật giao diện
    // treeModel.nodeStructureChanged(currentNode);

    // JTree jTree = guiPackage.getTreeListener().getJTree();
    // // Tạo đường dẫn TreePath đến node cha để thực hiện expand
    // TreePath parentPath = new TreePath(currentNode.getPath());
    // // Thực hiện mở rộng node cha để thấy được node CSV vừa thêm vào
    // jTree.expandPath(parentPath);

    // // // (Tùy chọn) Tự động chuyển vùng nhìn (focus) sang node CSV vừa tạo
    // // guiPackage.getTreeListener().getJTree().setSelectionPath(new
    // // javax.swing.tree.TreePath(newNode.getPath()));
    // } catch (Exception ex) {
    // log.error("Failed to add CSVDataSet", ex);
    // }
    // }
}
