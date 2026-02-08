import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

enum Status { TODO, IN_PROGRESS, DONE }
enum Priority { LOW, MEDIUM, HIGH }

class Task {
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private String assignee;
    private LocalDate dueDate;

    public Task(String title, String description, Status status,
                Priority priority, String assignee, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public String getAssignee() { return assignee; }
    public LocalDate getDueDate() { return dueDate; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(Status status) { this.status = status; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}

class Employee {
    final String name;
    final String role;

    Employee(String name, String role) {
        this.name = name;
        this.role = role;
    }

    @Override
    public String toString() { return name; }
}

public class MiniBoard extends JFrame {

    private final List<Task> tasks = new ArrayList<>();
    private final List<String> activityLog = new ArrayList<>();
    private static final String CSV_FILE = "tasks.csv";

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Employee> employees = Arrays.asList(
            new Employee("Sandesh", "Developer"),
            new Employee("junaid", "Designer"),
            new Employee("sagar", "Project Manager"),
            new Employee("sahil", "QA Engineer"),
            new Employee("Akshay", "Team Lead")
    );

    private final JPanel todoPanel = new JPanel();
    private final JPanel progressPanel = new JPanel();
    private final JPanel donePanel = new JPanel();
    private JLabel statsLabel;
    private boolean isAdmin = false;
    private JTextField searchField;

    public MiniBoard() {
        showLoginScreen();
    }

    private void showLoginScreen() {
        JDialog dlg = new JDialog((Frame) null, "Task management system", true);
        dlg.setSize(400, 320);
        dlg.setLocationRelativeTo(null);
        dlg.setLayout(new GridBagLayout());
        dlg.getContentPane().setBackground(new Color(245, 248, 252));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Task management system", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        gbc.gridwidth = 2;
        gbc.gridy = 0;
        dlg.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        dlg.add(new JLabel("Username:"), gbc);

        JTextField user = new JTextField(18);
        gbc.gridx = 1;
        dlg.add(user, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        dlg.add(new JLabel("Password:"), gbc);

        JPasswordField pass = new JPasswordField(18);
        gbc.gridx = 1;
        dlg.add(pass, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginBtn.setBackground(new Color(50, 130, 230));
        loginBtn.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        dlg.add(loginBtn, gbc);

        JLabel hint = new JLabel("<html><small>Admin → admin / 1234<br>Employee → your name / employee</small></html>");
        hint.setForeground(Color.GRAY);
        gbc.gridy++;
        dlg.add(hint, gbc);

        loginBtn.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword()).trim();
            if ("admin".equals(u) && "1234".equals(p)) {
                isAdmin = true;
                dlg.dispose();
                initMainUI();
            } else if (employees.stream().anyMatch(emp -> emp.name.equals(u)) && "employee".equals(p)) {
                isAdmin = false;
                dlg.dispose();
                initMainUI();
            } else {
                JOptionPane.showMessageDialog(dlg, "Invalid login", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dlg.setVisible(true);
    }

    private void initMainUI() {
        setTitle("MiniBoard – Task Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 880);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        loadTasksFromCSV();
        buildMainUI();

        checkOverdueTasks();
        refreshBoard();
        refreshStats();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    saveTasksToCSV();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        setVisible(true);
    }

    private void buildMainUI() {
        getContentPane().setBackground(new Color(248, 250, 253));
        setLayout(new BorderLayout(10, 10));


        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));
        top.setOpaque(false);

        JLabel hdr = new JLabel("MiniBoard", SwingConstants.CENTER);
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 36));
        hdr.setForeground(new Color(40, 80, 160));
        top.add(hdr, BorderLayout.NORTH);

        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        JLabel lblSearch = new JLabel("Search tasks:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchField.setPreferredSize(new Dimension(380, 38));

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshBoard(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshBoard(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshBoard(); }
        });

        searchRow.add(lblSearch, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        top.add(searchRow, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);


        JPanel kanbanWrapper = new JPanel(new GridLayout(1, 3, 20, 0));
        kanbanWrapper.setOpaque(false);
        kanbanWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        kanbanWrapper.add(createColumn("To Do", todoPanel, new Color(90, 170, 250)));
        kanbanWrapper.add(createColumn("In Progress", progressPanel, new Color(255, 160, 40)));
        kanbanWrapper.add(createColumn("Done", donePanel, new Color(70, 190, 110)));

        JScrollPane scrollKanban = new JScrollPane(kanbanWrapper);
        scrollKanban.setBorder(null);
        scrollKanban.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollKanban, BorderLayout.CENTER);


        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        bottom.setOpaque(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 14));
        btnPanel.setOpaque(true);
        btnPanel.setBackground(new Color(240, 243, 248));
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        btnPanel.setPreferredSize(new Dimension(800, 80));

        JButton[] buttons = {
                createButton("Add Task", new Color(60, 140, 230), e -> addTaskDialog()),
                createButton("Update Task", new Color(245, 150, 40), e -> updateTaskDialog()),
                createButton("Delete Task", new Color(220, 70, 80), e -> {
                    try {
                        deleteTaskDialog();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }),
                createButton("Deadlines", new Color(90, 180, 100), e -> showDeadlines()),
                createButton("Export CSV", new Color(80, 120, 200), e -> exportCSVWithFeedback()),
                createButton("Activity Log", new Color(140, 100, 180), e -> showActivityLog())
        };

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setEnabled(i < 3 ? isAdmin : true);
            btnPanel.add(buttons[i]);
        }

        bottom.add(btnPanel, BorderLayout.NORTH);

        statsLabel = new JLabel("", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bottom.add(statsLabel, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color bg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setOpaque(true);
        btn.addActionListener(al);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JPanel createColumn(String title, JPanel content, Color headerColor) {
        JPanel col = new JPanel(new BorderLayout());
        col.setBackground(new Color(250, 252, 255));
        col.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 230), 1));

        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(headerColor);
        head.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lbl = new JLabel(title, SwingConstants.LEFT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(Color.WHITE);

        JLabel cnt = new JLabel("0", SwingConstants.RIGHT);
        cnt.setFont(new Font("Segoe UI", Font.BOLD, 20));
        cnt.setForeground(Color.WHITE);

        head.add(lbl, BorderLayout.WEST);
        head.add(cnt, BorderLayout.EAST);
        col.add(head, BorderLayout.NORTH);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        col.add(new JScrollPane(content), BorderLayout.CENTER);
        content.putClientProperty("count", cnt);

        return col;
    }

    private void refreshBoard() {
        String q = searchField.getText().trim().toLowerCase();

        todoPanel.removeAll();
        progressPanel.removeAll();
        donePanel.removeAll();

        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) counts.put(s, 0);

        for (Task t : tasks) {
            if (q.isEmpty() ||
                    t.getTitle().toLowerCase().contains(q) ||
                    t.getDescription().toLowerCase().contains(q) ||
                    t.getAssignee().toLowerCase().contains(q)) {

                JPanel target = switch (t.getStatus()) {
                    case TODO       -> todoPanel;
                    case IN_PROGRESS -> progressPanel;
                    case DONE       -> donePanel;
                };

                target.add(createTaskCard(t));
                target.add(Box.createVerticalStrut(10));
                counts.merge(t.getStatus(), 1, Integer::sum);
            }
        }

        updateCountLabel(todoPanel, counts.get(Status.TODO));
        updateCountLabel(progressPanel, counts.get(Status.IN_PROGRESS));
        updateCountLabel(donePanel, counts.get(Status.DONE));

        todoPanel.revalidate();     todoPanel.repaint();
        progressPanel.revalidate(); progressPanel.repaint();
        donePanel.revalidate();     donePanel.repaint();

        refreshStats();
    }

    private void updateCountLabel(JPanel panel, int count) {
        JLabel lbl = (JLabel) panel.getClientProperty("count");
        if (lbl != null) lbl.setText(String.valueOf(count));
    }

    private void refreshStats() {
        long t = tasks.stream().filter(x -> x.getStatus() == Status.TODO).count();
        long p = tasks.stream().filter(x -> x.getStatus() == Status.IN_PROGRESS).count();
        long d = tasks.stream().filter(x -> x.getStatus() == Status.DONE).count();

        statsLabel.setText(String.format("Total: %d • To Do: %d • In Progress: %d • Done: %d",
                tasks.size(), t, p, d));
    }

    private JPanel createTaskCard(Task task) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 230), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        Color prioColor = switch (task.getPriority()) {
            case HIGH   -> new Color(225, 60, 80);
            case MEDIUM -> new Color(255, 165, 0);
            case LOW    -> new Color(70, 180, 110);
        };

        JLabel title = new JLabel("<html><b>" + escapeHtml(task.getTitle()) + "</b></html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel prio = new JLabel(task.getPriority().name());
        prio.setOpaque(true);
        prio.setBackground(prioColor);
        prio.setForeground(Color.WHITE);
        prio.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prio.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(prio, BorderLayout.EAST);

        String overdue = task.getDueDate().isBefore(LocalDate.now()) && task.getStatus() != Status.DONE
                ? " <font color='#d32f2f'><b>OVERDUE</b></font>" : "";

        JLabel meta = new JLabel(task.getAssignee() + " • Due: " + task.getDueDate() + overdue);
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        meta.setForeground(new Color(90, 90, 90));

        card.add(top, BorderLayout.NORTH);
        card.add(meta, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showTaskDetails(task); }
            public void mouseEntered(MouseEvent e) { card.setBorder(new LineBorder(prioColor, 2)); }
            public void mouseExited(MouseEvent e) { card.setBorder(new LineBorder(new Color(215,220,230),1)); }
        });

        return card;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void showTaskDetails(Task task) {
        JDialog d = new JDialog(this, "Task: " + task.getTitle(), true);
        d.setSize(520, 460);
        d.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridLayout(0, 2, 12, 14));
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        p.add(new JLabel("<b>Title:</b>"));          p.add(new JLabel(task.getTitle()));
        p.add(new JLabel("<b>Description:</b>"));    p.add(new JScrollPane(new JTextArea(task.getDescription(),4,30)));
        p.add(new JLabel("<b>Priority:</b>"));       p.add(new JLabel(task.getPriority().name()));
        p.add(new JLabel("<b>Assignee:</b>"));       p.add(new JLabel(task.getAssignee()));
        p.add(new JLabel("<b>Due:</b>"));            p.add(new JLabel(task.getDueDate().toString()));
        p.add(new JLabel("<b>Status:</b>"));         p.add(new JLabel(task.getStatus().name()));

        JButton edit = new JButton("Edit Task");
        edit.addActionListener(e -> {
            if (isAdmin) {
                editOrAddTask(task);
            } else {
                updateStatusOnlyDialog(task);
            }
            d.dispose();
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(edit);

        d.setLayout(new BorderLayout());
        d.add(p, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void addTaskDialog() {
        if (!isAdmin) return;
        editOrAddTask(null);
    }

    private void updateTaskDialog() {
        String title = JOptionPane.showInputDialog(this, "Enter task title to update:");
        if (title == null || title.trim().isEmpty()) return;

        Task task = tasks.stream()
                .filter(t -> t.getTitle().equalsIgnoreCase(title.trim()))
                .findFirst().orElse(null);

        if (task == null) {
            JOptionPane.showMessageDialog(this, "Task not found.", "Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isAdmin) {
            editOrAddTask(task);
        } else {
            updateStatusOnlyDialog(task);
        }
    }

    private void updateStatusOnlyDialog(Task task) {
        JDialog dialog = new JDialog(this, "Update Status: " + task.getTitle(), true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel current = new JLabel("Current Status: " + task.getStatus(), SwingConstants.CENTER);
        current.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JComboBox<Status> statusCombo = new JComboBox<>(Status.values());
        statusCombo.setSelectedItem(task.getStatus());
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton saveBtn = new JButton("Update Status");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setBackground(new Color(60, 140, 230));
        saveBtn.setForeground(Color.WHITE);

        saveBtn.addActionListener(e -> {
            Status newStatus = (Status) statusCombo.getSelectedItem();
            if (newStatus != task.getStatus()) {
                task.setStatus(newStatus);
                log("Employee updated status of \"" + task.getTitle() + "\" to " + newStatus);

                try {
                    saveTasksToCSV();
                    refreshBoard();
                    JOptionPane.showMessageDialog(dialog,
                            "Status updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Failed to save: " + ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            dialog.dispose();
        });

        panel.add(current);
        panel.add(statusCombo);
        panel.add(saveBtn);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void editOrAddTask(Task existing) {
        boolean isNew = (existing == null);
        String dialogTitle = isNew ? "Add New Task" : "Edit Task";

        JDialog d = new JDialog(this, dialogTitle, true);
        d.setSize(540, 520);
        d.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 14));
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        JTextField tfTitle = new JTextField(isNew ? "" : existing.getTitle());
        form.add(new JLabel("Title:")); form.add(tfTitle);

        JTextArea taDesc = new JTextArea(isNew ? "" : existing.getDescription(), 5, 30);
        taDesc.setLineWrap(true);
        form.add(new JLabel("Description:")); form.add(new JScrollPane(taDesc));

        JComboBox<Priority> cbPrio = new JComboBox<>(Priority.values());
        if (!isNew) cbPrio.setSelectedItem(existing.getPriority());
        form.add(new JLabel("Priority:")); form.add(cbPrio);

        JComboBox<Employee> cbAssign = new JComboBox<>(employees.toArray(Employee[]::new));
        if (!isNew) {
            for (int i = 0; i < employees.size(); i++)
                if (employees.get(i).name.equals(existing.getAssignee()))
                    cbAssign.setSelectedIndex(i);
        }
        form.add(new JLabel("Assignee:")); form.add(cbAssign);

        JTextField tfDue = new JTextField(isNew ? LocalDate.now().plusDays(7).format(DATE_FMT) : existing.getDueDate().format(DATE_FMT));
        form.add(new JLabel("Due (yyyy-MM-dd):")); form.add(tfDue);

        JComboBox<Status> cbStatus = new JComboBox<>(Status.values());
        if (!isNew) cbStatus.setSelectedItem(existing.getStatus());
        else cbStatus.setSelectedItem(Status.TODO);
        form.add(new JLabel("Status:")); form.add(cbStatus);

        JButton save = new JButton(isNew ? "Create" : "Save");
        save.addActionListener(ev -> {
            String titleText = tfTitle.getText().trim();
            if (titleText.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Title is required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate due;
            try {
                due = LocalDate.parse(tfDue.getText().trim(), DATE_FMT);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Invalid date format.\nUse: yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isNew) {
                Task nt = new Task(
                        titleText,
                        taDesc.getText().trim(),
                        (Status) cbStatus.getSelectedItem(),
                        (Priority) cbPrio.getSelectedItem(),
                        ((Employee) cbAssign.getSelectedItem()).name,
                        due
                );
                tasks.add(nt);
                log("Created task: " + titleText);
            } else {
                existing.setTitle(titleText);
                existing.setDescription(taDesc.getText().trim());
                existing.setStatus((Status) cbStatus.getSelectedItem());
                existing.setPriority((Priority) cbPrio.getSelectedItem());
                existing.setAssignee(((Employee) cbAssign.getSelectedItem()).name);
                existing.setDueDate(due);
                log("Updated task: " + titleText);
            }

            try {
                saveTasksToCSV();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            refreshBoard();
            d.dispose();
        });

        d.setLayout(new BorderLayout());
        d.add(form, BorderLayout.CENTER);
        d.add(save, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void deleteTaskDialog() throws IOException {
        if (!isAdmin) return;

        String t = JOptionPane.showInputDialog(this, "Task title to delete:");
        if (t == null || t.trim().isEmpty()) return;

        Task task = tasks.stream().filter(x -> x.getTitle().equalsIgnoreCase(t.trim())).findFirst().orElse(null);
        if (task == null) {
            JOptionPane.showMessageDialog(this, "Not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "Delete \"" + task.getTitle() + "\" ?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tasks.remove(task);
            log("Deleted task: " + task.getTitle());
            saveTasksToCSV();
            refreshBoard();
        }
    }

    private void showDeadlines() {
        var upcoming = tasks.stream()
                .filter(t -> t.getStatus() != Status.DONE && t.getDueDate().isBefore(LocalDate.now().plusDays(14)))
                .sorted(Comparator.comparing(Task::getDueDate))
                .toList();

        if (upcoming.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No deadlines in next 14 days.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder("<html><h3>Upcoming Deadlines</h3><ul>");
        upcoming.forEach(t -> sb.append("<li>").append(t.getTitle())
                .append(" – ").append(t.getDueDate())
                .append(" (").append(t.getAssignee()).append(")</li>"));
        sb.append("</ul></html>");

        JOptionPane.showMessageDialog(this, sb.toString(), "Deadlines", JOptionPane.PLAIN_MESSAGE);
    }

    private void checkOverdueTasks() {
        var overdue = tasks.stream()
                .filter(t -> t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != Status.DONE)
                .toList();

        if (!overdue.isEmpty()) {
            StringBuilder sb = new StringBuilder("<html><b>Overdue (" + overdue.size() + "):</b><br><ul>");
            overdue.forEach(t -> sb.append("<li>").append(t.getTitle()).append(" (").append(t.getDueDate()).append(")</li>"));
            sb.append("</ul></html>");
            JOptionPane.showMessageDialog(this, sb.toString(), "Overdue", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportCSVWithFeedback() {
        File file = new File(CSV_FILE).getAbsoluteFile();
        try {
            saveTasksToCSV();
            JOptionPane.showMessageDialog(this,
                    "Exported " + tasks.size() + " tasks to:\n" + file.getAbsolutePath(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Export failed:\n" + ex.getMessage() + "\n\nPath attempted: " + file.getAbsolutePath(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTasksToCSV() throws IOException {
        File file = new File(CSV_FILE);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("title,description,status,priority,assignee,dueDate");

            for (Task t : tasks) {
                pw.print(quote(t.getTitle()));
                pw.print(",");
                pw.print(quote(t.getDescription()));
                pw.print(",");
                pw.print(t.getStatus());
                pw.print(",");
                pw.print(t.getPriority());
                pw.print(",");
                pw.print(quote(t.getAssignee()));
                pw.print(",");
                pw.println(t.getDueDate().format(DATE_FMT));
            }
        }
        System.out.println("CSV saved to: " + file.getAbsolutePath());
        log("Exported CSV: " + file.getAbsolutePath());
    }

    private String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void loadTasksFromCSV() {
        File f = new File(CSV_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length < 6) continue;

                try {
                    String title = unquote(parts[0]);
                    String desc = unquote(parts[1]);
                    Status status = Status.valueOf(parts[2].trim());
                    Priority prio = Priority.valueOf(parts[3].trim());
                    String assignee = unquote(parts[4]);
                    LocalDate due = LocalDate.parse(parts[5].trim(), DATE_FMT);

                    tasks.add(new Task(title, desc, status, prio, assignee, due));
                } catch (Exception ignored) {}
            }
            log("Loaded tasks from CSV");
        } catch (Exception e) {
            System.err.println("Load failed: " + e);
        }
    }

    private String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }

    private void showActivityLog() {
        JTextArea ta = new JTextArea(18, 60);
        ta.setEditable(false);
        if (activityLog.isEmpty()) {
            ta.setText("No activity logged yet.");
        } else {
            activityLog.forEach(e -> ta.append(e + "\n"));
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Activity Log", JOptionPane.PLAIN_MESSAGE);
    }

    private void log(String message) {
        String time = LocalDateTime.now().format(TIMESTAMP_FMT);
        activityLog.add(time + " - " + message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MiniBoard::new);
    }
}