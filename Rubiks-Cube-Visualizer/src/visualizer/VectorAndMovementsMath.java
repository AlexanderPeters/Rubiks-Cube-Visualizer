package visualizer;

public class VectorAndMovementsMath implements Constants {
	public static int realMoveLength(int[] move) {
		int length = 0;
		for (int i = 0; i < move.length; i++)
			if (move[i] < 1000)
				length++;
		return length;
	}

	public static int realMovePos(int[] move, int pos) {
		int rpos = 0;
		for (int i = 0; i < pos; i++)
			if (move[i] < 1000)
				rpos++;
		return rpos;
	}

	public static int arrayMovePos(int[] move, int realPos) {
		int pos = 0;
		int rpos = 0;
		while (true) {
			while (pos < move.length && move[pos] >= 1000)
				pos++;
			if (rpos == realPos)
				break;
			if (pos < move.length) {
				rpos++;
				pos++;
			}
		}
		return pos;
	}

	public static int moveLength(int[] move, int end) {
		int length = 0;
		for (int i = 0; i < move.length && (i < end || end < 0); i++)
			length += turnLength(move[i]);
		return length;
	}

	public static int turnLength(int turn) {
		if (turn < 0 || turn >= 1000)
			return 0;
		int modifier = turn % 4;
		int mode = turn / 4 % 6;
		int n = 1;
		switch (AnimCube.metric) {
		case 1: // quarter-turn metric
			if (modifier == 1 || modifier == 3)
				n *= 2;
		case 2: // face-turn metric
			if (mode == 1 || mode == 4 || mode == 5)
				n *= 2;
		case 3: // slice-turn metric
			if (mode == 3)
				n = 0;
		}
		return n;
	}

	// Various useful vector functions

	public static double[] vCopy(double[] vector, double[] srcVec) {
		vector[0] = srcVec[0];
		vector[1] = srcVec[1];
		vector[2] = srcVec[2];
		return vector;
	}

	public static double[] vNorm(double[] vector) {
		double length = Math.sqrt(vProd(vector, vector));
		vector[0] /= length;
		vector[1] /= length;
		vector[2] /= length;
		return vector;
	}

	public static double[] vScale(double[] vector, double value) {
		vector[0] *= value;
		vector[1] *= value;
		vector[2] *= value;
		return vector;
	}

	public static double vProd(double[] vec1, double[] vec2) {
		return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2];
	}

	public static double[] vAdd(double[] vector, double[] srcVec) {
		vector[0] += srcVec[0];
		vector[1] += srcVec[1];
		vector[2] += srcVec[2];
		return vector;
	}

	public static double[] vSub(double[] vector, double[] srcVec) {
		vector[0] -= srcVec[0];
		vector[1] -= srcVec[1];
		vector[2] -= srcVec[2];
		return vector;
	}

	public static double[] vMul(double[] vector, double[] vec1, double[] vec2) {
		vector[0] = vec1[1] * vec2[2] - vec1[2] * vec2[1];
		vector[1] = vec1[2] * vec2[0] - vec1[0] * vec2[2];
		vector[2] = vec1[0] * vec2[1] - vec1[1] * vec2[0];
		return vector;
	}

	public static double[] vRotX(double[] vector, double angle) {
		double sinA = Math.sin(angle);
		double cosA = Math.cos(angle);
		double y = vector[1] * cosA - vector[2] * sinA;
		double z = vector[1] * sinA + vector[2] * cosA;
		vector[1] = y;
		vector[2] = z;
		return vector;
	}

	public static double[] vRotY(double[] vector, double angle) {
		double sinA = Math.sin(angle);
		double cosA = Math.cos(angle);
		double x = vector[0] * cosA - vector[2] * sinA;
		double z = vector[0] * sinA + vector[2] * cosA;
		vector[0] = x;
		vector[2] = z;
		return vector;
	}

	public static double[] vRotZ(double[] vector, double angle) {
		double sinA = Math.sin(angle);
		double cosA = Math.cos(angle);
		double x = vector[0] * cosA - vector[1] * sinA;
		double y = vector[0] * sinA + vector[1] * cosA;
		vector[0] = x;
		vector[1] = y;
		return vector;
	}
}
