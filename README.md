# Evolutionary-Image-Creator
Evolutionary algorithm (EA) in Java that reconstructs a target image using a sequence of semi transparent coloured squares.

## Overview

**Evolutionary Image Creator** is a Java project that reconstructs a target image using an evolutionary algorithm.

Instead of copying pixels directly, the program gradually approximates the original image by drawing many **semi-transparent colored squares** on a canvas. The algorithm starts from a uniform background filled with the **average color of the target image**, then evolves and adds one square at a time until the final reconstruction is produced.

This project was implemented in **Java** using only the standard library.

---

## How It Works

Each generated figure is a square defined by:

- position `(x, y)`
- size
- color `(R, G, B)`
- transparency `(alpha)`

For every step, the program runs a separate genetic algorithm to find the square that best reduces the error between the current canvas and the target image.

### Main workflow

1. Load the target image.
2. Compute its average RGB color.
3. Fill the initial canvas with that average color.
4. Repeatedly evolve and add new squares.
5. Save intermediate and final results.

The fitness function is based on the **squared RGB error** between the current canvas and the target image.

---

## Main Parameters

The current implementation uses the following configuration:

- **Image size:** `512 x 512`
- **Primitive type:** semi-transparent square
- **Total figures:** `400`
- **Population size:** `40`
- **Generations per figure:** `200`
- **Mutation probability:** `0.3`
- **Selection:** tournament selection
- **Crossover:** uniform crossover with occasional averaging
- **Initial canvas:** average color of the target image


## Project Structure

```text
Evolutionary-Image-Creator/
├── docs/
│   └── Report.pdf
├── output/
│   ├── ...
│   └── RuslanUstinov_Ouput_X_Y_Z.jpg
├── EvolutionaryImageCreator.java
└── README.md
````

---

## How to Run

### Compile

```bash
javac EvolutionaryImageCreator.java
```

### Run

```bash
java EvolutionaryImageCreator
```

---

## Input

The current version loads the target image from:

```java
input5.jpg
```

Before running the program, make sure the input image is available in the project directory with that name, or change the filename in `main()`.

---

## Output

During execution, the program saves:

* the **initial canvas**
* **intermediate snapshots**
* the **final reconstructed image**

The initial image is saved as:

* [Initial Output]

Intermediate images are saved periodically during evolution, for example:

* [Step 20]
* [Step 40]
* [Step 60]
* [Step 80]
* [Step 100]
* [Step 200]
* [Step 300]
* [Step 400]

The final reconstructed image is saved as:

* [Final Output]

---

## Example of Best Output Images

* [Output_images](output/)


---

## Report

The full report for this project is available here:

* [Project Report](docs/Report.pdf)

---

## Features

* evolutionary image reconstruction
* approximation with semi-transparent squares
* separate genetic algorithm for each new figure
* automatic saving of intermediate results
* simple Java implementation without external libraries

---

## Notes

* The program removes previously generated `.jpg` output files before starting a new run.
* The current implementation is designed for `512 x 512` images.
* Output quality depends on random initialization and the evolutionary process, so different runs may produce slightly different results.

---

## Author

**Ruslan Ustinov**
Innopolis University

