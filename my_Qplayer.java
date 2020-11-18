import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.*;

public class my_Qplayer {
	private static int flag = 0;
	private static Map<String, double[][]> state_QVal = new HashMap<>();
	private static Stack<String> historical_states = new Stack<>();
	public static void main(String[] args) {
		try {
			File filename =  new File("input.txt");
			InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(reader);
			int stoneType = Integer.parseInt(br.readLine());
			int[][] myPreState = new int[5][5], board = new int[5][5];
			for(int i = 0; i<5; i++) {
				String strRow = br.readLine();
				for(int j = 0; j<strRow.length();j++) {
					myPreState[i][j]=Character.getNumericValue(strRow.charAt(j));
				}
			}
			for(int i = 0; i<5; i++) {
				String strRow = br.readLine();
				for(int j = 0; j<strRow.length();j++) {
					board[i][j]=Character.getNumericValue(strRow.charAt(j));
				}
			}
			br.close();
			
			File fn1 = new File("historical_states.txt");
			if(!fn1.exists()) {
				fn1.createNewFile();
			}
			InputStreamReader rdHis = new InputStreamReader(new FileInputStream(fn1));
			BufferedReader brHis = new BufferedReader(rdHis);
			String hist = brHis.readLine();
			while(hist!=null) {
				historical_states.push(hist);
				hist=brHis.readLine();
			}

			File fn2 = new File("states_qvalues.txt");
			if(!fn2.exists()) {
				fn2.createNewFile();
			}
			InputStreamReader rdQ = new InputStreamReader(new FileInputStream(fn2));
			BufferedReader brQ = new BufferedReader(rdQ);
			//Set up state_Qval
			String line = brQ.readLine();
			int count = 0;
			String state = "";
			double[][] qTmp = new double[5][5];
			while(line!=null) {
				if(count==0) {
					state=line;
				}else if(count<6) {
					double[] arr = StringToDouble(line.split(" "));
					qTmp[count-1] = arr.clone();
				}
				count++;
				if(count==6) {	
					state_QVal.put(state,cloneMtx(qTmp));
					count=0;
				}
				line=brQ.readLine();
			}
			
			String result = move(board,myPreState,stoneType);
			writeToFile("output.txt",result);
			
		}catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	private static void writeToFile(String filename, String content) {
		try {
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));
			out.write(content);
			out.flush();
			out.close(); 
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	//String[] to double[]
	private static double[] StringToDouble(String[] strArr) {
		double[] res = new double[strArr.length];
		for(int i = 0; i<strArr.length; i++) {
			res[i] = Double.parseDouble(strArr[i]);
		}
		return res;
	}
	
	private static String stateEncode(int[][] board) {
		String res = "";
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				res+=board[i][j];
			}
		}
		return res;
	}
	
	private static String selectBestMove(int[][] board, int[][] myPreState, int stoneType) {
		String state = stateEncode(board);
		double[][] qTable = getQTable(stoneType+state);
		ArrayList<int[]> validMoves = new ArrayList<>();
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				if(isValidMove(board,myPreState,i,j,stoneType)) {
					validMoves.add(new int[] {i,j});
				}else {
					qTable[i][j]=-1;
				}
			}
		}
		if(!validMoves.isEmpty()) {
			int[] move = findMaxPos(qTable,validMoves);
			return move[0]+","+move[1];
		}else {
			return "PASS";
		}
		
	}
	
	private static int[] findMaxPos(double[][] qTable, ArrayList<int[]> validMoves) {
		double max = -Double.MAX_VALUE;
		int[] maxPos = new int[2];
		for(int i = 0; i<validMoves.size();i++) {
			int[] curPos = validMoves.get(i);
			if(max<qTable[curPos[0]][curPos[1]]) {
				maxPos = curPos.clone();
			}
		}
		return maxPos;
	}
	
	private static double findMaxInMtx(double[][] qTable) {
		double max = -Double.MAX_VALUE;
		int[] maxPos = new int[2];
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				max=Math.max(max, qTable[i][j]);
			}
		}
		return max;
	}
	 
	private static double[][] getQTable(String state){
		if(!state_QVal.containsKey(state)) {
			double[][] qTable = new double[5][5];
			for(int i = 0; i<5; i++) {
				Arrays.fill(qTable[i],0.5);
			}
			state_QVal.put(state, qTable);
		}
		return state_QVal.get(state);
	}
	
	private static String gameResult(int[][] board, int myStone) {
		int myScore = 0, enemyScore = 0;
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				if(board[i][j]==myStone) {
					myScore++;
				}else if(board[i][j]==3-myStone) {
					enemyScore++;
				}
			}
		}
		double finalScore = (myStone==1 ? myScore-enemyScore-2.5 : myScore+2.5-enemyScore);
		if(finalScore>0) {
			return "WIN";
		}else if(finalScore==0) {
			return "DRAW";
		}else {
			return "LOSE";
		}
	}
	
	private static String move(int[][] board, int[][] myPreState, int stoneType) {
		String move = selectBestMove(board,myPreState,stoneType);
		if(move.equals("PASS")) {
			learn(board,stoneType,0.7,0.9);
		}else {
			historical_states.push(stoneType+stateEncode(board)+move);
			if(historical_states.size()==12) {
				int[] movement = stringToIntArr(move);
				learn(generateNewBoard(board,movement[0],movement[1],stoneType),stoneType,0.7,0.9);
			}
		}
		Stack<String> stack = new Stack<>();
		while(!historical_states.isEmpty()) {
			stack.push(historical_states.pop());
		}
		writeToHist(stack);
		writeToQ();
		return move;
	}
	
	private static void writeToHist(Stack<String> stack) {
		try {
			File writename = new File("historical_states.txt");
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));
			while(!stack.isEmpty()) {
				out.write(stack.pop());
				out.write("\n");
			}
			out.flush();
			out.close(); 
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void writeToQ() {
		try {
			File writename = new File("states_qvalues.txt");
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));
			for(String key : state_QVal.keySet()) {
				out.write(key);
				out.write("\n");
				double[][] qTable = state_QVal.get(key);
				String oneRow = "";
				for(int i = 0; i<5; i++) {
					for(int j = 0; j<5; j++) {
						oneRow+=qTable[i][j]+" ";
					}
					out.write(oneRow);
					out.write("\n");
					oneRow="";
				}
			}
			out.flush();
			out.close(); 
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void learn(int[][] board, int stoneType, double alpha, double gamma) {
		String gameRes = gameResult(board,stoneType);
		double reward = 0;
		if(gameRes.equals("WIN")) {
			reward=1;
		}else if(gameRes.equals("DRAW")) {
			reward=0.5;
		}else if(gameRes.equals("LOSE")) {
			reward=0;
		}
		double max_q = -1;
		while(!historical_states.isEmpty()) {
			String stateAndMove = historical_states.pop();
			String state = stateAndMove.substring(0,26);
			String moveStr = stateAndMove.substring(26);
			int[] move = stringToIntArr(moveStr);
			double[][] qTable = getQTable(state);
			if(max_q<0) {
				qTable[move[0]][move[1]] = reward;
			}else {
				qTable[move[0]][move[1]] = qTable[move[0]][move[1]] * (1-alpha) + alpha * gamma * max_q;
			}
			max_q=findMaxInMtx(qTable);
		}
	}
	
	private static int[] stringToIntArr(String move) {
		int[] res = new int[2];
		res[0]=Character.getNumericValue(move.charAt(0));
		res[1]=Character.getNumericValue(move.charAt(2));
		return res;
	}
	
	private static boolean isValidMove(int[][] board, int[][] myPreState, int i, int j, int stoneType) {	
		if(board[i][j]!=0) {	//this position has been occupied
			return false;
		}
		
		int[][] newBoard = cloneMtx(board);
		newBoard[i][j] = stoneType;	//if I put my stone here:	
		
		//first check liberty
		flag=0;
		checkIfCanBeCaptured(newBoard,i,j,stoneType, new HashSet<>());	//check liberty   
		
		if(flag==1) {	//if no liberty, then we check if we can capture opStones by putting myStone here
			boolean canCaptureTop = false, canCaptureBot = false, canCaptureLeft = false, canCaptureRight = false;			
			flag=0;
			Set<String> visitedTop = new HashSet<>();	//store opStones' positions
			checkIfCanBeCaptured(newBoard,i-1,j,3-stoneType,visitedTop);	//first check up opStones
			if(flag==1) {
				canCaptureTop=true;
			}
			flag=0;
			Set<String> visitedBot = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i+1,j,3-stoneType,visitedBot);	//check down opStones
			if(flag==1) {
				canCaptureBot=true;
			}
			flag=0;
			Set<String> visitedLeft = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i,j-1,3-stoneType,visitedLeft);	//check left opStones
			if(flag==1) {
				canCaptureLeft=true;
			}
			flag=0;
			Set<String> visitedRight = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i,j+1,3-stoneType,visitedRight);	//check right opStones
			if(flag==1) {
				canCaptureRight=true;
			}	
			if(canCaptureTop||canCaptureBot||canCaptureLeft||canCaptureRight) {	//if any opStones can be captured
				if(canCaptureTop) {
					capture(newBoard,visitedTop);
				}
				if(canCaptureBot) {
					capture(newBoard,visitedBot);
				}
				if(canCaptureLeft) {
					capture(newBoard,visitedLeft);
				}
				if(canCaptureRight) {
					capture(newBoard,visitedRight);
				}
				if(!isSameBoard(myPreState,newBoard)) {	//no violate KO Rule:
					return true;
				}else {	//if violate KO Rule
					return false;
				}
			}else {	//if putting myStone here will cause suicide:
				return false;
			}
		}else{	//if myStone has liberty
			return true;
		}
	}
	
	private static int[][] generateNewBoard(int[][] board, int i, int j, int stoneType){
		int[][] newBoard = cloneMtx(board);
		newBoard[i][j] = stoneType;	//if I put my stone here:	
		
		//first check liberty
		flag=0;
		checkIfCanBeCaptured(newBoard,i,j,stoneType, new HashSet<>());	//check liberty   

		if(flag==1) {	//if no liberty, then we check if we can capture opStones by putting myStone here
			boolean canCaptureTop = false, canCaptureBot = false, canCaptureLeft = false, canCaptureRight = false;			
			flag=0;
			Set<String> visitedTop = new HashSet<>();	//store opStones' positions
			checkIfCanBeCaptured(newBoard,i-1,j,3-stoneType,visitedTop);	//first check up opStones
			if(flag==1) {
				canCaptureTop=true;
			}
			flag=0;
			Set<String> visitedBot = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i+1,j,3-stoneType,visitedBot);	//check down opStones
			if(flag==1) {
				canCaptureBot=true;
			}
			flag=0;
			Set<String> visitedLeft = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i,j-1,3-stoneType,visitedLeft);	//check left opStones
			if(flag==1) {
				canCaptureLeft=true;
			}
			flag=0;
			Set<String> visitedRight = new HashSet<>();
			checkIfCanBeCaptured(newBoard,i,j+1,3-stoneType,visitedRight);	//check right opStones
			if(flag==1) {
				canCaptureRight=true;
			}
			
			if(canCaptureTop||canCaptureBot||canCaptureLeft||canCaptureRight) {	//if any opStones can be captured
				if(canCaptureTop) {
					capture(newBoard,visitedTop);
				}
				if(canCaptureBot) {
					capture(newBoard,visitedBot);
				}
				if(canCaptureLeft) {
					capture(newBoard,visitedLeft);
				}
				if(canCaptureRight) {
					capture(newBoard,visitedRight);
				}
			}
		}
		return newBoard;
	}
	
	private static int[][] cloneMtx(int[][] board){
		int[][] res = new int[5][5];
		for(int i = 0; i<5; i++) {
			res[i]=board[i].clone();
		}
		return res;
	}
	
	private static double[][] cloneMtx(double[][] board){
		double[][] res = new double[5][5];
		for(int i = 0; i<5; i++) {
			res[i]=board[i].clone();
		}
		return res;
	}
	
	private static boolean isSameBoard(int[][] myPreState, int[][] myCurState) {
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				if(myPreState[i][j] != myCurState[i][j]) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static void capture(int[][] board, Set<String> set) {
		for(String s : set) {
			int i = Character.getNumericValue(s.charAt(0));
			int j = Character.getNumericValue(s.charAt(1));
			board[i][j]=0;
		}
	}
	
	private static void checkIfCanBeCaptured(int[][] board, int i, int j, int stoneType, Set<String> visited) {
		if(i<0||j<0||i>4||j>4||visited.contains(i+""+j)||board[i][j]==3-stoneType||flag==-1) {
			return;
		}
		if(board[i][j]==0) {
			flag=-1;
			return;
		}
		visited.add(i+""+j);
		checkIfCanBeCaptured(board,i-1,j,stoneType,visited);
		checkIfCanBeCaptured(board,i+1,j,stoneType,visited);
		checkIfCanBeCaptured(board,i,j-1,stoneType,visited);
		checkIfCanBeCaptured(board,i,j+1,stoneType,visited);
		if(flag==0) {
			flag=1;
		}
	}

}
