package ltm.spsa_tracker.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultFormatter;

import java.awt.Component;

import ltm.spsa_tracker.backend.*;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;

import java.awt.Label;
import javax.swing.JLabel;

public class Window {

	private JFrame frmSpsaTracker;
	private JTable table;
	private TableRowSorter<?> sorter;
	private JScrollPane scrollPane;
	private JComboBox comboBoxURL;
	private JSpinner spinnerTestID;
	private JSpinner spinnerDelay;
	private JMenuBar menuBar;

	private Timer scrapeTimer;
	public Scraper scraper;

	private final int SCRAPE_DELAY = 5000;
	
	private final String CFG_FILE = "last.cfg";
	private final String PROPERTY_URL = "instance.url";
	private final String PROPERTY_ID = "instance.testid";
	private final String PROPERTY_DELAY = "instance.delay";
	
	private boolean formLoaded = false;
	private ParameterSet lastParameterSet;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Window window = new Window();
					window.frmSpsaTracker.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Window() {
		initialize();
	}

	private void initialize() {

		scrapeTimer = new Timer(SCRAPE_DELAY, new ScrapeAction());
		scrapeTimer.start();

		frmSpsaTracker = new JFrame();
		frmSpsaTracker.setTitle("SPSA Tracker");
		frmSpsaTracker.setResizable(false);
		frmSpsaTracker.setBounds(100, 100, 710, 600);
		frmSpsaTracker.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SpringLayout springLayout = new SpringLayout();
		frmSpsaTracker.getContentPane().setLayout(springLayout);

		scrollPane = new JScrollPane();
		springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -42, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, frmSpsaTracker.getContentPane());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		frmSpsaTracker.getContentPane().add(scrollPane);

		table = new JTable();
		table.setFillsViewportHeight(true);
		scrollPane.setViewportView(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		comboBoxURL = new JComboBox(InstanceList.getInstanceList());
		springLayout.putConstraint(SpringLayout.WEST, comboBoxURL, 93, SpringLayout.WEST, frmSpsaTracker.getContentPane());
		comboBoxURL.addItemListener(new InstanceChangeListener());
		frmSpsaTracker.getContentPane().add(comboBoxURL);

		spinnerTestID = new JSpinner();
		springLayout.putConstraint(SpringLayout.NORTH, comboBoxURL, 1, SpringLayout.NORTH, spinnerTestID);
		springLayout.putConstraint(SpringLayout.NORTH, spinnerTestID, 507, SpringLayout.NORTH, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, spinnerTestID, -73, SpringLayout.EAST, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, spinnerTestID, -10, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		SpinnerModel idModel = new SpinnerNumberModel(0, 0, 100000, 1);
		spinnerTestID.setModel(idModel);

		NumberEditor editor = new JSpinner.NumberEditor(spinnerTestID, "#");
		spinnerTestID.setEditor(editor);
		((DefaultFormatter) (editor.getTextField()).getFormatter()).setCommitsOnValidEdit(true);
		spinnerTestID.addChangeListener(new IDChangeListener());
		springLayout.putConstraint(SpringLayout.EAST, spinnerTestID, -10, SpringLayout.EAST, frmSpsaTracker.getContentPane());
		frmSpsaTracker.getContentPane().add(spinnerTestID);

		Label labelInstance = new Label("Instance URL");
		springLayout.putConstraint(SpringLayout.EAST, labelInstance, -6, SpringLayout.WEST, comboBoxURL);
		springLayout.putConstraint(SpringLayout.WEST, labelInstance, 10, SpringLayout.WEST, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, labelInstance, -10, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		frmSpsaTracker.getContentPane().add(labelInstance);

		JLabel labelTestID = new JLabel("Test ID");
		springLayout.putConstraint(SpringLayout.NORTH, labelTestID, -31, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, labelTestID, -10, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, labelTestID, -6, SpringLayout.WEST, spinnerTestID);
		frmSpsaTracker.getContentPane().add(labelTestID);

		JLabel labelDelay = new JLabel("Delay");
		springLayout.putConstraint(SpringLayout.WEST, labelDelay, 467, SpringLayout.WEST, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, comboBoxURL, -22, SpringLayout.WEST, labelDelay);
		springLayout.putConstraint(SpringLayout.NORTH, labelDelay, 11, SpringLayout.SOUTH, scrollPane);
		springLayout.putConstraint(SpringLayout.SOUTH, labelDelay, -10, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		frmSpsaTracker.getContentPane().add(labelDelay);

		spinnerDelay = new JSpinner();
		springLayout.putConstraint(SpringLayout.EAST, labelDelay, -6, SpringLayout.WEST, spinnerDelay);
		springLayout.putConstraint(SpringLayout.NORTH, spinnerDelay, 11, SpringLayout.SOUTH, scrollPane);
		springLayout.putConstraint(SpringLayout.SOUTH, spinnerDelay, -10, SpringLayout.SOUTH, frmSpsaTracker.getContentPane());
		spinnerDelay.setModel(new SpinnerNumberModel(5, 1, 600, 1));
		springLayout.putConstraint(SpringLayout.WEST, labelTestID, 18, SpringLayout.EAST, spinnerDelay);
		springLayout.putConstraint(SpringLayout.WEST, spinnerDelay, 510, SpringLayout.WEST, frmSpsaTracker.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, spinnerDelay, -137, SpringLayout.EAST, frmSpsaTracker.getContentPane());
		spinnerDelay.addChangeListener(new DelayChangeListener());
		frmSpsaTracker.getContentPane().add(spinnerDelay);

		JMenu fileMenu = new JMenu("File");
		JMenu actionMenu = new JMenu("Actions");

		menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(actionMenu);
		frmSpsaTracker.setJMenuBar(menuBar);

		JMenuItem menuSaveButton = new JMenuItem("Save to file");
		menuSaveButton.addActionListener(new SaveAction());
		fileMenu.add(menuSaveButton);

		JMenuItem menuLoadButton = new JMenuItem("Load from file");
		menuLoadButton.addActionListener(new LoadAction());
		fileMenu.add(menuLoadButton);

		JMenuItem menuScrapeButton = new JMenuItem("Scrape now");
		menuScrapeButton.addActionListener(new ScrapeAction());
		actionMenu.add(menuScrapeButton);

		JMenuItem menuGraphButton = new JMenuItem("Open graph");
		menuGraphButton.addActionListener(new GraphAction());
		actionMenu.add(menuGraphButton);

		tryLoadLastCfg();

		formLoaded = true;
		reinitializeScraper();
	}

	private void tryLoadLastCfg() {
		Properties prop = new Properties();
		try (FileInputStream input = new FileInputStream(CFG_FILE)) {
			prop.load(input);
			String url = prop.getProperty(PROPERTY_URL);
			String id = prop.getProperty(PROPERTY_ID);
			String delay = prop.getProperty(PROPERTY_DELAY);
			System.out.println("Loaded " + url + " " + id + " " + delay);

			for (int i = 0; i < comboBoxURL.getItemCount(); i++) {
				Object o = comboBoxURL.getItemAt(i);
				if (url.endsWith(o.toString())) {
					comboBoxURL.setSelectedIndex(i);
					break;
				}
			}

			if (id != null) {
				spinnerTestID.setValue(Integer.parseInt(id));
			}
			
			if (delay != null) {
				spinnerDelay.setValue(Integer.parseInt(delay));
			}

		} catch (NumberFormatException | IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void trySaveLastCfg() {
		Properties prop = new Properties();
		
		try (FileInputStream input = new FileInputStream(CFG_FILE)) {
			prop.load(input);
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
		
		String url = getSelectedURL();
		String id = Integer.toString(getSelectedTestID());
		String delay = Integer.toString(getScrapeDelay());
		
		prop.setProperty(PROPERTY_URL, url);
		prop.setProperty(PROPERTY_ID, id);
		prop.setProperty(PROPERTY_DELAY, delay);
		
		System.out.println("Saving " + url + " " + id + " " + delay);
		
		try (FileOutputStream output = new FileOutputStream(CFG_FILE)) {
            prop.store(output, "");
        }
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void reinitializeScraper() {
		lastParameterSet = new ParameterSet(null, -1);

		String url = getSelectedURL();
		int testID = getSelectedTestID();
		int delay = getScrapeDelay();

		scrapeTimer.stop();
		scrapeTimer.setDelay(1000 * delay);
		
		scraper = new Scraper(url, testID);
		System.out.println("Scraping " + url + "/" + testID + " ...");
		scrapeTimer.restart();
		
		//	Skip modifying the cfg file until after all the components are good to go
		//	(their ActionListeners can be called when initializing to their default values)
		if (formLoaded)
			trySaveLastCfg();
	}

	private int getSelectedTestID() {
		return (int) spinnerTestID.getValue();
	}

	private int getScrapeDelay() {
		return (int) spinnerDelay.getValue();
	}

	private String getSelectedURL() {
		String url = comboBoxURL.getSelectedItem().toString();
		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}
		return url;
	}

	class SaveAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			doScrape();
		}
	}

	class ScrapeAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			doScrape();
		}
	}

	class GraphAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			doGraph();
		}
	}

	class LoadAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			doLoad();
		}
	}

	class InstanceChangeListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent event) {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				clearTable();
				reinitializeScraper();
			}
		}
	}

	class IDChangeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			clearTable();
			reinitializeScraper();
		}
	}

	class DelayChangeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			reinitializeScraper();
		}
	}

	private void doLoad() {
		String instanceURL = getSelectedURL();
		int testID = getSelectedTestID();

		var paramList = ParameterHandler.LoadFromFile(instanceURL, testID);
		for (ParameterSet paramSet : paramList) {
			doUpdateTable(paramSet);
			lastParameterSet = paramSet;
		}
	}

	private void doScrape() {
		String instanceURL = getSelectedURL();
		int testID = getSelectedTestID();

		var params = scraper.getCurrentParameters();
		if (params == null) {
			return;
		}

		if (params.iteration() == lastParameterSet.iteration()) {
			System.out.println("Iteration unchanged at " + lastParameterSet.iteration());
			return;
		}

		doUpdateTable(params);
		ParameterHandler.SaveToFile(instanceURL, testID, params);
		params.printDeltaFrom(lastParameterSet);
		lastParameterSet = params;
	}

	private void doUpdateTable(ParameterSet paramSet) {
		DefaultTableModel model = (DefaultTableModel) table.getModel();

		if (model.getColumnCount() == 0) {
			model.addColumn("iteration");
			for (var p : paramSet.parameters()) {
				model.addColumn(p.name());
			}
			resizeColumnWidth(table);
		}

		// Ignore duplicates
		for (int i = 0; i < table.getRowCount(); i++) {
			var iterStr = (String) table.getValueAt(i, 0);
			if (paramSet.iteration() == Integer.parseInt(iterStr))
				return;
		}

		boolean atBottom = isScrollBarAtBottom(scrollPane);

		model.addRow(paramSet.toVector());

		//	Keep the scroll bar scrolled to the bottom of the table if it was before the row was added
		if (atBottom) {
			SwingUtilities.invokeLater(() -> {
				JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
				verticalBar.setValue(verticalBar.getMaximum());
			});
		}
		
		//	Sort rows by iteration #
		//	(Necessary if a parameter set was scraped, then previous parameters were loaded from a file)
		sorter = new TableRowSorter<>(table.getModel());
		sorter.setComparator(0, Comparator.comparingInt(s -> Integer.parseInt(s.toString())));
		table.setRowSorter(sorter);
		sorter.sort();
	}

	private boolean isScrollBarAtBottom(JScrollPane scrollPane) {
		JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
		int bottom = verticalBar.getValue() + verticalBar.getVisibleAmount();
		return bottom >= verticalBar.getMaximum() - 1;
	}

	private void doGraph() {
		String cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
		String instance = getSelectedURL();
		instance = instance.substring(instance.lastIndexOf("/"));
		String testFolder = Path.of(cwd, ParameterHandler.SAVE_FOLDER, instance).toString();
		String testId = String.valueOf(getSelectedTestID());

		ProcessBuilder builder = new ProcessBuilder("python", "graph_csv.py", "--test-folder", testFolder, "--test-id", testId, "--treat-first-iter-zero");
		System.out.println(builder.command().toString());
		try {
			Process proc = builder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearTable() {
		table.setModel(new DefaultTableModel());
	}

	private void resizeColumnWidth(JTable table) {
		final TableColumnModel columnModel = table.getColumnModel();
		for (int column = 0; column < table.getColumnCount(); column++) {
			int width = 100;
			for (int row = 0; row < table.getRowCount(); row++) {
				TableCellRenderer renderer = table.getCellRenderer(row, column);
				Component comp = table.prepareRenderer(renderer, row, column);
				width = Math.max(comp.getPreferredSize().width + 1, width);
			}
			if (width > 600)
				width = 600;
			width = Math.max(width, table.getColumnModel().getColumn(column).getPreferredWidth());
			columnModel.getColumn(column).setPreferredWidth(width);
		}
	}
}
