
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame ;

// Author: Matic Jurglic

public class Marrh
{
	public static final int EDGE = 0;
	public static final int NOEDGE = 255;
	public static String inputFile = "0007.png";
	
	private static int[][] Rotate90_CW(int[][] mat) {
	    final int M = mat.length;
	    final int N = mat[0].length;
	    int[][] ret = new int[N][M];
	    for (int r = 0; r < M; r++) {
	        for (int c = 0; c < N; c++) {
	            ret[c][M-1-r] = mat[r][c];
	        }
	    }
	    return ret;
	}

	private static int[][] LinkEdges(int[][] after, int DOWN, int ACROSS){
		
		int gap = 1;
    	int k = 0;
    	int count = 0;
    	
    	for (int y = 0; y < DOWN-gap; y++){
    		for (int x = 0; x < ACROSS-gap; x++){
    			if(after[x][y] == EDGE && after[x+1][y] == NOEDGE){
    				k = x+1;
    				while(true){
    					if(after[k][y] == EDGE)
    						break;
    					else{
    						count++;
    						k++;
    						}
    				}
    				if (count < gap){
    					for (int i = 1; i<= gap; i++){
    						after[k-i][y] = EDGE;
    					}
    				}
    				count = 0;
    			}
    		}
    	}
    	
    	return after;
    	
	}
	
	
	private static void Save(int[][] image, int height, int width) throws IOException{
		
		BufferedImage theImage = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
        for(int y = 0; y<width; y++){
            for(int x = 0; x<height; x++){
                int value = image[y][x] << 16 | image[y][x] << 8 | image[y][x];
                theImage.setRGB(y, x, value);
            }
        }
        File outputfile = new File("EDGES_" + inputFile);
        ImageIO.write(theImage, "png", outputfile);
	}
	
	private static int[][] readImage(String fname) throws IOException{
		
		// zaradi neke neumne napake vrže out of bounds za slike, ki nimajo enake višine in širine
		
        File file = new File(fname);// file object to get the file, the second argument is the name of the image file
        BufferedImage image = ImageIO.read(file);
        Raster image_raster = image.getData();
       
        int[][] original;               
        int[] pixel = new int[1];
        int[] buffer = new int[1];
        int width = image_raster.getWidth();
        int height = image_raster.getHeight();
        original = new int[width][height];

        for(int i = 0 ; i < width ; i++)
            for(int j = 0 ; j < height ; j++)
            {
                pixel = image_raster.getPixel(i, j, buffer);
                original[i][j] = pixel[0];
            }
        
        return original;
	}
	
    public static void main(String[] args)  throws IOException {
    
    	int[][] pic = readImage(inputFile);
        int height = pic.length;
        int width = pic[0].length;
        
        // Inicializacija spremenljivk in parametrov
        
        int DOWN = height;
        int ACROSS = width;
       
        int[][] after = new int[DOWN][ACROSS];
        int[][] temp = new int[DOWN][ACROSS];
        int[][] edgeflag = new int[DOWN][ACROSS];
        int i,j,p,q,x,y,mr;
        int center = 50;
        
        double[][] mask = new double[100][100];
        double maskval, sigma, sum, max, lmax, lmin, scaled;
        double TOLERZ, TOLERD;     // how close to 0.0 IS 0.0, how great is the Diff
        
        sigma = 2;
        mr = (int) (3 * sigma);
        TOLERZ = 5;   
        TOLERD = 90;
        max = 0; lmax = 0; lmin = 255;

        // Laplacov filter je diferencialni filter za iskanje obmocij hitrih sprememb (robovi)
        // Ta filter je zelo obcutljiv na sum (noise), zato najprej zgladimo sliko z Gaussovim filtrom
        // To dvoje skupaj se imenuje LoG operator (Laplacian of Gaussian) in je podan s spodnjo enacbo
       
        for(j = -mr; j <= mr; j++){       
           for(i = -mr; i <= mr; i++){    
              maskval = ((2-(((i*i)+(j*j))/(sigma*sigma)))*(Math.exp(-1.0*(((i*i)+(j*j))/(2*(sigma*sigma))))));
              mask[center + j][center + i] = maskval;
           } 
        }
        
        // Apliciramo filter na sliko  s konvolucijo
        
        for (j=mr;j<DOWN-mr;j++){
        	for (i=mr;i<ACROSS-mr;i++){
             sum = 0;
             for (q=-mr;q<=mr;q++){
            	 for (p=-mr;p<=mr;p++){
            		 sum += pic[j+q][i+p] * mask[q + center][p + center];
                   }
            	 }
             temp[j][i] = (int)sum;
             if (Math.abs(sum) > max)  
            	 max = Math.abs(sum);
          }
        }
        
        for (y = 1; y < DOWN-1; y++)
            for (x = 1; x < ACROSS-1; x++){
               if (  Math.abs(temp[y][x]) < TOLERZ ){   
              
                  // Prehodi skozi 0 v mejah tolerance (zero crossings)
            	   
                  if (   (temp[y+1][x-1] * temp[y-1][x+1] < 0
                           &&   
                           Math.abs( Math.abs(temp[y+1][x-1]) - Math.abs(temp[y-1][x+1]) ) > TOLERD  )   ||
                         (temp[y][x-1]   * temp[y][x+1] < 0
                           &&   
                           Math.abs( Math.abs(temp[y][x-1]) - Math.abs(temp[y][x+1]) ) > TOLERD  )       ||
                         (temp[y-1][x-1] * temp[y+1][x+1] < 0
                           &&   Math.abs( Math.abs(temp[y-1][x-1]) - Math.abs(temp[y+1][x+1]) ) > TOLERD  )   ||
                         (temp[y+1][x]   * temp[y-1][x] < 0
                               &&   Math.abs( Math.abs(temp[y+1][x]) - Math.abs(temp[y-1][x]) ) > TOLERD  )  )
                     after[y][x] = EDGE; 
                  else  
                	  after[y][x] = NOEDGE;
               }
               else if (temp[y][x] > TOLERD)
               {
                  for ( j = -1; j <= 2; j++)
                     for ( i = -1; i <= 2; i++)
                     {
                        if (temp[y+j][x+i] < -TOLERD){  
                        	after[y][x] = EDGE;  
                        	edgeflag[y][x] = 1;   
                        	}
                        else if (edgeflag[y][x] != 1)
                           after[y][x] = NOEDGE;
                     }
               }
               else  
            	   after[y][x] = NOEDGE; 
            }

        // Odstranimo šum okoli robov - piksle osamelce
        
        for (y = 2; y < DOWN-2; y++){
            for (x = 2; x < ACROSS-2; x++){
            	if (after[x][y] == EDGE){
            		if (after[x+1][y] == NOEDGE && after[x+1][y-1] == NOEDGE && after[x+1][y+1] == NOEDGE
                     && after[x][y-1] == NOEDGE && after[x][y+1] == NOEDGE && after[x-1][y-1] == NOEDGE
                     && after[x-1][y] == NOEDGE && after[x-1][y+1] == NOEDGE){
            				after[x][y] = NOEDGE;
            		}
            	}
            }
        }
        
       	// Povezovanje robov
        
       	after = LinkEdges(after, DOWN, ACROSS);
       	after = Rotate90_CW(after);
       	after = LinkEdges(after, DOWN, ACROSS);
       	after = Rotate90_CW(after);
       	after = Rotate90_CW(after);
       	after = Rotate90_CW(after);    
        
       	Save(after, height, width);
        	
        JFrame f = new JFrame("Original");
        f.getContentPane().add(new javax.swing.JLabel(new javax.swing.ImageIcon(inputFile)));
        f.setSize(DOWN,ACROSS);
        f.setVisible(true);
            
        f = new JFrame("Detekcija robov");
        f.getContentPane().add(new javax.swing.JLabel(new javax.swing.ImageIcon("EDGES_"+inputFile)));
        f.setSize(DOWN,ACROSS);
        f.setVisible(true);
        
    } 
    
    	
} 