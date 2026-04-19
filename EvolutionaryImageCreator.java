import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * EvolutionaryImageCreator
 *
 * Creates an image using an evolutionary algorithm:
 * <ul>
 *     <li>Starts from a uniform background (average color of target).</li>
 *     <li>Gradually adds semi-transparent colored squares to the canvas.</li>
 *     <li>Each square is evolved by its own genetic algorithm to reduce error to the target image.</li>
 * </ul>
 */
public class EvolutionaryImageCreator {

    // ---------------- CONFIG ----------------

    /** Width of the image in pixels. */
    static final int IMAGE_WIDTH = 512;

    /** Height of the image in pixels. */
    static final int IMAGE_HEIGHT = 512;

    /** Number of squares to add to the canvas during one run. */
    static final int TOTAL_FIGURES = 400;

    /** Population size in the GA for a single square. */
    static final int POPULATION_SIZE = 40;

    /** Number of generations for evolving a single square. */
    static final int GENERATIONS_PER_FIGURE = 200;

    /** Probability that each parameter of a square will be mutated. */
    static final double MUTATION_PROBABILITY = 0.3;

    /** Random number generator used in the whole algorithm. */
    static final Random randomGenerator = new Random();


    /** Target image we want to approximate. */
    static BufferedImage TARGET_IMAGE;

    /** Current canvas that is being evolved. */
    static BufferedImage CURRENT_CANVAS;

    /** Sum of squared RGB errors between CURRENT_CANVAS and TARGET_IMAGE. */
    static long totalCanvasError;

    /** Average red channel of the target image (used for background color). */
    static int averageRed;

    /** Average green channel of the target image. */
    static int averageGreen;

    /** Average blue channel of the target image. */
    static int averageBlue;


    /**
     * SquareFigure describes one square to be drawn on the canvas.
     * It stores position, size, color and transparency.
     */
    static class SquareFigure {
        int xPosition;
        int yPosition;
        int size;
        int red, green, blue;
        float transparency;
    }

    /**
     * GeneticIndividual is one individual in the population for GA.
     * It wraps a SquareFigure plus its fitness value.
     */
    static class GeneticIndividual {
        SquareFigure figure;
        double fitnessScore;
    }


    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value value to clamp
     * @param min   minimum allowed value
     * @param max   maximum allowed value
     * @return clamped value in [min, max]
     */
    static int clampValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a color component between 0 and 255.
     *
     * @param color color channel value
     * @return clamped color channel in [0, 255]
     */
    static int clampColorValue(int color) {
        return clampValue(color, 0, 255);
    }

    /**
     * Computes squared distance between two RGB colors.
     *
     * @param red1   first color red
     * @param green1 first color green
     * @param blue1  first color blue
     * @param red2   second color red
     * @param green2 second color green
     * @param blue2  second color blue
     * @return squared distance (dr^2 + dg^2 + db^2)
     */
    static long calculateColorDistance(int red1, int green1, int blue1,
                                       int red2, int green2, int blue2) {
        int deltaRed = red1 - red2;
        int deltaGreen = green1 - green2;
        int deltaBlue = blue1 - blue2;
        return (long) deltaRed * deltaRed
                + (long) deltaGreen * deltaGreen
                + (long) deltaBlue * deltaBlue;
    }

    /**
     * Deletes all previously generated output files in current folder.
     * Files must start with "output_" and end with ".jpg".
     */
    static void removePreviousOutputs() {
        File directory = new File(".");
        File[] oldFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("output_") && (filename.endsWith(".jpg"));
            }
        });
        if (oldFiles == null) {
            return;
        }
        for (File file : oldFiles) {
            if (!file.delete()) {
                System.out.println("Could not delete file: " + file.getName());
            }
        }
    }

    /**
     * Loads a target image from disk.
     *
     * @param filepath path to the image file
     * @return loaded BufferedImage
     * @throws IOException if the file cannot be read
     */
    static BufferedImage loadTargetImage(String filepath) throws IOException {
        BufferedImage image = ImageIO.read(new File(filepath));
        return image;
    }

    /**
     * Saves a BufferedImage to disk as JPG.
     *
     * @param image    image to save
     * @param filename output file name
     */
    static void saveImage(BufferedImage image, String filename) {
        try {
            ImageIO.write(image, "jpg", new File(filename));
        } catch (IOException error) {
            error.printStackTrace();
        }
    }


    /**
     * Computes the average RGB color of the target image.
     * Uses a simple subsampling (step = 4) to speed up the process.
     * Result is stored in {@link #averageRed}, {@link #averageGreen}, {@link #averageBlue}.
     */
    static void calculateTargetAverageColor() {
        long sumRed = 0;
        long sumGreen = 0;
        long sumBlue = 0;
        int samplingStep = 4;
        int sampleCount = 0;

        for (int y = 0; y < IMAGE_HEIGHT; y += samplingStep) {
            for (int x = 0; x < IMAGE_WIDTH; x += samplingStep) {
                int pixelColor = TARGET_IMAGE.getRGB(x, y);
                sumRed += (pixelColor >> 16) & 0xFF;
                sumGreen += (pixelColor >> 8) & 0xFF;
                sumBlue += pixelColor & 0xFF;
                sampleCount++;
            }
        }

        averageRed = (int) (sumRed / sampleCount);
        averageGreen = (int) (sumGreen / sampleCount);
        averageBlue = (int) (sumBlue / sampleCount);

        System.out.printf("Average target color: R=%d G=%d B=%d%n",
                averageRed, averageGreen, averageBlue);
    }


    /**
     * Creates the initial canvas and fills it with the average target color.
     * The canvas size is IMAGE_WIDTH * IMAGE_HEIGHT.
     */
    static void initializeCanvas() {
        CURRENT_CANVAS = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = CURRENT_CANVAS.createGraphics();

        // Fill with average target color
        graphics.setColor(new Color(averageRed, averageGreen, averageBlue));
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        graphics.dispose();
    }

    /**
     * Computes the total squared RGB error between CURRENT_CANVAS and TARGET_IMAGE.
     *
     * @return total error (sum over all pixels and channels)
     */
    static long computeCanvasError() {
        long totalError = 0L;

        for (int y = 0; y < IMAGE_HEIGHT; y++) {
            for (int x = 0; x < IMAGE_WIDTH; x++) {
                int targetRGB = TARGET_IMAGE.getRGB(x, y);
                int canvasRGB = CURRENT_CANVAS.getRGB(x, y);

                int targetRed = (targetRGB >> 16) & 0xFF;
                int targetGreen = (targetRGB >> 8) & 0xFF;
                int targetBlue = targetRGB & 0xFF;

                int canvasRed = (canvasRGB >> 16) & 0xFF;
                int canvasGreen = (canvasRGB >> 8) & 0xFF;
                int canvasBlue = canvasRGB & 0xFF;

                totalError += calculateColorDistance(targetRed, targetGreen, targetBlue,
                        canvasRed, canvasGreen, canvasBlue);
            }
        }

        return totalError;
    }

    /**
     * Blends a foreground color with a background color using alpha.
     *
     * @param background background channel value
     * @param foreground foreground channel value
     * @param alpha      transparency of the foreground (0..1)
     * @return resulting blended channel value
     */
    static int blendColorChannel(int background, int foreground, float alpha) {
        return clampColorValue(Math.round(background * (1.0f - alpha) + foreground * alpha));
    }

    /**
     * Draws a single square on the CURRENT_CANVAS and updates {@link #totalCanvasError}.
     * For each covered pixel it blends the square color with current canvas pixel.
     *
     * @param figure square to apply
     */
    static void applyFigureToCanvas(SquareFigure figure) {
        int startX = clampValue(figure.xPosition, 0, IMAGE_WIDTH - 1);
        int startY = clampValue(figure.yPosition, 0, IMAGE_HEIGHT - 1);
        int endX = clampValue(figure.xPosition + figure.size, 0, IMAGE_WIDTH);
        int endY = clampValue(figure.yPosition + figure.size, 0, IMAGE_HEIGHT);

        long errorChange = 0L;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int targetRGB = TARGET_IMAGE.getRGB(x, y);
                int canvasRGB = CURRENT_CANVAS.getRGB(x, y);

                int targetRed = (targetRGB >> 16) & 0xFF;
                int targetGreen = (targetRGB >> 8) & 0xFF;
                int targetBlue = targetRGB & 0xFF;

                int canvasRed = (canvasRGB >> 16) & 0xFF;
                int canvasGreen = (canvasRGB >> 8) & 0xFF;
                int canvasBlue = canvasRGB & 0xFF;

                // Calculate error before applying figure
                long errorBefore = calculateColorDistance(targetRed, targetGreen, targetBlue,
                        canvasRed, canvasGreen, canvasBlue);

                // Calculate new color after applying figure
                int newRed = blendColorChannel(canvasRed, figure.red, figure.transparency);
                int newGreen = blendColorChannel(canvasGreen, figure.green, figure.transparency);
                int newBlue = blendColorChannel(canvasBlue, figure.blue, figure.transparency);

                // Calculate error after applying figure
                long errorAfter = calculateColorDistance(targetRed, targetGreen, targetBlue,
                        newRed, newGreen, newBlue);

                errorChange += (errorAfter - errorBefore);

                // Update canvas pixel
                int newRGB = (newRed << 16) | (newGreen << 8) | newBlue;
                CURRENT_CANVAS.setRGB(x, y, newRGB);
            }
        }

        totalCanvasError += errorChange;
    }


    /**
     * Creates a square with random position, size, color and transparency.
     * This is used as a base for individuals in the population.
     *
     * @return new random SquareFigure
     */
    static SquareFigure createRandomSquare() {
        SquareFigure square = new SquareFigure();
        square.size = 5 + randomGenerator.nextInt(60);
        square.xPosition = randomGenerator.nextInt(Math.max(1, IMAGE_WIDTH - square.size));
        square.yPosition = randomGenerator.nextInt(Math.max(1, IMAGE_HEIGHT - square.size));

        square.red = randomGenerator.nextInt(256);
        square.green = randomGenerator.nextInt(256);
        square.blue = randomGenerator.nextInt(256);

        square.transparency = 0.2f + randomGenerator.nextFloat() * 0.6f;
        return square;
    }

    /**
     * Creates a new GeneticIndividual with a random SquareFigure.
     *
     * @return new GeneticIndividual
     */
    static GeneticIndividual createRandomIndividual() {
        GeneticIndividual individual = new GeneticIndividual();
        individual.figure = createRandomSquare();
        return individual;
    }

    /**
     * Creates a deep copy of a GeneticIndividual (including its SquareFigure).
     *
     * @param original individual to copy
     * @return cloned individual
     */
    static GeneticIndividual cloneIndividual(GeneticIndividual original) {
        GeneticIndividual clone = new GeneticIndividual();
        SquareFigure figureCopy = new SquareFigure();

        figureCopy.xPosition = original.figure.xPosition;
        figureCopy.yPosition = original.figure.yPosition;
        figureCopy.size = original.figure.size;
        figureCopy.red = original.figure.red;
        figureCopy.green = original.figure.green;
        figureCopy.blue = original.figure.blue;
        figureCopy.transparency = original.figure.transparency;

        clone.figure = figureCopy;
        clone.fitnessScore = original.fitnessScore;
        return clone;
    }

    /**
     * Computes the fitness of a given square.
     * The fitness is defined as negative total error after applying this square on the current canvas.
     * Lower error means higher fitness.
     *
     * @param figure square for which fitness is computed
     * @return fitness value (negative error)
     */
    static double calculateFitness(SquareFigure figure) {
        int startX = clampValue(figure.xPosition, 0, IMAGE_WIDTH - 1);
        int startY = clampValue(figure.yPosition, 0, IMAGE_HEIGHT - 1);
        int endX = clampValue(figure.xPosition + figure.size, 0, IMAGE_WIDTH);
        int endY = clampValue(figure.yPosition + figure.size, 0, IMAGE_HEIGHT);

        long errorChange = 0L;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int targetRGB = TARGET_IMAGE.getRGB(x, y);
                int canvasRGB = CURRENT_CANVAS.getRGB(x, y);
                int targetRed = (targetRGB >> 16) & 0xFF;
                int targetGreen = (targetRGB >> 8) & 0xFF;
                int targetBlue = targetRGB & 0xFF;
                int canvasRed = (canvasRGB >> 16) & 0xFF;
                int canvasGreen = (canvasRGB >> 8) & 0xFF;
                int canvasBlue = canvasRGB & 0xFF;
                long errorBefore = calculateColorDistance(targetRed, targetGreen, targetBlue,
                        canvasRed, canvasGreen, canvasBlue);
                int newRed = blendColorChannel(canvasRed, figure.red, figure.transparency);
                int newGreen = blendColorChannel(canvasGreen, figure.green, figure.transparency);
                int newBlue = blendColorChannel(canvasBlue, figure.blue, figure.transparency);
                long errorAfter = calculateColorDistance(targetRed, targetGreen, targetBlue,
                        newRed, newGreen, newBlue);

                errorChange += (errorAfter - errorBefore);
            }
        }

        long potentialError = totalCanvasError + errorChange;
        return -potentialError; // Lower error = higher fitness
    }

    // ---------------- GENETIC ALGORITHM: OPERATORS ----------------

    /**
     * Performs crossover between two parent individuals.
     * The child square randomly takes each parameter from one of the parents,
     * and sometimes blends size and color.
     *
     * @param parent1 first parent individual
     * @param parent2 second parent individual
     * @return new child individual
     */
    static GeneticIndividual crossover(GeneticIndividual parent1, GeneticIndividual parent2) {
        GeneticIndividual child = new GeneticIndividual();
        SquareFigure childFigure = new SquareFigure();

        // Randomly select traits from parents
        if (randomGenerator.nextBoolean()) {
            childFigure.xPosition = parent1.figure.xPosition;
        } else {
            childFigure.xPosition = parent2.figure.xPosition;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.yPosition = parent1.figure.yPosition;
        } else {
            childFigure.yPosition = parent2.figure.yPosition;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.size = parent1.figure.size;
        } else {
            childFigure.size = parent2.figure.size;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.red = parent1.figure.red;
        } else {
            childFigure.red = parent2.figure.red;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.green = parent1.figure.green;
        } else {
            childFigure.green = parent2.figure.green;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.blue = parent1.figure.blue;
        } else {
            childFigure.blue = parent2.figure.blue;
        }

        if (randomGenerator.nextBoolean()) {
            childFigure.transparency = parent1.figure.transparency;
        } else {
            childFigure.transparency = parent2.figure.transparency;
        }

        // Occasionally blend traits
        if (randomGenerator.nextDouble() < 0.3) {
            childFigure.size = (parent1.figure.size + parent2.figure.size) / 2;
        }
        if (randomGenerator.nextDouble() < 0.3) {
            childFigure.red = (parent1.figure.red + parent2.figure.red) / 2;
            childFigure.green = (parent1.figure.green + parent2.figure.green) / 2;
            childFigure.blue = (parent1.figure.blue + parent2.figure.blue) / 2;
        }

        child.figure = childFigure;
        return child;
    }

    /**
     * Mutates a square by randomly shifting position, size, color and transparency.
     * All changes are bounded so that the square stays inside the canvas
     * and has reasonable size and alpha.
     *
     * @param figure square to mutate
     */
    static void mutateFigure(SquareFigure figure) {
        // Position mutation
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int positionShift = 10;
            figure.xPosition += randomGenerator.nextInt(2 * positionShift + 1) - positionShift;
        }
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int positionShift = 10;
            figure.yPosition += randomGenerator.nextInt(2 * positionShift + 1) - positionShift;
        }

        // Size mutation
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int sizeChange = randomGenerator.nextInt(11) - 5;
            figure.size = clampValue(figure.size + sizeChange, 3, 80);
        }

        // Color mutations
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int colorChange = randomGenerator.nextInt(41) - 20;
            figure.red = clampColorValue(figure.red + colorChange);
        }
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int colorChange = randomGenerator.nextInt(41) - 20;
            figure.green = clampColorValue(figure.green + colorChange);
        }
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            int colorChange = randomGenerator.nextInt(41) - 20;
            figure.blue = clampColorValue(figure.blue + colorChange);
        }

        // Transparency mutation
        if (randomGenerator.nextDouble() < MUTATION_PROBABILITY) {
            float transparencyChange = (randomGenerator.nextFloat() - 0.5f) * 0.3f;
            figure.transparency += transparencyChange;
            figure.transparency = clampValue((int) (figure.transparency * 100), 10, 100) / 100.0f;
        }

        // Boundary checks
        figure.size = clampValue(figure.size, 3, 80);
        figure.xPosition = clampValue(figure.xPosition, 0, IMAGE_WIDTH - figure.size);
        figure.yPosition = clampValue(figure.yPosition, 0, IMAGE_HEIGHT - figure.size);
    }

    /**
     * Selects one individual from the population using tournament selection.
     * A random subset of size {@code tournamentSize} is sampled,
     * and the best individual from this subset is returned.
     *
     * @param population     list of individuals
     * @param tournamentSize number of individuals in each tournament
     * @return selected individual
     */
    static GeneticIndividual tournamentSelection(List<GeneticIndividual> population, int tournamentSize) {
        GeneticIndividual bestCandidate = null;
        for (int i = 0; i < tournamentSize; i++) {
            GeneticIndividual candidate = population.get(randomGenerator.nextInt(population.size()));
            if (bestCandidate == null || candidate.fitnessScore > bestCandidate.fitnessScore) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    /**
     * Runs a genetic algorithm to evolve a single square for the current step.
     * The GA starts from random individuals and uses tournament selection,
     * crossover, mutation and elitism.
     *
     * @param stepNumber index of the current square (1..TOTAL_FIGURES), used only for logging
     * @return best evolved square after GENERATIONS_PER_FIGURE generations
     */
    static SquareFigure evolveSingleFigure(int stepNumber) {
        List<GeneticIndividual> population = new ArrayList<>();

        // Initialize population
        for (int i = 0; i < POPULATION_SIZE; i++) {
            GeneticIndividual individual = createRandomIndividual();
            individual.fitnessScore = calculateFitness(individual.figure);
            population.add(individual);
        }

        population.sort(Comparator.comparingDouble((GeneticIndividual ind) -> ind.fitnessScore).reversed());
        GeneticIndividual bestIndividual = cloneIndividual(population.get(0));

        for (int generation = 1; generation <= GENERATIONS_PER_FIGURE; generation++) {
            List<GeneticIndividual> newPopulation = new ArrayList<>();

            // Elitism: keep the best individual
            newPopulation.add(cloneIndividual(bestIndividual));

            // Fill the rest of the population
            while (newPopulation.size() < POPULATION_SIZE) {
                GeneticIndividual parent1 = tournamentSelection(population, 3);
                GeneticIndividual parent2 = tournamentSelection(population, 3);
                GeneticIndividual child = crossover(parent1, parent2);
                mutateFigure(child.figure);
                child.fitnessScore = calculateFitness(child.figure);
                newPopulation.add(child);
            }

            population = newPopulation;
            population.sort(Comparator.comparingDouble((GeneticIndividual ind) -> ind.fitnessScore).reversed());

            // Update best individual if improved
            if (population.get(0).fitnessScore > bestIndividual.fitnessScore) {
                bestIndividual = cloneIndividual(population.get(0));
            }

            // Progress reporting
            if (generation % 50 == 0 || generation == GENERATIONS_PER_FIGURE) {
                double meanSquaredError = -bestIndividual.fitnessScore / (IMAGE_WIDTH * IMAGE_HEIGHT * 3.0);
                System.out.printf("  Step %3d | Generation %3d: fitness=%.2f, MSE=%.4f%n",
                        stepNumber, generation, bestIndividual.fitnessScore, meanSquaredError);
            }
        }

        return bestIndividual.figure;
    }

    /**
     * Helper class that loads the target image and prepares the initial state:
     * target image, average color, initial canvas and initial error.
     */
    class InitialLoader {

        /**
         * Loads the target image, computes its average color, creates the initial canvas
         * and computes the initial canvas error.
         *
         * @param targetPath path to the target image file
         * @throws IOException if the image cannot be read
         */
        static void loadInitial(String targetPath) throws IOException {
            EvolutionaryImageCreator.TARGET_IMAGE =
                    EvolutionaryImageCreator.loadTargetImage(targetPath);

            EvolutionaryImageCreator.calculateTargetAverageColor();

            EvolutionaryImageCreator.initializeCanvas();

            EvolutionaryImageCreator.totalCanvasError =
                    EvolutionaryImageCreator.computeCanvasError();
        }
    }

    /**
     * Helper class for saving images to disk in a consistent way.
     */
    class ImageSaver {

        /**
         * Saves the given image under the given filename.
         *
         * @param image    image to save
         * @param filename output file name
         */
        static void save(BufferedImage image, String filename) {
            try {
                ImageIO.write(image, "jpg", new File(filename));
            } catch (IOException error) {
                error.printStackTrace();
            }
        }

        /**
         * Saves the initial canvas as "output_initial.jpg".
         *
         * @param canvas initial canvas
         */
        static void saveInitial(BufferedImage canvas) {
            save(canvas, "output_initial.jpg");
        }

        /**
         * Saves the final canvas as "output_final.jpg".
         *
         * @param canvas final canvas
         */
        static void saveFinal(BufferedImage canvas) {
            save(canvas, "output_final.jpg");
        }

        /**
         * Saves an intermediate canvas snapshot for a given step.
         * The filename format is "output_step_XXXX.jpg".
         *
         * @param canvas canvas image
         * @param step   current step index
         */
        static void saveStep(BufferedImage canvas, int step) {
            String filename = String.format("output_step_%04d.jpg", step);
            save(canvas, filename);
            System.out.println("Saved: " + filename);
        }
    }

    // ---------------- MAIN EXECUTION ----------------

    /**
     * Entry point of the program.
     * <ol>
     *     <li>Removes previous outputs, if there are ones.</li>
     *     <li>Loads the target image and prepares the initial canvas.</li>
     *     <li>Runs the evolutionary loop that adds TOTAL_FIGURES squares.</li>
     *     <li>Saves initial, intermediate and final images to disk.</li>
     * </ol>
     *
     * @param args command-line arguments (unused)
     * @throws Exception if loading or processing fails
     */
    public static void main(String[] args) throws Exception {
        removePreviousOutputs();

        InitialLoader.loadInitial("input5.jpg");

        ImageSaver.saveInitial(CURRENT_CANVAS);
        System.out.println("Initial canvas error = " + totalCanvasError);

        // Evolution process
        for (int step = 1; step <= TOTAL_FIGURES; step++) {
            System.out.println("=== Step " + step + " / " + TOTAL_FIGURES + " ===");

            SquareFigure bestFigure = evolveSingleFigure(step);
            applyFigureToCanvas(bestFigure);

            double meanSquaredError = totalCanvasError / (IMAGE_WIDTH * IMAGE_HEIGHT * 3.0);
            System.out.printf("After step %3d: total error=%d, MSE=%.4f%n",
                    step, totalCanvasError, meanSquaredError);

            // Save progress periodically
            if (step % 20 == 0 || step == TOTAL_FIGURES) {
                ImageSaver.saveStep(CURRENT_CANVAS, step);
            }
        }

        // Final image
        ImageSaver.saveFinal(CURRENT_CANVAS);
        System.out.println("Evolution completed. Final error = " + totalCanvasError);
    }

}
