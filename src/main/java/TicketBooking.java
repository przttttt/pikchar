import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TicketBooking extends JFrame {
    private static final int ROWS = 12;
    private static final int COLS = 10;
    private static final int MAX_TICKETS = 8;

    private static final Color BACKGROUND = new Color(224, 229, 232);
    private static final Color PANEL = new Color(246, 247, 244);
    private static final Color PANEL_LIGHT = Color.WHITE;
    private static final Color BORDER = new Color(147, 158, 165);
    private static final Color TEXT = new Color(35, 42, 49);
    private static final Color MUTED = new Color(83, 93, 101);
    private static final Color ACCENT = new Color(255, 190, 45);
    private static final Color ACCENT_DARK = new Color(59, 45, 20);
    private static final Color AVAILABLE = new Color(139, 224, 144);
    private static final Color BOOKED = new Color(211, 25, 61);
    private static final Color SUCCESS = new Color(38, 112, 53);
    private static final Color HEADER = new Color(35, 35, 112);

    private static final Movie[] MOVIES = {
            new Movie("Ramayana", 280),
            new Movie("Godzilla", 300),
            new Movie("Wildwood", 260),
            new Movie("Jailer 2", 270),
            new Movie("Dune 3", 320),
            new Movie("Doomsday", 290)
    };
    private static final String[] SHOWS = {
            "10:00 AM  ·  Morning",
            "01:30 PM  ·  Matinee",
            "06:00 PM  ·  Prime time",
            "09:30 PM  ·  Late show"
    };
    private static final String[] ROW_NAMES = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
    };

    private final Database db = new Database();
    private final JComboBox<Movie> movie = new JComboBox<>(MOVIES);
    private final JComboBox<String> show = new JComboBox<>(SHOWS);
    private final JTextField customer = new JTextField();
    private final JTextField phone = new JTextField();
    private final JSpinner count = new JSpinner(
            new SpinnerNumberModel(2, 1, MAX_TICKETS, 1));
    private final JToggleButton[][] seats = new JToggleButton[ROWS][COLS];
    private final JLabel movieDetails = new JLabel();
    private final JLabel theatreMovieDetails = new JLabel();
    private final JLabel availability = new JLabel();
    private final JLabel summaryMovie = new JLabel();
    private final JLabel summaryShow = new JLabel();
    private final JLabel ticketCount = new JLabel();
    private final JLabel seatSummary = new JLabel();
    private final JLabel total = new JLabel();
    private final JButton book = new JButton("Book tickets");

    public TicketBooking() {
        super("Pikchar | Movie Tickets");
        db.initialize();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirm("Exit Pikchar?")) {
                    dispose();
                }
            }
        });

        setJMenuBar(buildMenuBar());
        setContentPane(buildUI());
        refreshSeats();
        updateSummary();
        setMinimumSize(new Dimension(1120, 700));
        setSize(1360, 860);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> {
            if (confirm("Exit Pikchar?")) {
                dispose();
            }
        });
        fileMenu.add(exit);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About Pikchar");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Pikchar — Movie Ticket Booking\nChoose a film, show and seats to book.",
                "About Pikchar", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(about);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(12, 14, 12, 14));
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.setOpaque(false);
        content.add(buildBookingPanel(), BorderLayout.WEST);
        content.add(buildTheatrePanel(), BorderLayout.CENTER);
        content.add(buildSummaryPanel(), BorderLayout.EAST);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(true);
        header.setBackground(HEADER);
        header.setBorder(new EmptyBorder(9, 12, 9, 12));

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel brand = label("Pikchar  —  Book Your Movie Tickets", 20, true, Color.WHITE);
        JLabel tagline = label("Choose your show and seats", 12, false, new Color(222, 224, 245));
        identity.add(brand);
        identity.add(Box.createVerticalStrut(3));
        identity.add(tagline);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actions.setOpaque(false);
        JLabel capacity = label("12 rows  ·  120 seats", 12, true, Color.WHITE);
        JButton history = button("Booking history", HEADER);
        history.setForeground(Color.WHITE);
        history.setBorder(new LineBorder(new Color(170, 174, 218), 1));
        history.addActionListener(e -> showBookings());
        actions.add(capacity);
        actions.add(history);

        header.add(identity, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JComponent buildBookingPanel() {
        CardPanel panel = card(new BorderLayout(0, 16));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.add(sectionTitle("Your booking", "Enter details, then choose seats."), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        addField(fields, "Name", customer);
        addField(fields, "Mobile number", phone);

        addFieldLabel(fields, "Movie");
        styleCombo(movie);
        movie.setRenderer(new MovieRenderer());
        fields.add(movie);
        fields.add(Box.createVerticalStrut(5));
        movieDetails.setFont(font(12, false));
        movieDetails.setForeground(MUTED);
        fields.add(movieDetails);
        fields.add(Box.createVerticalStrut(16));

        addFieldLabel(fields, "Show time");
        styleCombo(show);
        fields.add(show);
        fields.add(Box.createVerticalStrut(16));

        addFieldLabel(fields, "Number of tickets  (max " + MAX_TICKETS + ")");
        JSpinner.NumberEditor countEditor = new JSpinner.NumberEditor(count, "0");
        count.setEditor(countEditor);
        styleSpinner(count);
        fields.add(count);
        fields.add(Box.createVerticalStrut(17));

        panel.add(fields, BorderLayout.CENTER);

        movie.addActionListener(e -> {
            updateMovieDetails();
            refreshSeats();
        });
        show.addActionListener(e -> refreshSeats());
        count.addChangeListener(e -> {
            trimSeats();
            updateSummary();
        });
        updateMovieDetails();
        return panel;
    }

    private JComponent buildTheatrePanel() {
        CardPanel panel = card(new BorderLayout(12, 14));

        JPanel theatreHeader = new JPanel(new BorderLayout(10, 8));
        theatreHeader.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(label("Select your seats", 16, true, TEXT));
        titleBlock.add(Box.createVerticalStrut(5));
        theatreMovieDetails.setFont(font(12, false));
        theatreMovieDetails.setForeground(MUTED);
        titleBlock.add(theatreMovieDetails);
        theatreHeader.add(titleBlock, BorderLayout.WEST);
        theatreHeader.add(availability, BorderLayout.EAST);
        availability.setFont(font(12, true));
        availability.setForeground(SUCCESS);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(theatreHeader, BorderLayout.NORTH);
        top.add(new ScreenPanel(), BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        JPanel seatGrid = createSeatGrid();
        JScrollPane scroll = new JScrollPane(seatGrid);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setBackground(PANEL);
        scroll.getVerticalScrollBar().setForeground(MUTED);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildSeatLegend(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSeatGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(4, 4, 10, 4));

        addGridCell(grid, label("ROW", 10, true, MUTED), 0, 0, 42, 26);
        for (int col = 0; col < COLS; col++) {
            int gridColumn = seatGridColumn(col);
            addGridCell(grid, label(String.format("%02d", col + 1), 10, true, MUTED),
                    gridColumn, 0, 44, 26);
        }
        addAisleSpacer(grid, 0);

        for (int row = 0; row < ROWS; row++) {
            int gridRow = row + 1;
            JLabel rowLabel = label(ROW_NAMES[row], 12, true, TEXT);
            rowLabel.setHorizontalAlignment(SwingConstants.CENTER);
            addGridCell(grid, rowLabel, 0, gridRow, 42, 36);
            addGridCell(grid, label(rowTierName(row), 9, false, MUTED),
                    12, gridRow, 66, 36);
            for (int col = 0; col < COLS; col++) {
                String seatId = ROW_NAMES[row] + (col + 1);
                JToggleButton seat = new JToggleButton();
                seat.setText(String.valueOf(col + 1));
                seat.setToolTipText(seatId + " · " + rowTierName(row) + " · ₹" + seatPrice(row));
                seat.setFont(font(11, true));
                seat.setForeground(TEXT);
                seat.setBackground(AVAILABLE);
                seat.setBorder(new LineBorder(BORDER, 1));
                seat.setFocusPainted(false);
                seat.setOpaque(true);
                seat.setPreferredSize(new Dimension(44, 34));
                seat.setMargin(new Insets(2, 2, 2, 2));
                seat.addActionListener(e -> {
                    if (seat.isSelected() && selectedSeats().size() > (int) count.getValue()) {
                        seat.setSelected(false);
                    }
                    paintSeat(seat);
                    updateSummary();
                });
                seats[row][col] = seat;
                addGridCell(grid, seat, seatGridColumn(col), gridRow, 44, 36);
            }
            addAisleSpacer(grid, gridRow);
        }
        return grid;
    }

    private JComponent buildSeatLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 3));
        legend.setOpaque(false);
        legend.add(legendItem("Available", AVAILABLE, TEXT));
        legend.add(legendItem("Selected", ACCENT, ACCENT_DARK));
        legend.add(legendItem("Reserved", BOOKED, TEXT));
        legend.add(Box.createHorizontalStrut(10));
        legend.add(label("A–D Standard", 11, false, MUTED));
        legend.add(label("E–H Premium", 11, false, MUTED));
        legend.add(label("I–L Recliner", 11, false, MUTED));
        return legend;
    }

    private JComponent buildSummaryPanel() {
        CardPanel panel = card(new BorderLayout(0, 14));
        panel.setPreferredSize(new Dimension(265, 0));
        panel.add(sectionTitle("Booking summary", "Review your selection."), BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        addSummaryLine(details, "Movie", summaryMovie);
        addSummaryLine(details, "Show", summaryShow);
        addSummaryDivider(details);
        details.add(label("Selected seats", 11, true, MUTED));
        seatSummary.setFont(font(13, false));
        seatSummary.setForeground(TEXT);
        seatSummary.setBorder(new EmptyBorder(7, 0, 14, 0));
        details.add(seatSummary);
        addSummaryDivider(details);
        details.add(label("Ticket count", 11, true, MUTED));
        ticketCount.setFont(font(13, false));
        ticketCount.setForeground(TEXT);
        ticketCount.setBorder(new EmptyBorder(7, 0, 14, 0));
        details.add(ticketCount);
        details.add(Box.createVerticalGlue());
        panel.add(details, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel totalCaption = label("Total", 11, true, MUTED);
        total.setFont(font(28, true));
        total.setForeground(TEXT);
        ticketCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        total.setAlignmentX(Component.LEFT_ALIGNMENT);

        book.setFont(font(14, true));
        book.setForeground(ACCENT_DARK);
        book.setBackground(ACCENT);
        book.setBorder(new EmptyBorder(13, 14, 13, 14));
        book.setFocusPainted(false);
        book.setAlignmentX(Component.LEFT_ALIGNMENT);
        book.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        book.addActionListener(e -> bookTickets());

        JButton clear = button("Clear selection", PANEL_LIGHT);
        clear.setAlignmentX(Component.LEFT_ALIGNMENT);
        clear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        clear.addActionListener(e -> resetSelection());

        bottom.add(totalCaption);
        bottom.add(Box.createVerticalStrut(3));
        bottom.add(total);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(book);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(clear);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void updateMovieDetails() {
        Movie selected = (Movie) movie.getSelectedItem();
        if (selected != null) {
            String description = "From ₹" + selected.basePrice + " per seat";
            movieDetails.setText(description);
            theatreMovieDetails.setText(selected.title + "  ·  " + description);
        }
    }

    private void refreshSeats() {
        Movie selectedMovie = (Movie) movie.getSelectedItem();
        String selectedShow = (String) show.getSelectedItem();
        if (selectedMovie == null || selectedShow == null) {
            return;
        }
        try {
            Set<String> booked = db.bookedSeats(selectedMovie.title, selectedShow);
            int openCount = 0;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    JToggleButton seat = seats[row][col];
                    String id = ROW_NAMES[row] + (col + 1);
                    seat.setSelected(false);
                    seat.setEnabled(!booked.contains(id));
                    if (seat.isEnabled()) {
                        openCount++;
                    }
                    seat.setToolTipText(id + " · " + rowTierName(row) + " · ₹" + seatPrice(row)
                            + (seat.isEnabled() ? " · Available" : " · Reserved"));
                    paintSeat(seat);
                }
            }
            availability.setText(openCount + " of " + (ROWS * COLS) + " seats available");
            updateSummary();
        } catch (SQLException ex) {
            fail("Could not load seat availability.", ex);
        }
    }

    private void paintSeat(JToggleButton seat) {
        if (!seat.isEnabled()) {
            seat.setBackground(BOOKED);
            seat.setForeground(new Color(223, 190, 194));
            seat.setBorder(new LineBorder(BOOKED, 1));
        } else if (seat.isSelected()) {
            seat.setBackground(ACCENT);
            seat.setForeground(ACCENT_DARK);
            seat.setBorder(new LineBorder(ACCENT.darker(), 1));
        } else {
            seat.setBackground(AVAILABLE);
            seat.setForeground(TEXT);
            seat.setBorder(new LineBorder(BORDER, 1));
        }
    }

    private List<String> selectedSeats() {
        List<String> selected = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (seats[row][col].isSelected()) {
                    selected.add(ROW_NAMES[row] + (col + 1));
                }
            }
        }
        return selected;
    }

    private void trimSeats() {
        List<String> selected = selectedSeats();
        int max = (int) count.getValue();
        for (int i = selected.size() - 1; i >= max; i--) {
            String id = selected.get(i);
            int row = id.charAt(0) - 'A';
            int col = Integer.parseInt(id.substring(1)) - 1;
            seats[row][col].setSelected(false);
            paintSeat(seats[row][col]);
        }
    }

    private int seatPrice(int row) {
        Movie selected = (Movie) movie.getSelectedItem();
        if (selected == null) {
            return 0;
        }
        int tierExtra = row < 4 ? 0 : row < 8 ? 60 : 120;
        int showExtra = show.getSelectedIndex() >= 2 ? 50 : 0;
        return selected.basePrice + tierExtra + showExtra;
    }

    private int ticketTotal() {
        int subtotal = 0;
        for (int row = 0; row < ROWS; row++) {
            for (JToggleButton seat : seats[row]) {
                if (seat.isSelected()) {
                    subtotal += seatPrice(row);
                }
            }
        }
        return subtotal;
    }

    private void updateSummary() {
        Movie selectedMovie = (Movie) movie.getSelectedItem();
        if (selectedMovie == null || show.getSelectedItem() == null) {
            return;
        }
        summaryMovie.setText(selectedMovie.title);
        summaryShow.setText((String) show.getSelectedItem());
        List<String> chosen = selectedSeats();
        String names = chosen.isEmpty() ? "No seats selected" : String.join(", ", chosen);
        seatSummary.setText("<html><div style='width:220px'>" + escapeHtml(names) + "</div></html>");
        ticketCount.setText(chosen.size() + " selected · " + (int) count.getValue() + " requested");
        total.setText("₹" + ticketTotal());
        book.setEnabled(chosen.size() == (int) count.getValue());
        book.setToolTipText(book.isEnabled() ? "Continue with simulated payment"
                : "Select exactly " + count.getValue() + " seats to continue");
    }

    private void bookTickets() {
        String name = customer.getText().trim();
        String mobile = phone.getText().trim();
        List<String> chosen = selectedSeats();
        if (name.length() < 2 || !name.matches("[\\p{L} .'-]+")) {
            error("Enter a valid name using letters, spaces, apostrophes or hyphens.");
            customer.requestFocus();
            return;
        }
        if (!mobile.matches("[6-9]\\d{9}")) {
            error("Enter a valid 10-digit Indian mobile number.");
            phone.requestFocus();
            return;
        }
        if (chosen.size() != (int) count.getValue()) {
            error("Select exactly " + count.getValue() + " available seats.");
            return;
        }
        if (!confirm("Confirm " + chosen.size() + " ticket(s) for ₹" + ticketTotal()
                + "?\nPayment is simulated. No real money will be charged.")) {
            return;
        }

        Movie selectedMovie = (Movie) movie.getSelectedItem();
        String selectedShow = (String) show.getSelectedItem();
        String id = "PK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        LocalDateTime bookedAt = LocalDateTime.now();
        String summary = "PIKCHAR  ·  BOOKING CONFIRMED\n\n"
                + "Booking ID   " + id + "\n"
                + "Date         " + bookedAt.format(DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm a", Locale.ENGLISH)) + "\n"
                + "Customer     " + name + "\n"
                + "Mobile       " + mobile + "\n"
                + "Movie        " + selectedMovie.title + "\n"
                + "Show         " + selectedShow + "\n"
                + "Seats        " + String.join(", ", chosen) + "\n"
                + "Total        ₹" + ticketTotal() + "\n"
                + "Payment      SIMULATED / APPROVED\n\n"
                + "Enjoy the show!";
        try {
            db.saveBooking(id, name, mobile, selectedMovie.title, selectedShow,
                    chosen, ticketTotal());
            JTextArea receipt = new JTextArea(summary, 15, 38);
            receipt.setEditable(false);
            receipt.setFont(font(13, false));
            receipt.setForeground(TEXT);
            receipt.setBackground(PANEL);
            receipt.setBorder(new EmptyBorder(14, 14, 14, 14));
            JOptionPane.showMessageDialog(this, new JScrollPane(receipt),
                    "Booking confirmed", JOptionPane.INFORMATION_MESSAGE);
            refreshSeats();
            clearCustomerDetails();
        } catch (SQLException ex) {
            fail("Booking could not be saved. No seats were reserved.", ex);
            refreshSeats();
        }
    }

    private void clearCustomerDetails() {
        customer.setText("");
        phone.setText("");
        count.setValue(2);
        updateSummary();
    }

    private void resetSelection() {
        for (JToggleButton[] row : seats) {
            for (JToggleButton seat : row) {
                seat.setSelected(false);
                paintSeat(seat);
            }
        }
        updateSummary();
    }

    private void showBookings() {
        try {
            String[] columns = {"Booking ID", "Customer", "Movie", "Show", "Seats", "Total"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            for (Object[] booking : db.bookingHistory()) {
                model.addRow(booking);
            }

            JTable history = new JTable(model);
            history.setFont(font(12, false));
            history.setRowHeight(26);
            history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            history.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            history.getTableHeader().setFont(font(12, true));
            history.getTableHeader().setReorderingAllowed(false);
            history.setForeground(TEXT);
            history.setBackground(PANEL);
            history.setGridColor(BORDER);
            history.setFillsViewportHeight(true);

            int[] widths = {105, 135, 95, 165, 210, 80};
            for (int i = 0; i < widths.length; i++) {
                history.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
            JScrollPane scroll = new JScrollPane(history);
            scroll.setPreferredSize(new Dimension(820, 360));

            JComponent content = scroll;
            if (model.getRowCount() == 0) {
                JPanel empty = new JPanel(new BorderLayout(0, 12));
                empty.add(new JLabel("No bookings yet."), BorderLayout.NORTH);
                empty.add(scroll, BorderLayout.CENTER);
                content = empty;
            }
            JOptionPane.showMessageDialog(this, content,
                    "Booking history", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            fail("Could not load booking history.", ex);
        }
    }

    private void addField(JPanel fields, String title, JTextField input) {
        addFieldLabel(fields, title);
        input.setFont(font(14, false));
        input.setForeground(TEXT);
        input.setCaretColor(TEXT);
        input.setBackground(PANEL_LIGHT);
        input.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(8, 10, 8, 10)));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fields.add(input);
        fields.add(Box.createVerticalStrut(15));
    }

    private void addFieldLabel(JPanel fields, String title) {
        JLabel label = label(title, 10, true, MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(label);
        fields.add(Box.createVerticalStrut(7));
    }

    private void addSummaryLine(JPanel panel, String caption, String value) {
        JLabel val = label(value, 12, false, TEXT);
        addSummaryLine(panel, caption, val);
    }

    private void addSummaryLine(JPanel panel, String caption, JLabel val) {
        JPanel line = new JPanel(new BorderLayout(8, 0));
        line.setOpaque(false);
        line.setBorder(new EmptyBorder(11, 0, 11, 0));
        JLabel key = label(caption, 11, true, MUTED);
        val.setFont(font(12, false));
        val.setForeground(TEXT);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        line.add(key, BorderLayout.WEST);
        line.add(val, BorderLayout.CENTER);
        panel.add(line);
    }

    private void addSummaryDivider(JPanel panel) {
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER);
        separator.setBackground(BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(font(13, false));
        combo.setForeground(TEXT);
        combo.setBackground(PANEL_LIGHT);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(font(14, false));
        spinner.setForeground(TEXT);
        spinner.setBackground(PANEL_LIGHT);
        spinner.setBorder(new LineBorder(BORDER, 1));
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
            field.setFont(font(14, false));
            field.setForeground(TEXT);
            field.setBackground(PANEL_LIGHT);
            field.setBorder(new EmptyBorder(5, 9, 5, 9));
        }
    }

    private CardPanel card(LayoutManager layout) {
        return new CardPanel(layout);
    }

    private JPanel sectionTitle(String title, String subtitle) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.add(label(title, 14, true, TEXT));
        block.add(Box.createVerticalStrut(5));
        block.add(label(subtitle, 12, false, MUTED));
        return block;
    }

    private JButton button(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(font(12, true));
        button.setForeground(TEXT);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(8, 12, 8, 12)));
        button.setFocusPainted(false);
        return button;
    }

    private JPanel legendItem(String text, Color color, Color foreground) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setBorder(new LineBorder(BORDER, 1));
        swatch.setPreferredSize(new Dimension(14, 14));
        item.add(swatch);
        item.add(label(text, 11, false, foreground));
        return item;
    }

    private void addGridCell(JPanel grid, Component component, int x, int y, int width, int height) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.insets = new Insets(3, 3, 3, 3);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipadx = Math.max(0, width - component.getPreferredSize().width);
        constraints.ipady = Math.max(0, height - component.getPreferredSize().height);
        grid.add(component, constraints);
    }

    private void addAisleSpacer(JPanel grid, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 6;
        constraints.gridy = row;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.ipadx = 18;
        constraints.ipady = row == 0 ? 20 : 28;
        grid.add(Box.createRigidArea(new Dimension(18, 1)), constraints);
    }

    private int seatGridColumn(int seatColumn) {
        return seatColumn < 5 ? seatColumn + 1 : seatColumn + 2;
    }

    private String rowTierName(int row) {
        return row < 4 ? "STANDARD" : row < 8 ? "PREMIUM" : "RECLINER";
    }

    private JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }

    private Font font(int size, boolean bold) {
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Pikchar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your booking",
                JOptionPane.WARNING_MESSAGE);
    }

    private void fail(String message, Exception ex) {
        JOptionPane.showMessageDialog(this, message + "\n" + ex.getMessage(),
                "Application error", JOptionPane.ERROR_MESSAGE);
    }

    private static class Movie {
        private final String title;
        private final int basePrice;

        private Movie(String title, int basePrice) {
            this.title = title;
            this.basePrice = basePrice;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private static class MovieRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, focused);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(6, 8, 6, 8));
            label.setBackground(selected ? ACCENT : PANEL_LIGHT);
            label.setForeground(selected ? ACCENT_DARK : TEXT);
            return label;
        }
    }

    private static class CardPanel extends JPanel {
        private CardPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(PANEL);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(BORDER);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ScreenPanel extends JPanel {
        private ScreenPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(480, 48));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            int width = Math.min(getWidth() - 24, 680);
            int x = (getWidth() - width) / 2;
            int y = 5;
            int height = 30;
            g.setColor(new Color(67, 70, 72));
            g.fillRect(x, y, width, height);
            g.setColor(BORDER);
            g.drawRect(x, y, width - 1, height - 1);
            g.setFont(new Font("Dialog", Font.BOLD, 11));
            g.setColor(Color.WHITE);
            FontMetrics metrics = g.getFontMetrics();
            String caption = "[  S C R E E N   T H I S   W A Y  ]";
            g.drawString(caption, (getWidth() - metrics.stringWidth(caption)) / 2, 24);
            g.dispose();
        }
    }

    private static class Database {
        private static final String URL = "jdbc:sqlite:pikchar.db";

        private Connection connect() throws SQLException {
            Connection connection = DriverManager.getConnection(URL);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys=ON");
            }
            return connection;
        }

        private void initialize() {
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS bookings ("
                        + "id TEXT PRIMARY KEY, customer TEXT NOT NULL, phone TEXT NOT NULL, "
                        + "movie TEXT NOT NULL, show_time TEXT NOT NULL, seats TEXT NOT NULL, "
                        + "total INTEGER NOT NULL, created_at TEXT NOT NULL)");
                boolean hasLegacySnacksColumn = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(bookings)")) {
                    while (columns.next()) {
                        if ("snacks".equalsIgnoreCase(columns.getString("name"))) {
                            hasLegacySnacksColumn = true;
                            break;
                        }
                    }
                }
                if (hasLegacySnacksColumn) {
                    statement.executeUpdate("ALTER TABLE bookings DROP COLUMN snacks");
                }
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS booked_seats ("
                        + "movie TEXT NOT NULL, show_time TEXT NOT NULL, seat TEXT NOT NULL, "
                        + "booking_id TEXT NOT NULL REFERENCES bookings(id), "
                        + "PRIMARY KEY(movie,show_time,seat))");
            } catch (SQLException ex) {
                throw new IllegalStateException("SQLite initialization failed: " + ex.getMessage(), ex);
            }
        }

        private Set<String> bookedSeats(String movie, String showTime) throws SQLException {
            Set<String> result = new HashSet<>();
            try (Connection connection = connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT seat FROM booked_seats WHERE movie=? AND show_time=?")) {
                statement.setString(1, movie);
                statement.setString(2, showTime);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        result.add(rows.getString(1));
                    }
                }
            }
            return result;
        }

        private void saveBooking(String id, String name, String phone, String movie, String showTime,
                                 List<String> seats, int total) throws SQLException {
            try (Connection connection = connect()) {
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO bookings "
                                    + "(id,customer,phone,movie,show_time,seats,total,created_at) "
                                    + "VALUES(?,?,?,?,?,?,?,?)")) {
                        statement.setString(1, id);
                        statement.setString(2, name);
                        statement.setString(3, phone);
                        statement.setString(4, movie);
                        statement.setString(5, showTime);
                        statement.setString(6, String.join(",", seats));
                        statement.setInt(7, total);
                        statement.setString(8, LocalDateTime.now().toString());
                        statement.executeUpdate();
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO booked_seats VALUES(?,?,?,?)")) {
                        for (String seat : seats) {
                            statement.setString(1, movie);
                            statement.setString(2, showTime);
                            statement.setString(3, seat);
                            statement.setString(4, id);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        }

        private List<Object[]> bookingHistory() throws SQLException {
            List<Object[]> bookings = new ArrayList<>();
            try (Connection connection = connect();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT id,customer,movie,show_time,seats,total FROM bookings ORDER BY created_at DESC")) {
                while (rows.next()) {
                    bookings.add(new Object[]{
                            rows.getString(1), rows.getString(2), rows.getString(3),
                            rows.getString(4), rows.getString(5), "₹" + rows.getInt(6)
                    });
                }
            }
            return bookings;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Could not apply the system look and feel: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new TicketBooking().setVisible(true);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
