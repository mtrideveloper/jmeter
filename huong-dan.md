## 23/06/26

### Luồng nạp cấu hình

1. Trong `JMeterUtils`, có phương thức để tạo 1 properties
   ```java
   public static Properties getProperties(String file) {
       loadJMeterProperties(file);
       initLocale(); // khởi tạo giá trị ngôn ngữ là thuộc tính language trong jmeter.properties
       return appProperties;
   }
   ...
   public void initializeProperties(String file) {
       System.out.println("Initializing Properties: " + file); // NOSONAR intentional
       getProperties(file);
   }
   ```
2. jmeter khởi tạo các cấu hình bằng cách gọi phương thức:

```java
new JMeterUtils().initializeProperties(filePrefix+"jmeter.properties"); // jmeter.properties là 1 file
```

## 25/06/26

### UI

1. `src\core\src\main\java\org\apache\jmeter\gui\MainFrame.java` của sổ giao diện chính.
2. Thêm guide tooltip cho 1 số component

```properties
# messages.properties
test_plan_classpath_browse_tooltip=Vd: Thêm JDBC Driver để test db, thêm custom plugin, thêm java code
user_defined_variables_tooltip=(Config Element) Khai báo các biến toàn cục (global variables)
```

## 27/06/26

1. Thêm 1 search bar `src\core\src\main\java\org\apache\jmeter\mtri\gui\JMeterMtriSearchBar.java`
   vào hàng toolbar `src\core\src\main\java\org\apache\jmeter\gui\util\JMeterToolBar.java`

```properties
# icons-toolbar.properties
search_mtri=Search by MTRI
search_mtri_tooltip=Search any things
search_mtri_reset=Reset
```

## 28/06/26

1. Sửa 1 số icon trông pro hơn.
   `src\core\src\main\resources\org\apache\jmeter\images\toolbar\icons-toolbar.properties`
   `src\core\src\main\java\org\apache\jmeter\gui\util\JMeterToolBar.java::DEFAULT_TOOLBAR_PROPERTY_FILE`
2. Cây:
   JMeterTreeModel
   |_ JMeterTreeNode
   |_ TestElement (Http Request, Thread Group,...) `src\core\src\main\kotlin\org\apache\jmeter\testelement\TestElement.kt`

3. Implements:
   `src\core\src\main\java\org\apache\jmeter\gui\Searchable.java`.getSearchableTokens được triển khai ở `src\core\src\main\java\org\apache\jmeter\testelement\AbstractTestElement.java` và `src\core\src\main\java\org\apache\jmeter\samplers\SampleResult.java`

4. `ActionRouter.java` có nhiệm vụ gì?

## 29/06/26

1. Nơi tạo border cho 1 node.testelement trong tree khi search: `src\core\src\main\java\org\apache\jmeter\gui\tree\JMeterCellRenderer.java` dòng 74->..
2. `src\core\src\main\java\org\apache\jmeter\mtri\gui\MtriFindInCurrentViewSearchBar.java`
   Kiểm tra

```java
c instanceof JTable
```

không tìm được text của

```java
ObjectTableModel tableModel
```

tại `src\core\src\main\java\org\apache\jmeter\gui\util\FileListPanel.java`

## 02/07/26

1. Đã hightlight được cell của Jtable [được bọc bởi JScrollPane](/src/core/src/main/java/org/apache/jmeter/mtri/gui/MtriFindInCurrentViewSearchBar.java#L244)
2. Nghẫm lại logic xử lý [code](/src/core/src/main/java/org/apache/jmeter/mtri/gui/MtriFindInCurrentViewSearchBar.java#L66).

## 10/07/26

1. Fix lỗi hightlight sai chỗ của text trong tab Response Body (nó dùng JEditorPane, không dùng chung JTextComponent)

### 21:55

`src\components\src\main\java\org\apache\jmeter\config\CSVDataSetBeanInfo.java` có gọi phương thức

```java
createPropertyGroup("csv_data",             //$NON-NLS-1$
                new String[] { FILENAME, FILE_ENCODING, VARIABLE_NAMES,
                        IGNORE_FIRST_LINE, DELIMITER, QUOTED_DATA,
                        RECYCLE, STOPTHREAD, SHAREMODE });
```

Giá trị lấy từ `src\components\src\main\resources\org\apache\jmeter\config\CSVDataSetResources.properties`.
Vậy nó được vẽ lên GUI bằng cách nào?

## 11/07/26
Chú ý `TestBeanGUI.java`

```java
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(TestElement element) {
        if (!initialized){
            init();
            setupGuiClassesList();
        }
        clearGui();
        super.configure(element); // chỗ này
        setValues(element);
        initialized = true;
    }
```

`JMeterTreeModel.java`

```java
    public JMeterTreeNode addComponent(TestElement component, JMeterTreeNode node) throws IllegalUserActionException {
        if (node.getUserObject() instanceof AbstractConfigGui) {
            throw new IllegalUserActionException("This node cannot hold sub-elements");
        }

        GuiPackage guiPackage = GuiPackage.getInstance();
        if (guiPackage != null) {
            // The node can be added in non GUI mode at startup
            guiPackage.updateCurrentNode();
            JMeterGUIComponent guicomp = guiPackage.getGui(component);
            guicomp.clearGui();
            guicomp.configure(component);
            guicomp.modifyTestElement(component);
            guiPackage.getCurrentGui(); // put the gui object back
                                        // to the way it was.
        }
        JMeterTreeNode newNode = new JMeterTreeNode(component, this);

        // This check the state of the TestElement and if returns false it
        // disable the loaded node
        try {
            newNode.setEnabled(component.isEnabled());
        } catch (Exception e) { // TODO - can this ever happen?
            newNode.setEnabled(true);
        }

        this.insertNodeInto(newNode, node, node.getChildCount());
        return newNode;
    }
```

Có lẽ đây là hành động thêm test element vào tree
`AddToTree.java`

```java
    /**
     * Adds the specified class to the current node of the tree.
     */
    @Override
    public void doAction(ActionEvent e) {
        GuiPackage guiPackage = GuiPackage.getInstance();
        try {
            guiPackage.updateCurrentNode();
            TestElement testElement = guiPackage.createTestElement(((JComponent) e.getSource()).getName()); // chỗ này
            JMeterTreeNode parentNode = guiPackage.getCurrentNode();
            JMeterTreeNode node = guiPackage.getTreeModel().addComponent(testElement, parentNode);
            guiPackage.getNamingPolicy().nameOnCreation(node);
            guiPackage.getMainFrame().getTree().setSelectionPath(new TreePath(node.getPath()));
        } catch (Exception err) {
            log.error("Exception while adding a component to tree.", err); // $NON-NLS-1$
            String msg = err.getMessage();
            if (msg == null) {
                msg = err.toString();
            }
            JMeterUtils.reportErrorToUser(msg);
        }
    }
```

`ActionRouter.java`

```java
 private void performAction(final ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if(!NO_TRANSACTION_ACTIONS.contains(actionCommand)) {
            GuiPackage.getInstance().beginUndoTransaction();
        }
        try {
            try {
                GuiPackage.getInstance().updateCurrentGui();
            } catch (Exception err){
                log.error("performAction({}) updateCurrentGui() on{} caused", actionCommand, e, err);
                JMeterUtils.reportErrorToUser("Problem updating GUI - see log file for details");
            }
            for (Command c : commands.get(actionCommand)) {
                try {
                    preActionPerformed(c.getClass(), e);
                    c.doAction(e);
                    postActionPerformed(c.getClass(), e);
                    // terminal in vscode
                    System.out.println("Action performed: " + actionCommand + " by " + c.getClass().getName());
                    // terminal in gui
                    log.info("Action performed: {} by {}", actionCommand, c.getClass().getName());
```

Mọi hành động click chuột trái vào 1 commponent có sự kiện đều ghi log. Vd, khi chọn Add > Config Element > CSV Data Set Config, log như sau:
2026-07-10 22:59:04,739 INFO o.a.j.g.a.ActionRouter: Action performed: Add by org.apache.jmeter.gui.action.AddToTree
2026-07-10 22:59:04,766 INFO o.a.j.g.a.ActionRouter: Action performed: edit by org.apache.jmeter.gui.action.EditCommand

`src\core\src\main\java\org\apache\jmeter\gui\tree\JMeterTreeListener.java`
log này cũng không bắt nguồn từ phương thức

```java
void keyPressed(KeyEvent e)
```

mà bắt nguồn từ:

```java
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        log.debug("value changed, updating currentPath");
        currentPath = e.getNewLeadSelectionPath();
        // Call requestFocusInWindow to ensure current component loses focus and
        // all values are correctly saved
        // see https://bz.apache.org/bugzilla/show_bug.cgi?id=55103
        // see https://bz.apache.org/bugzilla/show_bug.cgi?id=55459
        tree.requestFocusInWindow();
        actionHandler.actionPerformed(new ActionEvent(this, 3333, ActionNames.EDIT)); // $NON-NLS-1$
    }
```

Nhưng hàm này không phải mục tiêu để tạo 1 CSV Data Set Config,
hành động này đến từ `AbstractThreadGroupGui.java` sau đó mới tới action performed

```java
    private static JMenuItem createMenuItem(String name, String actionCommand) {
        JMenuItem menuItem = new JMenuItem(JMeterUtils.getResString(name));
        menuItem.setName(name);
        menuItem.addActionListener(ActionRouter.getInstance());
        menuItem.setActionCommand(actionCommand);
        // terminal in vscode
        System.out.println("createMenuItem (String name, String actionCommand) | actionCommand: " + actionCommand + " | name: " + name);
        // terminal in gui
        log.info("createMenuItem (String name, String actionCommand) | actionCommand: {} | name: {}", actionCommand, name);
        return menuItem;
    }

    private static JMenu createAddMenu() {
        String addAction = ActionNames.ADD;
        /// some code
```

Khi hành động chuột phải Test Plan > Add > Threads (Users) > click Thread Group, có log sau:
2026-07-11 11:54:11,402 INFO o.a.j.g.a.ActionRouter: Action performed: Add by org.apache.jmeter.gui.action.AddToTree
<Chuột phải>
2026-07-11 11:54:11,423 INFO o.a.j.g.t.JMeterTreeListener: createMenuItem (String name, String actionCommand) | actionCommand: Add Think Time between each step | name: add_think_times
2026-07-11 11:54:11,424 INFO o.a.j.g.t.JMeterTreeListener: createMenuItem (String name, String actionCommand) | actionCommand: run_tg | name: run_threadgroup
2026-07-11 11:54:11,424 INFO o.a.j.g.t.JMeterTreeListener: createMenuItem (String name, String actionCommand) | actionCommand: run_tg_no_timers | name: run_threadgroup_no_timers
2026-07-11 11:54:11,424 INFO o.a.j.g.t.JMeterTreeListener: createMenuItem (String name, String actionCommand) | actionCommand: validate_tg | name: validate_threadgroup
2026-07-11 11:54:11,426 INFO o.a.j.g.t.JMeterTreeListener: createPopupMenu()
</Chuột phải>
2026-07-11 11:54:11,426 INFO o.a.j.g.a.ActionRouter: Action performed: edit by org.apache.jmeter.gui.action.EditCommand

Hành động tạo menu item và item con là:
`gọi 1 createMenuItem(String name, String actionCommand) > gọi performAction(final ActionEvent e) để Add by org.apache.jmeter.gui.action.AddToTree`
Hành dộng tạo item con chỉ ghi ra 2 log:
2026-07-11 11:49:04,974 INFO o.a.j.g.a.ActionRouter: Action performed: Add by org.apache.jmeter.gui.action.AddToTree
2026-07-11 11:49:05,000 INFO o.a.j.g.a.ActionRouter: Action performed: edit by org.apache.jmeter.gui.action.EditCommand

--- Đã xóa log createMenuItem (String name, String actionCommand) | actionCommand: và createPopupMenu()

2 Đoạn log trên được kích hoạt từ đây `MenuFactory.java`

```java
    /**
     * Create a single menu item from a MenuInfo object
     *
     * @param info          the MenuInfo object
     * @param actionCommand predefined string, e.g. ActionNames.ADD
     *                      {@link ActionNames}
     * @return the menu item
     */
    private static Component makeMenuItem(MenuInfo info, String actionCommand) {
        if (info instanceof MenuSeparatorInfo) {
            return new JPopupMenu.Separator();
        }

        JMenuItem newMenuChoice = new JMenuItem(info.getLabel());
        newMenuChoice.setName(info.getClassName());
        newMenuChoice.setEnabled(info.getEnabled(actionCommand));
        newMenuChoice.addActionListener(ActionRouter.getInstance());
        if (actionCommand != null) {
            newMenuChoice.setActionCommand(actionCommand);
        }

        System.out.println(
                "makeMenuItem(MenuInfo info, String actionCommand)"
                        + " | info class: " + info.getClass().getName()
                        + " | info label: " + info.getLabel()
                        + " | actionCommand: " + actionCommand);
        // terminal in gui
        log.info(
                "makeMenuItem(MenuInfo info, String actionCommand) | info class: {} | info label: {} | actionCommand: {}",
                info.getClass().getName(),
                info.getLabel(),
                actionCommand);

        return newMenuChoice;
    }
```
Cứ chuột phải là có log, đương nhiên cũng có:
makeMenuItem(MenuInfo info, String actionCommand) | info class: org.apache.jmeter.gui.util.MenuInfo | info label: CSV Data Set Config | actionCommand: Add
-> MenuFactory.makeMenuItem được kích hoạt liên tục để dựng sẵn toàn bộ các item có trong menu.

### Kết luận
right click Thread Group > Config Element > click CSV Data Set Config
1. Sự kiện được gửi thẳng đến `ActionRouter`.
2. `ActionRouter` đọc `e.getActionCommand()` thấy chuỗi là `"Add"`, nó liền gọi lớp `AddToTree` xử lý.
3. Lớp `AddToTree` nhận sự kiện, nó bóc cái item ra bằng lệnh `e.getSource()`, 
rồi gọi tiếp `.getName()` để lấy ngược lại chuỗi `"org.apache.jmeter.config.CSVDataSet"`.
4. Cuối cùng, `AddToTree` dùng Reflection khởi tạo class đó và đưa vào cây kịch bản.

### Lưu ý
```java
class CSVDataSet extends ConfigTestElement
```

```csv
Hằng số,Vị trí,Ý nghĩa
BorderLayout.NORTH,Phía trên cùng,"Thường dùng cho tiêu đề, button hành động"
BorderLayout.SOUTH,Phía dưới cùng,Thường dùng cho status bar
BorderLayout.EAST,Bên phải,Thường dùng cho control nhỏ
BorderLayout.WEST,Bên trái,Thường dùng cho label hoặc control nhỏ
BorderLayout.CENTER,Giữa (chiếm hết phần còn lại),Phần chính
BorderLayout.PAGE_START,First line,Tương đương NORTH (hỗ trợ RTL languages)
BorderLayout.PAGE_END,Last line,Tương đương SOUTH
BorderLayout.LINE_START,Line start,Tương đương WEST (hỗ trợ RTL)
BorderLayout.LINE_END,Line end,Tương đương EAST
BorderLayout.BEFORE_FIRST_LINE,Before first line,"Cũ, ít dùng"
BorderLayout.AFTER_LAST_LINE,After last line,"Cũ, ít dùng"
BorderLayout.BEFORE_LINE_BEGINS,Before line begins,Cũ
BorderLayout.AFTER_LINE_ENDS,After line ends,Cũ
```

#### Cơ bản làm xong chức năng cào proxy tự động

## 12/07/26
1. Thêm tooltip cho Connect và Response timeout
2. Thêm HTTPS Proxies Scrape cho HTTP Request Defaults
3. Thêm tooltip cho các component trong Proxy Server
4. Thêm nút Open file location.., nơi chứa file csv_https_proxies.txt
5. Xóa csv data set config https proxies cũ khi cào proxy thành công