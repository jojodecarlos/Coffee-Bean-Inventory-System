package com.example.coffeedms;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Swing GUI that connects to MySQL for the Coffee Bean DMS.
 * Prompts for JDBC URL/user/password, then allows full CRUD + calculate.
 * Styled with a coffee‑inspired palette.
 */
public class CoffeeDmsDBGUI extends JFrame {
    // Coffee palette
    private static final Color CREAM        = new Color(0xEF,0xE1,0xD5);
    private static final Color TAN          = new Color(0xE2,0xC5,0xAF);
    private static final Color MEDIUM_BROWN = new Color(0x6C,0x4A,0x37);
    private static final Color DARK_BROWN   = new Color(0x43,0x2A,0x18);
    private static final Color COCOA        = new Color(0x2B,0x1A,0x0E);

    private DBBeanRepository repo;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{ "Bean ID","Origin","Farm","Roast Level",
                    "Roast Date","Qty (kg)","Cost/kg","Notes","Caffeine mg/g" },
            0
    );
    private final JTable table = new JTable(tableModel) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // Add‑lot fields
    private final JTextField addBeanID    = new JTextField(8);
    private final JTextField addOrigin    = new JTextField(8);
    private final JTextField addFarm      = new JTextField(8);
    private final JComboBox<RoastLevel> addRoastLevel = new JComboBox<>(RoastLevel.values());
    private final JTextField addRoastDate = new JTextField(8);
    private final JTextField addQuantity  = new JTextField(4);
    private final JTextField addCost      = new JTextField(5);
    private final JTextField addNotes     = new JTextField(10);
    private final JTextField addCaffeine  = new JTextField(4);

    // Update‑lot fields
    private final JTextField updSearchID  = new JTextField(8);
    private final JButton    btnLoad      = new JButton("Load");
    private final JTextField updBeanID    = new JTextField(8);
    private final JTextField updOrigin    = new JTextField(8);
    private final JTextField updFarm      = new JTextField(8);
    private final JComboBox<RoastLevel> updRoastLevel = new JComboBox<>(RoastLevel.values());
    private final JTextField updRoastDate = new JTextField(8);
    private final JTextField updQuantity  = new JTextField(4);
    private final JTextField updCost      = new JTextField(5);
    private final JTextField updNotes     = new JTextField(10);
    private final JTextField updCaffeine  = new JTextField(4);
    private final JButton    btnUpdate    = new JButton("Update");

    // Remove & calculate
    private final JTextField tfRemoveID   = new JTextField(8);
    private final JLabel     lblTotalValue= new JLabel("Total: $0.00");

    public CoffeeDmsDBGUI() {
        super("Coffee Bean DMS (MySQL)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        getContentPane().setBackground(CREAM);
        setLayout(new BorderLayout());

        initConnectionPanel();
        initTablePanel();
        initOperationsPanel();

        setVisible(true);
    }

    /** NORTH: Connection form */
    private void initConnectionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(TAN);
        p.setBorder(new TitledBorder(null, "MySQL Connection",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));

        JTextField tfUrl  = new JTextField("jdbc:mysql://localhost:3306/coffee_dms", 30);
        JTextField tfUser = new JTextField(10);
        JPasswordField pf = new JPasswordField(10);
        JButton btn      = new JButton("Connect");
        styleButton(btn);

        p.add(label("URL:"));      p.add(tfUrl);
        p.add(label("User:"));     p.add(tfUser);
        p.add(label("Password:")); p.add(pf);
        p.add(btn);
        add(p, BorderLayout.NORTH);

        btn.addActionListener(e -> {
            try {
                repo = new DBBeanRepository(
                        tfUrl.getText().trim(),
                        tfUser.getText().trim(),
                        new String(pf.getPassword())
                );
                refreshTable();
                JOptionPane.showMessageDialog(this, "Connected successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Connection failed: " + ex.getMessage());
            }
        });
    }

    /** CENTER: Table panel */
    private void initTablePanel() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setBackground(CREAM);
        table.setForeground(COCOA);
        table.setGridColor(MEDIUM_BROWN);

        JTableHeader header = table.getTableHeader();
        header.setBackground(DARK_BROWN);
        header.setForeground(CREAM);
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);

        int[] widths = {120,80,80,80,100,60,60,100,120};
        for (int i = 0; i < widths.length; i++) {
            var col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setResizable(false);
        }

        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setBackground(CREAM);
        scroll.setBorder(new TitledBorder(null, "Bean Lots Inventory",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));
        add(scroll, BorderLayout.CENTER);
    }

    /** SOUTH: Operations tabs */
    private void initOperationsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(TAN);
        tabs.setForeground(COCOA);

        tabs.addTab("Add Bean Lot", wrap(createAddPanel()));
        tabs.addTab("Update Bean Lot", wrap(createUpdatePanel()));
        tabs.addTab("Remove Bean Lot", wrap(createRemovePanel()));
        tabs.addTab("Calculate Value", wrap(createCalculatePanel()));

        add(tabs, BorderLayout.SOUTH);
    }

    private JScrollPane wrap(JPanel panel) {
        panel.setBackground(CREAM);
        JScrollPane sp = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        sp.setBorder(new TitledBorder(null, null,
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));
        return sp;
    }

    /** Add form */
    private JPanel createAddPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(CREAM);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.setBackground(CREAM);
        row1.setBorder(new TitledBorder(null, "Add Bean Lot",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));
        row1.add(label("Bean ID:"));    row1.add(addBeanID);
        row1.add(label("Origin:"));     row1.add(addOrigin);
        row1.add(label("Farm:"));       row1.add(addFarm);
        row1.add(label("Roast Level:"));row1.add(addRoastLevel);
        row1.add(label("Roast Date:")); row1.add(addRoastDate);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.setBackground(CREAM);
        row2.add(label("Qty (kg):"));      row2.add(addQuantity);
        row2.add(label("Cost/kg:"));       row2.add(addCost);
        row2.add(label("Notes:"));         row2.add(addNotes);
        row2.add(label("Caffeine mg/g:")); row2.add(addCaffeine);
        JButton btnAdd = new JButton("Add");
        styleButton(btnAdd);
        btnAdd.addActionListener(e -> handleAdd());
        row2.add(btnAdd);

        wrapper.add(row1);
        wrapper.add(row2);
        return wrapper;
    }

    /** Update form */
    private JPanel createUpdatePanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(CREAM);
        wrapper.setBorder(new TitledBorder(null, "Update Bean Lot",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        search.setBackground(CREAM);
        search.add(label("Existing Bean ID:")); search.add(updSearchID);
        styleButton(btnLoad);
        search.add(btnLoad);
        btnLoad.addActionListener(e -> handleLoad());

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.setBackground(CREAM);
        row1.add(label("Bean ID:"));    row1.add(updBeanID);
        row1.add(label("Origin:"));     row1.add(updOrigin);
        row1.add(label("Farm:"));       row1.add(updFarm);
        row1.add(label("Roast Level:"));row1.add(updRoastLevel);
        row1.add(label("Roast Date:")); row1.add(updRoastDate);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.setBackground(CREAM);
        row2.add(label("Qty (kg):"));      row2.add(updQuantity);
        row2.add(label("Cost/kg:"));       row2.add(updCost);
        row2.add(label("Notes:"));         row2.add(updNotes);
        row2.add(label("Caffeine mg/g:")); row2.add(updCaffeine);
        styleButton(btnUpdate);
        row2.add(btnUpdate);
        btnUpdate.addActionListener(e -> handleUpdate());

        wrapper.add(search);
        wrapper.add(row1);
        wrapper.add(row2);
        return wrapper;
    }

    /** Remove form */
    private JPanel createRemovePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(CREAM);
        p.setBorder(new TitledBorder(null, "Remove Bean Lot",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));
        p.add(label("Bean ID:")); p.add(tfRemoveID);
        JButton btn = new JButton("Remove");
        styleButton(btn);
        btn.addActionListener(e -> handleRemove());
        p.add(btn);
        return p;
    }

    /** Calculate form */
    private JPanel createCalculatePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(CREAM);
        p.setBorder(new TitledBorder(null, "Calculate Value",
                TitledBorder.LEFT, TitledBorder.TOP, null, DARK_BROWN));
        JButton btn = new JButton("Calculate");
        styleButton(btn);
        btn.addActionListener(e -> handleCalculate());
        p.add(btn);
        lblTotalValue.setForeground(COCOA);
        p.add(lblTotalValue);
        return p;
    }

    // Handler methods (unchanged functionality)
    private void handleAdd() {
        try {
            CoffeeBean b = new CoffeeBean(
                    addBeanID.getText().trim(),
                    addOrigin.getText().trim(),
                    addFarm.getText().trim(),
                    (RoastLevel)addRoastLevel.getSelectedItem(),
                    LocalDate.parse(addRoastDate.getText().trim()),
                    Double.parseDouble(addQuantity.getText().trim()),
                    new java.math.BigDecimal(addCost.getText().trim()),
                    addNotes.getText().trim(),
                    Double.parseDouble(addCaffeine.getText().trim())
            );
            if (!repo.add(b)) { showError("Bean ID already exists!"); return; }
            refreshTable();
        } catch (DateTimeParseException ex) {
            showError("Invalid date format.");
        } catch (NumberFormatException ex) {
            showError("Numeric fields invalid.");
        } catch (Exception ex) {
            showError("Add failed: " + ex.getMessage());
        }
    }

    private void handleLoad() {
        try {
            CoffeeBean b = repo.findByID(updSearchID.getText().trim());
            if (b == null) { showError("Bean ID not found."); return; }
            updBeanID.setText(b.getBeanID());
            updOrigin.setText(b.getOriginCountry());
            updFarm.setText(b.getFarmName());
            updRoastLevel.setSelectedItem(b.getRoastLevel());
            updRoastDate.setText(b.getRoastDate().toString());
            updQuantity.setText(String.valueOf(b.getQuantityKg()));
            updCost.setText(b.getCostPerKg().toString());
            updNotes.setText(b.getFlavorNotes());
            updCaffeine.setText(String.valueOf(b.getCaffeineContentMgPerGram()));
        } catch (Exception ex) {
            showError("Load failed: " + ex.getMessage());
        }
    }

    private void handleUpdate() {
        try {
            CoffeeBean b = new CoffeeBean(
                    updBeanID.getText().trim(),
                    updOrigin.getText().trim(),
                    updFarm.getText().trim(),
                    (RoastLevel)updRoastLevel.getSelectedItem(),
                    LocalDate.parse(updRoastDate.getText().trim()),
                    Double.parseDouble(updQuantity.getText().trim()),
                    new java.math.BigDecimal(updCost.getText().trim()),
                    updNotes.getText().trim(),
                    Double.parseDouble(updCaffeine.getText().trim())
            );
            if (!repo.update(b)) { showError("Update failed: ID not found"); return; }
            refreshTable();
        } catch (Exception ex) {
            showError("Update failed: " + ex.getMessage());
        }
    }

    private void handleRemove() {
        try {
            if (!repo.removeByID(tfRemoveID.getText().trim())) {
                showError("Remove failed: ID not found");
            } else {
                refreshTable();
            }
        } catch (Exception ex) {
            showError("Remove failed: " + ex.getMessage());
        }
    }

    private void handleCalculate() {
        try {
            BigDecimal total = repo.calculateTotalInventoryValue();
            lblTotalValue.setText("Total: $" + total);
        } catch (Exception ex) {
            showError("Calculation failed: " + ex.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<CoffeeBean> list = repo.findAll();
            for (CoffeeBean b : list) {
                tableModel.addRow(new Object[]{
                        b.getBeanID(),
                        b.getOriginCountry(),
                        b.getFarmName(),
                        b.getRoastLevel(),
                        b.getRoastDate(),
                        b.getQuantityKg(),
                        b.getCostPerKg(),
                        b.getFlavorNotes(),
                        b.getCaffeineContentMgPerGram()
                });
            }
        } catch (Exception ex) {
            showError("Refresh failed: " + ex.getMessage());
        }
    }

    private void styleButton(JButton b) {
        b.setBackground(MEDIUM_BROWN);
        b.setForeground(CREAM);
        b.setFocusPainted(false);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(DARK_BROWN);
        return l;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CoffeeDmsDBGUI::new);
    }
}
