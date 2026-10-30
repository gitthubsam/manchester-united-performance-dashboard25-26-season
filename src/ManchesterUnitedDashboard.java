import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manchester unitedperformance dashboard
 *
 * swing view and user interactions.
 * data parsing + statistics and image handling live in separate source files.
 *
 * 2025/26 pl results and player figures only.
 * bundled CSV records were transcribed from verified season-specific sources.
 * no generated/example match or player statistics are used.
 */
public class ManchesterUnitedDashboard extends JFrame {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK);

    private List<Match> allMatches = new ArrayList<>();
    private List<Match> visibleMatches = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private boolean updatingFilters = false;
    private String playerDescription = "2025/26 Premier League · sourced player statistics";
    private String sourceDescription = "2025/26 Premier League · verified season results";
    private static final String SEASON = SeasonData.SEASON;
    private static final String RESULTS_URL = "https://xg-edge.com/premier-league/manchester-united/2025/results";
    private static final String PLAYERS_URL = "https://xg-edge.com/premier-league/manchester-united/2025/squad";
    private boolean importedMatches=false;
    private CardLayout workspaceLayout;
    private JPanel workspaceCards, playerDirectory, playerDetails;
    private JTextField playerSearch;
    private JButton overviewNav, playersNav;
    private Player selectedPlayer;

    private JComboBox<String> seasonBox, competitionBox, venueBox;
    private JTextField searchField;
    private JLabel sourceLabel, playerSourceLabel, countLabel;
    private KpiCard gamesKpi, winsKpi, differenceKpi, winRateKpi;
    private ResultsChart resultsChart;
    private GoalsChart goalsChart;
    private DefaultTableModel matchTableModel;
    private JTable matchTable;
    private JPanel playerList;
    private JLabel homeSummary, awaySummary, goalsSummary;

    public ManchesterUnitedDashboard() {
        super("Manchester United | Matchday insight");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 730));
        setSize(1420, 910);
        setLocationRelativeTo(null);
        allMatches = SeasonData.loadMatches();
        players = SeasonData.loadPlayers();
        setContentPane(createWindow());
        updateFilters();
    }

    private JPanel createWindow() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(DashboardTheme.BG);
        main.add(createSidebar(), BorderLayout.WEST);
        workspaceLayout = new CardLayout();
        workspaceCards = new JPanel(workspaceLayout);
        workspaceCards.setBackground(DashboardTheme.BG);

        JPanel body = new JPanel();
        body.setBackground(DashboardTheme.BG);
        body.setBorder(new EmptyBorder(28, 28, 36, 30));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(createTopBar());
        gap(body, 23);
        body.add(createFilterBar());
        gap(body, 15);
        sourceLabel = label("", 12, DashboardTheme.MUTED, Font.PLAIN);
        JPanel sourceWrap = plain(new BorderLayout());
        sourceWrap.add(sourceLabel, BorderLayout.WEST);
        body.add(sourceWrap);
        gap(body, 18);
        body.add(createKpis());
        gap(body, 21);
        body.add(createChartRow());
        gap(body, 21);
        body.add(createLowerRow());
        gap(body, 18);
        body.add(createInsightRow());
        gap(body, 17);
        JLabel footer = label("BY SAMIUL  •  2025/26 epl ONLY  •  RESULTS: XGEDGE  •  PLAYERS: XGEDGE", 12, DashboardTheme.MUTED, Font.PLAIN);
        body.add(footer);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(DashboardTheme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(23);
        workspaceCards.add(scroll, "overview");
        workspaceCards.add(createPlayersPage(), "players");
        main.add(workspaceCards, BorderLayout.CENTER);
        workspaceLayout.show(workspaceCards, "overview");
        return main;
    }

    private JPanel createSidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(220, 800));
        side.setBackground(DashboardTheme.SIDEBAR);
        side.setBorder(new EmptyBorder(29, 18, 23, 16));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        JLabel icon = new JLabel("",SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(52, 52));
        icon.setOpaque(false);
        DashboardAssets.setCrestIcon(icon, 52);
        JPanel words = new JPanel(); words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label("Man united", 16, DashboardTheme.WHITE, Font.BOLD));
        words.add(label("Analytics 25/26", 10, DashboardTheme.MUTED, Font.BOLD));
        brand.add(icon);brand.add(words);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(188,56));
        side.add(brand);
        side.add(Box.createVerticalStrut(48));
        side.add(label("WORKSPACE", 10, DashboardTheme.MUTED, Font.BOLD));
        side.add(Box.createVerticalStrut(14));
        overviewNav = navButton("●   OVERVIEW", true);
        overviewNav.addActionListener(e -> showWorkspace("overview"));
        side.add(overviewNav);
        side.add(Box.createVerticalStrut(7));
        playersNav = navButton("●   PLAYERS", false);
        playersNav.addActionListener(e -> showWorkspace("players"));
        side.add(playersNav);
        side.add(Box.createVerticalStrut(35));
        side.add(label("YOUR DATA", 10, DashboardTheme.MUTED, Font.BOLD));
        side.add(Box.createVerticalStrut(12));
        side.add(sideButton("Import matches CSV", e -> importMatches()));
        side.add(Box.createVerticalStrut(7));
        side.add(sideButton("Import player stats", e -> importPlayers()));
        side.add(Box.createVerticalStrut(7));
        side.add(sideButton("Reload verified 25/26 data", e -> resetPublished()));
        side.add(Box.createVerticalStrut(7));
        side.add(sideButton("Export filtered CSV", e -> exportMatches()));
        side.add(Box.createVerticalGlue());
        JPanel tip = squarePanel(DashboardTheme.CARD, 16);
        tip.setLayout(new BoxLayout(tip, BoxLayout.Y_AXIS));
        tip.setBorder(new EmptyBorder(16, 13, 16, 13));
        JLabel tipHead = label("tip", 11, DashboardTheme.RED, Font.BOLD);
        tip.add(tipHead);
        tip.add(Box.createVerticalStrut(7));
        JLabel tipBody = html("Season locked to <b>2025/26</b>. No synthetic statistics.", 168, DashboardTheme.MUTED);
        tip.add(tipBody);
        tip.setMaximumSize(new Dimension(186, 115));
        side.add(tip);
        return side;
    }

    private JPanel createTopBar() {
        JPanel bar = plain(new BorderLayout(10, 12));
        JPanel text = plain();text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label("PERFORMANCE OVERVIEW", 11, DashboardTheme.RED, Font.BOLD));
        text.add(Box.createVerticalStrut(6));
        text.add(label("Matchday insights", 29, DashboardTheme.WHITE, Font.BOLD));
        text.add(Box.createVerticalStrut(6));
        text.add(label("Have a look at results, attacking form, and player contributions.", 13, DashboardTheme.MUTED, Font.PLAIN));
        bar.add(text, BorderLayout.CENTER);
        JButton getReal = actionButton("Source: 25/26 results ↗", true);
        getReal.addActionListener(e -> openWeb(RESULTS_URL));
        JPanel buttonWrap = plain(new FlowLayout(FlowLayout.RIGHT, 0, 2));buttonWrap.add(getReal);
        bar.add(buttonWrap, BorderLayout.EAST);
        return bar;
    }

    private JPanel createFilterBar() {
        JPanel outer = squarePanel(DashboardTheme.CARD, 18);
        outer.setLayout(new BorderLayout(13, 0));
        outer.setBorder(new EmptyBorder(15, 18, 15, 18));
        JPanel filters = plain(new FlowLayout(FlowLayout.LEFT, 12, 0));
        seasonBox = dropdown(); competitionBox = dropdown(); venueBox = dropdown();
        seasonBox.setEnabled(false);
        venueBox.addItem("All venues");venueBox.addItem("Home");venueBox.addItem("Away");
        filters.add(filterBlock("SEASON", seasonBox, 126));
        filters.add(filterBlock("COMPETITION", competitionBox, 165));
        filters.add(filterBlock("VENUE", venueBox, 112));
        outer.add(filters, BorderLayout.WEST);
        JPanel search = plain();search.setLayout(new BoxLayout(search, BoxLayout.Y_AXIS));
        search.add(label("SEARCH OPPONENT", 10, DashboardTheme.MUTED, Font.BOLD));
        search.add(Box.createVerticalStrut(5));
        searchField = new JTextField();
        searchField.setToolTipText("Filter by opponent name");
        searchField.setPreferredSize(new Dimension(175, 33));
        searchField.setMaximumSize(new Dimension(300, 33));
        searchField.setBackground(DashboardTheme.INPUT);searchField.setForeground(DashboardTheme.WHITE);
        searchField.setCaretColor(DashboardTheme.WHITE);searchField.setFont(DashboardTheme.uiFont(Font.PLAIN,13));
        searchField.setBorder(new CompoundBorder(new LineBorder(DashboardTheme.BORDER),new EmptyBorder(4,9,4,9)));
        search.add(searchField);
        outer.add(search, BorderLayout.CENTER);
        seasonBox.addActionListener(e -> { if (!updatingFilters) refresh(); });
        competitionBox.addActionListener(e -> { if (!updatingFilters) refresh(); });
        venueBox.addActionListener(e -> { if (!updatingFilters) refresh(); });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        return outer;
    }

    private JPanel createKpis() {
        JPanel row = plain(new GridLayout(1, 4, 14, 0));
        gamesKpi = new KpiCard("MATCHES PLAYED", "0", "Selected fixtures", DashboardTheme.BLUE);
        winsKpi = new KpiCard("WINS", "0", "Matches won", DashboardTheme.GREEN);
        differenceKpi = new KpiCard("GOAL DIFFERENCE", "+0", "Goals for minus against", DashboardTheme.AMBER);
        winRateKpi = new KpiCard("WIN RATE", "0%", "Of selected matches", DashboardTheme.RED);
        row.add(gamesKpi);row.add(winsKpi);row.add(differenceKpi);row.add(winRateKpi);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        return row;
    }

    private JPanel createChartRow() {
        JPanel row = plain(new GridLayout(1, 2, 16, 0));
        resultsChart = new ResultsChart(); goalsChart = new GoalsChart();
        row.add(chartCard("Results by month", "Wins · Draws · Losses", resultsChart));
        row.add(chartCard("Goals trend", "Goals scored vs goals conceded · last 12 games", goalsChart));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 305));
        return row;
    }

    private JPanel chartCard(String title, String sub, JPanel chart) {
        JPanel p = squarePanel(DashboardTheme.CARD, 18);
        p.setLayout(new BorderLayout(0, 9));
        p.setBorder(new EmptyBorder(19, 18, 15, 18));
        JPanel top = plain(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(label(title, 17, DashboardTheme.WHITE, Font.BOLD));
        top.add(Box.createVerticalStrut(3));top.add(label(sub, 11, DashboardTheme.MUTED, Font.PLAIN));
        p.add(top, BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        return p;
    }

    private JPanel createLowerRow() {
        JPanel row = plain(new BorderLayout(16, 0));
        JPanel left = squarePanel(DashboardTheme.CARD, 18);
        left.setLayout(new BorderLayout(0, 14));
        left.setBorder(new EmptyBorder(18, 16, 16, 16));
        JPanel titleRow = plain(new BorderLayout());
        titleRow.add(label("Recent fixtures", 17, DashboardTheme.WHITE, Font.BOLD), BorderLayout.WEST);
        countLabel=label("", 11, DashboardTheme.MUTED, Font.PLAIN);
        titleRow.add(countLabel, BorderLayout.EAST);
        left.add(titleRow, BorderLayout.NORTH);
        String[] cols={"DATE", "OPPONENT", "VENUE", "SCORE", "RESULT", "COMPETITION"};
        matchTableModel=new DefaultTableModel(cols,0) {
            @Override public boolean isCellEditable(int row,int col){return false;}
        };
        matchTable=new JTable(matchTableModel);
        matchTable.setRowHeight(33);
        matchTable.setBackground(DashboardTheme.CARD);matchTable.setForeground(DashboardTheme.WHITE);
        matchTable.setSelectionBackground(DashboardTheme.INPUT);
        matchTable.setSelectionForeground(DashboardTheme.WHITE);
        matchTable.setFont(DashboardTheme.uiFont(Font.PLAIN,12));
        matchTable.setShowGrid(false);
        matchTable.setIntercellSpacing(new Dimension(0,0));
        matchTable.setFillsViewportHeight(true);
        matchTable.setAutoCreateRowSorter(true);
        matchTable.getTableHeader().setBackground(DashboardTheme.INPUT);
        matchTable.getTableHeader().setForeground(DashboardTheme.MUTED);
        matchTable.getTableHeader().setFont(DashboardTheme.uiFont(Font.BOLD,10));
        matchTable.getTableHeader().setPreferredSize(new Dimension(10,38));
        matchTable.getColumnModel().getColumn(1).setPreferredWidth(133);
        matchTable.getColumnModel().getColumn(5).setPreferredWidth(104);
        matchTable.getColumnModel().getColumn(4).setMaxWidth(75);
        matchTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                setBorder(new EmptyBorder(0,7,0,7));
                setBackground(selected?DashboardTheme.INPUT:DashboardTheme.CARD);
                setForeground(col==4?("W".equals(value)?DashboardTheme.GREEN:("D".equals(value)?DashboardTheme.AMBER:DashboardTheme.RED)):DashboardTheme.WHITE);
                if(col==4) setHorizontalAlignment(SwingConstants.CENTER);
                else setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        });
        JScrollPane tableScroll=new JScrollPane(matchTable);
        tableScroll.setBorder(new LineBorder(DashboardTheme.BORDER));
        tableScroll.getViewport().setBackground(DashboardTheme.CARD);
        left.add(tableScroll,BorderLayout.CENTER);
        row.add(left, BorderLayout.CENTER);

        JPanel right=squarePanel(DashboardTheme.CARD,18);
        right.setLayout(new BorderLayout(0,12));
        right.setBorder(new EmptyBorder(18,16,14,16));
        right.setPreferredSize(new Dimension(304,310));
        JPanel rightTitle=plain();rightTitle.setLayout(new BoxLayout(rightTitle,BoxLayout.Y_AXIS));
        rightTitle.add(label("Top performers",17,DashboardTheme.WHITE,Font.BOLD));
        rightTitle.add(Box.createVerticalStrut(4));
        playerSourceLabel=label("",11,DashboardTheme.MUTED,Font.PLAIN);
        rightTitle.add(playerSourceLabel);
        right.add(rightTitle,BorderLayout.NORTH);
        playerList=plain();playerList.setLayout(new BoxLayout(playerList,BoxLayout.Y_AXIS));
        JScrollPane playerScroll=new JScrollPane(playerList);
        playerScroll.setBorder(null);playerScroll.getViewport().setBackground(DashboardTheme.CARD);
        playerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        playerScroll.getVerticalScrollBar().setUnitIncrement(14);
        right.add(playerScroll,BorderLayout.CENTER);
        row.add(right,BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,345));
        return row;
    }

    private JPanel createInsightRow() {
        JPanel outer=squarePanel(DashboardTheme.CARD,18);
        outer.setLayout(new GridLayout(1,3,10,0));
        outer.setBorder(new EmptyBorder(15,17,15,17));
        homeSummary=label("",13,DashboardTheme.WHITE,Font.BOLD);
        awaySummary=label("",13,DashboardTheme.WHITE,Font.BOLD);
        goalsSummary=label("",13,DashboardTheme.WHITE,Font.BOLD);
        outer.add(smallSummary("HOME WIN RATE",homeSummary));
        outer.add(smallSummary("AWAY WIN RATE",awaySummary));
        outer.add(smallSummary("AVG GOALS SCORED",goalsSummary));
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE,73));
        return outer;
    }

    private JPanel smallSummary(String title,JLabel value){
        JPanel p=plain();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(label(title,10,DashboardTheme.MUTED,Font.BOLD));p.add(Box.createVerticalStrut(6));p.add(value);return p;
    }

    private void updateFilters(){
        updatingFilters=true;
        String existingSeason=selected(seasonBox), existingCompetition=selected(competitionBox);
        seasonBox.removeAllItems();seasonBox.addItem(SEASON);
        if(existingSeason!=null && comboContains(seasonBox,existingSeason)) seasonBox.setSelectedItem(existingSeason);
        if(seasonBox.getSelectedIndex()<0)seasonBox.setSelectedIndex(0);
        competitionBox.removeAllItems();competitionBox.addItem("All competitions");
        allMatches.stream().map(m->m.competition).distinct().sorted().forEach(competitionBox::addItem);
        if(existingCompetition!=null && comboContains(competitionBox,existingCompetition))competitionBox.setSelectedItem(existingCompetition);
        if(competitionBox.getSelectedIndex()<0)competitionBox.setSelectedIndex(0);
        updatingFilters=false;
        refresh();
    }

    private void refresh(){
        if(seasonBox==null||updatingFilters)return;
        String s=selected(seasonBox),c=selected(competitionBox),v=selected(venueBox);
        String q=searchField.getText().trim().toLowerCase(Locale.ROOT);
        visibleMatches = SeasonStats.filter(allMatches, s, c, v, q);
        SeasonStats stats = SeasonStats.from(visibleMatches);
        int count = stats.matches;
        long wins = stats.wins;
        int gf = stats.goalsFor;
        int ga = stats.goalsAgainst;
        gamesKpi.setValue(""+count);
        winsKpi.setValue(""+wins);
        differenceKpi.setValue(String.format("%+d",gf-ga));
        winRateKpi.setValue(count==0?"—":Math.round(100.0*wins/count)+"%");
        sourceLabel.setText((importedMatches?"● IMPORTED 2025/26  |  ":"● VERIFIED 2025/26  |  ")+sourceDescription);
        sourceLabel.setForeground(importedMatches?DashboardTheme.AMBER:DashboardTheme.GREEN);
        resultsChart.setMatches(visibleMatches);
        goalsChart.setMatches(visibleMatches);
        matchTableModel.setRowCount(0);
        List<Match> desc=new ArrayList<>(visibleMatches); Collections.reverse(desc);
        for(Match m:desc){
            matchTableModel.addRow(new Object[]{m.date.format(DISPLAY_DATE),m.opponent,m.venue,
                m.goalsFor+" - "+m.goalsAgainst,""+m.result(),m.competition});
        }
        countLabel.setText(count+" matches");
        refreshPlayers(s);
        homeSummary.setText(formatVenueRate("Home"));
        awaySummary.setText(formatVenueRate("Away"));
        goalsSummary.setText(count==0?"—":String.format(Locale.UK,"%.2f per match",gf*1.0/count));
    }

    private String formatVenueRate(String venue) {
        return SeasonStats.venueWinRate(visibleMatches, venue);
    }

    private void refreshPlayers(String chosenSeason){
        playerList.removeAll();
        List<Player> current=players.stream()
            .filter(p->chosenSeason==null||chosenSeason.equals("All seasons")||p.season.equals(chosenSeason)||p.season.equals("All"))
            .sorted(Comparator.comparingInt((Player p)->p.goals).reversed().thenComparingInt(p->-p.assists))
            .collect(Collectors.toList());
        playerSourceLabel.setText(playerDescription);
        if(current.isEmpty()){
            JLabel msg=html("Player stats aren't supplied with league match CSVs.<br><br><b>Import player stats CSV</b> to add your own.",270,DashboardTheme.MUTED);
            playerList.add(msg);
        }else{
            int max=Math.max(1,current.stream().mapToInt(p->p.goals).max().orElse(1));
            for(Player p:current.stream().limit(8).collect(Collectors.toList())){
                JPanel item=plain();item.setLayout(new BoxLayout(item,BoxLayout.Y_AXIS));
                JPanel info=plain(new BorderLayout());
                info.add(label(p.name,12,DashboardTheme.WHITE,Font.BOLD),BorderLayout.WEST);
                info.add(label(p.goals+" G  ·  "+p.assists+" A",11,DashboardTheme.MUTED,Font.PLAIN),BorderLayout.EAST);
                item.add(info);item.add(Box.createVerticalStrut(7));
                JComponent bar=new JComponent(){
                    @Override protected void paintComponent(Graphics g){
                        Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(DashboardTheme.INPUT);g2.fillRect(0,0,getWidth(),7);
                        g2.setColor(DashboardTheme.RED);g2.fillRect(0,0,(int)(getWidth()*((double)p.goals/max)),7);g2.dispose();
                    }
                };bar.setPreferredSize(new Dimension(250,7));bar.setMaximumSize(new Dimension(Integer.MAX_VALUE,7));
                item.add(bar);item.add(Box.createVerticalStrut(15));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE,59));
                playerList.add(item);
            }
        }
        playerList.revalidate();playerList.repaint();
    }

    private void resetPublished(){
        allMatches=SeasonData.loadMatches();players=SeasonData.loadPlayers();importedMatches=false;
        sourceDescription="38 Premier League results · xgEdge · 2025/26";
        playerDescription="xgEdge · 2025/26 PL season";
        searchField.setText(""); updateFilters(); updatePlayersPage();
    }

    private void importMatches(){
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Select match results CSV");
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try{
            List<Match> imported=SeasonData.parseMatches(Files.readAllLines(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8));
            if(imported.isEmpty())throw new IOException("No Manchester United results found. Check the CSV format.");
            SeasonData.requireSeason(imported.stream().map(m -> m.season).collect(Collectors.toList()));
            allMatches=imported;importedMatches=true;
            playerDescription="2025/26 · player stats still sourced separately";
            sourceDescription=chooser.getSelectedFile().getName()+" · "+allMatches.size()+" fixtures";
            searchField.setText("");updateFilters();
        }catch(Exception ex){showError("Couldn't import matches",ex);}
    }

    private void importPlayers(){
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Select player statistics CSV");
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try{
            List<Player> imported=SeasonData.parsePlayers(Files.readAllLines(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8));
            if(imported.isEmpty())throw new IOException("No player rows found.");
            SeasonData.requireSeason(imported.stream().map(p -> p.season).collect(Collectors.toList()));
            players=imported;playerDescription="2025/26 · "+chooser.getSelectedFile().getName();refresh();updatePlayersPage();
        }catch(Exception ex){showError("Couldn't import player data",ex);}
    }

    private void fetchRealSeason(){
        // Optional refresh of this one season only, from the provider's public CSV.
        final String url="https://www.football-data.co.uk/mmz4281/2526/E0.csv";
        new SwingWorker<List<Match>,Void>(){
            protected List<Match> doInBackground() throws Exception {
                HttpClient client=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(15)).GET().build();
                HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()!=200)throw new IOException("Provider returned HTTP "+response.statusCode());
                List<Match> matches=SeasonData.parseMatches(Arrays.asList(response.body().split("\\R")));
                SeasonData.requireSeason(matches.stream().map(m->m.season).collect(Collectors.toList()));
                return matches;
            }
            protected void done(){
                try { List<Match> m=get();if(m.isEmpty())throw new IOException("No 2025/26 results found.");
                    allMatches=m;importedMatches=false;sourceDescription="football-data.co.uk · 2025/26 PL";updateFilters();
                }catch(Exception e){showError("2025/26 refresh failed",e.getCause()!=null?e.getCause():e);}
            }
        }.execute();
    }

    private void exportMatches(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("manchester-united-filtered.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(BufferedWriter out=Files.newBufferedWriter(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8)){
            out.write("Date,Season,Competition,Opponent,Venue,GoalsFor,GoalsAgainst,Result\n");
            for(Match m:visibleMatches){
                out.write(String.join(",", SeasonData.csv(m.date.toString()),SeasonData.csv(m.season),SeasonData.csv(m.competition),SeasonData.csv(m.opponent),SeasonData.csv(m.venue),
                    ""+m.goalsFor,""+m.goalsAgainst,""+m.result())+"\n");
            }
            JOptionPane.showMessageDialog(this,"Saved "+visibleMatches.size()+" matches to CSV.","Export complete",JOptionPane.INFORMATION_MESSAGE);
        }catch(IOException ex){showError("Couldn't export file",ex);}
    }
    private void showError(String title,Throwable ex){
        JOptionPane.showMessageDialog(this,ex.getMessage()+"\n\nYou can still import a downloaded CSV from your computer.",title,JOptionPane.ERROR_MESSAGE);
    }

    private JButton navButton(String title,boolean active){
        JButton b=new JButton(title);
        b.setMaximumSize(new Dimension(190,45));
        b.setPreferredSize(new Dimension(190,45));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(DashboardTheme.uiFont(Font.BOLD,18));
        b.setForeground(active?DashboardTheme.WHITE:DashboardTheme.MUTED);
        b.setBackground(active?new Color(69,35,48):DashboardTheme.SIDEBAR);
        b.setOpaque(true);b.setFocusPainted(false);b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(9,14,9,7));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private void showWorkspace(String name){
        workspaceLayout.show(workspaceCards,name);
        for(JButton b:new JButton[]{overviewNav,playersNav}){
            boolean isActive=(b==overviewNav && name.equals("overview"))||(b==playersNav&&name.equals("players"));
            b.setBackground(isActive?new Color(69,35,48):DashboardTheme.SIDEBAR);
            b.setForeground(isActive?DashboardTheme.WHITE:DashboardTheme.MUTED);
        }
    }
    private JPanel createPlayersPage(){
        JPanel page=plain(new BorderLayout(18,18));
        page.setBackground(DashboardTheme.BG); page.setOpaque(true);
        page.setBorder(new EmptyBorder(28,26,26,26));
        JPanel header=plain();header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
        header.add(label("THE 2025/26 SQUAD",14,DashboardTheme.RED,Font.BOLD));
        header.add(Box.createVerticalStrut(6));
        header.add(label("Player Intelligence",34,DashboardTheme.WHITE,Font.BOLD));
        header.add(Box.createVerticalStrut(7));
        header.add(label("2025/26 Premier League  /  sourced player statistics",16,DashboardTheme.MUTED,Font.PLAIN));
        JPanel top=plain(new BorderLayout());top.add(header,BorderLayout.WEST);
        JButton source=actionButton("VIEW STATS SOURCE ↗",true);
        source.addActionListener(e->openWeb(PLAYERS_URL));
        JPanel sourceWrap=plain(new FlowLayout(FlowLayout.RIGHT,0,0));sourceWrap.add(source);
        top.add(sourceWrap,BorderLayout.EAST);
        page.add(top,BorderLayout.NORTH);

        JPanel content=plain(new BorderLayout(16,0));
        JPanel left=squarePanel(DashboardTheme.CARD,0);left.setLayout(new BorderLayout(0,10));
        left.setPreferredSize(new Dimension(360,650));
        left.setBorder(new EmptyBorder(17,14,15,14));
        JPanel search=plain();search.setLayout(new BoxLayout(search,BoxLayout.Y_AXIS));
        search.add(label("choose a player",17,DashboardTheme.WHITE,Font.BOLD));
        search.add(Box.createVerticalStrut(11));
        playerSearch=new JTextField();playerSearch.setBackground(DashboardTheme.INPUT);playerSearch.setForeground(DashboardTheme.WHITE);
        playerSearch.setCaretColor(DashboardTheme.WHITE);playerSearch.setBorder(new CompoundBorder(new LineBorder(DashboardTheme.BORDER),new EmptyBorder(7,10,7,10)));
        playerSearch.setFont(DashboardTheme.uiFont(Font.PLAIN,17));playerSearch.setPreferredSize(new Dimension(320,39));
        search.add(playerSearch);left.add(search,BorderLayout.NORTH);
        playerDirectory=plain();playerDirectory.setLayout(new BoxLayout(playerDirectory,BoxLayout.Y_AXIS));
        JScrollPane scroller=new JScrollPane(playerDirectory);
        scroller.setBorder(null);scroller.getViewport().setBackground(DashboardTheme.CARD);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(20);
        left.add(scroller,BorderLayout.CENTER);
        content.add(left,BorderLayout.WEST);
        playerDetails=squarePanel(DashboardTheme.CARD,0);playerDetails.setLayout(new BorderLayout());
        playerDetails.setBorder(new EmptyBorder(28,30,24,30));
        content.add(playerDetails,BorderLayout.CENTER);
        page.add(content,BorderLayout.CENTER);
        JLabel notice=label("PLAYER PHOTOS LOAD FROM PUBLIC ONLINE PROFILE IMAGES · SOME MAY BE UNAVAILABLE · THIS IS AN UNOFFICIAL FAN PROJECT",12,DashboardTheme.MUTED,Font.PLAIN);
        page.add(notice,BorderLayout.SOUTH);
        playerSearch.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){populatePlayerDirectory();}
            public void removeUpdate(DocumentEvent e){populatePlayerDirectory();}
            public void changedUpdate(DocumentEvent e){populatePlayerDirectory();}
        });
        if(!players.isEmpty())selectedPlayer=players.stream().filter(p->p.name.equals("Bruno Fernandes")).findFirst().orElse(players.get(0));
        updatePlayersPage();
        return page;
    }
    private void updatePlayersPage(){
        if(playerDirectory==null)return;
        if(selectedPlayer==null || players.stream().noneMatch(p->p.name.equals(selectedPlayer.name))){
            selectedPlayer=players.isEmpty()?null:players.get(0);
        }
        updatePlayerDetails();
        populatePlayerDirectory();
    }
    private void populatePlayerDirectory(){
        if(playerDirectory==null)return;
        playerDirectory.removeAll();
        String search=playerSearch.getText().trim().toLowerCase(Locale.ROOT);
        List<Player> options=players.stream().filter(p->p.name.toLowerCase(Locale.ROOT).contains(search))
            .sorted(Comparator.comparingInt((Player p)->p.goals+p.assists).reversed().thenComparing(p->p.name)).collect(Collectors.toList());
        if(options.isEmpty())playerDirectory.add(label("NO MATCHING 2025/26 PLAYERS",16,DashboardTheme.MUTED,Font.PLAIN));
        for(Player p:options){
            boolean current=selectedPlayer!=null&&selectedPlayer.name.equals(p.name);
            JPanel item=new JPanel(new BorderLayout(12,0));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE,76));
            item.setPreferredSize(new Dimension(310,76));
            item.setOpaque(true);item.setBackground(current?new Color(66,38,49):DashboardTheme.CARD);
            item.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,DashboardTheme.BORDER),new EmptyBorder(6,7,6,7)));
            JLabel photo=new JLabel("PHOTO",SwingConstants.CENTER);
            photo.setPreferredSize(new Dimension(54,64));photo.setOpaque(true);photo.setBackground(DashboardTheme.INPUT);
            photo.setForeground(DashboardTheme.MUTED);photo.setFont(DashboardTheme.uiFont(Font.BOLD,13));
            DashboardAssets.loadPlayerPhoto(p.name,photo,54,64);
            item.add(photo,BorderLayout.WEST);
            JPanel words=plain();words.setLayout(new BoxLayout(words,BoxLayout.Y_AXIS));
            words.add(Box.createVerticalStrut(9));
            words.add(label(p.name.toUpperCase(Locale.ROOT),19,DashboardTheme.WHITE,Font.BOLD));
            words.add(Box.createVerticalStrut(3));
            words.add(label(p.position.toUpperCase(Locale.ROOT)+"  /  "+p.goals+" GOALS  /  "+p.assists+" ASSISTS",12,DashboardTheme.MUTED,Font.PLAIN));
            item.add(words,BorderLayout.CENTER);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter pick=new MouseAdapter(){public void mouseClicked(MouseEvent e){selectedPlayer=p;updatePlayersPage();}};
            item.addMouseListener(pick);
            for(Component part:new Component[]{photo,words})part.addMouseListener(pick);
            for(Component child:words.getComponents())child.addMouseListener(pick);
            playerDirectory.add(item);
        }
        playerDirectory.revalidate();playerDirectory.repaint();
    }
    private void updatePlayerDetails(){
        if(playerDetails==null)return;
        playerDetails.removeAll();
        Player p=selectedPlayer;
        if(p==null){playerDetails.add(label("NO VERIFIED PLAYER RECORDS",18,DashboardTheme.MUTED,Font.PLAIN));return;}
        JPanel detail=plain();detail.setLayout(new BoxLayout(detail,BoxLayout.Y_AXIS));
        detail.add(label("2025/26  /  PREMIER LEAGUE",14,DashboardTheme.RED,Font.BOLD));
        detail.add(Box.createVerticalStrut(17));
        JPanel hero=plain(new BorderLayout(24,0));
        JLabel photo=new JLabel("PLAYER IMAGE",SwingConstants.CENTER);photo.setOpaque(true);photo.setBackground(DashboardTheme.INPUT);
        photo.setForeground(DashboardTheme.MUTED);photo.setFont(DashboardTheme.uiFont(Font.PLAIN,18));
        photo.setPreferredSize(new Dimension(165,192));
        DashboardAssets.loadPlayerPhoto(p.name,photo,165,192);
        hero.add(photo,BorderLayout.WEST);
        JPanel heading=plain();heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS));
        heading.add(Box.createVerticalStrut(10));
        heading.add(label(p.name.toUpperCase(Locale.ROOT),34,DashboardTheme.WHITE,Font.BOLD));
        heading.add(Box.createVerticalStrut(8));
        heading.add(label(p.position.toUpperCase(Locale.ROOT),17,DashboardTheme.MUTED,Font.PLAIN));
        heading.add(Box.createVerticalStrut(20));
        heading.add(label("2025/26 PREMIER LEAGUE PERFORMANCE",15,DashboardTheme.RED,Font.BOLD));
        heading.add(Box.createVerticalStrut(6));
        heading.add(label(playerDescription.toUpperCase(Locale.ROOT),13,DashboardTheme.MUTED,Font.PLAIN));
        hero.add(heading,BorderLayout.CENTER);
        detail.add(hero);detail.add(Box.createVerticalStrut(24));
        JPanel metrics=plain(new GridLayout(2,3,11,11));
        metrics.add(playerMetric("APPEARANCES",Integer.toString(p.appearances)));
        metrics.add(playerMetric("STARTS",p.starts<0?"—":Integer.toString(p.starts)));
        metrics.add(playerMetric("MINUTES",p.minutes<0?"—":String.format(Locale.UK,"%,d",p.minutes)));
        metrics.add(playerMetric("GOALS",Integer.toString(p.goals)));
        metrics.add(playerMetric("ASSISTS",Integer.toString(p.assists)));
        metrics.add(playerMetric("GOAL CONTRIBUTIONS",Integer.toString(p.goals+p.assists)));
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE,240));
        detail.add(metrics);detail.add(Box.createVerticalStrut(25));
        JPanel more=squarePanel(DashboardTheme.INPUT,0);more.setLayout(new GridLayout(1,4,10,0));
        more.setBorder(new EmptyBorder(18,16,16,14));
        more.add(smallSummary("EXPECTED GOALS (xG)",label(Double.isNaN(p.xg)?"—":String.format(Locale.UK,"%.2f",p.xg),21,DashboardTheme.WHITE,Font.BOLD)));
        more.add(smallSummary("EXPECTED ASSISTS (xA)",label(Double.isNaN(p.xa)?"—":String.format(Locale.UK,"%.2f",p.xa),21,DashboardTheme.WHITE,Font.BOLD)));
        more.add(smallSummary("GOALS / 90",label(p.minutes<=0?"—":String.format(Locale.UK,"%.2f",p.goals*90.0/p.minutes),21,DashboardTheme.WHITE,Font.BOLD)));
        more.add(smallSummary("ASSISTS / 90",label(p.minutes<=0?"—":String.format(Locale.UK,"%.2f",p.assists*90.0/p.minutes),21,DashboardTheme.WHITE,Font.BOLD)));
        more.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));
        detail.add(more);detail.add(Box.createVerticalStrut(20));
        detail.add(label("ALL FIGURES APPLY TO THE 2025/26 PREMIER LEAGUE SEASON ONLY.",13,DashboardTheme.MUTED,Font.PLAIN));
        playerDetails.add(detail,BorderLayout.NORTH);
        playerDetails.revalidate();playerDetails.repaint();
    }
    private JPanel playerMetric(String labelText,String value){
        JPanel cell=squarePanel(DashboardTheme.INPUT,0);cell.setLayout(new BorderLayout(0,8));
        cell.setBorder(new EmptyBorder(15,17,14,10));
        cell.add(label(labelText,13,DashboardTheme.MUTED,Font.BOLD),BorderLayout.NORTH);
        cell.add(label(value,33,DashboardTheme.WHITE,Font.BOLD),BorderLayout.CENTER);
        return cell;
    }
    private static void openWeb(String uri){
        try{if(Desktop.isDesktopSupported())Desktop.getDesktop().browse(URI.create(uri));}
        catch(Exception ignored){}
    }
    private class KpiCard extends JPanel {
        JLabel num;
        KpiCard(String caption,String value,String sub,Color accent){
            setOpaque(false);setLayout(new BorderLayout(0,4));
            setBorder(new EmptyBorder(18,19,17,14));
            JPanel top=plain(new BorderLayout());
            top.add(label(caption,10,DashboardTheme.MUTED,Font.BOLD),BorderLayout.CENTER);
            JLabel dot=label("●",14,accent,Font.BOLD);top.add(dot,BorderLayout.EAST);
            add(top,BorderLayout.NORTH);
            num=label(value,31,DashboardTheme.WHITE,Font.BOLD);add(num,BorderLayout.CENTER);
            add(label(sub,11,DashboardTheme.MUTED,Font.PLAIN),BorderLayout.SOUTH);
        }
        void setValue(String val){num.setText(val);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DashboardTheme.CARD);g2.fillRect(0,0,getWidth(),getHeight());g2.dispose();
        }
    }
    private static JPanel plain(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    private static JPanel plain(LayoutManager layout){JPanel p=new JPanel(layout);p.setOpaque(false);return p;}
    private static JPanel squarePanel(Color bg,int unusedRadius){
        JPanel p=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);g2.fillRect(0,0,getWidth(),getHeight());g2.dispose();super.paintComponent(g);
            }
        };p.setOpaque(false);return p;
    }
    private static JLabel label(String text,int size,Color color,int weight){
        JLabel l=new JLabel(text);l.setFont(DashboardTheme.uiFont(weight,size));l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;
    }
    private static JLabel html(String msg,int width,Color color){
        JLabel l=new JLabel("<html><div style='width:"+width+"px'>"+msg+"</div></html>");
        l.setForeground(color);l.setFont(DashboardTheme.uiFont(Font.PLAIN,12));return l;
    }
    private static void gap(JPanel panel,int px){panel.add(Box.createRigidArea(new Dimension(0,px)));}
    private static JComboBox<String> dropdown(){
        JComboBox<String> combo=new JComboBox<>();
        combo.setPreferredSize(new Dimension(140,33));combo.setBackground(DashboardTheme.INPUT);combo.setForeground(DashboardTheme.WHITE);
        combo.setFont(DashboardTheme.uiFont(Font.PLAIN,12));combo.setFocusable(false);
        combo.setBorder(new LineBorder(DashboardTheme.BORDER));return combo;
    }
    private static JPanel filterBlock(String title,JComboBox<String> combo,int width){
        JPanel p=plain();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(label(title,10,DashboardTheme.MUTED,Font.BOLD));p.add(Box.createVerticalStrut(5));
        combo.setPreferredSize(new Dimension(width,33));combo.setMaximumSize(new Dimension(width,33));
        p.add(combo);return p;
    }
    private static JButton actionButton(String text,boolean primary){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D gg=(Graphics2D)g.create();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                gg.setColor(primary?DashboardTheme.RED:DashboardTheme.INPUT);gg.fillRect(0,0,getWidth(),getHeight());gg.dispose();super.paintComponent(g);
            }
        };
        b.setFont(DashboardTheme.uiFont(Font.BOLD,12));b.setForeground(DashboardTheme.WHITE);
        b.setPreferredSize(new Dimension(185,39));b.setContentAreaFilled(false);
        b.setFocusPainted(false);b.setBorderPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    private static JButton sideButton(String text,ActionListener click){
        JButton b=new JButton(text);b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(192,39));b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(DashboardTheme.uiFont(Font.PLAIN,12));b.setForeground(DashboardTheme.MUTED);
        b.setContentAreaFilled(false);b.setFocusPainted(false);b.setBorder(new EmptyBorder(9,10,9,3));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(click);return b;
    }
    private static boolean comboContains(JComboBox<String> combo,String value){
        for(int i=0;i<combo.getItemCount();i++)if(value.equals(combo.getItemAt(i)))return true;
        return false;
    }
    private static String selected(JComboBox<String> combo){return combo==null?null:(String)combo.getSelectedItem();}

    public static void main(String[] args) {
        // A first-use font download may take a few seconds; never perform it on Swing's UI thread.
        DashboardAssets.initialiseFonts();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing appearance is fine if the platform theme is unavailable.
            }
            DashboardAssets.applySwingDefaults();
            new ManchesterUnitedDashboard().setVisible(true);
        });
    }
}
