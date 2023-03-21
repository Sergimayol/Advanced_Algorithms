package View;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import Chess.ChessBoard;
import Master.MVC;
import Request.Notify;
import Request.Request;
import Request.RequestCode;
import betterSwing.DirectionAndPosition;
import betterSwing.Section;
import betterSwing.Window;

public class View implements Notify {

    /**
     * The MVC hub of the view.
     */
    private MVC hub;
    /**
     * The window of the view.
     */
    private Window window;
    /**
     * The board of the view.
     */
    private Board board;
    /**
     * The size of the board.
     */
    private int boardSize;
    /**
     * Indicates if the algorithm has started and start a timer.
     */
    private boolean hasStarted;
    /**
     * The progress bar of the view. Indicates the progress of the algorithm.
     */
    private JProgressBar progressBar;

    /**
     * This constructor creates a view with the MVC hub without any configuration
     *
     * @param mvc The MVC hub of the view.
     * @see MVC
     */
    public View(MVC mvc) {
        this.hub = mvc;
        this.window = new Window();
        this.hasStarted = false;
        this.loadContent();
    }

    /**
     * This constructor creates a view with the MVC hub and configures itself given
     * a config path.
     *
     * @param mvc        The MVC hub of the view.
     * @param configPath The path to its config.
     * @see MVC
     */
    public View(MVC mvc, String configPath) {
        this.hub = mvc;
        this.window = new Window(configPath);
        this.hasStarted = false;
        this.loadContent();
    }

    @Override
    public void notifyRequest(Request request) {
        switch (request.code) {
            case UpdateBoard -> {
                this.updateBoard(this.hub.getModel().getBoard());
            }
            default -> {
                throw new UnsupportedOperationException(
                        request + " is not implemented in " + this.getClass().getSimpleName());
            }
        }
    }

    /**
     * Updates the board of the view.
     * 
     * @param board The new board.
     * @see Board
     */
    private void updateBoard(ChessBoard board) {
        this.progressBar.setValue(getProgressValueToFinish());
        this.board.setBoard(board);
        this.board.paintComponent(this.board.getGraphics());
        this.board.validate();
    }

    private int getProgressValueToFinish() {
        return (int) ((this.hub.getModel().getIteration() / this.hub.getModel().getBoard().size) * 100);
    }

    /**
     * Loads all the view content.
     * 
     * @see #headerSection()
     * @see #mainSection()
     * @see #sideBarSection()
     * @see #footerSection()
     */
    private void loadContent() {
        this.createProgressBar();
        this.window.addSection(this.headerSection(), DirectionAndPosition.POSITION_TOP, "Header");
        this.window.addSection(this.mainSection(), DirectionAndPosition.POSITION_CENTER, "MainContent");
        this.window.addSection(this.sideBarSection(), DirectionAndPosition.POSITION_RIGHT, "SideBar");
        this.window.addSection(this.footerSection(), DirectionAndPosition.POSITION_BOTTOM, "Footer");
    }

    /**
     * Creates and returns the header section of the view. This section is mainly
     * used for allowing the user to select the piece to play and the game mode.
     * 
     * @return The header section of the view.
     */
    private Section headerSection() {
        Section header = new Section();
        JPanel headerContent = new JPanel();
        headerContent.setBackground(Color.LIGHT_GRAY);
        JLabel title = new JLabel("TODO: Header Section");
        title.setFont(new Font("Arial", Font.ITALIC, 13));
        headerContent.add(title);
        header.createFreeSection(headerContent);
        return header;
    }

    /**
     * Creates and returns the main section of the view. This section is mainly used
     * for showing the game board.
     * 
     * @return The main section of the view.
     */
    private Section mainSection() {
        Section main = new Section();
        this.board = new Board(this.hub.getModel().getBoard());
        this.board.setPreferredSize(new Dimension(400, 400));
        this.board.paintComponent(this.board.getGraphics());
        main.createFreeSection(this.board);
        return main;
    }

    /**
     * Creates and returns the side bar section of the view. This section is mainly
     * used for showing the stats of the game.
     * 
     * @return The side bar section of the view.
     */
    private Section sideBarSection() {
        Section sideBar = new Section();
        JPanel sideBarContent = new JPanel();
        sideBarContent.setBackground(Color.LIGHT_GRAY);
        sideBarContent.setLayout(new BoxLayout(sideBarContent, BoxLayout.Y_AXIS));

        JPanel contentTitleLayout = new JPanel();
        contentTitleLayout.setBackground(Color.LIGHT_GRAY);
        JLabel title = new JLabel("Estadísticas");
        title.setFont(new Font("Arial", Font.ITALIC, 30));
        contentTitleLayout.add(addMargin(10, 10));
        contentTitleLayout.add(title);
        contentTitleLayout.add(addMargin(10, 10));

        sideBarContent.add(addMargin(0, 10));
        sideBarContent.add(contentTitleLayout);

        JPanel infoTam = new JPanel();
        infoTam.setBackground(Color.LIGHT_GRAY);
        JLabel tam = new JLabel("Tamaño del tablero: ");
        tam.setFont(new Font("Arial", Font.ITALIC, 20));
        infoTam.add(tam);
        infoTam.add(addMargin(10, 10));
        JLabel tamValue = new JLabel("8");
        tamValue.setFont(new Font("Arial", Font.ITALIC, 20));
        infoTam.add(tamValue);
        infoTam.add(addMargin(10, 10));

        sideBarContent.add(addMargin(0, 10));
        sideBarContent.add(infoTam);

        JPanel infoPiezas = new JPanel();
        infoPiezas.setBackground(Color.LIGHT_GRAY);
        JLabel piezas = new JLabel("Piezas en el tablero: ");
        piezas.setFont(new Font("Arial", Font.ITALIC, 20));
        infoPiezas.add(piezas);
        infoPiezas.add(addMargin(10, 10));
        JLabel piezasValue = new JLabel("32");
        piezasValue.setFont(new Font("Arial", Font.ITALIC, 20));
        infoPiezas.add(piezasValue);
        infoPiezas.add(addMargin(10, 10));

        sideBarContent.add(addMargin(0, 10));
        sideBarContent.add(infoPiezas);

        JPanel infoTiempo = new JPanel();
        infoTiempo.setBackground(Color.LIGHT_GRAY);
        JLabel tiempo = new JLabel("Tiempo de juego: ");
        tiempo.setFont(new Font("Arial", Font.ITALIC, 20));
        infoTiempo.add(tiempo);
        infoTiempo.add(addMargin(10, 10));
        JLabel tiempoValue = new JLabel("0 ms");
        tiempoValue.setFont(new Font("Arial", Font.ITALIC, 20));
        infoTiempo.add(tiempoValue);
        infoTiempo.add(addMargin(10, 10));

        sideBarContent.add(addMargin(0, 10));
        sideBarContent.add(infoTiempo);

        JPanel infProgresoPanel = new JPanel();
        infProgresoPanel.setBackground(Color.LIGHT_GRAY);
        JLabel progreso = new JLabel("Progreso: ");
        progreso.setFont(new Font("Arial", Font.ITALIC, 20));
        infProgresoPanel.add(addMargin(10, 10));
        infProgresoPanel.add(progreso);
        infProgresoPanel.add(this.progressBar);
        infProgresoPanel.add(addMargin(10, 10));

        sideBarContent.add(addMargin(0, 10));
        sideBarContent.add(infProgresoPanel);

        sideBar.createFreeSection(sideBarContent);
        return sideBar;
    }

    private Component addMargin(int onX, int onY) {
        return Box.createRigidArea(new Dimension(onX, onY));
    }

    private void createProgressBar() {
        this.progressBar = new JProgressBar(0, 100);
        this.progressBar.setValue(0);
        this.progressBar.setForeground(Color.BLACK);
        this.progressBar.setStringPainted(true);
    }

    /**
     * Creates and returns the footer section of the view. This section is mainly
     * used for allowing the user to navigate through different views.
     * 
     * @return The footer section of the view.
     */
    private Section footerSection() {
        Section footer = new Section();

        Section buttonsSection = new Section();
        JButton[] buttons = new JButton[3];
        buttons[0] = new JButton("Iniciar");
        buttons[0].addActionListener(e -> {
            buttons[2].setEnabled(true);
            String btnText = buttons[0].getText();
            String newBtnText = btnText.equals("Pausar") ? "Reanudar" : "Pausar";
            buttons[4].setText(newBtnText);
            RequestCode code;
            if (btnText.equals("Pausar")) {
                code = RequestCode.Stop;
            } else {
                code = RequestCode.Resume;
            }
            this.hub.notifyRequest(new Request(code, this));
        });
        buttons[1] = new JButton("Siguiente iteración");
        buttons[1].addActionListener(e -> {
            buttons[2].setEnabled(true);
            this.hub.notifyRequest(new Request(RequestCode.Next, this));
        });
        buttons[2] = new JButton("Reiniciar");
        buttons[2].setEnabled(false);
        buttons[2].addActionListener(e -> {
            buttons[0].setText("Iniciar");
            buttons[2].setEnabled(false);
            this.hub.notifyRequest(new Request(RequestCode.ReStart, this));
        });
        buttonsSection.addButtons(buttons, DirectionAndPosition.DIRECTION_ROW);

        JPanel boardSizePanel = new JPanel();
        JLabel tableSize = new JLabel("Tamaño del tablero: ");
        SpinnerNumberModel size = new SpinnerNumberModel(1, 1, 20, 1);
        JSpinner tableSizeSpinner = new JSpinner(size);
        tableSizeSpinner.addChangeListener(e -> {
            this.boardSize = (int) tableSizeSpinner.getValue();
            this.hub.notifyRequest(new Request(RequestCode.ChangedTableSize, this));
        });
        boardSizePanel.add(tableSize);
        boardSizePanel.add(tableSizeSpinner);

        JPanel footerPanel = new JPanel();
        footerPanel.add(buttonsSection.getPanel());
        footerPanel.add(boardSizePanel);

        footer.createFreeSection(footerPanel);
        return footer;
    }

    /**
     * Returns the window of the view.
     * 
     * @return The window of the view.
     */
    public Window getWindow() {
        return this.window;
    }

}
