package visualizer;

import java.applet.Applet;

@SuppressWarnings("serial")
public class AnimationThread extends Applet implements Constants, Runnable {
	private Thread animThread; // thread to perform the animation
	private boolean restarted = false; // animation was stopped
	private boolean interrupted = false; // thread was interrupted
	private boolean animating = false; // animation run
	
	AnimationThread() {
		// Create Animation Thread
		animThread = new Thread(this, "Cube Animator");
		animThread.start();
	}

	public void startAnimation(int mode) {
		synchronized (animThread) {
			stopAnimation();
			if (!AnimCube.demo && (AnimCube.move.length == 0 || AnimCube.move[AnimCube.curMove].length == 0))
				return;
			if (AnimCube.demo && (AnimCube.demoMove.length == 0 || AnimCube.demoMove[0].length == 0))
				return;
			AnimCube.moveDir = 1;
			AnimCube.moveOne = false;
			AnimCube.moveAnimated = true;
			switch (mode) {
			case 0: // play forward
				break;
			case 1: // play backward
				AnimCube.moveDir = -1;
				break;
			case 2: // step forward
				AnimCube.moveOne = true;
				break;
			case 3: // step backward
				AnimCube.moveDir = -1;
				AnimCube.moveOne = true;
				break;
			case 4: // fast forward
				AnimCube.moveAnimated = false;
				break;
			}
			// System.err.println("start: notify");
			animThread.notify();
		}
	}

	public void stopAnimation() {
		synchronized (animThread) {
			restarted = true;
			// System.err.println("stop: notify");
			animThread.notify();
			try {
				// System.err.println("stop: wait");
				animThread.wait();
				// System.err.println("stop: run");
			} catch (InterruptedException e) {
				interrupted = true;
			}
			restarted = false;
		}
	}

	public void run() {
		synchronized (animThread) {
			interrupted = false;
			do {
				if (restarted) {
					animThread.notify();
				}
				try {
					animThread.wait();
				} catch (InterruptedException e) {
					break;
				}
				if (restarted)
					continue;
				boolean restart = false;
				animating = true;
				AnimCube.drawButtons = true;
				int[] mv = AnimCube.demo ? AnimCube.demoMove[0] : AnimCube.move[AnimCube.curMove];
				if (AnimCube.moveDir > 0) {
					if (AnimCube.movePos >= mv.length) {
						AnimCube.movePos = 0;
						AnimCube.initInfoText(mv);
					}
				} else {
					AnimCube.curInfoText = -1;
					if (AnimCube.movePos == 0)
						AnimCube.movePos = mv.length;
				}
				while (true) {
					if (AnimCube.moveDir < 0) {
						if (AnimCube.movePos == 0)
							break;
						AnimCube.movePos--;
					}
					if (mv[AnimCube.movePos] == -1) {
						repaint();
						if (!AnimCube.moveOne)
							sleep(33 * AnimCube.speed);
					} else if (mv[AnimCube.movePos] >= 1000) {
						AnimCube.curInfoText = AnimCube.moveDir > 0 ? mv[AnimCube.movePos] - 1000 : -1;
					} else {
						int num = mv[AnimCube.movePos] % 4 + 1;
						int mode = mv[AnimCube.movePos] / 4 % 6;
						boolean clockwise = num < 3;
						if (num == 4)
							num = 2;
						if (AnimCube.moveDir < 0) {
							clockwise = !clockwise;
							num = 4 - num;
						}
						AnimCube.spin(mv[AnimCube.movePos] / 24, num, mode, clockwise, AnimCube.moveAnimated);
						if (AnimCube.moveOne)
							restart = true;
					}
					if (AnimCube.moveDir > 0) {
						AnimCube.movePos++;
						if (AnimCube.movePos < mv.length && mv[AnimCube.movePos] >= 1000) {
							AnimCube.curInfoText = mv[AnimCube.movePos] - 1000;
							AnimCube.movePos++;
						}
						if (AnimCube.movePos == mv.length) {
							if (!AnimCube.demo)
								break;
							AnimCube.movePos = 0;
							AnimCube.initInfoText(mv);
							for (int i = 0; i < 6; i++)
								for (int j = 0; j < 9; j++)
									cube[i][j] = initialCube[i][j];
						}
					} else
						AnimCube.curInfoText = -1;
					if (interrupted || restarted || restart)
						break;
				}
				animating = false;
				AnimCube.drawButtons = true;
				repaint();
				if (AnimCube.demo) {
					AnimCube.clear();
					AnimCube.demo = false;
				}
			} while (!interrupted);
		}
	} 

	public void sleep(int time) {
		synchronized (animThread) {
			try {
				animThread.wait(time);
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
	}
	
	public boolean isInterupted() {
		return interrupted;
	}
	
	public boolean isRestarted() {
		return restarted;
	}
	
	public boolean isAnimating() {
		return animating;
	}
	
	
}
