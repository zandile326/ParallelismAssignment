import java.awt.image.BufferedImage;
import java.io.File;//important classes
import java.io.IOException;
import java.util.concurrent.RecursiveTask;
import javax.imageio.ImageIO;
package serialAbelianSandpile;


public class ParallelGrid extends RecursiveTask<Boolean> {
    private static final int THRESHOLD = 10000;

    private int rows, columns;
    private int[][] grid, updateGrid;
    private int startRow, endRow;
    private int startColumn, endColumn;

   //initialisation of fields in the constructor 
    public ParallelGrid(int[][] initialGrid, int numThreads) {
        this.rows = initialGrid.length;
        this.columns = initialGrid[0].length;
        this.grid = new int[rows + 2][columns + 2];
        this.updateGrid = new int[rows + 2][columns + 2];

        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid[i + 1][j + 1] = initialGrid[i][j];
            }
        }

        this.startRow = 1;
        this.endRow = rows;
        this.startColumn = 1;
        this.endColumn = columns;
    }

    //it create instances of the ParallelGrid class for some subregions 
    private ParallelGrid(int[][] grid, int startRow, int endRow, int startColumn, int endColumn, int[][] updateGrid) {
        this.rows = grid.length;
        this.columns = grid[0].length;
        this.grid = grid;
        this.updateGrid = updateGrid;
        this.startRow = startRow;
        this.endRow = endRow;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    @Override
    //core computation for updating the grid in parallel
    protected Boolean compute() {
        boolean change = false;

        if ((endRow - startRow) * (endColumn - startColumn) <= THRESHOLD) {
           
            for (int i = startRow; i <= endRow; i++) {
                for (int j = startColumn; j <= endColumn; j++) {
                    updateGrid[i][j] = (grid[i][j] % 4) +
                                       (grid[i - 1][j] / 4) +
                                       (grid[i + 1][j] / 4) +
                                       (grid[i][j - 1] / 4) +
                                       (grid[i][j + 1] / 4);
                    if (grid[i][j] != updateGrid[i][j]) {
                        change = true;
                    }
                }
            }
        } else {
            
            int midRow = (startRow + endRow) / 2;
            int midColumn = (startColumn + endColumn) / 2;

            ParallelGrid topLeft = new ParallelGrid(grid, startRow, midRow, startColumn, midColumn, updateGrid);
            ParallelGrid topRight = new ParallelGrid(grid, startRow, midRow, midColumn + 1, endColumn, updateGrid);
            ParallelGrid bottomLeft = new ParallelGrid(grid, midRow + 1, endRow, startColumn, midColumn, updateGrid);
            ParallelGrid bottomRight = new ParallelGrid(grid, midRow + 1, endRow, midColumn + 1, endColumn, updateGrid);

            invokeAll(topLeft, topRight, bottomLeft, bottomRight);

            change = topLeft.join() || topRight.join() || bottomLeft.join() || bottomRight.join();
        }

        return change;
    }
//synchronizes the current state of the grid with the computed values that are new.
    public void nextTimeStep() {
       
        for (int i = 1; i <= rows; i++) {
            for (int j = 1; j <= columns; j++) {
                grid[i][j] = updateGrid[i][j];
            }
        }
    }
//transition grid to image
    public void gridToImage(String file_Name) throws IOException {
        BufferedImage dstImage = new BufferedImage(columns + 2, rows + 2, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < rows + 2; i++) {
            for (int j = 0; j < columns + 2; j++) {
                int a = 255; 
                int r = 0; 
                int g = 0; 
                int b = 0; 

                switch (grid[i][j]) {
                    case 0:
                        a = 0; 
                        break;
                    case 1:
                        g = 255;
                        break;
                    case 2:
                        b = 255;
                        break;
                    case 3:
                        r = 255;
                        break;
                    default:
                        break;
                }

                int dpixel = (a << 24) | (r << 16) | (g << 8) | b;
                dstImage.setRGB(j, i, dpixel); 
            }
        }

        File dstFile = new File(file_Name);
        ImageIO.write(dstImage, "png", dstFile);
    }
    }