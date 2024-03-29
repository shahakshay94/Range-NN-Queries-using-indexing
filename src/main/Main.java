package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import MultipleKeyIndex.RootIndex;
import MultipleKeyIndex.XIndex;
import MultipleKeyIndex.YIndex;

public class Main {
	private static String inputFilePath = "C:\\Users\\Quoc Minh Vu\\EclipseWorkspace\\COMP6521_LAB2\\src\\input\\LA2.txt";
	private static String outputPath = "C:\\Users\\Quoc Minh Vu\\EclipseWorkspace\\COMP6521_LAB2\\src\\output\\";
	private static int bucketSize = 1000;
//	private static double scanRadius = 10;
	private static double minXValue, maxXValue, pointCount;
	private static Scanner scanner;
	
	public static void main(String[] args) {
		String userChoice = "";
		RootIndex root = buildIndex();
		while (true) {
			scanner = new Scanner(System.in);
			System.out.println("---------------MENU---------------");
			System.out.println("1. Range Query");
			System.out.println("2. Nearest-neighbor Query");
			System.out.println("0. Exit");
			System.out.print("Your choice: ");
			userChoice = scanner.next();
			System.out.println("----------------------------------");
			switch (userChoice) {
				case "1":
					query1(root);
					break;
				case "2":
					query2(root);
					break;
				case "0":
					return;
				default:
					break;
			}
		}
	}
	
	private static void query1(RootIndex root) {
		if (root == null) {
			System.out.println("There is no Index");
			return;
		}
		
		double x1, x2, y1, y2, z1, z2;
		while (true) {
			System.out.println("----------------------------------");
			System.out.print("Enter x1: "); x1 = scanner.nextDouble();
			System.out.print("Enter x2: "); x2 = scanner.nextDouble();
			System.out.print("Enter y1: "); y1 = scanner.nextDouble();
			System.out.print("Enter y2: "); y2 = scanner.nextDouble();
			System.out.print("Enter z1: "); z1 = scanner.nextDouble();
			System.out.print("Enter z2: "); z2 = scanner.nextDouble();
			System.out.println("----------------------------------");
			if (x1 < x2 && y1 < y2 && z1 < z2)
				break;
			else
				System.out.println("Invalid range");
		}
		
		long startTime = System.nanoTime();
		List<Point> queryResult = doRangeQuery(root, x1, x2, y1, y2, z1, z2);
		System.out.println("Result size = " + queryResult.size());
		System.out.printf("Query time: %d(ms) %n", ((System.nanoTime() - startTime) / 1000000));
		
		decideWhatToDoWithResult(queryResult);
	}
	
	private static List<Point> doRangeQuery(RootIndex root, double x1, double x2, double y1, double y2, double z1, double z2) {
		List<Point> result = new ArrayList<>();
		List<XIndex> xIndexList = root.getIndexesOfRange(x1, x2);
		for (XIndex xIndex : xIndexList) {
			List<YIndex> yIndexList = xIndex.getIndexesOfRange(y1, y2);
			for (YIndex yIndex : yIndexList) {
				List<PointList> zIndex = yIndex.getIndexesOfRange(z1, z2);
				for (PointList pointList : zIndex) {
					for (Point point : pointList) {
						if (point.isInRange(x1, x2, y1, y2, z1, z2)) {
							result.add(point);
						}
					}
				}
			}
		}
		return result;
	}	
	
	private static void query2(RootIndex root) {
		if (root == null) {
			System.out.println("There is no Index");
			return;
		}
		
		System.out.println("----------------------------------");
		System.out.print("Enter x: "); double x = scanner.nextDouble();
		System.out.print("Enter y: "); double y = scanner.nextDouble();
		System.out.print("Enter z: "); double z = scanner.nextDouble();
		System.out.println("----------------------------------");
		
		long startTime = System.nanoTime();
		List<Point> queryResult = doNearestNeighborQuery(root, x, y, z);
		System.out.println("Result size = " + queryResult.size());
		System.out.printf("Query time: %d(ms) %n", ((System.nanoTime() - startTime) / 1000000));
		
		decideWhatToDoWithResult(queryResult);
	}
	
	private static List<Point> doNearestNeighborQuery(RootIndex root, double x, double y, double z) {
		Point center = new Point(x, y, z);
		
		double scanRadius = ((Math.abs(maxXValue) - Math.abs(minXValue)) / pointCount) * 100000;
		double x1 = x - scanRadius;
		double x2 = x + scanRadius;
		double y1 = y - scanRadius;
		double y2 = y + scanRadius;
		double z1 = z - scanRadius;
		double z2 = z + scanRadius;
		
		List<Point> result = null;
		while (true) {
			List<Point> pointsInRange = doRangeQuery(root, x1, x2, y1, y2, z1, z2);
			if (pointsInRange.isEmpty()) {
				x1 -= scanRadius;
				x2 += scanRadius;
				y1 -= scanRadius;
				y2 += scanRadius;
				z1 -= scanRadius;
				z2 += scanRadius;
				continue;
			}
			double smallestDistance = Double.MAX_VALUE;
			for (Point currPoint : pointsInRange) {
				double currDistance = calDistance(center, currPoint);
				if (currDistance < smallestDistance) {
					smallestDistance = currDistance;
					result = new ArrayList<>();
					result.add(currPoint);
				} else if (currDistance == smallestDistance) {
					result.add(currPoint);
				}
			}
			if (!result.isEmpty())
				break;
		}
		return result;
	}
	
	private static RootIndex buildIndex() {
		System.out.println("Building Index...");
		long startTime1 = System.nanoTime();
		PointList xPointList = readFile();
		Map<String, PointList> xBucket = hash(xPointList, "X");

		RootIndex rootIndex = new RootIndex();
		for (Entry<String, PointList> xEntry : xBucket.entrySet()) {
			String xBucketRange = xEntry.getKey();
			double xLowerBound = Double.valueOf(xBucketRange.split("-")[0]);
			double xUpperBound = Double.valueOf(xBucketRange.split("-")[1]);
			PointList yPointList = xEntry.getValue();
			Map<String, PointList> yBucket = hash(yPointList, "Y");
			
			XIndex xIndex = new XIndex();
			for (Entry<String, PointList> yEntry : yBucket.entrySet()) {
				String yBucketRange = yEntry.getKey();
				double yLowerBound = Double.valueOf(yBucketRange.split("-")[0]);
				double yUpperBound = Double.valueOf(yBucketRange.split("-")[1]);
				PointList zPointList = yEntry.getValue();
				Map<String, PointList> zBucket = hash(zPointList, "Z");
				
				YIndex yIndex = new YIndex();
				for (Entry<String, PointList> zEntry : zBucket.entrySet()) {
					String zBucketRange = zEntry.getKey();
					double zLowerBound = Double.valueOf(zBucketRange.split("-")[0]);
					double zUpperBound = Double.valueOf(zBucketRange.split("-")[1]);
					PointList pointList = zEntry.getValue();
					yIndex.addNewItem(zLowerBound, zUpperBound, pointList);
				}
				xIndex.addNewItem(yLowerBound, yUpperBound, yIndex);
			}
			rootIndex.addNewItem(xLowerBound, xUpperBound, xIndex);
		}
		System.out.printf("Build Index time: %d(ms) %n", ((System.nanoTime() - startTime1) / 1000000));
		return rootIndex;
	}
	
	private static Map<String, PointList> hash(PointList pointList, String dimension) {
		pointList.sort(pointList, 0, pointList.size() - 1, dimension);
		minXValue = pointList.get(0).getDimension("X");
		maxXValue = pointList.get(pointList.size() - 1).getDimension("X");
		pointCount = pointList.size();
		return pointList.divideIntoBuckets(bucketSize, dimension);
	}
	
	private static PointList readFile() {
		PointList result = new PointList();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(inputFilePath));
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> data = Arrays.asList(line.substring(1,  line.length() - 1).replaceAll(" ", "").split(","));
				double x = Double.valueOf(data.get(0));
				double y = Double.valueOf(data.get(1));
				double z = Double.valueOf(data.get(2));
				Point point = new Point(x, y, z);
				result.add(point);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	private static void dumpResultToFile(String filePath, List<Point> points) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(filePath));
			for (Point point : points) {
				bw.write(point.toString());
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static double calDistance(Point a, Point b) {
		return Math.sqrt(
				Math.pow(a.getDimension("x") - b.getDimension("x"), 2) +
				Math.pow(a.getDimension("y") - b.getDimension("y"), 2) +
				Math.pow(a.getDimension("z") - b.getDimension("z"), 2));
	}
	
	private static void decideWhatToDoWithResult(List<Point> queryResult) {
		while (true) {
			System.out.println("------WHAT-TO-DO-WITH-RESULT------");
			System.out.println("1. Export result to file");
			System.out.println("2. Print result to screen");
			System.out.println("0. Back");
			System.out.print("Your choice: ");
			String userChoice = scanner.next();
			System.out.println("----------------------------------");
			switch (userChoice) {
				case "1":
					dumpResultToFile(outputPath + "range_query_result.txt", queryResult);
					return;
				case "2":
					queryResult.forEach(point -> System.out.println(point));
					return;
				case "0":
					return;
				default:
					break;
			}
		}
	}
}
