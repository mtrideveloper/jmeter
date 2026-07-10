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