package View;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;

import Master.MVC;
import Services.Service;
import Services.Comunication.Request.Request;
import Services.Comunication.Request.RequestCode;
import Services.Comunication.Response.Response;
import betterSwing.Section;
import betterSwing.Window;
import betterSwing.utils.DirectionAndPosition;
import utils.Config;

public class View implements Service {

	/**
	 * The window of the view.
	 */
	private Window window;

	/**
	 * This constructor creates a view with the MVC hub without any configuration
	 *
	 * @param mvc The MVC hub of the view.
	 * @see MVC
	 */
	public View() {
		this.window = new Window();
		this.window.initConfig();
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
	public View(String configPath) {
		this.window = new Window(configPath);
		this.window.initConfig();
	}

	@Override
	public void start() {
		Logger.getLogger(this.getClass().getSimpleName())
				.log(Level.INFO, "View started.");
		this.loadContent();
		this.window.start();
	}

	@Override
	public void stop() {
		Logger.getLogger(this.getClass().getSimpleName())
				.log(Level.INFO, "View stopped.");
	}

	@Override
	public void notifyRequest(Request request) {
		switch (request.code) {
			default -> {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "{0} is not implemented.", request);
			}
		}
	}

	@Override
	public void sendRequest(Request request) {
		// Hello World example
		try (Socket socket = new Socket(Config.SERVER_HOST, Config.SERVER_PORT)) {
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(request);

			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			Response response = (Response) in.readObject();
			Logger.getLogger(this.getClass().getSimpleName())
					.log(Level.INFO, "Response: {0}", response);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getSimpleName())
					.log(Level.SEVERE, "Error while sending request.", e);
		}
	}

	@Override
	public void sendResponse(Response response) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Loads all the view content.
	 *
	 */
	private void loadContent() {
		Section demoSection = new Section();
		JButton demoButton = new JButton("Click me!");
		demoButton.addActionListener(e -> {
			Request request = new Request(RequestCode.HELLO_WORLD, this);
			this.sendRequest(request);
		});
		demoSection.createButtons(new JButton[] { demoButton }, DirectionAndPosition.DIRECTION_ROW);
		this.window.addSection(demoSection, DirectionAndPosition.POSITION_TOP, "Demo");
	}

}
