import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.print.attribute.ResolutionSyntax;

import java.util.Scanner;

public class Clustering {
	private static final int threshold = 10;
	static int N;
	/* doesn't change */
	static ArrayList<Data> trainingDataSet;
	static ArrayList<Data> testDataSet;
	static HashMap<Integer, Integer> labels = new HashMap<>();
	static ArrayList<Double> ProbC = new ArrayList<>();

	/* values change */
	ArrayList<Double[]> clusterVectorsMean = new ArrayList<>();
	ArrayList<ArrayList<Data>> clusters = new ArrayList<>();
	ArrayList<ArrayList<Double>> probVgivenC = new ArrayList<>();
	ArrayList<ArrayList<Double>> probCgivenV = new ArrayList<>();
	ArrayList<Integer> estimatedCgivenV = new ArrayList<>();

	class Data {
		int label;
		Double[] vector;
	}

	private double distance(Double[] p1, Double[] p2) {
		return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
	}

	private Double[] mean(ArrayList<Data> array) {
		Double[] mean = new Double[2];
		double sum1 = 0;
		double sum2 = 0;
		for (Data d : array) {
			sum1 += d.vector[0];
			sum2 += d.vector[1];
		}
		mean[0] = sum1 / array.size();
		mean[1] = sum2 / array.size();
		return mean;
	}

	private int minDistanceCluster(Double[] p1, ArrayList<Double[]> array) {
		if (null == array) {
			System.out.println("empty array given. so no min cluster index");
			return -1;
		}
		double minDistance = distance(p1, array.get(0));
		int minIndex = 0;
		for (int i = 1; i < array.size(); i++) {
			double curDistance = distance(p1, array.get(i));
			if (curDistance < minDistance) {
				minDistance = curDistance;
				minIndex = i;
			}

		}
		return minIndex;
	}

	/* read the file */
	private static ArrayList<Data> readFiledata(String filePath, boolean isTrainingSet) {
		ArrayList<Data> dataSet = new ArrayList<>();
		try {
			FileReader fr = new FileReader(filePath);
			BufferedReader b = new BufferedReader(fr);
			String line = b.readLine();
			String[] splitStrings;
			int count = 0;
			while (null != line) {
				count++;
				splitStrings = line.split("\\s+");
				if (splitStrings.length != 3) {
					System.out.println("bad input file");
				} else {

					int label = Integer.valueOf(splitStrings[0]);
					double point1 = Double.valueOf(splitStrings[1]);
					double point2 = Double.valueOf(splitStrings[2]);

					Clustering c = new Clustering();
					Data d = c.new Data();
					d.label = label;
					d.vector = new Double[] { point1, point2 };

					dataSet.add(d);
					if (isTrainingSet) {
						if (!labels.containsKey(label)) {
							labels.put(label, 1);

						} else {
							labels.put(label, labels.get(label) + 1);
						}
					}

				}
				line = b.readLine();
			}
			if (isTrainingSet)
				N = count;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataSet;
	}

	/* K-means clustering */
	private void kClustering(int k) {
		if (k > N) {
			System.out.println("cant have clusters more than the given data set");
			return;
		}
		if (trainingDataSet == null) {
			System.out.println("training data is not available yet");
			return;
		}

		int iter = 0;
		boolean vectorUnAssigned = false;
		do {
			vectorUnAssigned = false;
			iter++;
			clusterVectorsMean.clear();
			clusters.clear();

			ArrayList<Integer> indices = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				int index;
				index = (int) (Math.random() * N);
				while (indices.contains(index)) {
					//System.out.println("random value " + index + " already present. guessing again");
					index = (int) (Math.random() * N);
				}
				indices.add(index);

				Double[] d = new Double[2];
				d[0] = trainingDataSet.get(index).vector[0];
				d[1] = trainingDataSet.get(index).vector[1];
				clusterVectorsMean.add(d);

				ArrayList<Data> list = new ArrayList<>();
				clusters.add(list);

			}

			for (int i = 0; i < trainingDataSet.size(); i++) {
				int min_distance_cluster = minDistanceCluster(trainingDataSet.get(i).vector, clusterVectorsMean);
				clusters.get(min_distance_cluster).add(trainingDataSet.get(i));
			}
			// System.out.println("clusterVectorsMean size " +
			// clusterVectorsMean.size());
			for (int i = 0; i < k; i++) {
				clusterVectorsMean.set(i, mean(clusters.get(i)));
			}
			boolean isClusterChanged = false;
			do {
				isClusterChanged = false;
				for (int i = 0; i < k; i++) {
					ArrayList<Data> list = clusters.get(i);
					for (Iterator<Data> iterator = list.iterator(); iterator.hasNext();) {
						Data d = iterator.next();
						int newCluster = minDistanceCluster(d.vector, clusterVectorsMean);
						if (newCluster != i) {
							isClusterChanged = true;
							clusters.get(newCluster).add(d);
							iterator.remove();
						}
					}
				}
			} while (isClusterChanged);

			for (int i = 0; i < clusters.size(); i++) {
				ArrayList<Data> list = clusters.get(i);
				if (list.size() == 0) {
					//System.out.println("vector " + i + " is unassigned");
					vectorUnAssigned = true;
				}
			}
		} while (vectorUnAssigned);
		//System.out.println("  total Iterations : " + iter);
	}

	private static void computeProbofC() {
		Iterator<Map.Entry<Integer, Integer>> it = labels.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, Integer> pair = it.next();
			double label = (int) pair.getKey();
			double count = (int) pair.getValue();
			Double d = count / trainingDataSet.size();
			int c = (int) label;
			System.out.println("  P(c=" + c + ") = " + d);
			ProbC.add(d);
		}
		System.out.print("\n");
	}

	private void computePrbVgivenC(boolean resultsNeeded) {
		Iterator<Map.Entry<Integer, Integer>> it = labels.entrySet().iterator();

		while (it.hasNext()) {
			ArrayList<Double> probVgivenClist = new ArrayList<>();
			probVgivenC.add(probVgivenClist);

			Entry<Integer, Integer> pair = it.next();
			int c = pair.getKey();
			for (int j = 0; j < clusters.size(); j++) {
				double countInCluster = 0;
				ArrayList<Data> list = clusters.get(j);
				for (int k = 0; k < list.size(); k++) {
					Data d = list.get(k);
					int label = d.label;
					if (label == c) {
						countInCluster++;
					}
				}
				/* P(V|C) = P(V,C)/P(C) */
				Double prob = countInCluster / (trainingDataSet.size() * ProbC.get(c));
				probVgivenC.get(c).add(j, prob);
			}
		}
		if(resultsNeeded){
			for (int i = 0; i < probVgivenC.size(); i++) {
				ArrayList<Double> list = probVgivenC.get(i);
				System.out.print("  P(V|c=" + i + ") = <");
				for (int j = 0; j < list.size(); j++) {
					System.out.print(probVgivenC.get(i).get(j) + ",");
				}
				System.out.println(">");
			}
		}
		

	}

	private void computeProbCgivenV(boolean resultsNeeded) {
		/*
		 * estimatedCgivenV.clear(); probCgivenV.clear();
		 */

		for (int i = 0; i < this.clusters.size(); i++) {
			ArrayList<Double> probCgivenVList = new ArrayList<>();
			this.probCgivenV.add(probCgivenVList);

			Iterator<Map.Entry<Integer, Integer>> it = labels.entrySet().iterator();
			double alpha = 0;
			double sum = 0;
			while (it.hasNext()) {
				Entry<Integer, Integer> pair = it.next();
				int c = (int) pair.getKey();
				double prob = this.probVgivenC.get(c).get(i) * ProbC.get(c);
				this.probCgivenV.get(i).add(c, prob);
				sum += prob;
			}
			alpha = 1 / sum;
			if(resultsNeeded)
				System.out.print("  P(C|v=" + i + ") = <");
			double maxProb = 0;
			int estimatedClass = 0;
			for (int k = 0; k < this.probCgivenV.get(i).size(); k++) {
				double initialValue = this.probCgivenV.get(i).get(k);
				double finalValue = alpha * initialValue;
				this.probCgivenV.get(i).set(k, finalValue);
				if(resultsNeeded)
					System.out.print(this.probCgivenV.get(i).get(k) + ",");
				if (finalValue > maxProb) {
					maxProb = finalValue;
					estimatedClass = k;
				}
			}
			this.estimatedCgivenV.add(estimatedClass);
			if(resultsNeeded)
				System.out.println(">");
		}

		/*for (int i = 0; i < this.estimatedCgivenV.size(); i++) {
			System.out.println("  estimated C when V = " + i + " : " + this.estimatedCgivenV.get(i));
		}*/
	}

	public double classificationError(ArrayList<Data> testSet) {
		Double[] errorArray = new Double[testSet.size()];
		double errorSum = 0;
		for (int i = 0; i < testSet.size(); i++) {
			int index = minDistanceCluster(testSet.get(i).vector, this.clusterVectorsMean);
			int estimatedC = this.estimatedCgivenV.get(index);
			double error = Math.abs(estimatedC - testSet.get(i).label);
			if(error != 0)
				error = 1;
			errorArray[i] = error;
			errorSum += error;
		}
		return errorSum / testSet.size();
	}

	private void clearData() {
		this.clusterVectorsMean.clear();
		this.clusters.clear();
		this.probVgivenC.clear();
		this.probCgivenV.clear();
		this.estimatedCgivenV.clear();
	}

	private static void sample(int k, int loops) {
		
		double sum = 0;
		double[] errorRates = new double[loops];
		//System.out.println("  Classification error rate in each loop : ");
		for (int i = 0; i < loops; i++) {
			Clustering c1 = new Clustering();
			c1.kClustering(k);
			c1.computePrbVgivenC(false);
			c1.computeProbCgivenV(false);
			double errorRate = c1.classificationError(testDataSet);
			//System.out.println("  loop " + i + ":" + errorRate);
			errorRates[i] = errorRate;
			sum += errorRate;
		}

		double mean = sum / 10;
		System.out.println("  Average Classification error rate for k="+k+" : " + mean);
		double diffSquaredSum = 0;
		for (int i = 0; i < loops; i++) {
			double diff = errorRates[i] - mean;
			double diffSquare = Math.pow(diff, 2);
			diffSquaredSum += diffSquare;
		}
		double meanSquared = diffSquaredSum / 10;
		double sd = Math.sqrt(meanSquared);
		System.out.println("  SD of classification error rate for k="+k+" : " + sd);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Scanner sc = new Scanner(System.in);

		System.out.println("enter training file path");
		String trainingPath = sc.nextLine();

		System.out.println("enter test data file path");
		String testPath = sc.nextLine();

		/*
		 * System.out.println("enter the value of k"); int k = sc.nextInt();
		 */

		Clustering c = new Clustering();
		trainingDataSet = readFiledata(trainingPath, true);
		c.kClustering(10);

		System.out.println("Step 1.");
		System.out.println("  Clusters[Mean Vector] :-");
		for (int i = 0; i < c.clusterVectorsMean.size(); i++) {
			System.out.print("  cluster " + i + "[" + c.clusterVectorsMean.get(i)[0] + ","
					+ c.clusterVectorsMean.get(i)[1] + "] : ");
			ArrayList<Data> list = c.clusters.get(i);
			for (int j = 0; j < list.size(); j++) {
				Data d = list.get(j);
				System.out.print(d.label + " - ");
			}
			System.out.print("\n");
		}

		System.out.println("\n\nStep 2.");
		computeProbofC();
		c.computePrbVgivenC(true);

		System.out.println("\n\nStep 3.");
		c.computeProbCgivenV(true);
		
		testDataSet = readFiledata(testPath, false);
		sample(10, 10);

		System.out.println("\n\nStep 4.");
		int[] clusterArray = { 2, 5, 6, 8, 12, 15, 20, 50 };
		for(int i=0; i<clusterArray.length; i++)
			sample(clusterArray[i], 10);

	}

}
