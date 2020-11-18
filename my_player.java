import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class my_player {

	public static void main(String[] args) {
		try {
			String pathname = "input.txt";
			File filename =  new File(pathname);
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
		
			String[] result = putStone_MinMaxAlphaBeta(board,myPreState,0,stoneType,-Double.MAX_VALUE, Double.MAX_VALUE, 0);
			writeToFile("output.txt",result[0]);
			
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
	
	private static String[] putStone_MinMaxAlphaBeta(int[][] board, int[][] myPreState, int curPlayer, int stoneType, double alpha, double beta, int deepth) {
		boolean putDown = false;
		
		if(isTerminal(deepth)) {
			String[] res = new String[2];
			res[0] = null;	//move
			res[1] = String.valueOf(curPlayer==0 ? (stoneType==1 ? stoneDiff(board,stoneType)-2.5 : stoneDiff(board,stoneType)+2.5) 
					: (stoneType==1 ? stoneDiff(board,3-stoneType)+2.5 : stoneDiff(board,3-stoneType)-2.5));	//utility
			return res;
		}
		
		int[][] expanded_order = new int[25][2];
		moveOrderSetUp(expanded_order);
		ArrayList<String[]> scoreArray = new ArrayList<>();
		double MAX = -Double.MAX_VALUE;
		double MIN = Double.MAX_VALUE;
		String calcMove = "";
		String calcScore = "";
		
		for(int i = 0; i<expanded_order.length; i++) {			
			int[] intendedMove = expanded_order[i];
			if(board[intendedMove[0]][intendedMove[1]]!=0) {	//this position has been occupied
				continue;
			}
			int[][] newBoard = cloneMtx(board);
			newBoard[intendedMove[0]][intendedMove[1]] = stoneType;	//if I put my stone here:	
			
			int liberty=countLiberty(newBoard,intendedMove[0],intendedMove[1],stoneType, new HashSet<>());	//check liberty   
			int eatNum = 0;
			if(liberty==0) {	//if no liberty, then we check if we can capture opStones by putting myStone here	
				eatNum = removeDeadStones(newBoard,intendedMove,stoneType);
				if(eatNum>0) {	
					if(!isSameBoard(myPreState,newBoard)) {	//no violate KO Rule:
						putDown = true;
						String[] opRes = putStone_MinMaxAlphaBeta(newBoard,board,1-curPlayer,3-stoneType,alpha,beta,deepth+1);	//what will my op do if i do this?
						String[] curMove = new String[2];
						curMove[0]=intendedMove[0]+","+intendedMove[1];
						curMove[1]=opRes[1];
						scoreArray.add(curMove);	//store my curMove and corresponding score to scoreArray
					}else {	//if violate KO Rule
						continue;
					}
				}else {	//if putting myStone here will cause suicide:
					continue;
				}
			}else if(liberty>0){	//if myStone has liberty
				putDown = true;
				eatNum = removeDeadStones(newBoard,intendedMove,stoneType);	//If we can, we eat, update newBoard
				String[] opRes = putStone_MinMaxAlphaBeta(newBoard,board,1-curPlayer,3-stoneType,alpha,beta,deepth+1);
				String[] curMove = new String[2];
				curMove[0]=intendedMove[0]+","+intendedMove[1];
				curMove[1]=opRes[1];
				scoreArray.add(curMove);	
			}

			double endangeredScore = 2*(numOfEndangeredPlaces(newBoard,stoneType));
			
			if(curPlayer==0) {	//me
				for(int k = 0; k<scoreArray.size();k++) {
					double curScore = Double.parseDouble(scoreArray.get(k)[1]) + 2*eatNum - endangeredScore + stoneDiff(newBoard,stoneType);
					if(MAX<curScore) {
						MAX=curScore;
						calcScore = String.valueOf(MAX);
						calcMove = scoreArray.get(k)[0];
						if(beta<=MAX) {
							String[] tmpRes = new String[2];
							tmpRes[0]=calcMove;
							tmpRes[1]=calcScore;
							return tmpRes;
						}
						if(alpha<MAX) {
							alpha=MAX;
						}
					}
				}
			}else if(curPlayer==1) {	//op
				for(int k = 0; k<scoreArray.size();k++) {
					double curScore = Double.parseDouble(scoreArray.get(k)[1]) - 2*eatNum + endangeredScore + stoneDiff(newBoard,3-stoneType);
					if(MIN>curScore) {
						MIN=curScore;
						calcScore = String.valueOf(MIN);
						calcMove = scoreArray.get(k)[0];
						if(alpha>=MIN) {
							String[] tmpRes = new String[2];
							tmpRes[0]=calcMove;
							tmpRes[1]=calcScore;
							return tmpRes;
						}
						if(beta>MIN) {
							beta=MIN;
						}
					}
				}
			}		
		}

		String[] res = new String[2];
		if(!putDown) {	//If nowhere to move we pass
			res[0]="PASS";
			res[1] = String.valueOf(curPlayer==0 ? (stoneType==1 ? stoneDiff(board,stoneType)-2.5 : stoneDiff(board,stoneType)+2.5) 
					: (stoneType==1 ? stoneDiff(board,3-stoneType)+2.5 : stoneDiff(board,3-stoneType)-2.5));
		}else {
			res[0]=calcMove;
			res[1]=calcScore;
		}
		return res;
	}
	
	private static int removeDeadStones(int[][] newBoard, int[] intendedMove, int stoneType) {	//return the num of eaten stones
		boolean canCaptureTop = false, canCaptureBot = false, canCaptureLeft = false, canCaptureRight = false;
		Set<String> visitedTop = new HashSet<>();	//store opStones' positions	
		if(countLiberty(newBoard,intendedMove[0]-1,intendedMove[1],3-stoneType,visitedTop)==0) {	//first check up opStones
			canCaptureTop=true;
		}

		Set<String> visitedBot = new HashSet<>();
		if(countLiberty(newBoard,intendedMove[0]+1,intendedMove[1],3-stoneType,visitedBot)==0) {	//check down opStones
			canCaptureBot=true;
		}
		
		Set<String> visitedLeft = new HashSet<>();
		if(countLiberty(newBoard,intendedMove[0],intendedMove[1]-1,3-stoneType,visitedLeft)==0) {	//check left opStones
			canCaptureLeft=true;
		}

		Set<String> visitedRight = new HashSet<>();		
		if(countLiberty(newBoard,intendedMove[0],intendedMove[1]+1,3-stoneType,visitedRight)==0) {	//check right opStones
			canCaptureRight=true;
		}
		
		Set<String> eaten = new HashSet<>();			
		if(canCaptureTop) {
			for(String pos : visitedTop) {
				if(!eaten.contains(pos)) {
					eaten.add(pos);
				}
			}
		}
		if(canCaptureBot) {
			for(String pos : visitedBot) {
				if(!eaten.contains(pos)) {
					eaten.add(pos);
				}
			}
		}
		if(canCaptureLeft) {
			for(String pos : visitedLeft) {
				if(!eaten.contains(pos)) {
					eaten.add(pos);
				}
			}
		}
		if(canCaptureRight) {
			for(String pos : visitedRight) {
				if(!eaten.contains(pos)) {
					eaten.add(pos);
				}
			}
		}
		capture(newBoard,eaten);
		return eaten.size();
	}
	
	private static int numOfEndangeredPlaces(int[][] board, int stoneType) {
		Map<Integer,Set<String>> map = new HashMap<>();
		Set<String> visited = new HashSet<>();
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				if(board[i][j]==stoneType && !visited.contains(i+""+j)) {
					Set<String> tmp = new HashSet<>();
					int liberty = countLiberty(board,i,j,stoneType,tmp);
					map.putIfAbsent(liberty,new HashSet<>());
					for(String pos : tmp) {
						if(!visited.contains(pos)) {
							visited.add(pos);
						}
						map.get(liberty).add(pos);
					}	
				}
			}
		}
		if(map.containsKey(1)) {
			return map.get(1).size();
		}else {
			return 0;
		}
		
	}
	
	private static int countLiberty(int[][] board, int i, int j, int stoneType, Set<String> visited) {
		int res = 0;
		if(i<0||j<0||i>4||j>4||board[i][j]==3-stoneType||visited.contains(i+""+j)) {
			return 0;
		}
		if(board[i][j]==0) {
			return 1;
		}
		visited.add(i+""+j);
		return res += countLiberty(board,i-1,j,stoneType,visited)
				+countLiberty(board,i+1,j,stoneType,visited)
				+countLiberty(board,i,j-1,stoneType,visited)
				+countLiberty(board,i,j+1,stoneType,visited);
	}
	
	private static int[][] cloneMtx(int[][] board){
		int[][] res = new int[5][5];
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
		if(set.size()==0) {
			return;
		}
		for(String s : set) {
			int i = Character.getNumericValue(s.charAt(0));
			int j = Character.getNumericValue(s.charAt(1));
			board[i][j]=0;
		}
	}
	
	private static double stoneDiff(int[][] board, int myStone) {
		double black = 0, white = 0;
		for(int i = 0; i<5; i++) {
			for(int j = 0; j<5; j++) {
				if(board[i][j]==1) {
					black++;
				}else if(board[i][j]==2) {
					white++;
				}
			}
		}
		return myStone==1 ? black-white : white-black;
	}
	
	private static void moveOrderSetUp(int[][] expanded_order) {
		expanded_order[0] = new int[] {1,1};
		expanded_order[1] = new int[] {3,3};
		expanded_order[2] = new int[] {3,1};
		expanded_order[3] = new int[] {1,3};
		expanded_order[4] = new int[] {2,2};
		expanded_order[5] = new int[] {0,2};
		expanded_order[6] = new int[] {4,2};
		expanded_order[7] = new int[] {2,0};
		expanded_order[8] = new int[] {2,4};
		expanded_order[9] = new int[] {0,1};
		expanded_order[10] = new int[] {0,3};
		expanded_order[11] = new int[] {1,4};
		expanded_order[12] = new int[] {3,4};
		expanded_order[13] = new int[] {4,3};
		expanded_order[14] = new int[] {4,1};
		expanded_order[15] = new int[] {3,0};
		expanded_order[16] = new int[] {1,0};
		expanded_order[17] = new int[] {1,2};
		expanded_order[18] = new int[] {3,2};
		expanded_order[19] = new int[] {2,1};
		expanded_order[20] = new int[] {2,3};
		expanded_order[21] = new int[] {0,0};
		expanded_order[22] = new int[] {4,4};
		expanded_order[23] = new int[] {0,4};
		expanded_order[24] = new int[] {4,0};
	}
	
	private static boolean isTerminal(int deepth) {
		return deepth>=6;
	}
}
