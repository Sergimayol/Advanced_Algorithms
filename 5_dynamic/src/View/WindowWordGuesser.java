package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import Services.Comunication.Request.Request;
import Services.Comunication.Request.RequestCode;
import betterSwing.Section;
import betterSwing.Window;
import betterSwing.utils.DirectionAndPosition;
import utils.Config;

public class WindowWordGuesser {

	private Window window;
	private View view;

	public WindowWordGuesser(View view) {
		this.view = view;
		this.window = new Window(Config.VIEW_USER_MANUAL_WIN_CONFIG_PATH);
		this.window.initConfig();
		this.loadContent();
	}

	private void loadContent() {
		this.window.addSection(this.sectionUsage(), DirectionAndPosition.POSITION_CENTER, "Body");
	}

	private Section sectionUsage() {
		Section section = new Section();
		JPanel panel = new JPanel();
		section.createFreeSection(panel);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BorderLayout());
		JLabel titleLabel = new JLabel("Word Guesser", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);

		JLabel resultLabel = new JLabel("", SwingConstants.CENTER);
		resultLabel.setFont(new Font("Arial", Font.BOLD, 20));
		panel.add(resultLabel, BorderLayout.CENTER);

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		JLabel inputLabel = new JLabel("Input text:", SwingConstants.CENTER);
		JTextField inputField = new JTextField();
		inputPanel.add(inputLabel, BorderLayout.NORTH);
		inputPanel.add(inputField, BorderLayout.CENTER);
		panel.add(inputPanel, BorderLayout.SOUTH);
		JButton detect = new JButton("Detect Language");
		detect.setBounds(new Rectangle(new Dimension(10, 10)));
		// Log panel

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());

		JPanel infoPanel = new JPanel();
		infoPanel.setBackground(Color.WHITE);
		infoPanel.setLayout(new BorderLayout());
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setText("Logs: \n");

		// Wrap a scrollpane around it.
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		infoPanel.add(scrollPane, BorderLayout.CENTER);
		rightPanel.add(infoPanel, BorderLayout.CENTER);
		rightPanel.add(detect, BorderLayout.SOUTH);
		panel.add(rightPanel, BorderLayout.EAST);
		detect.addActionListener(e -> {
			String input = inputField.getText();
			System.out.println("La palabra que buscamos es: " + input);
			this.view.sendRequest(new Request(RequestCode.GUESS_LANG, this));
		});

		return section;
	}

	public void show() {
		this.window.start();
	}
}
