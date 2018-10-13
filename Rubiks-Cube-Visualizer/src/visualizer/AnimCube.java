package visualizer;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

/**
 * @author Josef Jelinek
 * @version 3.5b
 */

public final class AnimCube extends Applet implements MouseListener, MouseMotionListener, Constants {
	private static final long serialVersionUID = 1L;
	// background colors
	private Color bgColor;
	private Color bgColor2;
	private Color hlColor;
	private Color textColor;
	private static Color buttonBgColor;
	// current twisted layer
	private static int twistedLayer;
	private static int twistedMode;
	// directions of facelet cycling for all faces
	private static final int[] faceTwistDirs = { 1, 1, -1, -1, -1, -1 };
	// angle of rotation of the twistedLayer
	private static double currentAngle; // edited angle of twisted layer
	private static double originalAngle; // angle of twisted layer
	// animation speed
	public static int speed;
	private static int doubleSpeed;
	// current state of the program
	private static boolean natural = true; // cube is compact, no layer is twisted
	private boolean toTwist; // layer can be twisted
	private static boolean mirrored; // mirroring of the cube view
	private boolean editable; // editation of the cube with a mouse
	private static boolean twisting; // a user twists a cube layer
	private static boolean spinning; // an animation twists a cube layer
	private boolean dragging; // progress bar is controlled
	public static boolean demo; // demo mode
	private int persp; // perspective deformation
	private double scale; // cube scale
	private int align; // cube alignment (top, center, bottom)
	private boolean hint;
	private double faceShift;
	// move sequence data
	public static int[][] move;
	public static int[][] demoMove;
	public static int curMove;
	public static int movePos;
	public static int moveDir;
	public static boolean moveOne;
	public static boolean moveAnimated;
	public static int metric;
	private String[] infoText;
	public static int curInfoText;
	// state of buttons
	private static int buttonBar; // button bar mode
	private static int buttonHeight;
	public static boolean drawButtons = true;
	private boolean pushed;
	private static int buttonPressed = -1;
	private int progressHeight = 6;
	private int textHeight;
	private int moveText;
	private boolean outlined = true;
	//Animation Thread
	static AnimationThread thread = null;

	public void init() {
		// register to receive all mouse events
		addMouseListener(this);
		addMouseMotionListener(this);
		
		// Create a new Animation Thread.
		thread = new AnimationThread();
		
		// setup default configuration
		String param = getParameter("config");
		if (param != null) {
			try {
				URL url = new URL(getDocumentBase(), param);
				InputStream input = url.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				String line = reader.readLine();
				while (line != null) {
					int pos = line.indexOf('=');
					if (pos > 0) {
						String key = line.substring(0, pos).trim();
						String value = line.substring(pos + 1).trim();
						config.put(key, value);
					}
					line = reader.readLine();
				}
				reader.close();
			} catch (MalformedURLException ex) {
				System.err.println("Malformed URL: " + param + ": " + ex);
			} catch (IOException ex) {
				System.err.println("Input error: " + param + ": " + ex);
			}
		}
		// setup window background color
		param = getParameter("bgcolor");
		if (param != null && param.length() == 6) {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 16; j++) {
					if (Character.toLowerCase(param.charAt(i)) == "0123456789abcdef".charAt(j)) {
						hex[i] = j;
						break;
					}
				}
			}
			bgColor = new Color(hex[0] * 16 + hex[1], hex[2] * 16 + hex[3], hex[4] * 16 + hex[5]);
		} else
			bgColor = Color.gray;
		// setup button bar background color
		param = getParameter("butbgcolor");
		if (param != null && param.length() == 6) {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 16; j++) {
					if (Character.toLowerCase(param.charAt(i)) == "0123456789abcdef".charAt(j)) {
						hex[i] = j;
						break;
					}
				}
			}
			buttonBgColor = new Color(hex[0] * 16 + hex[1], hex[2] * 16 + hex[3], hex[4] * 16 + hex[5]);
		} else
			buttonBgColor = bgColor;
		// custom colors
		param = getParameter("colors");
		if (param != null) {
			for (int k = 0; k < 10 && k < param.length() / 6; k++) {
				for (int i = 0; i < 6; i++) {
					for (int j = 0; j < 16; j++) {
						if (Character.toLowerCase(param.charAt(k * 6 + i)) == "0123456789abcdef".charAt(j)) {
							hex[i] = j;
							break;
						}
					}
				}
				colors[k] = new Color(hex[0] * 16 + hex[1], hex[2] * 16 + hex[3], hex[4] * 16 + hex[5]);
			}
		}
		// clean the cube
		for (int i = 0; i < 6; i++)
			for (int j = 0; j < 9; j++)
				cube[i][j] = i + 10;
		String initialPosition = "lluu";
		// setup color configuration of the solved cube
		param = getParameter("colorscheme");
		if (param != null && param.length() == 6) {
			for (int i = 0; i < 6; i++) { // udfblr
				int color = 23;
				for (int j = 0; j < 23; j++) {
					if (Character.toLowerCase(param.charAt(i)) == "0123456789wyorgbldmcpnk".charAt(j)) {
						color = j;
						break;
					}
				}
				for (int j = 0; j < 9; j++)
					cube[i][j] = color;
			}
		}
		// setup facelets - compatible with Lars's applet
		param = getParameter("pos");
		if (param != null && param.length() == 54) {
			initialPosition = "uuuuff";
			if (bgColor == Color.gray)
				bgColor = Color.white;
			for (int i = 0; i < 6; i++) {
				int ti = posFaceTransform[i];
				for (int j = 0; j < 9; j++) {
					int tj = posFaceletTransform[i][j];
					cube[ti][tj] = 23;
					for (int k = 0; k < 14; k++) {
						// "abcdefgh" ~ "gbrwoyld"
						if (param.charAt(i * 9 + j) == "DFECABdfecabgh".charAt(k)) {
							cube[ti][tj] = k + 4;
							break;
						}
					}
				}
			}
		}
		// setup color facelets
		param = getParameter("facelets");
		if (param != null && param.length() == 54) {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 9; j++) {
					cube[i][j] = 23;
					for (int k = 0; k < 23; k++) {
						if (Character.toLowerCase(param.charAt(i * 9 + j)) == "0123456789wyorgbldmcpnk".charAt(k)) {
							cube[i][j] = k;
							break;
						}
					}
				}
			}
		}
		// setup move sequence (and info texts)
		param = getParameter("move");
		move = (param == null ? new int[0][0] : getMove(param, true));
		movePos = 0;
		curInfoText = -1;
		// setup initial move sequence
		param = getParameter("initmove");
		if (param != null) {
			int[][] initialMove = param.equals("#") ? move : getMove(param, false);
			if (initialMove.length > 0)
				doMove(cube, initialMove[0], 0, initialMove[0].length, false);
		}
		// setup initial reversed move sequence
		param = getParameter("initrevmove");
		if (param != null) {
			int[][] initialReversedMove = param.equals("#") ? move : getMove(param, false);
			if (initialReversedMove.length > 0)
				doMove(cube, initialReversedMove[0], 0, initialReversedMove[0].length, true);
		}
		// setup initial reversed move sequence
		param = getParameter("demo");
		if (param != null) {
			demoMove = param.equals("#") ? move : getMove(param, true);
			if (demoMove.length > 0 && demoMove[0].length > 0)
				demo = true;
		}
		// setup initial cube position
		param = getParameter("position");
		VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vMul(eyeY, eye, eyeX));
		if (param == null)
			param = initialPosition;
		double pi12 = Math.PI / 12;
		for (int i = 0; i < param.length(); i++) {
			double angle = pi12;
			switch (Character.toLowerCase(param.charAt(i))) {
			case 'd':
				angle = -angle;
			case 'u':
				VectorAndMovementsMath.vRotY(eye, angle);
				VectorAndMovementsMath.vRotY(eyeX, angle);
				break;
			case 'f':
				angle = -angle;
			case 'b':
				VectorAndMovementsMath.vRotZ(eye, angle);
				VectorAndMovementsMath.vRotZ(eyeX, angle);
				break;
			case 'l':
				angle = -angle;
			case 'r':
				VectorAndMovementsMath.vRotX(eye, angle);
				VectorAndMovementsMath.vRotX(eyeX, angle);
				break;
			}
		}
		VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vMul(eyeY, eye, eyeX)); // fix eyeY
		// setup quarter-turn speed and double-turn speed
		speed = 0;
		doubleSpeed = 0;
		param = getParameter("speed");
		if (param != null)
			for (int i = 0; i < param.length(); i++)
				if (param.charAt(i) >= '0' && param.charAt(i) <= '9')
					speed = speed * 10 + (int) param.charAt(i) - '0';
		param = getParameter("doublespeed");
		if (param != null)
			for (int i = 0; i < param.length(); i++)
				if (param.charAt(i) >= '0' && param.charAt(i) <= '9')
					doubleSpeed = doubleSpeed * 10 + (int) param.charAt(i) - '0';
		if (speed == 0)
			speed = 10;
		if (doubleSpeed == 0)
			doubleSpeed = speed * 3 / 2;
		// perspective deformation
		persp = 0;
		param = getParameter("perspective");
		if (param == null)
			persp = 2;
		else
			for (int i = 0; i < param.length(); i++)
				if (param.charAt(i) >= '0' && param.charAt(i) <= '9')
					persp = persp * 10 + (int) param.charAt(i) - '0';
		// cube scale
		int intscale = 0;
		param = getParameter("scale");
		if (param != null)
			for (int i = 0; i < param.length(); i++)
				if (param.charAt(i) >= '0' && param.charAt(i) <= '9')
					intscale = intscale * 10 + (int) param.charAt(i) - '0';
		scale = 1.0 / (1.0 + intscale / 10.0);
		// hint displaying
		hint = false;
		param = getParameter("hint");
		if (param != null) {
			hint = true;
			faceShift = 0.0;
			for (int i = 0; i < param.length(); i++)
				if (param.charAt(i) >= '0' && param.charAt(i) <= '9')
					faceShift = faceShift * 10 + (int) param.charAt(i) - '0';
			if (faceShift < 1.0)
				hint = false;
			else
				faceShift /= 10.0;
		}
		// appearance and configuration of the button bar
		buttonBar = 1;
		buttonHeight = 13;
		progressHeight = move.length == 0 ? 0 : 6;
		param = getParameter("buttonbar");
		if ("0".equals(param)) {
			buttonBar = 0;
			buttonHeight = 0;
			progressHeight = 0;
		} else if ("1".equals(param))
			buttonBar = 1;
		else if ("2".equals(param) || move.length == 0) {
			buttonBar = 2;
			progressHeight = 0;
		}
		// whether the cube can be edited with mouse
		param = getParameter("edit");
		if ("0".equals(param))
			editable = false;
		else
			editable = true;
		// displaying the textual representation of the move
		param = getParameter("movetext");
		if ("1".equals(param))
			moveText = 1;
		else if ("2".equals(param))
			moveText = 2;
		else if ("3".equals(param))
			moveText = 3;
		else if ("4".equals(param))
			moveText = 4;
		else
			moveText = 0;
		// how texts are displayed
		param = getParameter("fonttype");
		if (param == null || "1".equals(param))
			outlined = true;
		else
			outlined = false;
		// metric
		metric = 0;
		param = getParameter("metric");
		if (param != null) {
			if ("1".equals(param)) // quarter-turn
				metric = 1;
			else if ("2".equals(param)) // face-turn
				metric = 2;
			else if ("3".equals(param)) // slice-turn
				metric = 3;
		}
		// metric
		align = 1;
		param = getParameter("align");
		if (param != null) {
			if ("0".equals(param)) // top
				align = 0;
			else if ("1".equals(param)) // center
				align = 1;
			else if ("2".equals(param)) // bottom
				align = 2;
		}
		// setup initial values
		for (int i = 0; i < 6; i++)
			for (int j = 0; j < 9; j++)
				initialCube[i][j] = cube[i][j];
		for (int i = 0; i < 3; i++) {
			initialEye[i] = eye[i];
			initialEyeX[i] = eyeX[i];
			initialEyeY[i] = eyeY[i];
		}
		// setup colors (contrast)
		int red = bgColor.getRed();
		int green = bgColor.getGreen();
		int blue = bgColor.getBlue();
		int average = (red * 299 + green * 587 + blue * 114) / 1000;
		if (average < 128) {
			textColor = Color.white;
			hlColor = bgColor.brighter();
			hlColor = new Color(hlColor.getBlue(), hlColor.getRed(), hlColor.getGreen());
		} else {
			textColor = Color.black;
			hlColor = bgColor.darker();
			hlColor = new Color(hlColor.getBlue(), hlColor.getRed(), hlColor.getGreen());
		}
		bgColor2 = new Color(red / 2, green / 2, blue / 2);
		curInfoText = -1;
		if (demo)
			thread.startAnimation(-1);
	} 
	
	public String getParameter(String name) {
		String parameter = super.getParameter(name);
		if (parameter == null)
			return (String) config.get(name);
		return parameter;
	}

	private int[][] getMove(String sequence, boolean info) {
		if (info) {
			int inum = 0;
			int pos = sequence.indexOf('{');
			while (pos != -1) {
				inum++;
				pos = sequence.indexOf('{', pos + 1);
			}
			if (infoText == null) {
				curInfoText = 0;
				infoText = new String[inum];
			} else {
				String[] infoText2 = new String[infoText.length + inum];
				for (int i = 0; i < infoText.length; i++)
					infoText2[i] = infoText[i];
				curInfoText = infoText.length;
				infoText = infoText2;
			}
		}
		int num = 1;
		int pos = sequence.indexOf(';');
		while (pos != -1) {
			num++;
			pos = sequence.indexOf(';', pos + 1);
		}
		int[][] move = new int[num][];
		int lastPos = 0;
		pos = sequence.indexOf(';');
		num = 0;
		while (pos != -1) {
			move[num++] = getMovePart(sequence.substring(lastPos, pos), info);
			lastPos = pos + 1;
			pos = sequence.indexOf(';', lastPos);
		}
		move[num] = getMovePart(sequence.substring(lastPos), info);
		return move;
	}

	private int[] getMovePart(String sequence, boolean info) {
		int length = 0;
		int[] move = new int[sequence.length()]; // overdimmensioned
		for (int i = 0; i < sequence.length(); i++) {
			if (sequence.charAt(i) == '.') {
				move[length] = -1;
				length++;
			} else if (sequence.charAt(i) == '{') {
				i++;
				String s = "";
				while (i < sequence.length()) {
					if (sequence.charAt(i) == '}')
						break;
					if (info)
						s += sequence.charAt(i);
					i++;
				}
				if (info) {
					infoText[curInfoText] = s;
					move[length] = 1000 + curInfoText;
					curInfoText++;
					length++;
				}
			} else {
				for (int j = 0; j < 21; j++) {
					if (sequence.charAt(i) == "UDFBLRESMXYZxyzudfblr".charAt(j)) {
						i++;
						int mode = moveModes[j];
						move[length] = moveCodes[j] * 24;
						if (i < sequence.length()) {
							if (moveModes[j] == 0) { // modifiers for basic characters UDFBLR
								for (int k = 0; k < modeChar.length; k++) {
									if (sequence.charAt(i) == modeChar[k]) {
										mode = k + 1;
										i++;
										break;
									}
								}
							}
						}
						move[length] += mode * 4;
						if (i < sequence.length()) {
							if (sequence.charAt(i) == '1')
								i++;
							else if (sequence.charAt(i) == '\'' || sequence.charAt(i) == '3') {
								move[length] += 2;
								i++;
							} else if (sequence.charAt(i) == '2') {
								i++;
								if (i < sequence.length() && sequence.charAt(i) == '\'') {
									move[length] += 3;
									i++;
								} else
									move[length] += 1;
							}
						}
						length++;
						i--;
						break;
					}
				}
			}
		}
		int[] returnMove = new int[length];
		for (int i = 0; i < length; i++)
			returnMove[i] = move[i];
		return returnMove;
	}

	private String moveText(int[] move, int start, int end) {
		if (start >= move.length)
			return "";
		String s = "";
		for (int i = start; i < end; i++)
			s += turnText(move, i);
		return s;
	}

	private String turnText(int[] move, int pos) {
		if (pos >= move.length)
			return "";
		if (move[pos] >= 1000)
			return "";
		if (move[pos] == -1)
			return ".";
		String s = turnSymbol[moveText - 1][move[pos] / 4 % 6][move[pos] / 24];
		if (s.charAt(0) == '~')
			return s.substring(1) + modifierStrings[(move[pos] + 2) % 4];
		return s + modifierStrings[move[pos] % 4];
	}

	public static void initInfoText(int[] move) {
		if (move.length > 0 && move[0] >= 1000)
			curInfoText = move[0] - 1000;
		else
			curInfoText = -1;
	}

	private void doMove(int[][] cube, int[] move, int start, int length, boolean reversed) {
		int position = reversed ? start + length : start;
		while (true) {
			if (reversed) {
				if (position <= start)
					break;
				position--;
			}
			if (move[position] >= 1000) {
				curInfoText = reversed ? -1 : move[position] - 1000;
			} else if (move[position] >= 0) {
				int modifier = move[position] % 4 + 1;
				int mode = move[position] / 4 % 6;
				if (modifier == 4) // reversed double turn
					modifier = 2;
				if (reversed)
					modifier = 4 - modifier;
				twistLayers(cube, move[position] / 24, modifier, mode);
			}
			if (!reversed) {
				position++;
				if (position >= start + length)
					break;
			}
		}
	}

	
	public static void clear() {
		synchronized (thread) {
			movePos = 0;
			if (move.length > 0)
				initInfoText(move[curMove]);
			natural = true;
			mirrored = false;
			for (int i = 0; i < 6; i++)
				for (int j = 0; j < 9; j++)
					cube[i][j] = initialCube[i][j];
			for (int i = 0; i < 3; i++) {
				eye[i] = initialEye[i];
				eyeX[i] = initialEyeX[i];
				eyeY[i] = initialEyeY[i];
			}
		}
	}

	public static void spin(int layer, int num, int mode, boolean clockwise, boolean animated) {
		twisting = false;
		natural = true;
		spinning = true;
		originalAngle = 0;
		if (faceTwistDirs[layer] > 0)
			clockwise = !clockwise;
		if (animated) {
			double phit = Math.PI / 2; // target for currentAngle (default pi/2)
			double phis = clockwise ? 1.0 : -1.0; // sign
			int turnTime = 67 * speed; // milliseconds to be used for one turn
			if (num == 2) {
				phit = Math.PI;
				turnTime = 67 * doubleSpeed; // double turn is usually faster than two quarter turns
			}
			twisting = true;
			twistedLayer = layer;
			twistedMode = mode;
			splitCube(layer); // start twisting
			long sTime = System.currentTimeMillis();
			long lTime = sTime;
			double d = phis * phit / turnTime;
			for (currentAngle = 0; currentAngle * phis < phit; currentAngle = d * (lTime - sTime)) {
				repaint();
				thread.sleep(25);
				if (thread.isInterupted() || thread.isRestarted())
					break;
				lTime = System.currentTimeMillis();
			}
		}
		currentAngle = 0;
		twisting = false;
		natural = true;
		twistLayers(cube, layer, num, mode);
		spinning = false;
		if (animated)
			repaint();
	}

	private static void splitCube(int layer) {
		for (int i = 0; i < 6; i++) { // for all faces
			topBlocks[i] = topBlockTable[topBlockFaceDim[layer][i]];
			botBlocks[i] = topBlockTable[botBlockFaceDim[layer][i]];
			midBlocks[i] = midBlockTable[midBlockFaceDim[layer][i]];
		}
		natural = false;
	}

	private static void twistLayers(int[][] cube, int layer, int num, int mode) {
		switch (mode) {
		case 3:
			twistLayer(cube, layer ^ 1, num, false);
		case 2:
			twistLayer(cube, layer, 4 - num, false);
		case 1:
			twistLayer(cube, layer, 4 - num, true);
			break;
		case 5:
			twistLayer(cube, layer ^ 1, 4 - num, false);
			twistLayer(cube, layer, 4 - num, false);
			break;
		case 4:
			twistLayer(cube, layer ^ 1, num, false);
		default:
			twistLayer(cube, layer, 4 - num, false);
		}
	}

	private final static int[] twistBuffer = new int[12];

	private static void twistLayer(int[][] cube, int layer, int num, boolean middle) {
		if (!middle) {
			// rotate top facelets
			for (int i = 0; i < 8; i++) // to buffer
				twistBuffer[(i + num * 2) % 8] = cube[layer][cycleOrder[i]];
			for (int i = 0; i < 8; i++) // to cube
				cube[layer][cycleOrder[i]] = twistBuffer[i];
		}
		// rotate side facelets
		int k = num * 3;
		for (int i = 0; i < 4; i++) { // to buffer
			int n = adjacentFaces[layer][i];
			int c = middle ? cycleCenters[layer][i] : cycleLayerSides[layer][i];
			int factor = cycleFactors[c];
			int offset = cycleOffsets[c];
			for (int j = 0; j < 3; j++) {
				twistBuffer[k % 12] = cube[n][j * factor + offset];
				k++;
			}
		}
		k = 0; // MS VM JIT bug if placed into the loop init
		for (int i = 0; i < 4; i++) { // to cube
			int n = adjacentFaces[layer][i];
			int c = middle ? cycleCenters[layer][i] : cycleLayerSides[layer][i];
			int factor = cycleFactors[c];
			int offset = cycleOffsets[c];
			int j = 0; // MS VM JIT bug if for is used
			while (j < 3) {
				cube[n][j * factor + offset] = twistBuffer[k];
				j++;
				k++;
			}
		}
	}

	// double buffered animation
	private Graphics graphics = null;
	private Image image = null;
	// cube window size (applet window is resizable)
	private static int width;
	private static int height;
	// last position of mouse (for dragging the cube)
	private int lastX;
	private int lastY;
	// last position of mouse (when waiting for clear decission)
	private int lastDragX;
	private int lastDragY;
	// drag areas
	private int dragAreas;

	private int[] dragLayers = new int[18]; // which layers belongs to dragCorners
	private int[] dragModes = new int[18]; // which layer modes dragCorners
	// current drag directions
	private double dragX;
	private double dragY;

	public void paint(Graphics g) {
		Dimension size = getSize(); // inefficient - Java 1.1
		// create offscreen buffer for double buffering
		if (image == null || size.width != width || size.height - buttonHeight != height) {
			width = size.width;
			height = size.height;
			image = createImage(width, height);
			graphics = image.getGraphics();
			textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getLeading();
			if (buttonBar == 1)
				height -= buttonHeight;
			drawButtons = true;
		}
		graphics.setColor(bgColor);
		graphics.setClip(0, 0, width, height);
		graphics.fillRect(0, 0, width, height);
		synchronized (animThread) {
			dragAreas = 0;
			if (natural) // compact cube
				fixBlock(eye, eyeX, eyeY, cubeBlocks, 3); // draw cube and fill drag areas
			else { // in twisted state
					// compute top observer
				double cosA = Math.cos(originalAngle + currentAngle);
				double sinA = Math.sin(originalAngle + currentAngle) * rotSign[twistedLayer];
				for (int i = 0; i < 3; i++) {
					tempEye[i] = 0;
					tempEyeX[i] = 0;
					for (int j = 0; j < 3; j++) {
						int axis = twistedLayer / 2;
						tempEye[i] += eye[j]
								* (rotVec[axis][i][j] + rotCos[axis][i][j] * cosA + rotSin[axis][i][j] * sinA);
						tempEyeX[i] += eyeX[j]
								* (rotVec[axis][i][j] + rotCos[axis][i][j] * cosA + rotSin[axis][i][j] * sinA);
					}
				}
				VectorAndMovementsMath.vMul(tempEyeY, tempEye, tempEyeX);
				// compute bottom anti-observer
				double cosB = Math.cos(originalAngle - currentAngle);
				double sinB = Math.sin(originalAngle - currentAngle) * rotSign[twistedLayer];
				for (int i = 0; i < 3; i++) {
					tempEye2[i] = 0;
					tempEyeX2[i] = 0;
					for (int j = 0; j < 3; j++) {
						int axis = twistedLayer / 2;
						tempEye2[i] += eye[j]
								* (rotVec[axis][i][j] + rotCos[axis][i][j] * cosB + rotSin[axis][i][j] * sinB);
						tempEyeX2[i] += eyeX[j]
								* (rotVec[axis][i][j] + rotCos[axis][i][j] * cosB + rotSin[axis][i][j] * sinB);
					}
				}
				VectorAndMovementsMath.vMul(tempEyeY2, tempEye2, tempEyeX2);
				eyeArray[0] = eye;
				eyeArrayX[0] = eyeX;
				eyeArrayY[0] = eyeY;
				eyeArray[1] = tempEye;
				eyeArrayX[1] = tempEyeX;
				eyeArrayY[1] = tempEyeY;
				eyeArray[2] = tempEye2;
				eyeArrayX[2] = tempEyeX2;
				eyeArrayY[2] = tempEyeY2;
				blockArray[0] = topBlocks;
				blockArray[1] = midBlocks;
				blockArray[2] = botBlocks;
				// perspective corrections
				VectorAndMovementsMath
						.vSub(VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(perspEye, eye), 5.0 + persp),
								VectorAndMovementsMath.vScale(
										VectorAndMovementsMath.vCopy(perspNormal, faceNormals[twistedLayer]),
										1.0 / 3.0));
				VectorAndMovementsMath
						.vSub(VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(perspEyeI, eye), 5.0 + persp),
								VectorAndMovementsMath.vScale(
										VectorAndMovementsMath.vCopy(perspNormal, faceNormals[twistedLayer ^ 1]),
										1.0 / 3.0));
				double topProd = VectorAndMovementsMath.vProd(perspEye, faceNormals[twistedLayer]);
				double botProd = VectorAndMovementsMath.vProd(perspEyeI, faceNormals[twistedLayer ^ 1]);
				int orderMode;
				if (topProd < 0 && botProd > 0) // top facing away
					orderMode = 0;
				else if (topProd > 0 && botProd < 0) // bottom facing away: draw it first
					orderMode = 1;
				else // both top and bottom layer facing away: draw them first
					orderMode = 2;
				fixBlock(eyeArray[eyeOrder[twistedMode][drawOrder[orderMode][0]]],
						eyeArrayX[eyeOrder[twistedMode][drawOrder[orderMode][0]]],
						eyeArrayY[eyeOrder[twistedMode][drawOrder[orderMode][0]]], blockArray[drawOrder[orderMode][0]],
						blockMode[twistedMode][drawOrder[orderMode][0]]);
				fixBlock(eyeArray[eyeOrder[twistedMode][drawOrder[orderMode][1]]],
						eyeArrayX[eyeOrder[twistedMode][drawOrder[orderMode][1]]],
						eyeArrayY[eyeOrder[twistedMode][drawOrder[orderMode][1]]], blockArray[drawOrder[orderMode][1]],
						blockMode[twistedMode][drawOrder[orderMode][1]]);
				fixBlock(eyeArray[eyeOrder[twistedMode][drawOrder[orderMode][2]]],
						eyeArrayX[eyeOrder[twistedMode][drawOrder[orderMode][2]]],
						eyeArrayY[eyeOrder[twistedMode][drawOrder[orderMode][2]]], blockArray[drawOrder[orderMode][2]],
						blockMode[twistedMode][drawOrder[orderMode][2]]);
			}
			if (!pushed && !animating) // no button should be deceased
				buttonPressed = -1;
			if (!demo && move.length > 0) {
				if (move[curMove].length > 0) { // some turns
					graphics.setColor(Color.black);
					graphics.drawRect(0, height - progressHeight, width - 1, progressHeight - 1);
					graphics.setColor(textColor);
					int progress = (width - 2) * VectorAndMovementsMath.realMovePos(move[curMove], movePos) / VectorAndMovementsMath.realMoveLength(move[curMove]);
					graphics.fillRect(1, height - progressHeight + 1, progress, progressHeight - 2);
					graphics.setColor(bgColor.darker());
					graphics.fillRect(1 + progress, height - progressHeight + 1, width - 2 - progress,
							progressHeight - 2);
					String s = "" + VectorAndMovementsMath.moveLength(move[curMove], movePos) + "/" + VectorAndMovementsMath.moveLength(move[curMove], -1)
							+ metricChar[metric];
					int w = graphics.getFontMetrics().stringWidth(s);
					int x = width - w - 2;
					int y = height - progressHeight - 2; // int base = graphics.getFontMetrics().getDescent();
					if (moveText > 0 && textHeight > 0) {
						drawString(graphics, s, x, y - textHeight);
						drawMoveText(graphics, y);
					} else
						drawString(graphics, s, x, y);
				}
				if (move.length > 1) { // more sequences
					graphics.setClip(0, 0, width, height);
					int b = graphics.getFontMetrics().getDescent();
					int y = textHeight - b;
					String s = "" + (curMove + 1) + "/" + move.length;
					int w = graphics.getFontMetrics().stringWidth(s);
					int x = width - w - buttonHeight - 2;
					drawString(graphics, s, x, y);
					// draw button
					graphics.setColor(buttonBgColor);
					graphics.fill3DRect(width - buttonHeight, 0, buttonHeight, buttonHeight, buttonPressed != 7);
					drawButton(graphics, 7, width - buttonHeight / 2, buttonHeight / 2);
				}
			}
			if (curInfoText >= 0) {
				graphics.setClip(0, 0, width, height);
				int b = graphics.getFontMetrics().getDescent();
				int y = textHeight - b;
				drawString(graphics, infoText[curInfoText], 0, y);
			}
			if (drawButtons && buttonBar != 0) // omit unneccessary redrawing
				drawButtons(graphics);
		}
		g.drawImage(image, 0, 0, this);
	} // paint()

	public void update(Graphics g) {
		paint(g);
	}

	private void fixBlock(double[] eye, double[] eyeX, double[] eyeY, int[][][] blocks, int mode) {
		// project 3D co-ordinates into 2D screen ones
		for (int i = 0; i < 8; i++) {
			double min = width < height ? width : height - progressHeight;
			double x = min / 3.7 * VectorAndMovementsMath.vProd(cornerCoords[i], eyeX) * scale;
			double y = min / 3.7 * VectorAndMovementsMath.vProd(cornerCoords[i], eyeY) * scale;
			double z = min / (5.0 + persp) * VectorAndMovementsMath.vProd(cornerCoords[i], eye) * scale;
			x = x / (1 - z / min); // perspective transformation
			y = y / (1 - z / min); // perspective transformation
			coordsX[i] = width / 2.0 + x;
			if (align == 0)
				coordsY[i] = (height - progressHeight) / 2.0 * scale - y;
			else if (align == 2)
				coordsY[i] = height - progressHeight - (height - progressHeight) / 2.0 * scale - y;
			else
				coordsY[i] = (height - progressHeight) / 2.0 - y;
		}
		// setup corner co-ordinates for all faces
		for (int i = 0; i < 6; i++) { // all faces
			for (int j = 0; j < 4; j++) { // all face corners
				cooX[i][j] = coordsX[faceCorners[i][j]];
				cooY[i][j] = coordsY[faceCorners[i][j]];
			}
		}
		if (hint) { // draw hint hiden facelets
			for (int i = 0; i < 6; i++) { // all faces
				VectorAndMovementsMath.vSub(
						VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(perspEye, eye), 5.0 + persp),
						faceNormals[i]); // perspective correction
				if (VectorAndMovementsMath.vProd(perspEye, faceNormals[i]) < 0) { // draw only hiden faces
					VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(tempNormal, faceNormals[i]), faceShift);
					double min = width < height ? width : height - progressHeight;
					double x = min / 3.7 * VectorAndMovementsMath.vProd(tempNormal, eyeX);
					double y = min / 3.7 * VectorAndMovementsMath.vProd(tempNormal, eyeY);
					double z = min / (5.0 + persp) * VectorAndMovementsMath.vProd(tempNormal, eye);
					x = x / (1 - z / min); // perspective transformation
					y = y / (1 - z / min); // perspective transformation
					int sideW = blocks[i][0][1] - blocks[i][0][0];
					int sideH = blocks[i][1][1] - blocks[i][1][0];
					if (sideW > 0 && sideH > 0) { // this side is not only black
						// draw colored facelets
						for (int n = 0, p = blocks[i][1][0]; n < sideH; n++, p++) {
							for (int o = 0, q = blocks[i][0][0]; o < sideW; o++, q++) {
								for (int j = 0; j < 4; j++) {
									getCorners(i, j, fillX, fillY, q + border[j][0], p + border[j][1], mirrored);
									fillX[j] += mirrored ? -x : x;
									fillY[j] -= y;
								}
								graphics.setColor(colors[cube[i][p * 3 + q]]);
								graphics.fillPolygon(fillX, fillY, 4);
								graphics.setColor(colors[cube[i][p * 3 + q]].darker());
								graphics.drawPolygon(fillX, fillY, 4);
							}
						}
					}
				}
			}
		}
		// draw black antialias
		for (int i = 0; i < 6; i++) { // all faces
			int sideW = blocks[i][0][1] - blocks[i][0][0];
			int sideH = blocks[i][1][1] - blocks[i][1][0];
			if (sideW > 0 && sideH > 0) {
				for (int j = 0; j < 4; j++) // corner co-ordinates
					getCorners(i, j, fillX, fillY, blocks[i][0][factors[j][0]], blocks[i][1][factors[j][1]], mirrored);
				if (sideW == 3 && sideH == 3)
					graphics.setColor(bgColor2);
				else
					graphics.setColor(Color.black);
				graphics.drawPolygon(fillX, fillY, 4);
			}
		}
		// find and draw black inner faces
		for (int i = 0; i < 6; i++) { // all faces
			int sideW = blocks[i][0][1] - blocks[i][0][0];
			int sideH = blocks[i][1][1] - blocks[i][1][0];
			if (sideW <= 0 || sideH <= 0) { // this face is inner and only black
				for (int j = 0; j < 4; j++) { // for all corners
					int k = oppositeCorners[i][j];
					fillX[j] = (int) (cooX[i][j] + (cooX[i ^ 1][k] - cooX[i][j]) * 2.0 / 3.0);
					fillY[j] = (int) (cooY[i][j] + (cooY[i ^ 1][k] - cooY[i][j]) * 2.0 / 3.0);
					if (mirrored)
						fillX[j] = width - fillX[j];
				}
				graphics.setColor(Color.black);
				graphics.fillPolygon(fillX, fillY, 4);
			} else {
				// draw black face background (do not care about normals and visibility!)
				for (int j = 0; j < 4; j++) // corner co-ordinates
					getCorners(i, j, fillX, fillY, blocks[i][0][factors[j][0]], blocks[i][1][factors[j][1]], mirrored);
				graphics.setColor(Color.black);
				graphics.fillPolygon(fillX, fillY, 4);
			}
		}
		// draw all visible faces and get dragging regions
		for (int i = 0; i < 6; i++) { // all faces
			VectorAndMovementsMath.vSub(
					VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(perspEye, eye), 5.0 + persp),
					faceNormals[i]); // perspective correction
			if (VectorAndMovementsMath.vProd(perspEye, faceNormals[i]) > 0) { // draw only faces towards us
				int sideW = blocks[i][0][1] - blocks[i][0][0];
				int sideH = blocks[i][1][1] - blocks[i][1][0];
				if (sideW > 0 && sideH > 0) { // this side is not only black
					// draw colored facelets
					for (int n = 0, p = blocks[i][1][0]; n < sideH; n++, p++) {
						for (int o = 0, q = blocks[i][0][0]; o < sideW; o++, q++) {
							for (int j = 0; j < 4; j++)
								getCorners(i, j, fillX, fillY, q + border[j][0], p + border[j][1], mirrored);
							graphics.setColor(colors[cube[i][p * 3 + q]].darker());
							graphics.drawPolygon(fillX, fillY, 4);
							graphics.setColor(colors[cube[i][p * 3 + q]]);
							graphics.fillPolygon(fillX, fillY, 4);
						}
					}
				}
				if (!editable || animating) // no need of twisting while animating
					continue;
				// horizontal and vertical directions of face - interpolated
				double dxh = (cooX[i][1] - cooX[i][0] + cooX[i][2] - cooX[i][3]) / 6.0;
				double dyh = (cooX[i][3] - cooX[i][0] + cooX[i][2] - cooX[i][1]) / 6.0;
				double dxv = (cooY[i][1] - cooY[i][0] + cooY[i][2] - cooY[i][3]) / 6.0;
				double dyv = (cooY[i][3] - cooY[i][0] + cooY[i][2] - cooY[i][1]) / 6.0;
				if (mode == 3) { // just the normal cube
					for (int j = 0; j < 6; j++) { // 4 areas 3x1 per face + 2 center slices
						for (int k = 0; k < 4; k++) // 4 points per area
							getCorners(i, k, dragCornersX[dragAreas], dragCornersY[dragAreas], dragBlocks[j][k][0],
									dragBlocks[j][k][1], false);
						dragDirsX[dragAreas] = (dxh * areaDirs[j][0] + dxv * areaDirs[j][1]) * twistDirs[i][j];
						dragDirsY[dragAreas] = (dyh * areaDirs[j][0] + dyv * areaDirs[j][1]) * twistDirs[i][j];
						dragLayers[dragAreas] = adjacentFaces[i][j % 4];
						if (j >= 4)
							dragLayers[dragAreas] &= ~1;
						dragModes[dragAreas] = j / 4;
						dragAreas++;
						if (dragAreas == 18)
							break;
					}
				} else if (mode == 0) { // twistable top layer
					if (i != twistedLayer && sideW > 0 && sideH > 0) { // only 3x1 faces
						int j = sideW == 3 ? (blocks[i][1][0] == 0 ? 0 : 2) : (blocks[i][0][0] == 0 ? 3 : 1);
						for (int k = 0; k < 4; k++)
							getCorners(i, k, dragCornersX[dragAreas], dragCornersY[dragAreas], dragBlocks[j][k][0],
									dragBlocks[j][k][1], false);
						dragDirsX[dragAreas] = (dxh * areaDirs[j][0] + dxv * areaDirs[j][1]) * twistDirs[i][j];
						dragDirsY[dragAreas] = (dyh * areaDirs[j][0] + dyv * areaDirs[j][1]) * twistDirs[i][j];
						dragLayers[dragAreas] = twistedLayer;
						dragModes[dragAreas] = 0;
						dragAreas++;
					}
				} else if (mode == 1) { // twistable center layer
					if (i != twistedLayer && sideW > 0 && sideH > 0) { // only 3x1 faces
						int j = sideW == 3 ? 4 : 5;
						for (int k = 0; k < 4; k++)
							getCorners(i, k, dragCornersX[dragAreas], dragCornersY[dragAreas], dragBlocks[j][k][0],
									dragBlocks[j][k][1], false);
						dragDirsX[dragAreas] = (dxh * areaDirs[j][0] + dxv * areaDirs[j][1]) * twistDirs[i][j];
						dragDirsY[dragAreas] = (dyh * areaDirs[j][0] + dyv * areaDirs[j][1]) * twistDirs[i][j];
						dragLayers[dragAreas] = twistedLayer;
						dragModes[dragAreas] = 1;
						dragAreas++;
					}
				}
			}
		}
	}

	private void getCorners(int face, int corner, int[] cornersX, int[] cornersY, double factor1, double factor2,
			boolean mirror) {
		factor1 /= 3.0;
		factor2 /= 3.0;
		double x1 = cooX[face][0] + (cooX[face][1] - cooX[face][0]) * factor1;
		double y1 = cooY[face][0] + (cooY[face][1] - cooY[face][0]) * factor1;
		double x2 = cooX[face][3] + (cooX[face][2] - cooX[face][3]) * factor1;
		double y2 = cooY[face][3] + (cooY[face][2] - cooY[face][3]) * factor1;
		cornersX[corner] = (int) (0.5 + x1 + (x2 - x1) * factor2);
		cornersY[corner] = (int) (0.5 + y1 + (y2 - y1) * factor2);
		if (mirror)
			cornersX[corner] = width - cornersX[corner];
	}

	public static void drawButtons(Graphics g) {
		if (buttonBar == 2) { // only clear (rewind) button
			g.setColor(buttonBgColor);
			g.fill3DRect(0, height - buttonHeight, buttonHeight, buttonHeight, buttonPressed != 0);
			drawButton(g, 0, buttonHeight / 2, height - (buttonHeight + 1) / 2);
			return;
		}
		if (buttonBar == 1) { // full buttonbar
			g.setClip(0, height, width, buttonHeight);
			int buttonX = 0;
			for (int i = 0; i < 7; i++) {
				int buttonWidth = (width - buttonX) / (7 - i);
				g.setColor(buttonBgColor);
				g.fill3DRect(buttonX, height, buttonWidth, buttonHeight, buttonPressed != i);
				drawButton(g, i, buttonX + buttonWidth / 2, height + buttonHeight / 2);
				buttonX += buttonWidth;
			}
			drawButtons = false;
			return;
		}
	}

	private static void drawButton(Graphics g, int i, int x, int y) {
		g.setColor(Color.white);
		switch (i) {
		case 0: // rewind
			drawRect(g, x - 4, y - 3, 3, 7);
			drawArrow(g, x + 3, y, -1); // left
			break;
		case 1: // reverse step
			drawRect(g, x + 2, y - 3, 3, 7);
			drawArrow(g, x, y, -1); // left
			break;
		case 2: // reverse play
			drawArrow(g, x + 2, y, -1); // left
			break;
		case 3: // stop / mirror
			if (thread.isAnimating())
				drawRect(g, x - 3, y - 3, 7, 7);
			else {
				drawRect(g, x - 3, y - 2, 7, 5);
				drawRect(g, x - 1, y - 4, 3, 9);
			}
			break;
		case 4: // play
			drawArrow(g, x - 2, y, 1); // right
			break;
		case 5: // step
			drawRect(g, x - 4, y - 3, 3, 7);
			drawArrow(g, x, y, 1); // right
			break;
		case 6: // fast forward
			drawRect(g, x + 1, y - 3, 3, 7);
			drawArrow(g, x - 4, y, 1); // right
			break;
		case 7: // next sequence
			drawArrow(g, x - 2, y, 1); // right
			break;
		}
	}

	private static void drawArrow(Graphics g, int x, int y, int dir) {
		g.setColor(Color.black);
		g.drawLine(x, y - 3, x, y + 3);
		x += dir;
		for (int i = 0; i >= -3 && i <= 3; i += dir) {
			int j = 3 - i * dir;
			g.drawLine(x + i, y + j, x + i, y - j);
		}
		g.setColor(Color.white);
		for (int i = 0; i >= -1 && i <= 1; i += dir) {
			int j = 1 - i * dir;
			g.drawLine(x + i, y + j, x + i, y - j);
		}
	}

	private static void drawRect(Graphics g, int x, int y, int width, int height) {
		g.setColor(Color.black);
		g.drawRect(x, y, width - 1, height - 1);
		g.setColor(Color.white);
		g.fillRect(x + 1, y + 1, width - 2, height - 2);
	}

	private static final int[] textOffset = { 1, 1, -1, -1, -1, 1, 1, -1, -1, 0, 1, 0, 0, 1, 0, -1 };

	private void drawString(Graphics g, String s, int x, int y) {
		if (outlined) {
			g.setColor(Color.black);
			for (int i = 0; i < textOffset.length; i += 2)
				g.drawString(s, x + textOffset[i], y + textOffset[i + 1]);
			g.setColor(Color.white);
		} else
			g.setColor(textColor);
		g.drawString(s, x, y);
	}

	private void drawMoveText(Graphics g, int y) {
		g.setClip(0, height - progressHeight - textHeight, width, textHeight);
		g.setColor(Color.black);
		int pos = movePos == 0 ? VectorAndMovementsMath.arrayMovePos(move[curMove], movePos) : movePos;
		String s1 = moveText(move[curMove], 0, pos);
		String s2 = turnText(move[curMove], pos);
		String s3 = moveText(move[curMove], pos + 1, move[curMove].length);
		int w1 = g.getFontMetrics().stringWidth(s1);
		int w2 = g.getFontMetrics().stringWidth(s2);
		int w3 = g.getFontMetrics().stringWidth(s3);
		int x = 1;
		if (x + w1 + w2 + w3 > width) {
			x = Math.min(1, width / 2 - w1 - w2 / 2);
			x = Math.max(x, width - w1 - w2 - w3 - 2);
		}
		if (w2 > 0) {
			g.setColor(hlColor);
			g.fillRect(x + w1 - 1, height - progressHeight - textHeight, w2 + 2, textHeight);
		}
		if (w1 > 0)
			drawString(g, s1, x, y);
		if (w2 > 0)
			drawString(g, s2, x + w1, y);
		if (w3 > 0)
			drawString(g, s3, x + w1 + w2, y);
	}

	private int selectButton(int x, int y) {
		if (buttonBar == 0)
			return -1;
		if (move.length > 1 && x >= width - buttonHeight && x < width && y >= 0 && y < buttonHeight)
			return 7;
		if (buttonBar == 2) { // only clear (rewind) button present
			if (x >= 0 && x < buttonHeight && y >= height - buttonHeight && y < height)
				return 0;
			return -1;
		}
		if (y < height)
			return -1;
		int buttonX = 0;
		for (int i = 0; i < 7; i++) {
			int buttonWidth = (width - buttonX) / (7 - i);
			if (x >= buttonX && x < buttonX + buttonWidth && y >= height && y < height + buttonHeight)
				return i;
			buttonX += buttonWidth;
		}
		return -1;
	}

	// Mouse event handlers

	private final static int[] buttonAction = { -1, 3, 1, -1, 0, 2, 4, -1 };

	public void mousePressed(MouseEvent e) {
		lastDragX = lastX = e.getX();
		lastDragY = lastY = e.getY();
		toTwist = false;
		buttonPressed = selectButton(lastX, lastY);
		if (buttonPressed >= 0) {
			pushed = true;
			if (buttonPressed == 3) {
				if (!thread.isAnimating()) // special feature
					mirrored = !mirrored;
				else
					thread.stopAnimation();
			} else if (buttonPressed == 0) { // clear everything to the initial setup
				thread.stopAnimation();
				clear();
			} else if (buttonPressed == 7) { // next sequence
				thread.stopAnimation();
				clear();
				curMove = curMove < move.length - 1 ? curMove + 1 : 0;
			} else
				thread.startAnimation(buttonAction[buttonPressed]);
			drawButtons = true;
			repaint();
		} else if (progressHeight > 0 && move.length > 0 && move[curMove].length > 0 && lastY >= height - progressHeight
				&& lastY < height) {
			thread.stopAnimation();
			int len = VectorAndMovementsMath.realMoveLength(move[curMove]);
			int pos = ((lastX - 1) * len * 2 / (width - 2) + 1) / 2;
			pos = Math.max(0, Math.min(len, pos));
			if (pos > 0)
				pos = VectorAndMovementsMath.arrayMovePos(move[curMove], pos);
			if (pos > movePos)
				doMove(cube, move[curMove], movePos, pos - movePos, false);
			if (pos < movePos)
				doMove(cube, move[curMove], pos, movePos - pos, true);
			movePos = pos;
			dragging = true;
			repaint();
		} else {
			if (mirrored)
				lastDragX = lastX = width - lastX;
			if (editable && !thread.isAnimating() && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
					&& (e.getModifiers() & InputEvent.SHIFT_MASK) == 0)
				toTwist = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		dragging = false;
		if (pushed) {
			pushed = false;
			drawButtons = true;
			repaint();
		} else if (twisting && !spinning) {
			twisting = false;
			originalAngle += currentAngle;
			currentAngle = 0.0;
			double angle = originalAngle;
			while (angle < 0.0)
				angle += 32.0 * Math.PI;
			int num = (int) (angle * 8.0 / Math.PI) % 16; // 2pi ~ 16
			if (num % 4 == 0 || num % 4 == 3) { // close enough to a corner
				num = (num + 1) / 4; // 2pi ~ 4
				if (faceTwistDirs[twistedLayer] > 0)
					num = (4 - num) % 4;
				originalAngle = 0;
				natural = true; // the cube in the natural state
				twistLayers(cube, twistedLayer, num, twistedMode); // rotate the facelets
			}
			repaint();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (pushed)
			return;
		if (dragging) {
			thread.stopAnimation();
			int len = VectorAndMovementsMath.realMoveLength(move[curMove]);
			int pos = ((e.getX() - 1) * len * 2 / (width - 2) + 1) / 2;
			pos = Math.max(0, Math.min(len, pos));
			if (pos > 0)
				pos = VectorAndMovementsMath.arrayMovePos(move[curMove], pos);
			if (pos > movePos)
				doMove(cube, move[curMove], movePos, pos - movePos, false);
			if (pos < movePos)
				doMove(cube, move[curMove], pos, movePos - pos, true);
			movePos = pos;
			repaint();
			return;
		}
		int x = mirrored ? width - e.getX() : e.getX();
		int y = e.getY();
		int dx = x - lastX;
		int dy = y - lastY;
		if (editable && toTwist && !twisting && !thread.isAnimating()) { // we do not twist but we can
			lastDragX = x;
			lastDragY = y;
			for (int i = 0; i < dragAreas; i++) { // check if inside a drag area
				double d1 = dragCornersX[i][0];
				double x1 = dragCornersX[i][1] - d1;
				double y1 = dragCornersX[i][3] - d1;
				double d2 = dragCornersY[i][0];
				double x2 = dragCornersY[i][1] - d2;
				double y2 = dragCornersY[i][3] - d2;
				double a = (y2 * (lastX - d1) - y1 * (lastY - d2)) / (x1 * y2 - y1 * x2);
				double b = (-x2 * (lastX - d1) + x1 * (lastY - d2)) / (x1 * y2 - y1 * x2);
				if (a > 0 && a < 1 && b > 0 && b < 1) { // we are in
					if (dx * dx + dy * dy < 144) // delay the decision about twisting
						return;
					dragX = dragDirsX[i];
					dragY = dragDirsY[i];
					double d = Math.abs(dragX * dx + dragY * dy)
							/ Math.sqrt((dragX * dragX + dragY * dragY) * (dx * dx + dy * dy));
					if (d > 0.75) {
						twisting = true;
						twistedLayer = dragLayers[i];
						twistedMode = dragModes[i];
						break;
					}
				}
			}
			toTwist = false;
			lastX = lastDragX;
			lastY = lastDragY;
		}
		dx = x - lastX;
		dy = y - lastY;
		if (!twisting || thread.isAnimating()) { // whole cube rotation
			VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vAdd(eye,
					VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(eyeD, eyeX), dx * -0.016)));
			VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vMul(eyeX, eyeY, eye));
			VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vAdd(eye,
					VectorAndMovementsMath.vScale(VectorAndMovementsMath.vCopy(eyeD, eyeY), dy * 0.016)));
			VectorAndMovementsMath.vNorm(VectorAndMovementsMath.vMul(eyeY, eye, eyeX));
			lastX = x;
			lastY = y;
		} else {
			if (natural)
				splitCube(twistedLayer);
			currentAngle = 0.03 * (dragX * dx + dragY * dy) / Math.sqrt(dragX * dragX + dragY * dragY); // dv * cos a
		}
		repaint();
	}

	private String buttonDescription = "";

	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		String description = "Drag the cube with a mouse";
		if (x >= 0 && x < width) {
			if (y >= height && y < height + buttonHeight || y >= 0 && y < buttonHeight) {
				buttonPressed = selectButton(x, y);
				if (buttonPressed >= 0)
					description = buttonDescriptions[buttonPressed];
				if (buttonPressed == 3 && !thread.isAnimating())
					description = "Mirror the cube view";
			} else if (progressHeight > 0 && move.length > 0 && move[curMove].length > 0 && y >= height - progressHeight
					&& y < height) {
				description = "Current progress";
			}
		}
		if (description != buttonDescription) {
			buttonDescription = description;
			showStatus(description);
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}