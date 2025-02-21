package ltm.spsa_tracker.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatter;

import java.awt.Component;

import ltm.spsa_tracker.backend.*;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SpringLayout;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;

import java.awt.Label;
import javax.swing.JLabel;

public class Window {

    private JFrame frmSpsaTracker;
    private JTable table;
    private JScrollPane scrollPane;
    private JComboBox comboBoxURL;
    private JSpinner spinnerTestID;
    private JMenuBar menuBar;

    private Timer scrapeTimer;
    public Scraper scraper;

    private final int SCRAPE_DELAY = 10000;

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
	springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH,
		frmSpsaTracker.getContentPane());
	springLayout.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST,
		frmSpsaTracker.getContentPane());
	springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST,
		frmSpsaTracker.getContentPane());
	scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	frmSpsaTracker.getContentPane().add(scrollPane);

	table = new JTable();
	table.setFillsViewportHeight(true);
	scrollPane.setViewportView(table);
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	comboBoxURL = new JComboBox(InstanceList.getInstanceList());
	comboBoxURL.addItemListener(new InstanceChangeListener());
	springLayout.putConstraint(SpringLayout.NORTH, comboBoxURL, 10, SpringLayout.SOUTH, scrollPane);
	frmSpsaTracker.getContentPane().add(comboBoxURL);

	spinnerTestID = new JSpinner();
	SpinnerModel idModel = new SpinnerNumberModel(0, 0, 100000, 1);
	spinnerTestID.setModel(idModel);

	NumberEditor editor = new JSpinner.NumberEditor(spinnerTestID, "#");
	spinnerTestID.setEditor(editor);
	((DefaultFormatter) (editor.getTextField()).getFormatter()).setCommitsOnValidEdit(true);
	spinnerTestID.addChangeListener(new IDChangeListener());

	springLayout.putConstraint(SpringLayout.NORTH, spinnerTestID, 0, SpringLayout.NORTH, comboBoxURL);
	springLayout.putConstraint(SpringLayout.WEST, spinnerTestID, -110, SpringLayout.EAST,
		frmSpsaTracker.getContentPane());
	springLayout.putConstraint(SpringLayout.EAST, spinnerTestID, -10, SpringLayout.EAST,
		frmSpsaTracker.getContentPane());
	frmSpsaTracker.getContentPane().add(spinnerTestID);

	Label labelInstance = new Label("Instance URL");
	springLayout.putConstraint(SpringLayout.SOUTH, spinnerTestID, 0, SpringLayout.SOUTH, labelInstance);
	springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.NORTH, labelInstance);
	springLayout.putConstraint(SpringLayout.SOUTH, comboBoxURL, 0, SpringLayout.SOUTH, labelInstance);
	springLayout.putConstraint(SpringLayout.EAST, comboBoxURL, 410, SpringLayout.EAST, labelInstance);
	springLayout.putConstraint(SpringLayout.SOUTH, labelInstance, -10, SpringLayout.SOUTH,
		frmSpsaTracker.getContentPane());
	springLayout.putConstraint(SpringLayout.WEST, comboBoxURL, 10, SpringLayout.EAST, labelInstance);
	springLayout.putConstraint(SpringLayout.WEST, labelInstance, 10, SpringLayout.WEST,
		frmSpsaTracker.getContentPane());
	frmSpsaTracker.getContentPane().add(labelInstance);

	JLabel labelTestID = new JLabel("Test ID");
	springLayout.putConstraint(SpringLayout.NORTH, labelTestID, 0, SpringLayout.NORTH, labelInstance);
	springLayout.putConstraint(SpringLayout.WEST, labelTestID, -60, SpringLayout.WEST, spinnerTestID);
	springLayout.putConstraint(SpringLayout.SOUTH, labelTestID, 0, SpringLayout.SOUTH, labelInstance);
	springLayout.putConstraint(SpringLayout.EAST, labelTestID, -10, SpringLayout.WEST, spinnerTestID);
	frmSpsaTracker.getContentPane().add(labelTestID);

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

	lastParameterSet = new ParameterSet(null, -1);
	reinitializeScraper();
    }

    private void reinitializeScraper() {
	String url = getSelectedURL();
	int testID = getSelectedTestID();

	scraper = new Scraper(url, testID);
	System.out.println("Scraping " + url + "/" + testID + " ...");
	scrapeTimer.restart();
    }

    private int getSelectedTestID() {
	return (int) spinnerTestID.getValue();
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

    class LoadAction implements ActionListener {
	public void actionPerformed(ActionEvent e) {
	    doLoad();
	}
    }

    class InstanceChangeListener implements ItemListener {
	@Override
	public void itemStateChanged(ItemEvent event) {
	    if (event.getStateChange() == ItemEvent.SELECTED) {
		reinitializeScraper();
	    }
	}
    }

    class IDChangeListener implements ChangeListener {
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

	model.addRow(paramSet.toVector());
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
