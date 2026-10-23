
package com.powerhub;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class PowerHubApp extends JFrame {

    // ---------- THEME ----------
    private static final Color NAVY = new Color(24, 65, 48);
    private static final Color NAVY_LIGHT = new Color(45, 105, 75);
    private static final Color BLUE = new Color(35, 126, 82);
    private static final Color BLUE_HOVER = new Color(25, 96, 62);
    private static final Color BG = new Color(242, 247, 241);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(36, 54, 43);
    private static final Color MUTED = new Color(104, 125, 110);
    private static final Color BORDER = new Color(215, 229, 216);
    private static final Color RED = new Color(177, 62, 62);
    private static final Color GREEN = new Color(35, 126, 82);
    private static final Color AMBER = new Color(166, 119, 35);

    private final EmergencyManager manager = new EmergencyManager();

    // Main layout
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pagePanel = new JPanel(pageLayout);
    private final JPanel sidebar = new JPanel();
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel pageSubtitle =
            new JLabel("Emergency Power Support Overview");

    // Dashboard values
    private final JLabel totalValue = new JLabel("0");
    private final JLabel pendingValue = new JLabel("0");
    private final JLabel progressValue = new JLabel("0");
    private final JLabel resolvedValue = new JLabel("0");

    // Request form
    private final JTextField nameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField locationField = new JTextField();

    private final JComboBox<String> typeBox = new JComboBox<>(
            new String[]{
                "Critical medical power need",
                "Assistive-device power need",
                "Other urgent support",
                "Routine request"
            });

    private final JTextArea descriptionArea = new JTextArea(4, 25);

    // Request table
    private final String[] columns = {
        "ID", "Name", "Phone", "Location", "Emergency Type",
        "Priority", "Status", "Created"
    };

    private final DefaultTableModel tableModel =
            new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable requestTable = new JTable(tableModel);
    // Each page needs its own JTable because Swing components cannot
    // be displayed in two scroll panes at the same time.
    private final JTable queueTable = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(tableModel);
    private final TableRowSorter<DefaultTableModel> queueSorter =
            new TableRowSorter<>(tableModel);

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{
                "All statuses", "PENDING", "IN PROGRESS", "RESOLVED"
            });

    private final JLabel requestCountLabel = new JLabel("0 requests");

    private final List<JButton> navigationButtons = new ArrayList<>();

    public PowerHubApp() {
        setTitle("PowerHub | Emergency Power Support");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1350, 820);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        buildSidebar();
        buildPages();
        buildMainLayout();
        configureTable();
        configureSearch();

        refreshAll();
    }

    // =====================================================
    // MAIN LAYOUT
    // =====================================================

    private void buildMainLayout() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(18, 28, 18, 28)));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        pageTitle.setFont(new Font("Georgia", Font.BOLD, 25));
        pageTitle.setForeground(NAVY);

        pageSubtitle.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        pageSubtitle.setForeground(MUTED);

        titlePanel.add(pageTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(pageSubtitle);

        JButton refreshButton = secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        main.add(header, BorderLayout.NORTH);

        pagePanel.setBackground(BG);
        main.add(pagePanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    // =====================================================
    // SIDEBAR NAVIGATION
    // =====================================================

    private void buildSidebar() {
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 18, 14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("  POWERHUB");
        logo.setFont(new Font("Georgia", Font.BOLD, 22));
        logo.setForeground(WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("  Emergency Support");
        tagline.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        tagline.setForeground(new Color(190, 218, 199));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(35));

        addNavButton("Dashboard", "dashboard");
        addNavButton("New Request", "new");
        addNavButton("All Requests", "all");
        addNavButton("Emergency Queue", "queue");

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel(
                "<html><div style='width:175px;'>"
                + "POWERHUB<br>Community Power Support"
                + "</div></html>");

        footer.setForeground(new Color(190, 218, 199));
        footer.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        footer.setBorder(new EmptyBorder(12, 8, 8, 8));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(footer);
    }

    private void addNavButton(String label, String page) {
        JButton button = new JButton(label);

        button.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        button.setForeground(new Color(225, 240, 229));
        button.setBackground(NAVY);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(13, 14, 13, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> showPage(page, label));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!button.getBackground().equals(NAVY_LIGHT)) {
                    button.setBackground(new Color(31, 82, 59));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!button.getBackground().equals(NAVY_LIGHT)) {
                    button.setBackground(NAVY);
                }
            }
        });

        navigationButtons.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void showPage(String page, String title) {
        pageLayout.show(pagePanel, page);

        pageTitle.setText(title);

        switch (page) {
            case "dashboard" ->
                pageSubtitle.setText(
                        "Emergency Power Support Overview");
            case "new" ->
                pageSubtitle.setText(
                        "Register a community emergency request");
            case "all" ->
                pageSubtitle.setText(
                        "Search, filter and manage requests");
            case "queue" ->
                pageSubtitle.setText(
                        "Requests arranged by priority");
            default ->
                pageSubtitle.setText("PowerHub");
        }

        for (JButton button : navigationButtons) {
            boolean selected = button.getText().equals(title);
            button.setBackground(selected ? NAVY_LIGHT : NAVY);
            button.setForeground(WHITE);
        }

        if (page.equals("dashboard")
                || page.equals("all")
                || page.equals("queue")) {
            refreshAll();
        }
    }

    // =====================================================
    // PAGE SETUP
    // =====================================================

    private void buildPages() {
        pagePanel.add(createDashboardPage(), "dashboard");
        pagePanel.add(createNewRequestPage(), "new");
        pagePanel.add(createAllRequestsPage(), "all");
        pagePanel.add(createQueuePage(), "queue");
    }

    private JPanel pageContainer() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(22, 26, 22, 26));
        return panel;
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    private JPanel createDashboardPage() {
        JPanel page = pageContainer();

        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome to PowerHub");
        welcome.setFont(new Font("Georgia", Font.BOLD, 22));
        welcome.setForeground(TEXT);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel intro = new JLabel(
                "Monitor emergency power requests and their progress.");
        intro.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        intro.setForeground(MUTED);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(welcome);
        content.add(Box.createVerticalStrut(6));
        content.add(intro);
        content.add(Box.createVerticalStrut(22));

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setBackground(BG);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));

        cards.add(createStatCard(
                "TOTAL REQUESTS", totalValue, BLUE));
        cards.add(createStatCard(
                "PENDING", pendingValue, AMBER));
        cards.add(createStatCard(
                "IN PROGRESS", progressValue, BLUE));
        cards.add(createStatCard(
                "RESOLVED", resolvedValue, GREEN));

        content.add(cards);
        content.add(Box.createVerticalStrut(25));

        JPanel recentCard = createWhiteCard();
        recentCard.setLayout(new BorderLayout(10, 12));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);

        JLabel recentTitle = sectionTitle("Recent Requests");
        JButton viewAll = linkButton("View all requests");

        viewAll.addActionListener(e ->
                showPage("all", "All Requests"));

        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(viewAll, BorderLayout.EAST);

        JTable recentTable = new JTable(tableModel);
        styleTable(recentTable);
        recentTable.setRowSorter(new TableRowSorter<>(tableModel));

        recentCard.add(recentHeader, BorderLayout.NORTH);
        recentCard.add(new JScrollPane(recentTable), BorderLayout.CENTER);

        content.add(recentCard);
        page.add(content, BorderLayout.CENTER);

        return page;
    }

    private JPanel createStatCard(
            String title, JLabel value, Color accent) {

        JPanel card = createWhiteCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(15, 16, 15, 12)));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Trebuchet MS", Font.BOLD, 11));
        label.setForeground(MUTED);

        value.setFont(new Font("Georgia", Font.BOLD, 30));
        value.setForeground(TEXT);

        card.add(label);
        card.add(Box.createVerticalStrut(12));
        card.add(value);

        return card;
    }

    // =====================================================
    // NEW REQUEST PAGE
    // =====================================================

    private JPanel createNewRequestPage() {
        JPanel page = pageContainer();

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel form = createWhiteCard();
        form.setLayout(new GridBagLayout());
        form.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(24, 28, 24, 28)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 0, 7, 0);

        JLabel formTitle = sectionTitle("Emergency Request Details");
        form.add(formTitle, c);

        c.gridy++;
        JLabel formDescription = new JLabel(
                "Enter the resident's information below.");
        formDescription.setForeground(MUTED);
        form.add(formDescription, c);

        addFormField(form, c, "Full name", nameField);
        addFormField(form, c, "Phone number", phoneField);
        addFormField(form, c, "Location / Address", locationField);
        addFormField(form, c, "Emergency category", typeBox);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        descriptionArea.setForeground(TEXT);
        descriptionArea.setBackground(WHITE);
        descriptionArea.setBorder(new EmptyBorder(8, 9, 8, 9));

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(400, 90));
        descriptionScroll.setBorder(new LineBorder(BORDER, 1, true));

        addFormField(form, c, "Additional details", descriptionScroll);

        JLabel priorityNote = new JLabel(
                "<html><div style='width:400px;'>"
                + "Priority is assigned from the selected category. "
                + "The operator must verify urgency; this is not "
                + "a substitute for professional medical triage."
                + "</div></html>");
        priorityNote.setForeground(MUTED);
        priorityNote.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));

        c.gridy++;
        c.insets = new Insets(10, 0, 12, 0);
        form.add(priorityNote, c);

        JButton submit = primaryButton("SUBMIT EMERGENCY REQUEST");
        submit.setPreferredSize(new Dimension(400, 48));
        submit.addActionListener(e -> submitRequest());

        c.gridy++;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(submit, c);

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.gridy = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;
        outer.anchor = GridBagConstraints.NORTH;
        outer.insets = new Insets(0, 0, 0, 0);

        wrapper.add(form, outer);
        page.add(wrapper, BorderLayout.CENTER);

        return page;
    }

    private void addFormField(
            JPanel panel,
            GridBagConstraints c,
            String label,
            Component input) {

        c.gridy++;
        c.insets = new Insets(11, 0, 4, 0);

        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
        fieldLabel.setForeground(TEXT);
        panel.add(fieldLabel, c);

        c.gridy++;
        c.insets = new Insets(3, 0, 7, 0);

        input.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));

        if (input instanceof JTextField field) {
            field.setPreferredSize(new Dimension(400, 38));
            field.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1, true),
                    new EmptyBorder(5, 9, 5, 9)));
        } else if (input instanceof JComboBox<?> combo) {
            combo.setPreferredSize(new Dimension(400, 38));
            combo.setBackground(WHITE);
        }

        panel.add(input, c);
    }

    // =====================================================
    // ALL REQUESTS PAGE
    // =====================================================

    private JPanel createAllRequestsPage() {
        JPanel page = pageContainer();

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(12, 14));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tools.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(TEXT);

        searchField.setPreferredSize(new Dimension(230, 36));
        searchField.setToolTipText("Search name, location, type or ID");

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setForeground(TEXT);

        statusFilter.setPreferredSize(new Dimension(155, 36));

        JButton update = secondaryButton("Update Status");
        update.addActionListener(e -> updateSelectedStatus());

        JButton refresh = secondaryButton("Refresh");
        refresh.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            refreshAll();
        });

        tools.add(searchLabel);
        tools.add(searchField);
        tools.add(statusLabel);
        tools.add(statusFilter);
        tools.add(update);
        tools.add(refresh);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        requestCountLabel.setForeground(MUTED);
        footer.add(requestCountLabel, BorderLayout.WEST);

        card.add(tools, BorderLayout.NORTH);
        card.add(new JScrollPane(requestTable), BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        page.add(card, BorderLayout.CENTER);

        return page;
    }

    // =====================================================
    // EMERGENCY QUEUE PAGE
    // =====================================================

    private JPanel createQueuePage() {
        JPanel page = pageContainer();

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(12, 12));

        JLabel description = new JLabel(
                "<html><div style='width:650px;'>"
                + "Requests are sorted with lower priority numbers first. "
                + "Requests with equal priority are ordered by creation time. "
                + "Confirm the details and urgency before taking action."
                + "</div></html>");

        description.setForeground(MUTED);
        description.setBorder(new EmptyBorder(4, 4, 10, 4));

        JButton update = primaryButton("Update Selected Request");
        update.addActionListener(e -> updateSelectedStatus());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(update);

        styleTable(queueTable);
        queueTable.setRowSorter(queueSorter);
        queueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        card.add(description, BorderLayout.NORTH);
        card.add(new JScrollPane(queueTable), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        page.add(card, BorderLayout.CENTER);

        return page;
    }

    // =====================================================
    // TABLE CONFIGURATION
    // =====================================================

    private void configureTable() {
        styleTable(requestTable);
        requestTable.setRowSorter(sorter);
        requestTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(WHITE);
        table.setGridColor(new Color(232, 240, 232));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(215, 237, 220));
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(
                new Font("Trebuchet MS", Font.BOLD, 12));
        table.getTableHeader().setForeground(NAVY);
        table.getTableHeader().setBackground(
                new Color(232, 241, 232));
        table.getTableHeader().setPreferredSize(
                new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void configureSearch() {
        searchField.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        applyFilters();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        applyFilters();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        applyFilters();
                    }
                });

        statusFilter.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        List<RowFilter<DefaultTableModel, Integer>> filters =
                new ArrayList<>();

        String query = searchField.getText().trim();

        if (!query.isEmpty()) {
            filters.add(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(query)));
        }

        String selectedStatus =
                (String) statusFilter.getSelectedItem();

        if (selectedStatus != null
                && !selectedStatus.equals("All statuses")) {

            filters.add(RowFilter.regexFilter(
                    "^" + Pattern.quote(selectedStatus) + "$", 6));
        }

        sorter.setRowFilter(filters.isEmpty()
                ? null : RowFilter.andFilter(filters));

        requestCountLabel.setText(
                sorter.getViewRowCount() + " requests shown");
    }

    // =====================================================
    // DATABASE + PRIORITY QUEUE REFRESH
    // =====================================================

    private void refreshAll() {
        try {
            List<EmergencyRequest> requests =
                    manager.getAllRequests();

            updateDashboardCounts(requests);
            populateTable(requests);
            applyFilters();

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void populateTable(List<EmergencyRequest> requests) {
        tableModel.setRowCount(0);

        for (EmergencyRequest r : requests) {
            tableModel.addRow(new Object[]{
                r.getId(),
                r.getFullName(),
                r.getPhone(),
                r.getLocation(),
                r.getEmergencyType(),
                r.getPriority(),
                r.getStatus(),
                r.getCreatedAt() == null
                        ? "" : r.getCreatedAt().toString()
            });
        }
    }

    private void updateDashboardCounts(
            List<EmergencyRequest> requests) {

        int pending = 0;
        int inProgress = 0;
        int resolved = 0;

        for (EmergencyRequest r : requests) {
            String status = r.getStatus();

            if ("PENDING".equalsIgnoreCase(status)) {
                pending++;
            } else if ("IN PROGRESS".equalsIgnoreCase(status)) {
                inProgress++;
            } else if ("RESOLVED".equalsIgnoreCase(status)) {
                resolved++;
            }
        }

        totalValue.setText(String.valueOf(requests.size()));
        pendingValue.setText(String.valueOf(pending));
        progressValue.setText(String.valueOf(inProgress));
        resolvedValue.setText(String.valueOf(resolved));
    }

    // =====================================================
    // SUBMIT REQUEST
    // =====================================================

    private void submitRequest() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String location = locationField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please fill in the name, phone number and location.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!phone.matches("[0-9+() -]{7,20}")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid phone number.",
                    "Invalid Phone Number",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int priority = typeBox.getSelectedIndex() + 1;

        EmergencyRequest request = new EmergencyRequest(
                name,
                phone,
                location,
                (String) typeBox.getSelectedItem(),
                description,
                priority);

        try {
            manager.addRequest(request);

            JOptionPane.showMessageDialog(
                    this,
                    "Request submitted successfully!\n\n"
                    + "Request ID: " + request.getId()
                    + "\nPriority: " + priority,
                    "Request Registered",
                    JOptionPane.INFORMATION_MESSAGE);

            nameField.setText("");
            phoneField.setText("");
            locationField.setText("");
            descriptionArea.setText("");
            typeBox.setSelectedIndex(0);

            refreshAll();
            showPage("all", "All Requests");

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // =====================================================
    // STATUS UPDATE
    // =====================================================

    private void updateSelectedStatus() {
        // Read selection from whichever request table the user is viewing.
        JTable activeTable = pageTitle.getText().equals("Emergency Queue")
                ? queueTable : requestTable;
        int viewRow = activeTable.getSelectedRow();

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a request from the table first.",
                    "No Request Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = activeTable.convertRowIndexToModel(viewRow);
        int id = ((Number) tableModel.getValueAt(row, 0)).intValue();

        String[] statuses = {
            "PENDING", "IN PROGRESS", "RESOLVED"
        };

        String currentStatus =
                String.valueOf(tableModel.getValueAt(row, 6));

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select the new status:",
                "Update Request Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                statuses,
                currentStatus);

        if (selected == null) {
            return;
        }

        try {
            manager.updateStatus(id, selected);
            refreshAll();

            JOptionPane.showMessageDialog(
                    this,
                    "Request #" + id + " updated to " + selected + ".",
                    "Status Updated",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // =====================================================
    // UI HELPERS
    // =====================================================

    private JPanel createWhiteCard() {
        JPanel panel = new JPanel();
        panel.setBackground(WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Georgia", Font.BOLD, 17));
        label.setForeground(NAVY);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        button.setForeground(WHITE);
        button.setBackground(BLUE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 18, 12, 18));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(BLUE_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(BLUE);
            }
        });

        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        button.setForeground(NAVY);
        button.setBackground(new Color(229, 240, 229));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 13, 8, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        button.setForeground(BLUE);
        button.setBackground(WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showDatabaseError(SQLException ex) {
        ex.printStackTrace();

        JOptionPane.showMessageDialog(
                this,
                "Unable to load or update requests.\n\n"
                + ex.getMessage()
                + "\n\nCheck MySQL and Database.java.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================
    // APPLICATION ENTRY POINT
    // =====================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Continue with default Swing look and feel.
            }

            new PowerHubApp().setVisible(true);
        });
    }
}
