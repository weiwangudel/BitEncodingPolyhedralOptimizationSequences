import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class EncodingOptimizations {
	public class OptSequence {
		public int fuse;    // 0: not applied, 001 Nofuse 010 Smartfuse 100 Maxfuse 
		public int unroll;  // 0: not applied, 0001 u2 0010 u4 0100 u6  1000 u8 
		public boolean vectorized; // vectorized or not
		public boolean tiled;      // tiled or not
		public int level;          // number of levels nested (2,3,4 for polybench
		public int tileSizes[];
		// construct 
		OptSequence(String seq) {
			if (!seq.contains("fuse")) {
				fuse = 0;
			}
			if (seq.contains("nofuse")) {
				fuse = 1;
			}
			if (seq.contains("smartfuse")) {
				fuse = 2;
			}
			if (seq.contains("maxfuse")) {
				fuse = 3;
			}
			
			if (!seq.contains("ufactor")) {
				unroll = 0;
			}
			if (seq.contains("ufactor_2")) {
				unroll = 1;
			}
			if (seq.contains("ufactor_4")) {
				unroll = 2;
			}
			if (seq.contains("ufactor_6")) {
				unroll = 3;
			}
			if (seq.contains("ufactor_8")) {
				unroll = 4;
			}
			
			// vectorization
			if (!seq.contains("vector")) {
				vectorized = false;
			}
			else {
				vectorized = true;
			}
			
			if (!seq.contains("tile")) {
				tiled = false;
			}
			
			// tiled, determine level of tiling and what tile size was used in each level
			else {
				tiled = true;   // first set the tiled bit
				
				// deal with tiling, using regular expression to determine level
				// pay attention to precedence!
				if (seq.matches("(.*)tile(.*)x(.*)x(.*)x(.*)")) {
					level = 4;					
				}
				else if (seq.matches("(.*)tile(.*)x(.*)x(.*)")) {
					level = 3;
				}
				else if (seq.matches("(.*)tile(.*)x(.*)")) {
					level = 2;
				}
				tileSizes = new int[level];
				
				//extract detailed tiling size 
				int start = seq.indexOf("tile_");
				int end = seq.indexOf(".c");
				String tileInfo = seq.substring(start+5, end);
				String[] temp = tileInfo.split("x");
				for (int i=0; i<level; i++) {
					tileSizes[i] = Integer.parseInt(temp[i]);
				}
			}
		}
		
		public void printEncoding() {
			StringBuilder sb = new StringBuilder();
			switch (fuse) {
				case 0: sb.append("0 0 0 "); break;
				case 1: sb.append("0 0 1 "); break;
				case 2: sb.append("0 1 0 "); break;
				case 3: sb.append("1 0 0 "); break;
			}
			
			switch (unroll) {
				case 0: sb.append("0 0 0 0 "); break;
				case 1: sb.append("0 0 0 1 "); break;
				case 2: sb.append("0 0 1 0 "); break;
				case 3: sb.append("0 1 0 0 "); break;
				case 4: sb.append("1 0 0 0 "); break;
			}
			
			if (vectorized) {
				sb.append("1 ");
			}
			else {
				sb.append("0 ");
			}
			
			// deal with no-tiling print
			if (!tiled) {
				sb.append("0 ");
				//append 4 levels of 0s.
				for (int i=0; i<4; i++) {
					sb.append("0 0 0 0 ");
				}
			}
			else {
				sb.append("1 ");
				
				for (int i=0; i<level; i++) {
					switch(tileSizes[i]) {
						case 1: sb.append("0 0 0 1 "); break;
						case 16: sb.append("0 0 1 0 "); break;
						case 32: sb.append("0 1 0 0 "); break;
						case 64: sb.append("1 0 0 0 "); break;
					}	
				}
				
				for (int i=level; i<4; i++) {
					sb.append("0 0 0 0 ");
				}
			}
			
			
			System.out.println(sb.toString());
		}
	}
	
	 //get from a file 
	public void getOptSequences(String optseqFile) {
	    String line = new String();
	    try {

	        // Always wrap FileReader in BufferedReader.
	        // File should be under workspace folder name directory
	        BufferedReader bufferedReader = 
	            new BufferedReader(new FileReader(optseqFile));

	        while((line = bufferedReader.readLine()) != null) {
	        	OptSequence curOptSeq = new OptSequence(line);
	        	curOptSeq.printEncoding();
	        }  
	        // Always close files.
	        bufferedReader.close();      
	     }
	        catch(FileNotFoundException ex) {
	            System.out.println(
	                "Unable to open file '" + 
	                    optseqFile + "'");        
	        }
	       catch(IOException ex) {
	            System.out.println(
	                "Error reading file '" 
	                + optseqFile + "'");          
	            // Or we could just do this: 
	            // ex.printStackTrace();
	        }
	  }  // end getOptSequences
	
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length != 1) {
			System.out.println("java -classpath bin EncodingOptimizations: optseq-file-name!" ); 
			System.out.println("doitgen_--pluto-fuse_maxfuse_--pluto-parallel.c\n"
					+ "doitgen_--pluto-fuse_maxfuse_--pluto-parallel_--pluto-tile_16x16x16x16.c\n");
			return;
		}
		EncodingOptimizations encoding = new EncodingOptimizations();
		encoding.getOptSequences(args[0]);
	}

}
