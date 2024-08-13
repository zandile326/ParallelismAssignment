package serialAbelianSandpile;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
public class ParallelGrid {
private int[][] localUpdateGrid;
    private ForkJoinPool pool;

    private int rows, columns;
    private int[][] grid;
        private static final int THRESHOLD = 10;

    public ParallelGrid(int rows, int columns, int numThreads) {
        this.rows = rows + 2; 
        this.columns = columns + 2; 
        this.grid = new int[this.rows][this.columns];
        this.localUpdateGrid = new int[this.rows][this.columns];
        this.pool = new ForkJoinPool(numThreads); 
        initializeGrid();
    }

    public ParallelGrid(int[][] initialGrid, int numThreads) {
        this.rows = initialGrid.length + 2;
        this.columns = initialGrid[0].length + 2;
        this.grid = new int[this.rows][this.columns];
        this.localUpdateGrid = new int[this.rows][this.columns];
        this.pool = new ForkJoinPool(numThreads); 
        initializeGrid(initialGrid);
    }

    private void initializeGrid() {
       
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid[i][j] = 0;
                localUpdateGrid[i][j] = 0;
            }
        }
    }

    private void initializeGrid(int[][] initialGrid) {
        initializeGrid(); // Initialize the grid
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < columns - 1; j++) {
                grid[i][j] = initialGrid[i - 1][j - 1];
            }
        }
    }

    public void runSimulation() {
    int steps = 0;
        boolean changed;
        long startTime = System.currentTimeMillis();
                do {
            changed = update();
            steps++;
        } while (changed);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Rows: " + (rows - 2)+", "+"Columns: " + (columns - 2));
        System.out.println("Simulation complete, writing image...");
        System.out.println("Number of steps to stable state: " + steps);
        System.out.println("Time: " + duration + " ms");

        try {
            gridToImage("output.png");
        } catch (IOException e) {
            System.err.println("Error writing image: " + e.getMessage());
        }
    }

    private boolean update() {
        boolean[] hasChange = {false};
        GridUpdateTask task = new GridUpdateTask(1, rows - 1, 1, columns - 1, hasChange);
        pool.invoke(task);
        boolean changeOccurred = hasChange[0];

        if (changeOccurred) {
            copyLocalToGrid();
        }

        return changeOccurred;
    }

    private void copyLocalToGrid() {
  
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < columns - 1; j++) {
                grid[i][j] = localUpdateGrid[i][j];
            }
        }
    }

    private class GridUpdateTask extends RecursiveAction {
        private int startRow, endRow, startCol, endCol;
        private boolean[] change;

        GridUpdateTask(int startRow, int endRow, int startCol, int endCol, boolean[] change) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.startCol = startCol;
            this.endCol = endCol;
            this.change = change;
        }

        @Override
        protected void compute() {
            if ((endRow - startRow) <= THRESHOLD && (endCol - startCol) <= THRESHOLD) {
               
                for (int i = startRow; i < endRow; i++) {
                    for (int j = startCol; j < endCol; j++) {
                        int currentValue = grid[i][j];
                        int newValue = (currentValue % 4) +
                                (grid[i - 1][j] / 4) +
                                (grid[i + 1][j] / 4) +
                                (grid[i][j - 1] / 4) +
                                (grid[i][j + 1] / 4);

                        if (currentValue != newValue) {
                            change[0] = true;
                        }

                        localUpdateGrid[i][j] = newValue;
                    }
                }
            } else {
          int midRow = (startRow + endRow) / 2;//spill the tasks
                int midCol = (startCol + endCol) / 2;

                invokeAll(
                        new GridUpdateTask(startRow, midRow, startCol, midCol, change),
                        new GridUpdateTask(startRow, midRow, midCol, endCol, change),
                        new GridUpdateTask(midRow, endRow, startCol, midCol, change),
                        new GridUpdateTask(midRow, endRow, midCol, endCol, change)
                );
            }
        }
    }

   
    public void printGrid() {
        
        System.out.printf("Grid:\n");
        System.out.printf("+");
        for (int j = 1; j < columns - 1; j++) System.out.printf("  --");
        System.out.printf("+\n");
        for (int i = 1; i < rows - 1; i++) {
            System.out.printf("|");
            for (int j = 1; j < columns - 1; j++) {
                if (grid[i][j] > 0)
                    System.out.printf("%4d", grid[i][j]);
                else
                    System.out.printf("    ");
            }
            System.out.printf("|\n");
        }
        System.out.printf("+");
        for (int j = 1; j < columns - 1; j++) System.out.printf("  --");
        System.out.printf("+\n\n");
    }
 public void setAll(int value) {
       
        for (int k = 1; k < rows - 1; k++) {
            for (int j = 1; j < columns - 1; j++) {
                grid[k][j] = value;
            }
        }
    }

    public void gridToImage(String fileName) throws IOException {
       
        BufferedImage dstImage = new BufferedImage(rows - 2, columns - 2, BufferedImage.TYPE_INT_ARGB);
        int b = 0;
        int r = 0;
        int a = 0;
        int g = 0;
        

        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < columns - 1; j++) {
                b = 0;
                r = 0;
                g = 0;
                

                switch (grid[i][j]) {
                    case 0:
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
                int dpixel = (0xff000000)
                        | (a << 24)
                        | (r << 16)
                        | (g << 8)
                        | b;
                dstImage.setRGB(i - 1, j - 1, dpixel); 
            }
        }

        File dstFile = new File(fileName);
        ImageIO.write(dstImage, "png", dstFile);
    }

    public static void main(String[] args) {
        
        int rows = 65;
        int columns = 65;//trying to test when the dimensions are static to 65(test)
        int numThreads = Runtime.getRuntime().availableProcessors();

        ParallelGrid grid = new ParallelGrid(rows, columns, numThreads);
        grid.setAll(1); 
        grid.runSimulation(); // Run the simulation 
    }
}