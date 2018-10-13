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

@SuppressWarnings("serial")
public class AnimCube extends AnimationThread implements Constants {
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
				thread.repaint();
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
			thread.repaint();
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
}